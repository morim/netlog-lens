package com.morim.netloglens

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.morim.netloglens.ui.AnswerScreen
import com.morim.netloglens.ui.CameraScreen
import com.morim.netloglens.ui.HistoryScreen
import com.morim.netloglens.ui.HomeScreen
import com.morim.netloglens.ui.ReviewScreen

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                BackHandler(enabled = vm.screen != Screen.HOME) {
                    vm.screen = when (vm.screen) {
                        Screen.REVIEW -> Screen.CAMERA
                        else -> Screen.HOME
                    }
                }
                when (vm.screen) {
                    Screen.HOME -> HomeScreen(vm)
                    Screen.CAMERA -> CameraScreen(vm)
                    Screen.REVIEW -> ReviewScreen(vm)
                    Screen.ANSWER -> AnswerScreen(vm)
                    Screen.HISTORY -> HistoryScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
