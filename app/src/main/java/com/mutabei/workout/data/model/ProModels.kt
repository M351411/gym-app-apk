package com.mutabei.workout.data.model

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

data class AiWeeklyCoachReport(
    val overallScore: Int,
    val headline: String,
    val wins: List<String>,
    val attentionNeeded: List<String>,
    val nextWeekActions: List<String>
)
