//package com.app.scene.mediapipe
//
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.Path
//import android.os.Bundle
//import android.os.SystemClock
//import android.view.SurfaceHolder
//import android.view.SurfaceView
//import android.widget.Toast
//import androidx.activity.addCallback
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.content.ContextCompat
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import com.app.scene.R
//import com.app.scene.databinding.ActivityMediapipeBinding
//import com.google.android.filament.Engine
//import com.google.android.filament.utils.ModelViewer
//import com.google.android.filament.utils.Utils
//import com.google.mediapipe.framework.image.BitmapImageBuilder
//import com.google.mediapipe.framework.image.MPImage
//import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
//import com.google.mediapipe.tasks.core.BaseOptions
//import com.google.mediapipe.tasks.vision.core.RunningMode
//import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
//import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
//import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
//import java.nio.ByteBuffer
//import java.util.concurrent.Executors
//
//class MediaPipeActivity : AppCompatActivity() {
//    private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
//    private val REQUEST_CODE_PERMISSIONS = 10
//    private lateinit var modelViewer: ModelViewer
//    private lateinit var faceLandmarker: FaceLandmarker
//    private lateinit var surfaceView: SurfaceView
//    private lateinit var lipstickOverlay: SurfaceView
//    private val executor = Executors.newSingleThreadExecutor()
//    private lateinit var binding : ActivityMediapipeBinding
//
//    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
//        ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun requestPermissions() {
//        if (!allPermissionsGranted()) {
//            androidx.core.app.ActivityCompat.requestPermissions(
//                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
//            )
//        }
//    }
//
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_CODE_PERMISSIONS) {
//            if (allPermissionsGranted()) {
//                startCamera()
//            } else {
//                Toast.makeText(this, "Camera permission required", android.widget.Toast.LENGTH_SHORT).show()
//                //finish()
//            }
//        }
//    }
//
//    companion object {
//        init {
//            Utils.init()
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        binding = ActivityMediapipeBinding.inflate(layoutInflater)
//        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//        setContentView(binding.root)
//        setupFilament()
//        setupMediaPipe()
//        startCamera()
//    }
//
//
//    private fun setupFilament() {
//        try {
//            surfaceView = binding.filamentSurfaceView
//            // Create the Engine with error handling
//            val engine = try {
//                Engine.create(Engine.Backend.OPENGL)
//            } catch (e: Exception) {
//                Toast.makeText(this, "Failed to create Filament Engine", Toast.LENGTH_LONG).show()
//                //finish()
//                return
//            }
//
//            modelViewer = ModelViewer(surfaceView = surfaceView , engine = engine)
//            val buffer = readAsset("models/black1.glb")
//            modelViewer.loadModelGlb(buffer)
//            modelViewer.transformToUnitCube()
//
//        } catch (e: UnsatisfiedLinkError) {
//            e.printStackTrace()
//            Toast.makeText(this, "Failed to load Filament libraries", Toast.LENGTH_LONG).show()
//           // finish()
//        } catch (e: Exception) {
//            Toast.makeText(this, "Error setting up Filament: ${e.message}", Toast.LENGTH_LONG).show()
//            //finish()
//        }
//    }
//
//    private fun readAsset(assetName: String): ByteBuffer {
//        val input = assets.open(assetName)
//        val bytes = ByteArray(input.available())
//        input.read(bytes)
//        return ByteBuffer.wrap(bytes)
//    }
//
//    private fun setupMediaPipe() {
//        lipstickOverlay = binding.lipstickOverlay
//        lipstickOverlay.setZOrderOnTop(true)
//        lipstickOverlay.holder.setFormat(android.graphics.PixelFormat.TRANSPARENT)
//
//        val baseOptions = BaseOptions.builder()
//            .setModelAssetPath("face_landmarker.task")
//            .build()
//
//        val options = FaceLandmarkerOptions.builder()
//            .setBaseOptions(baseOptions)
//            .setOutputFacialTransformationMatrixes(true)
//            .setRunningMode(RunningMode.LIVE_STREAM)
//            .setResultListener { result: FaceLandmarkerResult, _: MPImage ->
//                runOnUiThread {
//                    //updateGlassesModel(result)
//                    //drawLipstickOverlay(result)
//                }
//            }
//            .build()
//
//
//        faceLandmarker = FaceLandmarker.createFromOptions(this, options)
//    }
//
//    private fun startCamera() {
//        if (!allPermissionsGranted()) {
//            requestPermissions()
//            return
//        }
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//           val cameraProvider = cameraProviderFuture.get()
//            val preview = Preview.Builder()
//                .setTargetRotation(binding.previewView.display.rotation)
//                .build()
//
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//                .also {
//                    it.setAnalyzer(executor) { image ->
//                        val bitmap = image.toBitmap()
//                        processFrame(bitmap)
//                        image.close()
//                    }
//                }
//
//            val cameraSelector =
//                CameraSelector.Builder()
//                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
//
//            cameraProvider.unbindAll()
//
//            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
//
//            preview.setSurfaceProvider(binding.previewView.surfaceProvider)
//
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    private fun processFrame(bitmap: Bitmap) {
//        val mpImage = BitmapImageBuilder(bitmap).build()
//        faceLandmarker.detectAsync(mpImage, SystemClock.uptimeMillis())
//    }
//
//    private fun updateGlassesModel(result: FaceLandmarkerResult) {
//        val transformMatrices = result.facialTransformationMatrixes()
//        if (!transformMatrices.isEmpty) {
//            val transformMatrix = transformMatrices.get().first()
//            val entity = modelViewer.asset?.entities?.first() ?: return
//            val transformManager = modelViewer.engine.transformManager
//            val instance = transformManager.getInstance(entity)
//            transformManager.setTransform(instance, transformMatrix)
//        }
//    }
//
//
//    private fun drawLipstickOverlay(result: FaceLandmarkerResult) {
//        val holder: SurfaceHolder = lipstickOverlay.holder
//        val canvas: Canvas = holder.lockCanvas() ?: return
//        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
//
//        val landmarks: List<NormalizedLandmark>? = result.faceLandmarks().firstOrNull()
//        val lipIndices = listOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375)
//        if (landmarks == null) return
//
//        val paint = Paint().apply {
//            color = Color.RED
//            style = Paint.Style.FILL
//            alpha = 120
//        }
//
//        val path = Path()
//        val first = landmarks[lipIndices[0]]
//        path.moveTo(first.x() * lipstickOverlay.width, first.y() * lipstickOverlay.height)
//
//        for (i in 1 until lipIndices.size) {
//            val pt = landmarks[lipIndices[i]]
//            path.lineTo(pt.x() * lipstickOverlay.width, pt.y() * lipstickOverlay.height)
//        }
//        path.close()
//        canvas.drawPath(path, paint)
//
//        holder.unlockCanvasAndPost(canvas)
//    }
//
//    private fun ImageProxy.toBitmap(): Bitmap {
//        val yBuffer: ByteBuffer = planes[0].buffer
//        val uBuffer: ByteBuffer = planes[1].buffer
//        val vBuffer: ByteBuffer = planes[2].buffer
//        val ySize = yBuffer.remaining()
//        val uSize = uBuffer.remaining()
//        val vSize = vBuffer.remaining()
//
//        val nv21 = ByteArray(ySize + uSize + vSize)
//        yBuffer.get(nv21, 0, ySize)
//        vBuffer.get(nv21, ySize, vSize)
//        uBuffer.get(nv21, ySize + vSize, uSize)
//
//        val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
//        val out = java.io.ByteArrayOutputStream()
//        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
//        val imageBytes = out.toByteArray()
//        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//    }
//}
//
//
//fun main() {
//    val specialChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
//    val name = "VI@P!U#L".toCharArray()
//    println("result")
//    println(name.joinToString(""))
//    var fromStart = 0
//    var fromEnd = name.lastIndex
//    for (i in 0..name.lastIndex) {
//        if (fromStart >= fromEnd) {
//            break
//        }
//        if (!specialChars.contains(name[fromStart])) {
//            fromStart++
//        }
//        if (!specialChars.contains(name[fromEnd])) {
//            fromEnd--
//        }
//        if (specialChars.contains(name[fromStart]) && specialChars.contains(name[fromEnd])) {
//            val temp = name[fromStart]
//            name[fromStart] = name[fromEnd]
//            name[fromEnd] = temp
//            fromStart++
//            fromEnd--
//        }
//        println(i)
//    }
//    println(name.joinToString(""))
//}
