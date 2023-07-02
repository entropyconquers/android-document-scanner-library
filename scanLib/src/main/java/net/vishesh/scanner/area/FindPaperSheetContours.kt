package net.vishesh.scanner.area

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.util.Log
import net.vishesh.scanner.model.Corners
import net.vishesh.scanner.plugins.shape
import net.vishesh.scanner.helpers.Either
import net.vishesh.scanner.helpers.Left
import net.vishesh.scanner.helpers.Right
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min


class FindPaperSheetContours : UseCase<Pair<Bitmap, Corners?>, FindPaperSheetContours.Params>() {
    companion object {
        private const val ANGLES_NUMBER = 4
        private const val EPSILON_CONSTANT = 0.02
        private const val CLOSE_KERNEL_SIZE = 10.0
        private const val CANNY_THRESHOLD_LOW = 75.0
        private const val CANNY_THRESHOLD_HIGH = 200.0
        private const val CUTOFF_THRESHOLD = 155.0
        private const val TRUNCATE_THRESHOLD = 150.0
        private const val NORMALIZATION_MIN_VALUE = 0.0
        private const val NORMALIZATION_MAX_VALUE = 255.0
        private const val BLURRING_KERNEL_SIZE = 5.0
        private const val DOWNSCALE_IMAGE_SIZE = 600.0
        private const val FIRST_MAX_CONTOURS = 5
    }
    class Params(
        val bitmap: Bitmap,
        val returnOriginalMat: Boolean = false

    )
    private fun hull2Points(hull: MatOfInt, contour: MatOfPoint): MatOfPoint {
        val indexes = hull.toList()
        val points: MutableList<Point> = ArrayList()
        val ctrList = contour.toList()
        for (index in indexes) {
            points.add(ctrList[index])
        }
        val point = MatOfPoint()
        point.fromList(points)
        return point
    }

    private fun findLargestContours(inputMat: Mat, numTopContours: Int): List<MatOfPoint>? {
        val mHierarchy = Mat()
        val mContourList: List<MatOfPoint> = ArrayList()
        //finding contours - as we are sorting by area anyway, we can use RETR_LIST - faster than RETR_EXTERNAL.
        Imgproc.findContours(inputMat, mContourList, mHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        // Convert the contours to their Convex Hulls i.e. removes minor nuances in the contour
        val mHullList: MutableList<MatOfPoint> = ArrayList()
        val tempHullIndices = MatOfInt()
        for (i in mContourList.indices) {
            Imgproc.convexHull(mContourList[i], tempHullIndices)
            mHullList.add(hull2Points(tempHullIndices, mContourList[i]))
        }
        // Release mContourList as its job is done
        for (c in mContourList) c.release()
        tempHullIndices.release()
        mHierarchy.release()
        if (mHullList.size != 0) {
            mHullList.sortWith { lhs: MatOfPoint?, rhs: MatOfPoint? -> Imgproc.contourArea(rhs).compareTo(Imgproc.contourArea(lhs)) }
            return mHullList.subList(0, min(mHullList.size, numTopContours))
        }
        return null
    }

    private fun findQuadrilateral(mContourList: List<MatOfPoint>): MatOfPoint? {
        for (c in mContourList) {
            val c2f = MatOfPoint2f(*c.toArray())
            val peri = Imgproc.arcLength(c2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
            val points = approx.toArray()
            // select biggest 4 angles polygon
            if (approx.rows() == 4) {
                val foundPoints = sortPoints(points)
                val angle1: Double = atan2(foundPoints[0].y - foundPoints[1].y, foundPoints[0].x - foundPoints[1].x) * 180 / PI
                val angle2: Double = atan2(foundPoints[3].y - foundPoints[2].y, foundPoints[3].x - foundPoints[2].x) * 180 / PI
                val angle = Math.abs(Math.abs(angle1)-Math.abs(angle2))
                if(angle>10 ) return null
                Log.d(TAG, "DETECTED ANGLE: $angle")
                val mPoints = MatOfPoint()
                mPoints.fromArray(*foundPoints)
                return mPoints
            }
        }
        return null
    }
    internal fun MatOfPoint.scaleRectangle(scale: Double): MatOfPoint {
        val originalPoints = this.toList()
        val resultPoints: MutableList<Point> = java.util.ArrayList()
        for (point in originalPoints) {
            resultPoints.add(Point(point.x * scale, point.y * scale))
        }
        val result = MatOfPoint()
        result.fromList(resultPoints)
        return result
    }
    fun pipeine1(params: Params, original: Mat): MutableList<MatOfPoint> {
        val modified = Mat()
        //Log.d(TAG,"Detected Edges "+ rectf)


        // Convert image from RGBA to GrayScale
        Imgproc.cvtColor(original, modified, Imgproc.COLOR_RGBA2GRAY)

        // Strong Gaussian Filter
        Imgproc.GaussianBlur(modified, modified, Size(51.0, 51.0), 0.0)

        // Canny Edge Detection
        Imgproc.Canny(modified, modified, 100.0, 200.0, 5, false)

        // Closing: Dilation followed by Erosion
        Imgproc.dilate(
            modified, modified, Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT, Size(
                    8.0,
                    8.0
                )
            )
        )
        Imgproc.erode(
            modified, modified, Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                Size(3.0, 3.0)
            )
        )

        var contours: MutableList<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        Imgproc.findContours(
            modified,
            contours,
            hierarchy,
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        hierarchy.release()
        contours = contours
            .filter { it.shape.size == 4 && Imgproc.contourArea(it) >= (params.bitmap.width*params.bitmap.height)/10 }
            .toTypedArray()
            .toMutableList()

        contours.sortWith { lhs, rhs ->
            Imgproc.contourArea(rhs).compareTo(Imgproc.contourArea(lhs))
        }
        if(contours.size>0) Log.d(TAG,"Pipeline 1 selected")
        else{
            val largestContour = findLargestContours(modified, 10)
             contours = mutableListOf()
            if (largestContour != null) {
                val quad = findQuadrilateral(largestContour)
                if(quad!=null && quad.shape.size == 4 && Imgproc.contourArea(quad) >= (params.bitmap.width*params.bitmap.height)/10) contours.add(quad)
            }
            if(contours.size>0) Log.d(TAG,"Pipeline 1 selected")
            return contours
        }
        return contours
    }
    fun pipeine2(params: Params, src: Mat): MutableList<MatOfPoint> {



        /*val ratio = DOWNSCALE_IMAGE_SIZE / max(src.width(), src.height())
        val downscaledSize = Size(src.width() * ratio, src.height() * ratio)
        //val downscaled = Mat(downscaledSize, src.type())
        Imgproc.resize(src, src, downscaledSize)

         */

        val destination = Mat()
        // Convert image from RGBA to GrayScale
        Imgproc.blur(src, src, Size(BLURRING_KERNEL_SIZE, BLURRING_KERNEL_SIZE))

        Core.normalize(src, src, NORMALIZATION_MIN_VALUE, NORMALIZATION_MAX_VALUE, Core.NORM_MINMAX)

        Imgproc.threshold(src, src, TRUNCATE_THRESHOLD, NORMALIZATION_MAX_VALUE, Imgproc.THRESH_TRUNC)
        Core.normalize(src, src, NORMALIZATION_MIN_VALUE, NORMALIZATION_MAX_VALUE, Core.NORM_MINMAX)

        Imgproc.Canny(src, destination, CANNY_THRESHOLD_HIGH, CANNY_THRESHOLD_LOW)

        Imgproc.threshold(destination, destination, CUTOFF_THRESHOLD, NORMALIZATION_MAX_VALUE, Imgproc.THRESH_TOZERO)

        Imgproc.morphologyEx(
            destination, destination, Imgproc.MORPH_CLOSE,
            Mat(Size(CLOSE_KERNEL_SIZE, CLOSE_KERNEL_SIZE), CvType.CV_8UC1, Scalar(NORMALIZATION_MAX_VALUE)),
            Point(-1.0, -1.0), 1
        )

        /*var contours: MutableList<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        Imgproc.findContours(
            destination,
            contours,
            hierarchy,
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )*/

        //hierarchy.release()
        val largestContour = findLargestContours(destination, 10)
        val contours:MutableList<MatOfPoint> = mutableListOf()
        if (largestContour != null) {
            val quad = findQuadrilateral(largestContour)
            if(quad!=null && quad.shape.size == 4 && Imgproc.contourArea(quad) >= (params.bitmap.width*params.bitmap.height)/10) contours.add(quad)
        }
        if(contours.size>0) Log.d(TAG,"Pipeline 2 selected")
        return contours
        /*

        contours = contours
            .filter { it.shape.size == 4 && Imgproc.contourArea(it) >= (params.bitmap.width*params.bitmap.height)/10 }
            .toTypedArray()
            .toMutableList()

        contours.sortWith { lhs, rhs ->
            Imgproc.contourArea(rhs).compareTo(Imgproc.contourArea(lhs))
        }
        //contours.map { it.scaleRectangle(1f/ratio) }

        return contours*/
    }

    override suspend fun run(params: Params): Either<Failure, Pair<Bitmap, Corners?>> =
        try {

            val original = Mat()
            Utils.bitmapToMat(params.bitmap, original)
            val contours1 = pipeine1(params, original)
            var contours:MutableList<MatOfPoint> = mutableListOf()
            if(contours1.size != 0) contours = contours1
            else {
                val contours2 = pipeine2(params, original)
                if(contours2.size != 0) contours = contours2

            }
            if (params.returnOriginalMat) {

                Utils.matToBitmap(original, params.bitmap)
            } else {
                params.bitmap.recycle()
            }

            Right(contours.firstOrNull()?.let {
                Pair(
                    params.bitmap,
                    Corners(
                        it.shape.toList(),
                        original.size()
                    )
                )
            } ?: Pair(params.bitmap, null))
        } catch (throwable: Throwable) {
            Left(Failure(throwable))
        }
}
