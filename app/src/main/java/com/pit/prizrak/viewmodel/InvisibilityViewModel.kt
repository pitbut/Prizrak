package com.pit.prizrak.viewmodel

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.pit.prizrak.camera.CameraController
import com.pit.prizrak.camera.FrameProcessor
import com.pit.prizrak.camera.PrizrakSurfaceView
import com.pit.prizrak.model.DetectedPose
import com.pit.prizrak.model.EffectState
import com.pit.prizrak.model.PersonTarget
import com.pit.prizrak.model.ProcessingMode
import com.pit.prizrak.model.UiState
import com.pit.prizrak.tracking.PersonMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the camera/frame-processing pipeline and exposes UI state. [FrameProcessor]
 * callbacks arrive on the background analysis thread; StateFlow.value writes are safe
 * from any thread, so we update it directly instead of hopping to Main.
 */
class InvisibilityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState(statusText = IDLE_STATUS))
    val uiState: StateFlow<UiState> = _uiState

    private var frameProcessor: FrameProcessor? = null
    private var cameraController: CameraController? = null

    @Volatile
    private var latestPoses: List<DetectedPose> = emptyList()
    private var framesSinceSeen = 0

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun attachSurface(context: Context, lifecycleOwner: LifecycleOwner, surfaceView: PrizrakSurfaceView) {
        if (frameProcessor != null) return

        val processor = FrameProcessor(context, surfaceView, object : FrameProcessor.Callbacks {
            override fun onFrame(poses: List<DetectedPose>) {
                latestPoses = poses
            }

            override fun onTargetUpdated(target: PersonTarget, wasMatchedThisFrame: Boolean) {
                framesSinceSeen = if (wasMatchedThisFrame) 0 else framesSinceSeen + 1
                val lost = framesSinceSeen > PersonMatcher.MAX_FRAMES_LOST
                _uiState.update {
                    it.copy(
                        targetBoxNorm = target.bboxNorm(),
                        targetLost = lost,
                        statusText = if (lost) LOST_STATUS else RUNNING_STATUS
                    )
                }
            }
        })
        frameProcessor = processor

        val controller = CameraController(context, lifecycleOwner, processor)
        cameraController = controller
        controller.start()
    }

    fun detachSurface() {
        cameraController?.stop()
        frameProcessor?.close()
        cameraController = null
        frameProcessor = null
    }

    fun onTap(xNorm: Float, yNorm: Float) {
        val pose = PersonMatcher.findPoseAt(latestPoses, xNorm, yNorm)
        if (pose == null) {
            _uiState.update { it.copy(statusText = NOT_FOUND_STATUS) }
            return
        }
        val target = PersonMatcher.createTarget(pose, System.currentTimeMillis())
        framesSinceSeen = 0
        frameProcessor?.setTarget(target)
        _uiState.update {
            it.copy(
                effectState = EffectState.TARGET_SELECTED,
                targetBoxNorm = target.bboxNorm(),
                targetLost = false,
                statusText = SELECTED_STATUS
            )
        }
    }

    fun onStart() {
        val processor = frameProcessor ?: return
        if (processor.target == null) return
        processor.effectRunning = true
        _uiState.update { it.copy(effectState = EffectState.RUNNING, statusText = RUNNING_STATUS) }
    }

    fun onStop() {
        frameProcessor?.effectRunning = false
        _uiState.update { it.copy(effectState = EffectState.TARGET_SELECTED, statusText = SELECTED_STATUS) }
    }

    fun onReset() {
        frameProcessor?.effectRunning = false
        frameProcessor?.setTarget(null)
        framesSinceSeen = 0
        _uiState.update {
            it.copy(
                effectState = EffectState.IDLE,
                targetBoxNorm = null,
                targetLost = false,
                statusText = IDLE_STATUS
            )
        }
    }

    fun onModeSelected(mode: ProcessingMode) {
        frameProcessor?.processingMode = mode
        _uiState.update { it.copy(processingMode = mode) }
    }

    override fun onCleared() {
        detachSurface()
    }

    companion object {
        private const val IDLE_STATUS = "Наведите камеру на человека и коснитесь его на экране"
        private const val SELECTED_STATUS = "Цель выбрана — нажмите «Старт», чтобы скрыть её"
        private const val RUNNING_STATUS = "Эффект активен"
        private const val LOST_STATUS = "Цель потеряна из виду — верните её в кадр"
        private const val NOT_FOUND_STATUS = "Человек не найден в этой точке — коснитесь точнее"
    }
}
