package com.morim.netloglens.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.morim.netloglens.core.Masker
import com.morim.netloglens.share.AiShare
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnswerScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val entry = vm.current
    if (entry == null) {
        LaunchedEffect(Unit) { vm.screen = Screen.HOME }
        return
    }
    var restore by remember { mutableStateOf(true) }
    var showPrompt by remember { mutableStateOf(false) }

    Page(
        title = "解析結果",
        onBack = { vm.screen = Screen.HOME },
        actions = { TextButton(onClick = { vm.newSession() }) { Text("新しく撮る") } },
    ) {
        Text(
            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(entry.id)) + "　" + entry.purpose + (if (entry.mode.isNotEmpty()) "（${entry.mode}）" else ""),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))

        if (entry.answer.isBlank()) {
            Text(
                "${vm.target.label}の回答が出たら、回答をコピーしてこの画面に戻り、下のボタンを押してください。" +
                    "伏せ字（<IP_1> など）を元の値に戻して表示し、履歴に保存します。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val text = AiShare.paste(context)
                if (text.isBlank()) Toast.makeText(context, "クリップボードが空です", Toast.LENGTH_SHORT).show()
                else vm.saveAnswer(text)
            }) { Text(if (entry.answer.isBlank()) "回答を貼り付ける" else "回答を貼り直す") }
            OutlinedButton(onClick = { AiShare.send(context, entry.prompt, vm.target) }) { Text("もう一度送る") }
        }

        if (entry.answer.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("伏せ字を元の値に戻して表示", modifier = Modifier.weight(1f))
                Switch(checked = restore, onCheckedChange = { restore = it })
            }
            val shown = if (restore) Masker.unmask(entry.answer, entry.mapping) else entry.answer
            Card(Modifier.fillMaxWidth()) {
                SelectionContainer { Text(shown, modifier = Modifier.padding(12.dp)) }
            }
            TextButton(onClick = {
                AiShare.copy(context, shown)
                Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
            }) { Text("表示中の回答をコピー") }
        }

        if (entry.mapping.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("伏せ字の対応表（スマホ内のみ）", style = MaterialTheme.typography.titleSmall)
            Card(Modifier.fillMaxWidth()) {
                Text(
                    entry.mapping.entries.joinToString("\n") { "${it.key} = ${it.value}" },
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { showPrompt = !showPrompt }) { Text(if (showPrompt) "送った内容を閉じる" else "送った内容を見る") }
        if (showPrompt) {
            Card(Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Text(entry.prompt, modifier = Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
