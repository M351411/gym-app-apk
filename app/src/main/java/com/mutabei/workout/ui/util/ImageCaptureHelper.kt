package com.mutabei.workout.ui.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mutabei.workout.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "ImageCaptureHelper"

class ImageCaptureController(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)

@Composable
fun rememberImageCaptureController(
    onBitmapCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit
): ImageCaptureController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    fun decodeAndEmit(uri: Uri) {
        scope.launch {
            val bitmap = decodeScaledBitmap(context, uri, maxDim = 1024)
            if (bitmap != null) {
                onBitmapCaptured(bitmap)
            } else {
                onError("تعذّر قراءة الصورة. جرّب صورة تانية أوضح.")
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null) {
            decodeAndEmit(uri)
        }
    }

    val launchCameraDirect = {
        try {
            val dir = File(context.cacheDir, "captures").apply { mkdirs() }
            val photoFile = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            pendingPhotoUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera", e)
            onError("تعذّر فتح الكاميرا على الجهاز. تقدر تختار صورة من المعرض.")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraDirect()
        } else {
            onError(context.getString(R.string.camera_permission_denied))
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            decodeAndEmit(uri)
        }
    }

    return remember(context) {
        ImageCaptureController(
            launchCamera = {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    launchCameraDirect()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            launchGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }
}

suspend fun decodeScaledBitmap(
    context: Context,
    uri: Uri,
    maxDim: Int = 1280
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                if (longest > maxDim && longest > 0) {
                    val scale = maxDim.toFloat() / longest.toFloat()
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } ?: return@withContext null

        val longest = max(rawBitmap.width, rawBitmap.height)
        if (longest > maxDim && longest > 0) {
            val scale = maxDim.toFloat() / longest.toFloat()
            val w = (rawBitmap.width * scale).roundToInt().coerceAtLeast(1)
            val h = (rawBitmap.height * scale).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(rawBitmap, w, h, true)
            if (scaled != rawBitmap) rawBitmap.recycle()
            scaled
        } else {
            rawBitmap
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to decode bitmap from $uri", e)
        null
    }
}
