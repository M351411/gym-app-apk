package com.mutabei.workout.data.model

import kotlin.math.roundToInt

data class MuscleEllipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float)

data class MuscleGroupMeta(
    val key: String,
    val arabicLabel: String,
    val ellipses: List<MuscleEllipse>
)

data class WarmupVideo(val title: String, val youtubeId: String)

enum class HintLevel { OK, HIGH, LOW }
data class RepHint(val level: HintLevel, val message: String)

object WorkoutDefaults {
    val DEFAULT_SPLIT: List<SplitDay> = listOf(
        SplitDay(
            name = "صدر وضهر",
            exercises = listOf(
                ExerciseSpec("Incline Chest Press Machine", "1–2", 2, 4, 6, "DB Incline Press", "Smith Incline Press", "لو الجيم فيه كيبل والجهاز مش كويس العب كيبل"),
                ExerciseSpec("DB Flat Press", "1–2", 2, 6, 10, "Smith Flat Press", "", "لو الجيم فيه كيبل والجهاز مش كويس العب كيبل"),
                ExerciseSpec("T-Bar Row", "1–3", 2, 6, 8, "Incline DB Row", "Cable Row", "ركز على مسار الكوع وضم الكتف، من غير سحب بالضهر العلوي"),
                ExerciseSpec("Wide Grip Lat Pulldown (Cable)", "1–2", 1, 6, 8, "Wide Grip Pulldown Machine", "", "ركز على مسار الكوع وضم الكتف"),
                ExerciseSpec("Cable Shrugs", "0", 2, 6, 10, "", "", "ثبّت جسمك ومترجّحش"),
                ExerciseSpec("Single-Arm Lat Row", "1–2", 1, 5, 7, "Single-Arm Cable Row", "Single-Arm DB Row", "ثبات الجسم طول الحركة"),
                ExerciseSpec("Single-Arm Rear Delt Fly", "0", 2, 6, 10, "Reverse Pec Deck", "", "فك لوحي الكتف قبل ما تبدأ")
            )
        ),
        SplitDay(
            name = "كتف ودراع",
            exercises = listOf(
                ExerciseSpec("Lateral Raise Machine", "1–2", 2, 6, 8, "DB Lateral Raise", "", "الحركة تطلع من الكتف لوحده"),
                ExerciseSpec("Shoulder Press Machine", "1–3", 1, 5, 8, "DB Shoulder Press", "", "ضهرك مفرود، الحوض لقدام"),
                ExerciseSpec("Single-Arm Tricep Pushdown", "1–2", 2, 5, 7, "Double Rope Pushdown", "", "فرد الكوع بس من غير تمريجح"),
                ExerciseSpec("Wrist Curls", "0", 2, 6, 10, "", "", "فك صوابعك تحت في الاستطالة واعمل انقباض كامل"),
                ExerciseSpec("Overhead Tricep Extension", "1–2", 2, 8, 12, "DB Skull Crusher", "", "لو كوعك وجعك العب Pushdown"),
                ExerciseSpec("Reverse Grip Curls", "0", 2, 6, 10, "DB Reverse Curl", "Barbell Reverse Curl", "غيّر القبضة على راحتك بنفس الأداء"),
                ExerciseSpec("DB Preacher Curl", "1–2", 2, 8, 12, "Face Away Curl", "DB Curl", "الحركة من الكوع من غير تمريجح")
            )
        ),
        SplitDay(
            name = "رجل أ",
            exercises = listOf(
                ExerciseSpec("Hip Extension", "1–2", 2, 8, 12, "Lying Leg Curl", "", "لو الجهازين مش موجودين العب SLDL"),
                ExerciseSpec("Hip Adduction", "1–2", 2, 6, 8, "Cable Hip Adduction", "", "ثبّت نفسك بالمقبض كويس"),
                ExerciseSpec("Seated Leg Curl", "1–2", 3, 8, 12, "Lying Leg Curl", "", "لو الجهازين مش موجودين العب SLDL"),
                ExerciseSpec("Leg Extension", "1–2", 3, 8, 12, "", "", "لو الجهاز مش موجود العب Banded Extension"),
                ExerciseSpec("Leg Press Calf Raise", "1–2", 3, 5, 7, "Smith Calf Raise", "", "الركبة مفرودة في أي نسخة"),
                ExerciseSpec("Lat Pulldown Crunch", "0", 2, 6, 10, "Cable Crunch", "", "الحركة تطلع من تني العمود الفقري مش الضهر كله")
            )
        ),
        SplitDay(
            name = "علوي",
            exercises = listOf(
                ExerciseSpec("Wide Grip Lat Pulldown (Cable)", "1–2", 1, 6, 8, "Wide Grip Pulldown Machine", "", "ركز على مسار الكوع"),
                ExerciseSpec("Machine Chest Press", "1–2", 2, 6, 8, "DB Flat Press", "Smith Flat Press", "لو الجيم فيه كيبل والجهاز مش كويس العب كيبل"),
                ExerciseSpec("Single-Arm Lat Row", "1–2", 1, 5, 7, "Single-Arm Cable Row", "Single-Arm DB Row", "ثبات الجسم طول الحركة"),
                ExerciseSpec("T-Bar Row", "1–2", 2, 5, 7, "Incline DB Row", "Cable Row", "افتح الكوع لبره على قد راحة كتفك"),
                ExerciseSpec("Incline Chest Press Machine", "1–2", 2, 6, 10, "DB Incline Press", "Smith Incline Press", "لو الجيم فيه كيبل والجهاز مش كويس العب كيبل"),
                ExerciseSpec("Single-Arm Tricep Pushdown", "1–2", 3, 5, 7, "Double Rope Pushdown", "", "فرد الكوع بس"),
                ExerciseSpec("Face Away Curl", "0", 3, 6, 10, "DB Preacher Curl", "DB Curl", "الحركة من الكوع من غير تمريجح")
            )
        ),
        SplitDay(
            name = "رجل ب",
            exercises = listOf(
                ExerciseSpec("Cable Lateral Raise", "1–2", 3, 6, 8, "", "", "الحركة تطلع من الكتف لوحده"),
                ExerciseSpec("Hack Squat", "1–3", 2, 5, 8, "Smith Squat", "Leg Press", "انزل لآخر مدى تقدر عليه"),
                ExerciseSpec("Leg Press Calf Raise", "1–2", 2, 5, 7, "Smith Calf Raise", "", "الركبة مفرودة في أي نسخة"),
                ExerciseSpec("Leg Extension", "1–2", 2, 8, 12, "", "", "لو الجهاز مش موجود العب Banded Extension"),
                ExerciseSpec("Hip Adduction", "1–2", 2, 6, 8, "Cable Hip Adduction", "", "ثبّت نفسك بالمقبض كويس"),
                ExerciseSpec("Single-Arm Rear Delt Fly", "0", 2, 6, 10, "Reverse Pec Deck", "", "فك لوحي الكتف قبل ما تبدأ")
            )
        )
    )

    val YOUTUBE_IDS: Map<String, String> = mapOf(
        "Incline Chest Press Machine" to "Z5SvP3R_Szg",
        "DB Incline Press" to "YaDodATLCDY",
        "Smith Incline Press" to "pXnB5KhUxsQ",
        "DB Flat Press" to "jmlQ7szxTqg",
        "Smith Flat Press" to "pbOihKgGiwQ",
        "T-Bar Row" to "d0n2wyqP_0U",
        "Incline DB Row" to "tDJ1930wyyA",
        "Cable Row" to "cYITDnHHoUs",
        "Wide Grip Lat Pulldown (Cable)" to "EZyiohrj6AQ",
        "Wide Grip Pulldown Machine" to "z9qcfPgQ4x0",
        "Cable Shrugs" to "S9OXkqZB_uc",
        "Single-Arm Lat Row" to "nXwinX8Mf0Y",
        "Single-Arm Cable Row" to "o5Np1jY_mPU",
        "Single-Arm DB Row" to "ibFuncziJLE",
        "Single-Arm Rear Delt Fly" to "-FNusuPYDkM",
        "Reverse Pec Deck" to "p7PBTzvja4w",
        "Lateral Raise Machine" to "ay-KscqBbd8",
        "DB Lateral Raise" to "AMK2ju7Q68c",
        "Shoulder Press Machine" to "DtJY_WLvH7Q",
        "DB Shoulder Press" to "iXgTZx1RKrY",
        "Single-Arm Tricep Pushdown" to "Uliit9B7Xpg",
        "Double Rope Pushdown" to "qMen5GVP5fo",
        "Wrist Curls" to "2jGS_GuSA90",
        "Overhead Tricep Extension" to "0sNnjugunwo",
        "DB Skull Crusher" to "mAAJpc4Gwiw",
        "Reverse Grip Curls" to "DhRUGDH2NRw",
        "DB Preacher Curl" to "QSlGOTEGY6U",
        "Face Away Curl" to "ydM2_VRtEes",
        "DB Curl" to "x1SHMISDxxU",
        "Hip Extension" to "hnLjsUQsNQA",
        "Lying Leg Curl" to "fsjmpp8SZIQ",
        "Seated Leg Curl" to "j1nMlG_e79c",
        "Hip Adduction" to "qfiyJLkQC3s",
        "Cable Hip Adduction" to "jsNc9yG2x_Q",
        "Leg Extension" to "feQt2tJe_ks",
        "Leg Press Calf Raise" to "wfFMMCjSDDg",
        "Smith Calf Raise" to "8YuFuc9gqYg",
        "Lat Pulldown Crunch" to "PoJb1yOd6Y4",
        "Cable Crunch" to "hC35L2es1bs",
        "Machine Chest Press" to "dnp30xZH2UI",
        "Cable Lateral Raise" to "WF9oZyEHnU4",
        "Hack Squat" to "UzXKN92fujQ",
        "Smith Squat" to "auTv65tBsu8",
        "Leg Press" to "G5SNv5uM84Q"
    )

    val GENERAL_WARMUPS: List<WarmupVideo> = listOf(
        WarmupVideo("Pallof Press", "ABKRrQajZIE"),
        WarmupVideo("Pallof Rotation", "nbbkibIUTSc"),
        WarmupVideo("External Rotation", "y0zmesZewlA"),
        WarmupVideo("Scapula Push Plus", "-kcan1ahMxk"),
        WarmupVideo("فلوج الأسئلة الشائعة", "LvJWJSnQat0")
    )

    val MUSCLE_GROUPS: LinkedHashMap<String, MuscleGroupMeta> = linkedMapOf(
        "chest" to MuscleGroupMeta("chest", "صدر", listOf(MuscleEllipse(20f, 16f, 6f, 3.5f))),
        "back" to MuscleGroupMeta("back", "ضهر", listOf(MuscleEllipse(20f, 19f, 6.5f, 8f))),
        "sh" to MuscleGroupMeta("sh", "كتف", listOf(MuscleEllipse(11f, 13f, 3.5f, 2.5f), MuscleEllipse(29f, 13f, 3.5f, 2.5f))),
        "arm" to MuscleGroupMeta("arm", "دراع", listOf(MuscleEllipse(8.5f, 21f, 2.5f, 6f), MuscleEllipse(31.5f, 21f, 2.5f, 6f))),
        "core" to MuscleGroupMeta("core", "بطن", listOf(MuscleEllipse(20f, 25f, 4.5f, 4f))),
        "hip" to MuscleGroupMeta("hip", "فخذ داخلي", listOf(MuscleEllipse(17.5f, 38f, 1.8f, 5f), MuscleEllipse(22.5f, 38f, 1.8f, 5f))),
        "quad" to MuscleGroupMeta("quad", "فخذ أمامي", listOf(MuscleEllipse(16f, 40f, 3f, 8f), MuscleEllipse(24f, 40f, 3f, 8f))),
        "ham" to MuscleGroupMeta("ham", "فخذ خلفي", listOf(MuscleEllipse(16f, 40f, 3f, 8f), MuscleEllipse(24f, 40f, 3f, 8f))),
        "calf" to MuscleGroupMeta("calf", "سمانة", listOf(MuscleEllipse(16f, 49f, 2.5f, 4f), MuscleEllipse(24f, 49f, 2.5f, 4f)))
    )

    fun detectMuscleGroup(name: String): String = when {
        Regex("Calf", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "calf"
        Regex("Leg Curl|Hip Extension", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "ham"
        Regex("Adduction", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "hip"
        Regex("Leg Extension|Squat|Leg Press", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "quad"
        Regex("Crunch", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "core"
        Regex("Shoulder|Lateral|Rear Delt|Pec Deck", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "sh"
        Regex("Curl|Tricep|Skull|Wrist|Pushdown", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "arm"
        Regex("Chest|Flat Press|Incline", RegexOption.IGNORE_CASE).containsMatchIn(name) -> "chest"
        else -> "back"
    }

    fun youtubeUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    fun epley1Rm(weight: Double, reps: Int): Double =
        if (weight > 0.0 && reps > 0) weight * (1.0 + if (reps > 1) reps / 30.0 else 0.0) else 0.0

    fun repHint(repsRaw: String, minReps: Int, maxReps: Int): RepHint? {
        val r = repsRaw.trim().toIntOrNull() ?: return null
        return when {
            r > maxReps -> RepHint(HintLevel.HIGH, "فوق الرينج. زوّد الوزن المرة الجاية.")
            r < minReps -> RepHint(HintLevel.LOW, "تحت الرينج. قلّل الوزن شوية.")
            else -> RepHint(HintLevel.OK, "جوه الرينج. ثبّت الوزن وحاول تزوّد عدة.")
        }
    }

    private fun roundTo2Point5(x: Double): String {
        val v = (x / 2.5).roundToInt() * 2.5
        return if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
    }

    fun warmUpAdvice(warmupSpec: String, baseWeight: Double): String {
        if (warmupSpec == "0") return "التمرين ده من غير تسخين."
        if (baseWeight <= 0.0) return "اكتب وزن الشغل في المجموعة الأولى الأول."
        fun line(pct: Double, reps: String) = "${roundTo2Point5(baseWeight * pct)} كجم × $reps"
        return if (warmupSpec == "1–3" || warmupSpec == "1-3") {
            "1) ${line(0.5, "3–6")} · 2) ${line(0.775, "3")} · 3) ${line(0.875, "1–3")} (اعمل اللي محتاجه)"
        } else {
            "لو تمرين خفيف: ${line(0.6, "3–6")}. لو متوسط: ${line(0.5, "3–6")} ثم ${line(0.775, "3")}"
        }
    }
}
