package com.app.scene.mediapipe

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.scene.databinding.ActivityMediaPipeSolutionBinding
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark
import com.google.mediapipe.solutioncore.CameraInput
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView
import com.google.mediapipe.solutioncore.VideoInput
import com.google.mediapipe.solutions.facemesh.FaceMesh
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions
import com.google.mediapipe.solutions.facemesh.FaceMeshResult
import java.io.IOException
import java.io.InputStream


class MediaPipeSolutionActivity : AppCompatActivity() {

    private lateinit var facemesh: FaceMesh
    // Run the pipeline and the model inference on GPU or CPU.

    val RUN_ON_GPU: Boolean = true

    private enum class InputSource {
        UNKNOWN,
        IMAGE,
        VIDEO,
        CAMERA,
    }

    private var inputSource: InputSource = InputSource.UNKNOWN

    // Image demo UI and image loader components.
    private lateinit var imageGetter: ActivityResultLauncher<Intent>
    private lateinit var imageView: FaceMeshResultImageView

    // Video demo UI and video loader components.
    private lateinit var videoInput: VideoInput
    private lateinit var videoGetter: ActivityResultLauncher<Intent>

    // Live camera demo UI and camera components.
    private lateinit var cameraInput: CameraInput

    private lateinit var glSurfaceView: SolutionGlSurfaceView<FaceMeshResult>

    lateinit var binding : ActivityMediaPipeSolutionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMediaPipeSolutionBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)
        setupLiveDemoUiComponents()
    }


    override fun onResume() {
        super.onResume()
        if (inputSource === InputSource.CAMERA) {
            // Restarts the camera and the opengl surface rendering.
            cameraInput = CameraInput(this)
            cameraInput.setNewFrameListener({ textureFrame -> facemesh.send(textureFrame) })
            glSurfaceView.post({ this.startCamera() })
            glSurfaceView.setVisibility(View.VISIBLE)
        } else if (inputSource === InputSource.VIDEO) {
            videoInput.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (inputSource === InputSource.CAMERA) {
            glSurfaceView.setVisibility(View.GONE)
            cameraInput.close()
        } else if (inputSource === InputSource.VIDEO) {
            videoInput.pause()
        }
    }

    private fun downscaleBitmap(originalBitmap: Bitmap): Bitmap {
        val aspectRatio = originalBitmap.getWidth().toDouble() / originalBitmap.getHeight()
        var width: Int = imageView.width
        var height: Int = imageView.height
        if ((imageView.width / imageView.height) > aspectRatio) {
            width = (height * aspectRatio).toInt()
        } else {
            height = (width / aspectRatio).toInt()
        }
        return originalBitmap.scale(width, height, false)
    }

    @Throws(IOException::class)
    private fun rotateBitmap(inputBitmap: Bitmap, imageData: InputStream): Bitmap? {
        val orientation =
            ExifInterface(imageData)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            return inputBitmap
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> matrix.postRotate(0f)
        }
        return Bitmap.createBitmap(
            inputBitmap, 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), matrix, true
        )
    }

    /** Sets up the UI components for the static image demo.  */
    private fun setupStaticImageDemoUiComponents() {
        // The Intent to access gallery and read images as bitmap.
        imageGetter =
            registerForActivityResult<Intent, ActivityResult>(
                StartActivityForResult(),
                ActivityResultCallback { result: ActivityResult? ->
                    val resultIntent = result!!.data
                    if (resultIntent != null) {
                        if (result.resultCode == RESULT_OK) {
                            var bitmap: Bitmap? = null
                            try {
                                bitmap =
                                    downscaleBitmap(
                                        MediaStore.Images.Media.getBitmap(
                                            this.contentResolver, resultIntent.data
                                        )
                                    )
                            } catch (e: IOException) {
                                //Log.e(TAG, "Bitmap reading error:" + e)
                            }
                            try {
                                val imageData = this.contentResolver.openInputStream(resultIntent.data!!)
                                bitmap = rotateBitmap(bitmap!!, imageData!!)
                            } catch (e: IOException) {
                                //Log.e(TAG, "Bitmap rotation error:" + e)
                            }
                            if (bitmap != null) {
                                facemesh?.send(bitmap)
                            }
                        }
                    }
                })

//        loadImageButton.setOnClickListener(
//            View.OnClickListener { v: View? ->
//                if (inputSource !== InputSource.IMAGE) {
//                    stopCurrentPipeline()
//                    setupStaticImageModePipeline()
//                }
//                // Reads images from gallery.
//                val pickImageIntent = Intent(Intent.ACTION_PICK)
//                pickImageIntent.setDataAndType(
//                    MediaStore.Images.Media.INTERNAL_CONTENT_URI,
//                    "image/*"
//                )
//                imageGetter.launch(pickImageIntent)
//            })
        imageView = FaceMeshResultImageView(this)
    }

//    /** Sets up core workflow for static image mode.  */
//    private fun setupStaticImageModePipeline() {
//        val inputSource = InputSource.IMAGE
//        // Initializes a new MediaPipe Face Mesh solution instance in the static image mode.
//        facemesh =
//            FaceMesh(
//                this,
//                FaceMeshOptions.builder()
//                    .setStaticImageMode(true)
//                    .setRefineLandmarks(true)
//                    .setRunOnGpu(RUN_ON_GPU)
//                    .build()
//            )
//
//        // Connects MediaPipe Face Mesh solution to the user-defined FaceMeshResultImageView.
//        facemesh.setResultListener(
//            { faceMeshResult ->
//                logNoseLandmark(faceMeshResult,  /*showPixelValues=*/true)
//                imageView.setFaceMeshResult(faceMeshResult)
//                runOnUiThread(Runnable { imageView.update() })
//            })
//        facemesh.setErrorListener({ message, e ->
//            Log.e(
//                TAG,
//                "MediaPipe Face Mesh error:" + message
//            )
//        })
//
//        // Updates the preview layout.
//        val frameLayout = findViewById<FrameLayout?>(R.id.preview_display_layout)
//        frameLayout.removeAllViewsInLayout()
//        imageView.setImageDrawable(null)
//        frameLayout.addView(imageView)
//        imageView.setVisibility(View.VISIBLE)
//    }

    /** Sets up the UI components for the video demo.  */
    private fun setupVideoDemoUiComponents() {
        // The Intent to access gallery and read a video file.
        videoGetter =
            registerForActivityResult<Intent, ActivityResult>(
                StartActivityForResult(),
                ActivityResultCallback { result: ActivityResult? ->
                    val resultIntent = result!!.data
                    if (resultIntent != null) {
                        if (result.resultCode == RESULT_OK) {
                            glSurfaceView.post(
                                {
                                    videoInput.start(
                                        this,
                                        resultIntent.data,
                                        facemesh.glContext,
                                        glSurfaceView.width,
                                        glSurfaceView.height
                                    )
                                })
                        }
                    }
                })
//        val loadVideoButton = findViewById<Button?>(R.id.button_load_video)
//        loadVideoButton.setOnClickListener(
//            View.OnClickListener { v: View? ->
//                stopCurrentPipeline()
//                setupStreamingModePipeline(InputSource.VIDEO)
//                // Reads video from gallery.
//                val pickVideoIntent = Intent(Intent.ACTION_PICK)
//                pickVideoIntent.setDataAndType(
//                    MediaStore.Video.Media.INTERNAL_CONTENT_URI,
//                    "video/*"
//                )
//                videoGetter.launch(pickVideoIntent)
//            })
    }

    /** Sets up the UI components for the live demo with camera input.  */
    private fun setupLiveDemoUiComponents() {
        /*val startCameraButton = findViewById<Button?>(R.id.button_start_camera)
        startCameraButton.setOnClickListener(
            View.OnClickListener { v: View? ->
                if (inputSource === InputSource.CAMERA) {
                    return@setOnClickListener
                }
                stopCurrentPipeline()

            })*/
        setupStreamingModePipeline(InputSource.CAMERA)
    }

    /** Sets up core workflow for streaming mode.  */
    private fun setupStreamingModePipeline(inputSource: InputSource) {
        this.inputSource = inputSource
        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
        facemesh = FaceMesh(
                this,
                FaceMeshOptions.builder()
                    .setStaticImageMode(false)
                    .setRefineLandmarks(true)
                    .setRunOnGpu(RUN_ON_GPU)
                    .build()
            )
        facemesh.setErrorListener({ message, e ->
//            Log.e(
//                TAG,
//                "MediaPipe Face Mesh error:" + message
//            )
        })

        if (inputSource === InputSource.CAMERA) {
            cameraInput = CameraInput(this)
            cameraInput.setNewFrameListener({ textureFrame -> facemesh.send(textureFrame) })
        } else if (inputSource === InputSource.VIDEO) {
            videoInput = VideoInput(this)
            videoInput.setNewFrameListener({ textureFrame -> facemesh.send(textureFrame) })
        }

        // Initializes a new Gl surface view with a user-defined FaceMeshResultGlRenderer.
        glSurfaceView = SolutionGlSurfaceView(this, facemesh.glContext, facemesh.glMajorVersion)
        glSurfaceView.setSolutionResultRenderer(FaceMeshResultGlRenderer())
        glSurfaceView.setRenderInputImage(true)
        facemesh.setResultListener(
            { faceMeshResult ->
                logNoseLandmark(faceMeshResult,  /*showPixelValues=*/false)
                glSurfaceView.setRenderData(faceMeshResult)
                glSurfaceView.requestRender()
            })

        // The runnable to start camera after the gl surface view is attached.
        // For video input source, videoInput.start() will be called when the video uri is available.
        if (inputSource === InputSource.CAMERA) {
            glSurfaceView.post({ this.startCamera() })
        }

        // Updates the preview layout.
       // val frameLayout = findViewById<FrameLayout?>(R.id.preview_display_layout)
        //imageView.setVisibility(View.GONE)
        binding.previewDisplayLayout.removeAllViewsInLayout()
        binding.previewDisplayLayout.addView(glSurfaceView)
        glSurfaceView.setVisibility(View.VISIBLE)
        binding.previewDisplayLayout.requestLayout()
    }

    private fun startCamera() {
        cameraInput.start(
            this,
            facemesh.glContext,
            CameraInput.CameraFacing.FRONT,
            glSurfaceView.width,
            glSurfaceView.height
        )
    }

    private fun stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput.setNewFrameListener(null)
            cameraInput.close()
        }
        if (videoInput != null) {
            videoInput.setNewFrameListener(null)
            videoInput.close()
        }
        if (glSurfaceView != null) {
            glSurfaceView.setVisibility(View.GONE)
        }
        if (facemesh != null) {
            facemesh.close()
        }
    }

    private fun logNoseLandmark(result: FaceMeshResult?, showPixelValues: Boolean) {
        if (result == null || result.multiFaceLandmarks().isEmpty()) {
            return
        }
        val noseLandmark: NormalizedLandmark =
            result.multiFaceLandmarks()[0].landmarkList.get(1)
        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
        if (showPixelValues) {
            val width = result.inputBitmap().getWidth()
            val height = result.inputBitmap().getHeight()
            /*Log.i(
                TAG,
                String.format(
                    "MediaPipe Face Mesh nose coordinates (pixel values): x=%f, y=%f",
                    noseLandmark.getX() * width, noseLandmark.getY() * height
                )
            )*/
        } else {
            /*Log.i(
                TAG,
                String.format(
                    "MediaPipe Face Mesh nose normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                    noseLandmark.getX(), noseLandmark.getY()
                )
            )*/
        }
    }

}