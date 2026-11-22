package com.mutabei.workout.data.model

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

data class AiDeficiencySuggestion(
    val foodNameAr: String,
    val servingGrams: Int,
    val approxKcal: Int,
    val coversNutrients: List<String>,
    val note: String
)

data class AiDeficiencyFixResult(
    val summary: String,
    val suggestions: List<AiDeficiencySuggestion>
)

@Keep
@IgnoreExtraProperties
data class CoachSuggestedTargets(
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val reason: String = ""
) {
    fun isValid(): Boolean =
        kcal in 800..10000 &&
            proteinG in 30..600 &&
            carbsG in 0..1500 &&
            fatG in 15..400
}

@Keep
@IgnoreExtraProperties
data class CoachSuggestedFood(
    val name: String = "",
    val grams: Int = 100,
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0
)

data class AiWeeklyCoachReport(
    val overallScore: Int = 0,
    val headline: String = "",
    val wins: List<String> = emptyList(),
    val attentionNeeded: List<String> = emptyList(),
    val nextWeekActions: List<String> = emptyList(),
    val plateaus: List<String> = emptyList(),
    val deloadRecommended: Boolean = false,
    val deloadNote: String = "",
    val asymmetryNote: String = "",
    val previousPlanReview: String = "",
    val suggestedTargets: CoachSuggestedTargets? = null,
    val suggestedFoods: List<CoachSuggestedFood> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class CoachReportRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weekNumber: Int = 1,
    val date: String = todayIso(),
    val report: AiWeeklyCoachReport = AiWeeklyCoachReport(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "weekNumber" to weekNumber.coerceIn(1, 52),
            "date" to date.ifBlank { todayIso() }.take(16),
            "overallScore" to report.overallScore.coerceIn(0, 100),
            "headline" to report.headline.ifBlank { "ملخص أسبوعك التدريبي والغذائي" }.take(500),
            "wins" to report.wins.filter { it.isNotBlank() }.take(10).map { it.take(300) },
            "attentionNeeded" to report.attentionNeeded.filter { it.isNotBlank() }.take(10).map { it.take(300) },
            "nextWeekActions" to report.nextWeekActions.filter { it.isNotBlank() }.take(10).map { it.take(300) },
            "plateaus" to report.plateaus.filter { it.isNotBlank() }.take(15).map { it.take(300) },
            "deloadRecommended" to report.deloadRecommended,
            "deloadNote" to report.deloadNote.take(1000),
            "asymmetryNote" to report.asymmetryNote.take(1000),
            "previousPlanReview" to report.previousPlanReview.take(1000),
            "suggestedFoods" to report.suggestedFoods.filter { it.name.isNotBlank() }.take(10).map { f ->
                mapOf(
                    "name" to f.name.take(120),
                    "grams" to f.grams.coerceIn(1, 2000),
                    "kcal" to f.kcal.coerceIn(0, 5000),
                    "proteinG" to f.proteinG.coerceIn(0, 300),
                    "carbsG" to f.carbsG.coerceIn(0, 500),
                    "fatG" to f.fatG.coerceIn(0, 200)
                )
            },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val targets = report.suggestedTargets
        if (targets != null && targets.isValid()) {
            map["suggestedKcal"] = targets.kcal
            map["suggestedProteinG"] = targets.proteinG
            map["suggestedCarbsG"] = targets.carbsG
            map["suggestedFatG"] = targets.fatG
            map["suggestedReason"] = targets.reason.take(600)
        }
        return map
    }

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): CoachReportRecord? {
            if (!doc.exists()) return null
            val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
            fun strList(key: String): List<String> =
                (data[key] as? List<*>)?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotBlank() } }
                    ?: emptyList()

            val foods = (data["suggestedFoods"] as? List<*>)?.mapNotNull { raw ->
                val m = raw as? Map<*, *> ?: return@mapNotNull null
                val name = (m["name"] as? String)?.trim().orEmpty()
                if (name.isBlank()) return@mapNotNull null
                CoachSuggestedFood(
                    name = name,
                    grams = (m["grams"] as? Number)?.toInt()?.coerceIn(1, 2000) ?: 100,
                    kcal = (m["kcal"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    proteinG = (m["proteinG"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    carbsG = (m["carbsG"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    fatG = (m["fatG"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
                )
            } ?: emptyList()

            val sKcal = (data["suggestedKcal"] as? Number)?.toInt()
            val sP = (data["suggestedProteinG"] as? Number)?.toInt()
            val sC = (data["suggestedCarbsG"] as? Number)?.toInt()
            val sF = (data["suggestedFatG"] as? Number)?.toInt()
            val suggestedTargets = if (sKcal != null && sP != null && sC != null && sF != null) {
                CoachSuggestedTargets(
                    kcal = sKcal,
                    proteinG = sP,
                    carbsG = sC,
                    fatG = sF,
                    reason = (data["suggestedReason"] as? String).orEmpty()
                ).takeIf { it.isValid() }
            } else null

            return CoachReportRecord(
                id = doc.id,
                userId = (data["userId"] as? String).orEmpty(),
                weekNumber = (data["weekNumber"] as? Number)?.toInt()?.coerceIn(1, 52) ?: 1,
                date = (data["date"] as? String).orEmpty().ifBlank { todayIso() },
                report = AiWeeklyCoachReport(
                    overallScore = (data["overallScore"] as? Number)?.toInt()?.coerceIn(0, 100) ?: 0,
                    headline = (data["headline"] as? String).orEmpty(),
                    wins = strList("wins"),
                    attentionNeeded = strList("attentionNeeded"),
                    nextWeekActions = strList("nextWeekActions"),
                    plateaus = strList("plateaus"),
                    deloadRecommended = (data["deloadRecommended"] as? Boolean) ?: false,
                    deloadNote = (data["deloadNote"] as? String).orEmpty(),
                    asymmetryNote = (data["asymmetryNote"] as? String).orEmpty(),
                    previousPlanReview = (data["previousPlanReview"] as? String).orEmpty(),
                    suggestedTargets = suggestedTargets,
                    suggestedFoods = foods
                ),
                createdAt = data["createdAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp
            )
        }
    }
}

data class PreWorkoutTargetExercise(
    val exerciseName: String,
    val lastBest: String,
    val targetWeightReps: String,
    val cue: String
)

data class PreWorkoutGamePlan(
    val dayName: String,
    val readinessScore: Int,
    val fuelStatus: String,
    val preWorkoutFuelTip: String,
    val hydrationSodiumTip: String,
    val warmupFocus: String,
    val prTargets: List<PreWorkoutTargetExercise>,
    val cautionNote: String
)

data class CoachChatReply(
    val replyAr: String,
    val suggestedTargets: CoachSuggestedTargets? = null,
    val suggestedFoods: List<CoachSuggestedFood> = emptyList()
)

data class CoachChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val imageBitmap: Bitmap? = null,
    val suggestedTargets: CoachSuggestedTargets? = null,
    val suggestedFoods: List<CoachSuggestedFood> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis()
)

data class AiMacroTetrisOption(
    val titleAr: String,
    val ingredientsAr: List<String>,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val leucineG: Double,
    val tip: String
)

data class AiMacroTetrisResult(
    val summary: String,
    val options: List<AiMacroTetrisOption>
)
