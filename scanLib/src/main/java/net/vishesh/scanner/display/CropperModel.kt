package net.vishesh.scanner.display

import android.content.ContentResolver
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicConvolve3x3
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import net.vishesh.scanner.model.Corners
import net.vishesh.scanner.area.Failure
import net.vishesh.scanner.area.FindPaperSheetContours
import net.vishesh.scanner.area.PerspectiveTransform
import net.vishesh.scanner.area.UriToBitmap
import org.opencv.core.Point
import org.opencv.core.Size


class CropperModel : ViewModel() {
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()
    private val findPaperSheetUseCase: FindPaperSheetContours = FindPaperSheetContours()
    private val uriToBitmap: UriToBitmap = UriToBitmap()
    val cornersDef = MutableLiveData<Corners?>()
    val corners = MutableLiveData<Corners?>()
    val original = MutableLiveData<Bitmap>()
    val bitmapToCrop = MutableLiveData<Bitmap>()
    var isRotated: Boolean = false
    fun getBitmapFromColorMatrix(cm: ColorMatrix?, sourceBitmap: Bitmap): Bitmap {
        val ret = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, sourceBitmap.config)
        val canvas = Canvas(ret)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm!!)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)
        return ret
    }
    val sharp = floatArrayOf(
        -0.60f, -0.60f, -0.60f, -0.60f, 5.81f, -0.60f,
        -0.60f, -0.60f, -0.60f
    )
    fun rotate(b: Bitmap, degrees: Int): Bitmap {
        var b = b
        if (degrees != 0) {
            val m = Matrix()
            m.setRotate(degrees.toFloat(), b.width.toFloat() / 2, b.height.toFloat() / 2)
            try {
                val b2 = Bitmap.createBitmap(
                    b, 0, 0, b.width, b.height, m, true
                )
                if (b != b2) {
                    b.recycle()
                    b = b2
                }
            } catch (ex: OutOfMemoryError) {
                throw ex
            }
        }
        return b
    }
    fun doSharpen(original: Bitmap, radius: FloatArray?, context: Context): Bitmap? {
        val bitmap = Bitmap.createBitmap(
            original.width, original.height,
            Bitmap.Config.ARGB_8888
        )
        val rs = RenderScript.create(context)
        val allocIn = Allocation.createFromBitmap(rs, original)
        val allocOut = Allocation.createFromBitmap(rs, bitmap)
        val convolution = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
        convolution.setInput(allocIn)
        convolution.setCoefficients(radius)
        convolution.forEach(allocOut)
        allocOut.copyTo(bitmap)
        rs.destroy()
        return bitmap
    }
    fun onViewCreated(
        uri: Uri,
        //oldCorners: Corners,
        contentResolver: ContentResolver,
        oldCorners: Corners,
        cropperActivity: CropperActivity
    ) {

        uriToBitmap(
            UriToBitmap.Params(
                uri = uri,
                contentResolver = contentResolver
            )
        ) {
            it.fold(::handleFailure) { preview ->

                val pow = Math.pow(2.0, -0.4).toFloat()
                val exposureMatrix = ColorMatrix(
                    floatArrayOf(
                        pow,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        pow,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        pow,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f
                    )
                )
                var preview = getBitmapFromColorMatrix(exposureMatrix,preview)
                if(preview.width>preview.height){
                    isRotated = true
                    preview = rotate(preview, -90)
                }
                if(oldCorners.points.size == 4){
                    val ratioX:Double = (oldCorners.size.width).div(preview.width.toDouble())
                    val ratioY:Double = (oldCorners.size.height).div(preview.height.toDouble())
                    oldCorners.points[0].x = oldCorners.points[0].x/ratioX
                    oldCorners.points[1].x = oldCorners.points[1].x/ratioX
                    oldCorners.points[2].x = oldCorners.points[2].x/ratioX
                    oldCorners.points[3].x = oldCorners.points[3].x/ratioX

                    oldCorners.points[0].y = oldCorners.points[0].y/ratioY
                    oldCorners.points[1].y = oldCorners.points[1].y/ratioY
                    oldCorners.points[2].y = oldCorners.points[2].y/ratioY
                    oldCorners.points[3].y = oldCorners.points[3].y/ratioY

                    oldCorners.size = Size(preview.width.toDouble(), preview.height.toDouble())
                    Log.d(TAG, "ERROR A $ratioX $ratioY")
                    corners.value = Corners(oldCorners.points, size = oldCorners.size)
                    corners.value!!.log()
                    original.value = preview
                }
                else{
                    analyze(preview, returnOriginalMat = true) { pair ->

                        val points: MutableList<Point> = ArrayList()
                        val it = pair.first
                        points.add(Point((it.width / 4).toDouble(), (it.height / 3).toDouble()))
                        points.add(Point((3 * it.width / 4).toDouble(), (it.height / 3).toDouble()))
                        points.add(
                            Point(
                                (3 * it.width / 4).toDouble(),
                                (2 * it.height / 3).toDouble()
                            )
                        )
                        points.add(Point((it.width / 4).toDouble(), (2 * it.height / 3).toDouble()))
                        val newS = Size(it.width.toDouble(), it.height.toDouble())

                        cornersDef.value = Corners(points, newS)

                        if (pair.second != null) {
                            original.value = pair.first!!
                            corners.value = pair.second
                        }
                        else {
                            //corners.value = cornersDef.value
                            val image = InputImage.fromBitmap(preview, 0)
                            //mlCorners.value = null
                            val options = ObjectDetectorOptions.Builder()
                                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                                .build()
                            val objectDetector = ObjectDetection.getClient(options)
                            objectDetector.process(image)
                                .addOnSuccessListener { detectedObjects ->

                                    if(detectedObjects.size == 0) {
                                        original.value = pair.first!!
                                        corners.value = cornersDef.value
                                        Toast.makeText(
                                            cropperActivity,
                                            "Cannot find document, please select corners manually.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    //Log.d(TAG, "Detected Edges "+detectedObjects)
                                    // Task completed successfully
                                    // ...
                                    //val detectedObject = detectedObjects[0]
                                    for (detectedObject in detectedObjects) {
                                        var rect = detectedObject.boundingBox

                                        //proxy.close()
                                        /*val croppedBitmap = Bitmap.createBitmap(
                                            preview,
                                            (if(rect.left-0.1*rect.width()>=0) rect.left-0.1*rect.width() else 0).toInt(),
                                            (if(rect.top-0.1*rect.height()>=0) rect.top-0.1*rect.height() else 0).toInt(),
                                            ((if(rect.width()*1.2<preview.width)rect.width()*1.2 else rect.width()*1.0 ).toInt()),
                                            ((if(rect.height()*1.2<preview.height)rect.height()*1.2 else rect.height()*1.0 ).toInt()),
                                            null,
                                            false
                                        )
                                        val conf = Bitmap.Config.ARGB_8888 // see other conf types
                                        var pow = Math.pow(2.0, -1.5).toFloat()
                                        val exposureMatrix = ColorMatrix(
                                            floatArrayOf(
                                                pow,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                pow,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                pow,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                0f,
                                                1f,
                                                0f
                                            )
                                        )
                                        val bmp = getBitmapFromColorMatrix(ColorMatrix(exposureMatrix),preview)

                                        val canvas = Canvas(bmp)
                                        canvas.drawBitmap(bmp, Matrix(), null)
                                        canvas.drawBitmap(
                                            croppedBitmap,
                                           // ((bmp.width / 2) - croppedBitmap.width/2).toFloat(),
                                            //((bmp.height) / 2 - croppedBitmap.height/2).toFloat(),
                                            (if(rect.left-0.1*rect.width()>=0) rect.left-0.1*rect.width() else 0).toFloat(),
                                            (if(rect.top-0.1*rect.height()>=0) rect.top-0.1*rect.height() else 0).toFloat(),
                                            null
                                        )
                                        //rect = Rect()
                                        //rect.left = ((bmp.width / 2) - croppedBitmap.width/2)
                                        //rect.top = ((bmp.height) / 2 - croppedBitmap.height/2)
                                        //rect.right = rect.left + croppedBitmap.width
                                        //rect.bottom = rect.top + croppedBitmap.height

                                         */
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
                                        corners.value = Corners(pointList,
                                            Size(
                                                preview.width.toDouble(),
                                                preview.height.toDouble()
                                            ))
                                        original.value = preview

                                        Log.d(ContentValues.TAG, "Detected Edges PHOTO: "+rect)

                                    }

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
                                    Log.d(ContentValues.TAG, "Detected Edges: "+e)
                                    // Task failed with an exception
                                    // ...
                                }
                            //corners.value = cornersDef.value

                        }

                        pair.second?.let {


                            }
                        }
                    }
                }

        }
    }

    fun onCornersAccepted(bitmap: Bitmap, context: Context) {
        perspectiveTransform(
                PerspectiveTransform.Params(
                    bitmap = bitmap,
                    corners = corners.value!!
                )
            ) { result ->
                result.fold(::handleFailure) { bitmap ->
                    var bmp = doSharpen(bitmap, sharp, context)
                    if(isRotated){

                        bmp = rotate(bmp!!, 90)
                        isRotated = false
                    }
                    bitmapToCrop.value = bmp!!

                }
            }
    }

    private fun analyze(
        bitmap: Bitmap,
        onSuccess: (() -> Unit)? = null,
        returnOriginalMat: Boolean = false,
        callback: ((Pair<Bitmap, Corners?>) -> Unit)? = null
    ) {

        findPaperSheetUseCase(
            FindPaperSheetContours.Params(
                bitmap,
                returnOriginalMat
            )
        ) {
            it.fold(::handleFailure) { pair: Pair<Bitmap, Corners?> ->
                callback?.invoke(pair) ?: run { }
                onSuccess?.invoke()
            }
        }
    }

    private fun handleFailure(failure: Failure) { }
}
