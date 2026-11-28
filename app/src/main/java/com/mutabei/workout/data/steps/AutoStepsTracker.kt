package com.mutabei.workout.data.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.mutabei.workout.data.model.todayIso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "AutoStepsTracker"
private const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
private const val SENSOR_PREFS_NAME = "mutabei_auto_steps_sensor_v1"

enum class AutoStepsProvider {
    HEALTH_CONNECT,
    DEVICE_SENSOR,
    NONE
}

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE
}

data class AutoStepsStatus(
    val activeProvider: AutoStepsProvider = AutoStepsProvider.NONE,
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.UNAVAILABLE,
    val hasHealthConnectPermission: Boolean = false,
    val hasHardwareStepSensor: Boolean = false,
    val hasActivityRecognitionPermission: Boolean = false,
    val isSensorListening: Boolean = false,
    val isSyncing: Boolean = false,
    val liveTodaySteps: Int? = null,
    val lastSyncedLabel: String? = null,
    val statusNote: String? = null
) {
    val isAutoTrackingReady: Boolean
        get() = hasHealthConnectPermission || (hasHardwareStepSensor && hasActivityRecognitionPermission)

    val canRequestHealthConnect: Boolean
        get() = healthConnectAvailability == HealthConnectAvailability.AVAILABLE && !hasHealthConnectPermission

    val canRequestSensorPermission: Boolean
        get() = hasHardwareStepSensor && !hasActivityRecognitionPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}

/**
 * Automatic Android step counter combining:
 * 1. Health Connect (`StepsRecord.COUNT_TOTAL` aggregate per calendar day `YYYY-MM-DD`) — primary source
 *    supporting phone + smartwatch deduplication and 7-day history sync.
 * 2. Device Hardware Step Counter (`Sensor.TYPE_STEP_COUNTER` + `ACTIVITY_RECOGNITION`) — live on-device
 *    step updates with midnight calendar rollover & mid-day reboot protection.
 */
class AutoStepsTracker(
    private val appContext: Context
) {
    companion object {
        val HEALTH_CONNECT_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class)
        )
    }

    private val sensorManager: SensorManager? by lazy {
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    private val stepCounterSensor: Sensor? by lazy {
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }
    private val sensorPrefs by lazy {
        appContext.getSharedPreferences(SENSOR_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _status = MutableStateFlow(
        AutoStepsStatus(
            hasHardwareStepSensor = stepCounterSensor != null,
            hasActivityRecognitionPermission = hasActivityRecognitionPermission()
        )
    )
    val status: StateFlow<AutoStepsStatus> = _status.asStateFlow()

    private var sensorListenerRegistered = false
    private var onLiveTodayStepsCallback: ((dateIso: String, steps: Int) -> Unit)? = null

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
            val rawTotal = event.values.firstOrNull()?.toLong() ?: return
            if (rawTotal < 0L) return

            val today = todayIso()
            val todaySteps = computeDailySensorSteps(today, rawTotal)
            val timeNow = currentClockLabel()

            _status.update { cur ->
                val mergedToday = if (cur.hasHealthConnectPermission && cur.liveTodaySteps != null) {
                    maxOf(cur.liveTodaySteps, todaySteps)
                } else {
                    todaySteps
                }
                cur.copy(
                    activeProvider = if (cur.hasHealthConnectPermission) {
                        AutoStepsProvider.HEALTH_CONNECT
                    } else {
                        AutoStepsProvider.DEVICE_SENSOR
                    },
                    isSensorListening = true,
                    liveTodaySteps = mergedToday,
                    lastSyncedLabel = timeNow
                )
            }
            onLiveTodayStepsCallback?.invoke(today, todaySteps)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun setOnLiveTodayStepsListener(listener: (dateIso: String, steps: Int) -> Unit) {
        onLiveTodayStepsCallback = listener
    }

    fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkHealthConnectAvailability(): HealthConnectAvailability {
        return try {
            when (HealthConnectClient.getSdkStatus(appContext, HEALTH_CONNECT_PROVIDER_PACKAGE)) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
                else -> HealthConnectAvailability.UNAVAILABLE
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Health Connect availability check failed", t)
            HealthConnectAvailability.UNAVAILABLE
        }
    }

    private fun getHealthConnectClientOrNull(): HealthConnectClient? {
        return try {
            if (checkHealthConnectAvailability() != HealthConnectAvailability.AVAILABLE) return null
            HealthConnectClient.getOrCreate(appContext)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to obtain HealthConnectClient", t)
            null
        }
    }

    /**
     * Refreshes availability & permission states, starts live hardware step sensor if permitted,
     * and returns any synced daily step counts (`dateIso -> steps`) from Health Connect or device sensor.
     */
    suspend fun refreshAndSync(daysBack: Int = 7): Map<String, Int> = withContext(Dispatchers.IO) {
        val hcAvailability = checkHealthConnectAvailability()
        val hasSensor = stepCounterSensor != null
        val hasActivityPerm = hasActivityRecognitionPermission()

        _status.update {
            it.copy(
                healthConnectAvailability = hcAvailability,
                hasHardwareStepSensor = hasSensor,
                hasActivityRecognitionPermission = hasActivityPerm,
                isSyncing = true
            )
        }

        val hcClient = if (hcAvailability == HealthConnectAvailability.AVAILABLE) {
            getHealthConnectClientOrNull()
        } else null

        val hasHcPermission = if (hcClient != null) {
            try {
                val granted = hcClient.permissionController.getGrantedPermissions()
                granted.containsAll(HEALTH_CONNECT_PERMISSIONS)
            } catch (t: Throwable) {
                Log.w(TAG, "Error checking Health Connect permissions", t)
                false
            }
        } else false

        val syncedDays = linkedMapOf<String, Int>()
        var note: String? = null

        // 1. Primary: Health Connect aggregated steps per local calendar day
        if (hcClient != null && hasHcPermission) {
            try {
                val zone = ZoneId.systemDefault()
                val todayDate = LocalDate.now(zone)
                val nowInstant = Instant.now()
                for (offset in (daysBack - 1) downTo 0) {
                    val date = todayDate.minusDays(offset.toLong())
                    val startInstant = date.atStartOfDay(zone).toInstant()
                    val endInstant = if (offset == 0) {
                        nowInstant.coerceAtLeast(startInstant.plusSeconds(1))
                    } else {
                        date.plusDays(1).atStartOfDay(zone).toInstant()
                    }
                    val aggregate = hcClient.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                        )
                    )
                    val total = (aggregate[StepsRecord.COUNT_TOTAL] ?: 0L)
                        .toInt()
                        .coerceIn(0, 150_000)
                    if (total > 0 || offset == 0) {
                        syncedDays[date.toString()] = total
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Health Connect step sync error", t)
                note = "تعذّر تحديث الخطوات من Health Connect حالياً."
            }
        }

        // 2. Secondary / Real-Time Hardware Step Sensor
        if (hasSensor && hasActivityPerm) {
            startLiveSensorInternal()
            val today = todayIso()
            val cachedSensorSteps = readCachedTodaySensorSteps(today)
            if (cachedSensorSteps != null) {
                val existingToday = syncedDays[today] ?: 0
                syncedDays[today] = maxOf(existingToday, cachedSensorSteps)
            }
        } else {
            stopLiveSensor()
        }

        val todayKey = todayIso()
        val liveToday = syncedDays[todayKey]
        val provider = when {
            hasHcPermission -> AutoStepsProvider.HEALTH_CONNECT
            hasSensor && hasActivityPerm -> AutoStepsProvider.DEVICE_SENSOR
            else -> AutoStepsProvider.NONE
        }

        _status.update {
            it.copy(
                activeProvider = provider,
                healthConnectAvailability = hcAvailability,
                hasHealthConnectPermission = hasHcPermission,
                hasHardwareStepSensor = hasSensor,
                hasActivityRecognitionPermission = hasActivityPerm,
                isSensorListening = sensorListenerRegistered,
                isSyncing = false,
                liveTodaySteps = liveToday ?: it.liveTodaySteps,
                lastSyncedLabel = if (provider != AutoStepsProvider.NONE) currentClockLabel() else it.lastSyncedLabel,
                statusNote = note
            )
        }

        syncedDays
    }

    fun startLiveSensorIfPermitted() {
        val hasSensor = stepCounterSensor != null
        val hasPerm = hasActivityRecognitionPermission()
        if (hasSensor && hasPerm) {
            startLiveSensorInternal()
        } else {
            stopLiveSensor()
        }
        _status.update {
            it.copy(
                hasHardwareStepSensor = hasSensor,
                hasActivityRecognitionPermission = hasPerm,
                isSensorListening = sensorListenerRegistered
            )
        }
    }

    private fun startLiveSensorInternal() {
        if (sensorListenerRegistered) return
        val sm = sensorManager ?: return
        val sensor = stepCounterSensor ?: return
        val registered = try {
            sm.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register step counter sensor", t)
            false
        }
        sensorListenerRegistered = registered
    }

    fun stopLiveSensor() {
        if (!sensorListenerRegistered) return
        try {
            sensorManager?.unregisterListener(sensorEventListener)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to unregister step counter sensor", t)
        } finally {
            sensorListenerRegistered = false
            _status.update { it.copy(isSensorListening = false) }
        }
    }

    /**
     * Converts cumulative hardware `TYPE_STEP_COUNTER` (steps since device boot) into a clean
     * daily total scoped to `todayIso` (`YYYY-MM-DD`), handling both midnight rollover and
     * mid-day device reboots without losing today's progress.
     */
    @Synchronized
    private fun computeDailySensorSteps(today: String, sensorTotal: Long): Int {
        val anchorDate = sensorPrefs.getString("anchor_date", null)
        var anchorTotal = sensorPrefs.getLong("anchor_sensor_total", -1L)
        var accumulated = sensorPrefs.getLong("accumulated_today", 0L)
        val lastTotal = sensorPrefs.getLong("last_sensor_total", -1L)

        if (anchorDate != today || anchorTotal < 0L) {
            // New local calendar day -> reset anchor cleanly
            anchorTotal = sensorTotal
            accumulated = 0L
        } else if (lastTotal >= 0L && sensorTotal < lastTotal) {
            // Device rebooted mid-day -> bank pre-reboot delta & re-anchor
            val preRebootDelta = (lastTotal - anchorTotal).coerceAtLeast(0L)
            accumulated += preRebootDelta
            anchorTotal = sensorTotal
        }

        val currentDelta = (sensorTotal - anchorTotal).coerceAtLeast(0L)
        val totalToday = (accumulated + currentDelta).toInt().coerceIn(0, 150_000)

        sensorPrefs.edit()
            .putString("anchor_date", today)
            .putLong("anchor_sensor_total", anchorTotal)
            .putLong("accumulated_today", accumulated)
            .putLong("last_sensor_total", sensorTotal)
            .putInt("cached_today_steps", totalToday)
            .apply()

        return totalToday
    }

    private fun readCachedTodaySensorSteps(today: String): Int? {
        val anchorDate = sensorPrefs.getString("anchor_date", null)
        if (anchorDate != today) return null
        val cached = sensorPrefs.getInt("cached_today_steps", -1)
        return cached.takeIf { it >= 0 }
    }

    private fun currentClockLabel(): String =
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
}
