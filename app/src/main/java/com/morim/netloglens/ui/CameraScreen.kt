package com.morim.netloglens.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.morim.netloglens.AppViewModel
import com.morim.netloglens.Screen
import com.morim.netloglens.ocr.LogOcr

@Composable
fun CameraScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (!granted) {
        Page(title = "撮影", onBack = { vm.screen = Screen.HOME }) {
            Text("カメラの使用を許可してください。")
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("許可する") }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    var busy by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var message by remember {
        mutableStateOf("画面に正対して、文字がはっきり写るように撮ってください。長いログは何枚かに分けて撮れます。")
    }

    fun capture() {
        busy = true
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    LogOcr.recognize(
                        image,
                        onResult = { text ->
                            busy = false
                            if (text.isBlank()) {
                                message = "文字が読み取れませんでした。近づくか、明るさ・ピントを調整して撮り直してください。"
                            } else {
                                vm.appendOcr(text)
                                val n = text.lines().size
                                message = "${vm.shotCount}枚目：${n}行を読み取りました。続けて撮るか「確認へ」を押してください。"
                            }
                        },
                        onError = { e ->
                            busy = false
                            message = "読み取りに失敗しました：${e.message}"
                        },
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    busy = false
                    message = "撮影に失敗しました：${exception.message}"
                }
            },
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            message,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(12.dp),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (busy) CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = {
                    torch = !torch
                    controller.enableTorch(torch)
                }) { Text(if (torch) "ライト消" else "ライト") }
                Button(onClick = { capture() }, enabled = !busy) { Text("撮影して読み取る") }
                FilledTonalButton(onClick = { vm.screen = Screen.REVIEW }, enabled = vm.shotCount > 0 && !busy) {
                    Text("確認へ")
                }
            }
        }
    }
}
