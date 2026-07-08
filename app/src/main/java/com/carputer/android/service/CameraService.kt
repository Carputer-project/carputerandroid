package com.carputer.android.service

import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraService {

    companion object {
        private const val TAG = "CameraService"
    }

    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
    private var cameraHandler: Handler? = null
    private var job: Job? = null

    private val _streaming = MutableStateFlow(false)
    val streaming: StateFlow<Boolean> = _streaming.asStateFlow()

    private val _device = MutableStateFlow("")
    val device: StateFlow<String> = _device.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<String>>(emptyList())
    val availableDevices: StateFlow<List<String>> = _availableDevices.asStateFlow()

    private var onFrameCallback: ((ByteArray, Int, Int) -> Unit)? = null
    private var previewSurface: Surface? = null
    private var previewSize: Size = Size(640, 480)

    fun setOnFrameCallback(callback: (ByteArray, Int, Int) -> Unit) {
        onFrameCallback = callback
    }

    fun setPreviewSurface(surface: Surface?, size: Size = Size(640, 480)) {
        previewSurface = surface
        previewSize = size
    }

    fun initialize(camManager: CameraManager) {
        cameraManager = camManager
        scanDevices()
    }

    fun scanDevices() {
        try {
            val ids = cameraManager?.cameraIdList?.toList() ?: emptyList()
            _availableDevices.value = ids
            Log.d(TAG, "Available cameras: $ids")
        } catch (e: Exception) {
            Log.e(TAG, "Camera scan error", e)
        }
    }

    fun startStream(deviceId: String? = null) {
        val id = deviceId ?: _device.value.ifEmpty {
            _availableDevices.value.firstOrNull() ?: return
        }
        _device.value = id
        job?.cancel()
        job = CoroutineScope(backgroundDispatcher).launch {
            try {
                cameraManager?.openCamera(id, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        createCaptureSession(camera)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        _streaming.value = false
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e(TAG, "Camera error: $error")
                        camera.close()
                        _streaming.value = false
                    }
                }, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open camera", e)
                _streaming.value = false
            }
        }
    }

    private fun createCaptureSession(camera: CameraDevice) {
        try {
            val targets = mutableListOf<Surface>()

            if (previewSurface != null && previewSurface?.isValid() == true) {
                targets.add(previewSurface!!)
            }

            // Always create ImageReader for frame callbacks
            val characteristics = cameraManager?.getCameraCharacteristics(camera.id)
            val map = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val imgSize = map?.getOutputSizes(android.graphics.ImageFormat.YUV_420_888)?.firstOrNull()
                ?: Size(640, 480)

            imageReader = ImageReader.newInstance(
                imgSize.width, imgSize.height,
                android.graphics.ImageFormat.YUV_420_888, 2
            )

            val handlerThread = HandlerThread("CameraReader")
            handlerThread.start()
            cameraHandler = Handler(handlerThread.looper)
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    if (planes.size >= 3) {
                        val buffer = planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        onFrameCallback?.invoke(bytes, image.width, image.height)
                    }
                    image.close()
                }
            }, cameraHandler)

            targets.add(imageReader!!.surface)

            camera.createCaptureSession(
                targets,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraSession = session
                        val request = camera.createCaptureRequest(
                            CameraDevice.TEMPLATE_PREVIEW
                        ).apply {
                            targets.forEach { addTarget(it) }
                            set(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON
                            )
                        }
                        session.setRepeatingRequest(request.build(), null, null)
                        _streaming.value = true
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session config failed")
                        _streaming.value = false
                    }
                },
                null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Create capture session error", e)
            _streaming.value = false
        }
    }

    fun stopStream() {
        job?.cancel()
        try {
            cameraSession?.close()
            cameraDevice?.close()
            imageReader?.close()
        } catch (_: Exception) { }
        cameraSession = null
        cameraDevice = null
        imageReader = null
        _streaming.value = false
    }

    fun release() {
        stopStream()
    }
}
