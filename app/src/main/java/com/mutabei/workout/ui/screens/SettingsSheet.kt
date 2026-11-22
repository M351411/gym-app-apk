package com.mutabei.workout.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import android.net.Uri
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.VerifiedUser
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseUser
import com.mutabei.workout.BuildConfig
import com.mutabei.workout.R
import com.mutabei.workout.data.model.UserConfig
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.localPhoneDebugToken
import com.mutabei.workout.resetAppCheckProviderForUserRetry
import com.mutabei.workout.ui.components.ExpandableSectionCard
import com.mutabei.workout.ui.util.signOutUser
import com.mutabei.workout.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    user: FirebaseUser,
    config: UserConfig,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var startDateText by remember(config.startDate) { mutableStateOf(config.startDate) }
    var backupText by remember { mutableStateOf("") }
    var splitJsonText by remember { mutableStateOf("") }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = viewModel.exportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                viewModel.showToast("تم حفظ ملف النسخة الاحتياطية ✅")
            } catch (_: Exception) {
                viewModel.showToast("تعذّر حفظ الملف.")
            }
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    backupText = text
                    viewModel.importBackupJson(text)
                }
            } catch (_: Exception) {
                viewModel.showToast("تعذّرت قراءة ملف النسخة الاحتياطية.")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close)
                    )
                }
            }

            // Account card
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.displayName?.takeIf { it.isNotBlank() } ?: "حسابك في Google",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = user.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            signOutUser(credentialManager, scope)
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.sign_out_button))
                    }
                }
            }

            // Gemini AI status info
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "كل ميزات الكاميرا والملء التلقائي (ورقة InBody، ملصقات المنتجات، صور الوجبات، وصف الأكل، وقراءة الباركود) شغالة تلقائيًا عبر Gemini بدون الحاجة لأي مفتاح API.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Personal-device App Check Debug Token card (only on sideloaded debug APKs, never in Release or Preview harness)
            val phoneDebugToken = if (BuildConfig.DEBUG) localPhoneDebugToken else null
            if (!phoneDebugToken.isNullOrBlank()) {
                var verifyBusy by remember { mutableStateOf(false) }
                var verifyStatus by remember { mutableStateOf<String?>(null) }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "تفعيل Gemini على موبايلك الشخصي (مرة واحدة)",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = "لأن التطبيق مثبّت يدويًا خارج Google Play، انسخ الكود ده وضيفه مرة واحدة في Firebase Console ← App Check ← Apps ← com.mutabei.workout ← Manage debug tokens:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = phoneDebugToken,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("App Check Debug Token", phoneDebugToken))
                                    viewModel.showToast("تم نسخ التوكن ✅")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_appcheck_debug_token_button")
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("نسخ التوكن")
                            }
                            OutlinedButton(
                                onClick = {
                                    val url = "https://console.firebase.google.com/project/gen-lang-client-0923914224/appcheck/apps"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_firebase_appcheck_console_button")
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("فتح Firebase")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    verifyBusy = true
                                    verifyStatus = "جاري اختبار التوكن مع Firebase..."
                                    try {
                                        resetAppCheckProviderForUserRetry()
                                        val token = FirebaseAppCheck.getInstance().getAppCheckToken(true).await()
                                        verifyStatus = if (token.token.length > 20) {
                                            "ممتاز! التوكن متفعل و Gemini جاهز للعمل الآن ✅"
                                        } else {
                                            "التوكن غير صالح بعد — تأكد من حفظه في Firebase Console."
                                        }
                                    } catch (e: Exception) {
                                        verifyStatus = "لسا غير مسجل في Firebase Console (${e.message?.take(80).orEmpty()})"
                                    } finally {
                                        verifyBusy = false
                                    }
                                }
                            },
                            enabled = !verifyBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("verify_appcheck_debug_token_button")
                        ) {
                            Text(if (verifyBusy) "جاري الفحص..." else "اختبار تفعيل Gemini الآن")
                        }
                        if (!verifyStatus.isNullOrBlank()) {
                            Text(
                                text = verifyStatus.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (verifyStatus?.contains("✅") == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }

            // Theme selector
            Text("المظهر (الثيم)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "تلقائي", "light" to "فاتح", "dark" to "داكن").forEach { (mode, label) ->
                    FilterChip(
                        selected = config.themeMode == mode,
                        onClick = { viewModel.updateConfig(config.copy(themeMode = mode)) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("theme_chip_$mode")
                    )
                }
            }

            // Program start date
            Text("بداية البرنامج (لعداد الـ12 أسبوع)", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startDateText,
                    onValueChange = { startDateText = it },
                    label = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_date_input")
                )
                Button(
                    onClick = {
                        if (startDateText.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                            viewModel.updateConfig(config.copy(startDate = startDateText))
                            viewModel.showToast("تم تحديث تاريخ البداية ✅")
                        } else {
                            viewModel.showToast("اكتب التاريخ بصيغة YYYY-MM-DD")
                        }
                    },
                    modifier = Modifier.testTag("save_start_date_button")
                ) {
                    Text("حفظ")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Backup & Restore
            ExpandableSectionCard(
                title = "النسخة الاحتياطية والاسترجاع (JSON)",
                testTag = "settings_backup_section"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "بياناتك محفوظة سحابيًا على حسابك، وتقدر كمان تصدّر نسخة JSON أو تسترجع نسخة من النسخة القديمة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val json = viewModel.exportBackupJson()
                                backupText = json
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Mutabei Backup", json))
                                viewModel.showToast("تم نسخ النسخة الاحتياطية ✅")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_backup_button")
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("نسخ")
                        }
                        OutlinedButton(
                            onClick = {
                                saveFileLauncher.launch("mutabei-backup-${todayIso()}.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_file_backup_button")
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("حفظ ملف")
                        }
                        OutlinedButton(
                            onClick = {
                                val json = viewModel.exportBackupJson()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - متابع التمرين")
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة النسخة الاحتياطية"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_backup_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("مشاركة")
                        }
                    }

                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (clipText.isBlank()) {
                                viewModel.showToast("الحافظة فاضية. انسخ نص النسخة الاحتياطية الأول.")
                            } else {
                                viewModel.importBackupJson(clipText)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paste_restore_clipboard_button")
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("لصق واسترجاع من الحافظة مباشرة")
                    }

                    OutlinedButton(
                        onClick = {
                            openFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_file_backup_button")
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("استيراد ملف JSON")
                    }

                    OutlinedTextField(
                        value = backupText,
                        onValueChange = { backupText = it },
                        label = { Text("أو الصق نص النسخة الاحتياطية هنا") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_json_input")
                    )

                    OutlinedButton(
                        onClick = {
                            if (backupText.isNotBlank() && viewModel.importBackupJson(backupText)) {
                                backupText = ""
                            }
                        },
                        enabled = backupText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_backup_button")
                    ) {
                        Text("استرجاع النص")
                    }
                }
            }

            // Custom Split Management
            ExpandableSectionCard(
                title = "تعديل جدول التمرين (Split)",
                testTag = "settings_split_section"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "الصق جدول JSON بصيغة [{\"n\":\"اسم اليوم\",\"x\":[[\"اسم التمرين\",\"1-2\",2,6,10,\"بديل1\",\"بديل2\",\"ملاحظة\"]]}]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = splitJsonText,
                        onValueChange = { splitJsonText = it },
                        label = { Text("JSON الجدول المخصص") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_split_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (viewModel.importCustomSplitJson(splitJsonText)) {
                                    splitJsonText = ""
                                }
                            },
                            enabled = splitJsonText.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("apply_split_button")
                        ) {
                            Text("تطبيق السبليت")
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetToDefaultSplit() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_split_button")
                        ) {
                            Text("رجّع البالغامدي")
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
