package com.morim.netloglens.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.morim.netloglens.AppViewModel
import com.morim.netloglens.Screen
import com.morim.netloglens.core.DeviceProfiles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
    Page(title = "履歴", onBack = { vm.screen = Screen.HOME }) {
        if (vm.history.isEmpty()) Text("まだ履歴はありません。")
        vm.history.toList().forEach { e ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { vm.open(e) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${fmt.format(Date(e.id))}　${DeviceProfiles.byId(e.profileId).displayName}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(e.purpose, style = MaterialTheme.typography.titleSmall)
                        val summary = e.answer.lineSequence().map { it.trim() }
                            .firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
                            ?: if (e.answer.isBlank()) "（回答未保存）" else ""
                        Text(summary, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { vm.delete(e) }) { Text("削除") }
                }
            }
        }
    }
}
