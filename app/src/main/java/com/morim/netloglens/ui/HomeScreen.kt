package com.morim.netloglens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morim.netloglens.AppViewModel
import com.morim.netloglens.Screen
import com.morim.netloglens.core.DeviceProfiles
import com.morim.netloglens.core.Purpose

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: AppViewModel) {
    Page(
        title = "NetLog Lens",
        actions = { TextButton(onClick = { vm.screen = Screen.HISTORY }) { Text("履歴 ${vm.history.size}") } },
    ) {
        Text("機種", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeviceProfiles.all.forEach { p ->
                FilterChip(selected = p.id == vm.profileId, onClick = { vm.selectProfile(p.id) }, label = { Text(p.displayName) })
            }
        }
        if (!vm.profile.masking) {
            Text(
                "汎用モードは伏せ字をしません。社外に出せないログには使わないでください。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(16.dp))

        Text("やりたいこと", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Purpose.entries.forEach { p ->
                FilterChip(selected = p == vm.purpose, onClick = { vm.purpose = p }, label = { Text(p.label) })
            }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = vm.memo,
            onValueChange = { vm.memo = it },
            label = { Text("状況メモ（任意）") },
            placeholder = { Text("例：朝から2階のフロアだけ通信が切れる。昨日ケーブル交換あり") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Spacer(Modifier.height(24.dp))

        Button(onClick = { vm.screen = Screen.CAMERA }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (vm.shotCount == 0) "画面を撮影する" else "撮影を続ける（${vm.shotCount}枚読み取り済み）")
        }
        if (vm.shotCount > 0) {
            TextButton(onClick = { vm.screen = Screen.REVIEW }, modifier = Modifier.fillMaxWidth()) { Text("読み取り結果を確認する") }
            TextButton(onClick = { vm.newSession() }, modifier = Modifier.fillMaxWidth()) { Text("最初からやり直す") }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "撮影した画像はスマホの中で文字にするだけで、保存も送信もしません。" +
                if (vm.profile.masking) "Claudeに送るのは、伏せ字にした後の文字だけです。" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
