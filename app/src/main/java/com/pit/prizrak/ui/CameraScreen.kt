package com.pit.prizrak.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pit.prizrak.camera.PrizrakSurfaceView
import com.pit.prizrak.model.EffectState
import com.pit.prizrak.model.ProcessingMode
import com.pit.prizrak.model.UiState
import com.pit.prizrak.util.FrameGeometry
import com.pit.prizrak.viewmodel.InvisibilityViewModel

@Composable
fun CameraScreen(viewModel: InvisibilityViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onCameraPermissionResult(granted) }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onCameraPermissionResult(granted)
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.hasCameraPermission) {
            var viewSize by remember { mutableStateOf(IntSize.Zero) }

            AndroidView(
                factory = { ctx ->
                    PrizrakSurfaceView(ctx).also { surfaceView ->
                        viewModel.attachSurface(ctx, lifecycleOwner, surfaceView)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewSize = it }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val norm = FrameGeometry.viewTapToNormalizedImage(
                                offset.x, offset.y, viewSize.width.toFloat(), viewSize.height.toFloat()
                            )
                            if (norm != null) viewModel.onTap(norm.x, norm.y)
                        }
                    }
            )

            uiState.targetBoxNorm?.let { box ->
                TargetOverlay(boxNorm = box, lost = uiState.targetLost, viewSize = viewSize)
            }

            StatusBanner(
                text = uiState.statusText,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            ControlsBar(
                uiState = uiState,
                onStart = viewModel::onStart,
                onStop = viewModel::onStop,
                onReset = viewModel::onReset,
                onModeSelected = viewModel::onModeSelected,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            PermissionRationale(onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.detachSurface() }
    }
}

@Composable
private fun TargetOverlay(boxNorm: android.graphics.RectF, lost: Boolean, viewSize: IntSize) {
    val color = if (lost) Color(0xFFFF5252) else Color(0xFF7FE7DC)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rect = FrameGeometry.normalizedImageRectToView(
            boxNorm, viewSize.width.toFloat(), viewSize.height.toFloat()
        ) ?: return@Canvas
        drawRect(
            color = color,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width(), rect.height()),
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun StatusBanner(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            .background(Color(0xAA10151B), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ControlsBar(
    uiState: UiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onModeSelected: (ProcessingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC10151B))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.effectState != EffectState.IDLE) {
            ModeSelector(selected = uiState.processingMode, onSelected = onModeSelected)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (uiState.effectState) {
                EffectState.IDLE -> {
                    Button(onClick = {}, enabled = false) { Text("Коснитесь человека") }
                }
                EffectState.TARGET_SELECTED -> {
                    Button(onClick = onStart) { Text("Старт") }
                    OutlinedButton(onClick = onReset) { Text("Сбросить цель") }
                }
                EffectState.RUNNING -> {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                    ) { Text("Стоп") }
                    OutlinedButton(onClick = onReset) { Text("Сбросить цель") }
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(selected: ProcessingMode, onSelected: (ProcessingMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeChip("Статично", selected == ProcessingMode.STATIC) { onSelected(ProcessingMode.STATIC) }
        ModeChip("Дрожание", selected == ProcessingMode.SHAKE_COMPENSATION) { onSelected(ProcessingMode.SHAKE_COMPENSATION) }
        ModeChip("Полная (бета)", selected == ProcessingMode.FULL_HOMOGRAPHY) { onSelected(ProcessingMode.FULL_HOMOGRAPHY) }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = if (selected) {
        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    } else {
        ButtonDefaults.outlinedButtonColors()
    }
    if (selected) {
        Button(onClick = onClick, colors = colors) { Text(label, style = MaterialTheme.typography.labelSmall) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun PermissionRationale(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Призраку нужен доступ к камере, чтобы находить и скрывать выбранного человека.",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGrant) { Text("Разрешить доступ к камере") }
    }
}
