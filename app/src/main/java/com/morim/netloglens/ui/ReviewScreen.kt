package com.morim.netloglens.ui

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morim.netloglens.AppViewModel
import com.morim.netloglens.Screen
import com.morim.netloglens.core.MaskCategory
import com.morim.netloglens.core.MaskResult
import com.morim.netloglens.core.TapTarget
import com.morim.netloglens.core.Mode
import com.morim.netloglens.share.AiShare

private val LABELS = mapOf(
    "IP" to "IPv4", "IPV6" to "IPv6", "MAC" to "MAC", "HOST" to "ホスト名",
    "USER" to "ユーザー", "SN" to "シリアル", "SECRET" to "パスワード等", "WORD" to "指定した語",
)

@Composable
fun ReviewScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val result = remember(vm.ocrText, vm.enabled, vm.profileId, vm.mode, vm.manualWords, vm.alwaysWords, vm.keepValues) { vm.mask() }
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

        val tabs = if (vm.isProtected) listOf("読み取りを直す", "タップで伏せる", "送る内容") else listOf("読み取りを直す", "送る内容")
        val current = tabs[tab.coerceAtMost(tabs.lastIndex)]
        TabRow(selectedTabIndex = tabs.indexOf(current)) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = label == current, onClick = { tab = i }, text = { Text(label, fontSize = 13.sp) })
            }
        }
        Spacer(Modifier.height(8.dp))
        if (current == "タップで伏せる") {
            TapToMaskPane(vm, result)
        } else if (current == "読み取りを直す") {
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
                if (vm.isProtected) "これがそのまま${vm.target.label}に渡ります。伏せ字が漏れていたら「タップで伏せる」で伏せてください。"
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

/** 伏せ字にした文章を表示し、語のタップで伏せる／記号のタップで元に戻す */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TapToMaskPane(vm: AppViewModel, result: MaskResult) {
    var pending by remember { mutableStateOf<TapTarget?>(null) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val tokenStyle = SpanStyle(
        background = MaterialTheme.colorScheme.primaryContainer,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    val annotated = remember(result.text, tokenStyle) {
        buildAnnotatedString {
            append(result.text)
            TapTarget.TOKEN_RE.findAll(result.text).forEach { addStyle(tokenStyle, it.range.first, it.range.last + 1) }
        }
    }

    Text(
        "伏せたい語をタップしてください（ログ全体の同じ語が伏せ字になります）。色の付いた記号をタップすると元に戻せます。",
        style = MaterialTheme.typography.bodySmall,
    )
    Card(Modifier.fillMaxWidth()) {
        Text(
            annotated,
            modifier = Modifier
                .padding(12.dp)
                .pointerInput(result.text) {
                    detectTapGestures { pos ->
                        layout?.let { pending = TapTarget.at(result.text, it.getOffsetForPosition(pos)) }
                    }
                },
            onTextLayout = { layout = it },
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }

    if (vm.alwaysWords.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("いつも伏せる語（タップで外す）", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            vm.alwaysWords.sorted().forEach { w ->
                AssistChip(onClick = { vm.removeAlwaysWord(w) }, label = { Text("$w  ✕") })
            }
        }
    }

    when (val t = pending) {
        is TapTarget.Word -> AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("「${t.word}」を伏せ字にする") },
            text = { Text("ログ全体の同じ語を <WORD_n> にします。「いつも伏せる」にすると、次回からも自動で伏せます（スマホ内にだけ保存）。") },
            confirmButton = {
                Row {
                    TextButton(onClick = { vm.maskWord(t.word, always = false); pending = null }) { Text("今回だけ") }
                    TextButton(onClick = { vm.maskWord(t.word, always = true); pending = null }) { Text("いつも伏せる") }
                }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("やめる") } },
        )
        is TapTarget.Token -> {
            val original = result.mapping[t.token]
            if (!t.restorable || original == null) {
                AlertDialog(
                    onDismissRequest = { pending = null },
                    title = { Text(t.token) },
                    text = { Text("パスワード・鍵・証明書は、安全のため元の値を保存していないので戻せません。") },
                    confirmButton = { TextButton(onClick = { pending = null }) { Text("閉じる") } },
                )
            } else {
                AlertDialog(
                    onDismissRequest = { pending = null },
                    title = { Text("「${t.token}」を元に戻す") },
                    text = { Text("元の値：$original\n今回は、この値を伏せずに送ります。") },
                    confirmButton = {
                        Row {
                            if (original in vm.alwaysWords) {
                                TextButton(onClick = {
                                    vm.removeAlwaysWord(original)
                                    vm.restoreValue(original)
                                    pending = null
                                }) { Text("いつも伏せる語から外す") }
                            }
                            TextButton(onClick = { vm.restoreValue(original); pending = null }) { Text("戻す") }
                        }
                    },
                    dismissButton = { TextButton(onClick = { pending = null }) { Text("やめる") } },
                )
            }
        }
        null -> {}
    }
}
