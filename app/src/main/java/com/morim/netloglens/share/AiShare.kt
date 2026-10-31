package com.morim.netloglens.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.morim.netloglens.core.AiTarget

/** 選んだAIアプリ（無ければ共有先の選択画面）へテキストを渡す */
object AiShare {
    fun copy(context: Context, text: String) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("netlog-lens", text))
    }

    fun paste(context: Context): String {
        val cm = context.getSystemService(ClipboardManager::class.java)
        return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
    }

    /** AndroidManifest の <queries> に書いたアプリだけ判定できる */
    fun isInstalled(context: Context, target: AiTarget): Boolean = try {
        context.packageManager.getPackageInfo(target.packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** @return 選んだAIアプリへ直接送れたら true、選択画面を出したら false */
    fun send(context: Context, text: String, target: AiTarget): Boolean {
        // 共有で本文が入らない場合に備えて、必ずクリップボードにも入れておく
        copy(context, text)
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return try {
            context.startActivity(Intent(base).setPackage(target.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent.createChooser(base, "解析に使うアプリ").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            false
        }
    }
}
