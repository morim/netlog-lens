package com.morim.netloglens.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morim.netloglens.AppViewModel
import com.morim.netloglens.Screen
import com.morim.netloglens.core.MaskCategory
import com.morim.netloglens.core.Mode
import com.morim.netloglens.share.AiShare

private val LABELS = mapOf(
    "IP" to "IPv4", "IPV6" to "IPv6", "MAC" to "MAC", "HOST" to "ホスト名",
    "USER" to "ユーザー", "SN" to "シリアル", "SECRET" to "パスワード等",
)

@Composable
fun ReviewScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val result = remember(vm.ocrText, vm.enabled, vm.profileId) { vm.mask() }
    val prompt = remember(result, vm.purpose, vm.memo) { vm.buildPrompt(result) }
    var tab by remember { mutableIntStateOf(0) }

    Page(
        title = "送る前の確認",
        onBack = { vm.screen = Screen.CAMERA },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = {
                        AiShare.copy(context, prompt)
                        Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.weight(1f)) { Text("コピー") }
                    Button(
                        onClick = {
                            vm.startAnalysis(prompt, result)
                            val direct = AiShare.send(context, prompt, vm.target)
                            if (!direct) Toast.makeText(context, "${vm.target.label}アプリが見つからないため共有先を選んでください", Toast.LENGTH_LONG).show()
                            vm.screen = Screen.ANSWER
                        },
                        enabled = vm.ocrText.isNotBlank(),
                        modifier = Modifier.weight(2f),
                    ) { Text("${vm.target.label}で解析") }
                }
            }
        },
    ) {
        if (vm.isProtected) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("保護モード：伏せ字にする項目", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.selectMode(Mode.GENERAL) }) { Text("汎用モードへ") }
            }
            MaskCategory.entries.forEach { cat ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(cat.label, modifier = Modifier.weight(1f))
                    Switch(checked = cat in vm.enabled, onCheckedChange = { vm.toggle(cat, it) })
                }
            }
            val counts = result.counts
            Text(
                if (counts.isEmpty()) "伏せ字にした箇所はありません"
                else "伏せ字：" + counts.entries.joinToString("、") { "${LABELS[it.key] ?: it.key} ${it.value}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "汎用モード：伏せ字をしません。読み取った文字がそのまま${vm.target.label}に送られます。" +
                            "IPアドレス・パスワードなどが写っていないか、送る前に確認してください。",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = { vm.selectMode(Mode.PROTECTED) }) { Text("保護モードに切り替える") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("読み取り結果を直す") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("送る内容") })
        }
        Spacer(Modifier.height(8.dp))
        if (tab == 0) {
            Text(
                if (vm.isProtected) "OCRの読み間違いがあればここで直してください。伏せ字はこの文章から自動で作られます。"
                else "OCRの読み間違いがあればここで直してください。",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = vm.ocrText,
                onValueChange = { vm.ocrText = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                minLines = 12,
            )
            OutlinedButton(onClick = { vm.screen = Screen.CAMERA }, modifier = Modifier.fillMaxWidth()) {
                Text("続きを撮影して追加する")
            }
        } else {
            Text(
                if (vm.isProtected) "これがそのまま${vm.target.label}に渡ります。伏せ字が漏れていないか確認してください。"
                else "これがそのまま${vm.target.label}に渡ります。",
                style = MaterialTheme.typography.bodySmall,
            )
            Card(Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Text(
                        prompt,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
