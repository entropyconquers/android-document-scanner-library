package net.vishesh.scanner.display

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures.addCallback
import androidx.camera.view.CameraController.IMAGE_ANALYSIS
import androidx.camera.view.CameraController.IMAGE_CAPTURE
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import net.vishesh.scanner.model.Corners
import net.vishesh.scanner.model.OpenCVLoader
import net.vishesh.scanner.model.OpenCvStatus
import net.vishesh.scanner.area.Failure
import net.vishesh.scanner.area.FindPaperSheetContours
import org.opencv.core.Point
import org.opencv.core.Size
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor


private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

enum class FlashStatus {
    ON, OFF
}

class ScannerViewModel : ViewModel() {
    private lateinit var controller: LifecycleCameraController

    /**
     * Observable data
     */
    val isBusy = MutableLiveData<Boolean>()
    private val openCv = MutableLiveData<OpenCvStatus>()
    val corners = MutableLiveData<Corners?>()
    val mlCorners = MutableLiveData<Corners?>()
    val cornersDef = MutableLiveData<Corners?>()

    val errors = MutableLiveData<Throwable>()
    val flashStatus = MutableLiveData<FlashStatus>()

    private var didLoadOpenCv = false

    /**
     * Use cases
     */
    private val findPaperSheetUseCase: FindPaperSheetContours = FindPaperSheetContours()

    /**
     * Tries to load OpenCv native libraries
     */
    fun onViewCreated(
        openCVLoader: OpenCVLoader,
        scannerActivity: AppCompatActivity,
        viewFinder: PreviewView
    ) {

        isBusy.value = true
        setupCamera(scannerActivity, viewFinder) {
            if (!didLoadOpenCv) {
                openCVLoader.load {
                    isBusy.value = false
                    openCv.value = it
                    didLoadOpenCv = true
                }
            } else {
                isBusy.value = false
            }
        }
    }

    fun onFlashToggle() {
        flashStatus.value?.let { currentValue ->
            flashStatus.value = when (currentValue) {
                FlashStatus.ON -> FlashStatus.OFF
                FlashStatus.OFF -> FlashStatus.ON
            }
        } ?: // default flash status is off
        run {
            // default flash status is off
            // default flash status is off
            flashStatus.value = FlashStatus.ON
        }
        when (flashStatus.value) {
            FlashStatus.ON -> controller.enableTorch(true)
            FlashStatus.OFF -> controller.enableTorch(false)
            null -> controller.enableTorch(false)
        }
    }



    @SuppressLint("UnsafeOptInUsageError")
    fun onTakePicture(outputDirectory: File, context: Context) {
        Log.d(ContentValues.TAG, "ERROR A "+ corners.value)
        //corners.value?.log()
        isBusy.value = true
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        /*val imageCaptureBuilder = ImageCapture.Builder().apply {

            setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)


        }

        Camera2Interop.Extender(imageCaptureBuilder).setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 1)

        val imageCapture = imageCaptureBuilder.build()*/

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture
            .OutputFileOptions.Builder(photoFile)



        controller.takePicture(
            outputOptions.build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {
                    lastUri.value = Uri.fromFile(photoFile)
                    errors.value = exc
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    lastUri.value = Uri.fromFile(photoFile)

                }
            })
    }

    // CameraX setup
    var lastUri: MutableLiveData<Uri> = MutableLiveData()

    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun setupCamera(
        lifecycleOwner: AppCompatActivity,
        viewFinder: PreviewView,
        then: () -> Unit
    ) {
        isBusy.value = true
        /*val cameraProviderFuture = ProcessCameraProvider.getInstance(lifecycleOwner)

        cameraProviderFuture.addListener(Runnable {
            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            // Set up the preview use case to display camera preview.
            val builder = ImageAnalysis.Builder()
            builder.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

            val ext: Camera2Interop.Extender<*> = Camera2Interop.Extender(builder)
            ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF
            )
            ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range<Int>(60, 60)
            )
            val imageAnalysis = builder.build()

            val preview = Preview.Builder()
                .build()

            // Set up the capture use case to allow users to take photos.
            var imageCapture = ImageCapture.Builder()
                .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Choose the camera by requiring a lens facing
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            // Attach use cases to the camera with the same lifecycle owner


            // Connect the preview use case to the previewView
            preview.setSurfaceProvider(
                viewFinder.getSurfaceProvider())
            val executor: Executor = ContextCompat.getMainExecutor(lifecycleOwner)



            imageAnalysis.setAnalyzer(executor) { proxy: ImageProxy ->
                // could not find a performing way to transform
                // the proxy to a bitmap, so we are reading
                // the bitmap directly from the preview view
                val options = ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .build()
                val objectDetector = ObjectDetection.getClient(options)


                viewFinder.bitmap?.let {


                    val points: MutableList<Point> = ArrayList()
                    points.add(Point((it.width / 4).toDouble(), (it.height / 4).toDouble()))
                    points.add(Point((3 * it.width / 4).toDouble(), (it.height / 4).toDouble()))
                    points.add(Point((3 * it.width / 4).toDouble(), (3 * it.height / 4).toDouble()))
                    points.add(Point((it.width / 4).toDouble(), (3 * it.height / 4).toDouble()))
                    val newS = Size(it.width.toDouble(), it.height.toDouble())

                    cornersDef.value = Corners(points, newS)



                    analyze(it, objectDetector, onSuccess = {
                        proxy.close()
                    })
                } ?: run {
                    corners.value = cornersDef.value
                    proxy.close()
                }
            }
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalysis, imageCapture)
        }, ContextCompat.getMainExecutor(lifecycleOwner))*/
        val executor: Executor = ContextCompat.getMainExecutor(lifecycleOwner)
        controller = LifecycleCameraController(lifecycleOwner)
        controller.isTapToFocusEnabled = true



        controller.setImageAnalysisAnalyzer(executor) { proxy: ImageProxy ->
            // could not find a performing way to transform
            // the proxy to a bitmap, so we are reading
            // the bitmap directly from the preview view
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
            val objectDetector = ObjectDetection.getClient(options)


            viewFinder.bitmap?.let {



                val points: MutableList<Point> = ArrayList()
                points.add(Point((it.width / 4).toDouble(), (it.height / 4).toDouble()))
                points.add(Point((3 * it.width / 4).toDouble(), (it.height / 4).toDouble()))
                points.add(Point((3 * it.width / 4).toDouble(), (3 * it.height / 4).toDouble()))
                points.add(Point((it.width / 4).toDouble(), (3 * it.height / 4).toDouble()))
                val newS = Size(it.width.toDouble(), it.height.toDouble())

                cornersDef.value = Corners(points, newS)



                analyze(it,objectDetector, onSuccess = {
                    proxy.close()
                })
            } ?: run {
                corners.value = cornersDef.value
                proxy.close()
            }
        }
        controller.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST

        controller.setEnabledUseCases(IMAGE_CAPTURE or IMAGE_ANALYSIS)

        controller.bindToLifecycle(lifecycleOwner)
        viewFinder.controller = controller
        addCallback(
            controller.initializationFuture,
            object : FutureCallback<Void> {
                override fun onSuccess(result: Void?) {
                    then()
                }

                override fun onFailure(t: Throwable) {
                    errors.value = t
                }
            },
            executor
        )
        then.invoke()


    }


    private fun analyze(
        bitmap: Bitmap,
        objectDetector:ObjectDetector,
        onSuccess: (() -> Unit)? = null,
        returnOriginalMat: Boolean = false,
        callback: ((Pair<Bitmap, Corners?>) -> Unit)? = null
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        //mlCorners.value = null
        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                if(detectedObjects.size == 0) mlCorners.value = null
                //Log.d(TAG, "Detected Edges "+detectedObjects)
                // Task completed successfully
                // ...
                //val detectedObject = detectedObjects[0]
                for (detectedObject in detectedObjects) {
                    val rect = detectedObject.boundingBox
                    var pointList = listOf<Point>(
                        Point(
                            rect.left.toDouble(),
                            rect.top.toDouble()
                        ),
                        Point(
                            rect.right.toDouble(),
                            rect.top.toDouble()
                        ),
                        Point(
                            rect.right.toDouble(),
                            rect.bottom.toDouble()
                        ),
                        Point(
                            rect.left.toDouble(),
                            rect.bottom.toDouble()
                        )

                    )
                    mlCorners.value = Corners(pointList,
                        Size(
                            bitmap.width.toDouble(),
                            bitmap.height.toDouble()
                        ))
                    //proxy.close()
                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap,
                        rect.left,
                        rect.top,
                        rect.width(),
                        rect.height(),
                        null,
                        false
                    )
                    Log.d(TAG, "Detected Edges: "+rect)
                    findPaperSheetUseCase(FindPaperSheetContours.Params(bitmap, returnOriginalMat)) {


                        it.fold(::handleFailure) { pair: Pair<Bitmap, Corners?> ->
                            callback?.invoke(pair) ?: run {
                                if(pair.second!=null){
                                    corners.value = pair.second
                                    Log.d(TAG, "Detected Edges Corners: "+ corners.value?.points)
                                }

                                else corners.value = null
                            }
                            //onSuccess?.invoke()
                        }
                    }
                }
                onSuccess?.invoke()
                //val rectf = detectedObject.boundingBox
                /*val croppedBitmap = Bitmap.createBitmap(
                    params.bitmap,
                    rectf.left,
                    rectf.top,
                    rectf.width(),
                    rectf.height(),
                    null,
                    false
                )*/
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "Detected Edges: "+e)
                // Task failed with an exception
                // ...
            }
        /*findPaperSheetUseCase(FindPaperSheetContours.Params(bitmap, returnOriginalMat)) {


            it.fold(::handleFailure) { pair: Pair<Bitmap, Corners?> ->
                callback?.invoke(pair) ?: run {
                    if(pair.second!=null)
                    corners.value = pair.second
                    else corners.value = null
                }
                onSuccess?.invoke()
            }
        }*/
    }

    private fun handleFailure(failure: Failure) {
        errors.value = failure.origin
        isBusy.value = false
    }

    fun onClosePreview() {
        lastUri.value?.let {
            val file = File(it.path!!)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
