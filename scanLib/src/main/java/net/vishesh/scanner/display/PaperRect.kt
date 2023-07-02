package net.vishesh.scanner.display

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import net.vishesh.scanner.model.Corners
import org.opencv.core.Point
import kotlin.math.abs


class PaperRectangle : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributes: AttributeSet) : super(context, attributes)
    constructor(context: Context, attributes: AttributeSet, defTheme: Int) : super(
        context,
        attributes,
        defTheme
    )

    private val rectPaint = Paint()
    private val extCirclePaint = Paint()
    private val intCirclePaint = Paint()
    private val intCirclePaintR = Paint()
    private val extCirclePaintR = Paint()
    private val fillPaint = Paint()
    private var ratioX: Double = 1.0
    private var ratioY: Double = 1.0
    private var tl: Point = Point()
    private var tr: Point = Point()
    private var br: Point = Point()
    private var bl: Point = Point()
    private val path: Path = Path()
    private val path2: Path = Path()
    private var point2Move = Point()
    private var cropMode = false
    private var latestDownX = 0.0F
    private var latestDownY = 0.0F
    private var mscaleX = 0.0F
    private var mscaleY = 0.0F
    //val pathAnimator: ValueAnimator = ObjectAnimator.ofFloat(this, "mscaleX", "mscaleY", path)

    fun setMscaleX(mscaleX:Float){
        this.mscaleX = mscaleX
    }
    fun setMscaleY(mscaleY:Float){
        this.mscaleY = mscaleY
    }
    init {
        //pathAnimator.duration = 500
        //pathAnimator.interpolator = AccelerateDecelerateInterpolator()
        rectPaint.color = Color.parseColor("#FFFFFF")
        rectPaint.isAntiAlias = true
        rectPaint.isDither = true
        rectPaint.strokeWidth = 6F
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeJoin = Paint.Join.ROUND // set the join to round you want
        rectPaint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        rectPaint.pathEffect = CornerPathEffect(20f)

        fillPaint.color = Color.parseColor("#FF8A00")
        fillPaint.alpha = 75
        fillPaint.isAntiAlias = true
        fillPaint.isDither = true
        fillPaint.strokeWidth = 6F
        fillPaint.style = Paint.Style.FILL
        fillPaint.strokeJoin = Paint.Join.ROUND // set the join to round you want
        fillPaint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        fillPaint.pathEffect = CornerPathEffect(20f)

        extCirclePaint.color = Color.parseColor("#FFFFFF")
        extCirclePaint.isDither = true
        extCirclePaint.isAntiAlias = true
        extCirclePaint.strokeWidth = 8F
        extCirclePaint.style = Paint.Style.STROKE

        intCirclePaint.color = Color.BLUE
        intCirclePaint.isDither = true
        intCirclePaint.isAntiAlias = true
        intCirclePaint.strokeWidth = 10F
        intCirclePaint.style = Paint.Style.FILL

        intCirclePaintR.color = Color.RED
        intCirclePaintR.isDither = true
        intCirclePaintR.isAntiAlias = true
        intCirclePaintR.strokeWidth = 10F
        intCirclePaintR.style = Paint.Style.FILL

        extCirclePaintR.color = Color.RED
        extCirclePaintR.isDither = true
        extCirclePaintR.isAntiAlias = true
        extCirclePaintR.strokeWidth = 8F
        extCirclePaintR.style = Paint.Style.STROKE
    }



    fun onCorners(corners: Corners, width: Int, height: Int) {
        cropMode = true
        ratioX = corners.size.width.div(width)
        ratioY = corners.size.height.div(height)
        tl = corners.tl
        tr = corners.tr
        br = corners.br
        bl = corners.bl
//        corners.log()
        resize()
        path.reset()
        path.close()
        //path2.reset()
        //path2.close()
        path2.reset()
        path2.close()
        path2.moveTo(tl.x.toFloat(), tl.y.toFloat())
        path2.lineTo(tr.x.toFloat(), tr.y.toFloat())
        path2.lineTo(br.x.toFloat(), br.y.toFloat())
        path2.lineTo(bl.x.toFloat(), bl.y.toFloat())
        path2.close()
        invalidate()
    }

    fun onCornersDetected(corners: Corners) {
        ratioX = corners.size.width.div(measuredWidth)
        ratioY = corners.size.height.div(measuredHeight)
        tl = corners.tl
        tr = corners.tr
        br = corners.br
        bl = corners.bl
//        corners.log()
        resize()
        path.reset()


        path.moveTo(tl.x.toFloat(), tl.y.toFloat())
        path.lineTo(tr.x.toFloat(), tr.y.toFloat())
        path.lineTo(br.x.toFloat(), br.y.toFloat())
        path.lineTo(bl.x.toFloat(), bl.y.toFloat())


        path.close()

        invalidate()
    }
    fun onMlCornersDetected(corners: Corners
                            //, preview: Bitmap, rect:Rect
    ) {
        ratioX = corners.size.width.div(measuredWidth)
        ratioY = corners.size.height.div(measuredHeight)
        tl = corners.tl
        tr = corners.tr
        br = corners.br
        bl = corners.bl
       /* val croppedBitmap = Bitmap.createBitmap(
            preview,
            (rect.left).toInt(),
            (rect.top).toInt(),
            ((rect.width()*1.0 ).toInt()),
            ((rect.height()*1.0 ).toInt()),
            null,
            false
        )
        val bitmap = Bitmap.createScaledBitmap(
            croppedBitmap, 1, 1, false
        )
        val pixels = IntArray(1)
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        val color: Int = pixels[0] // x + y * width
        var redBucket = 0
        var greenBucket = 0
        var blueBucket = 0
        redBucket += color shr 16 and 0xFF // Color.red

        greenBucket += color shr 8 and 0xFF // Color.greed

        blueBucket += color and 0xFF // Color.blue
        val pixelCount = 1
        fillPaint.color = Color.rgb(

            redBucket / pixelCount,
            greenBucket / pixelCount,
            blueBucket / pixelCount
        )*/
//        corners.log()
        resize()

        path2.reset()
        //ObjectAnimator.ofFloat(this, this.path2., View.Y, path).start();


        path2.moveTo(tl.x.toFloat(), tl.y.toFloat())
        path2.lineTo(tr.x.toFloat(), tr.y.toFloat())
        path2.lineTo(br.x.toFloat(), br.y.toFloat())
        path2.lineTo(bl.x.toFloat(), bl.y.toFloat())

        path2.close()
        invalidate()
    }

    fun onCornersNotDetected() {
        path.reset()
        invalidate()
    }
    fun onMlCornersNotDetected() {

        //pathAnimator.start()
        //val matrix = Matrix()
        //matrix.setScale(mscaleX, mscaleY)
        //path.transform(matrix)
        //path2.reset()

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas?.drawPath(path, fillPaint)
        canvas?.drawPath(path, rectPaint)
        canvas?.drawPath(path2, rectPaint)


        if (cropMode) {
            canvas?.drawCircle(tl.x.toFloat(), tl.y.toFloat(), 40F, extCirclePaint)
            canvas?.drawCircle(tr.x.toFloat(), tr.y.toFloat(), 40F, extCirclePaint)
            canvas?.drawCircle(bl.x.toFloat(), bl.y.toFloat(), 40F, extCirclePaint)
            canvas?.drawCircle(br.x.toFloat(), br.y.toFloat(), 40F, extCirclePaint)

            canvas?.drawCircle(tl.x.toFloat(), tl.y.toFloat(), 5F, intCirclePaint)
            canvas?.drawCircle(tr.x.toFloat(), tr.y.toFloat(), 5F, intCirclePaint)
            canvas?.drawCircle(bl.x.toFloat(), bl.y.toFloat(), 5F, intCirclePaint)
            canvas?.drawCircle(br.x.toFloat(), br.y.toFloat(), 5F, intCirclePaint)

        }
    }

    fun onTouch(event: MotionEvent?): Boolean {
        if (!cropMode) {
            return false
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                latestDownX = event.x
                latestDownY = event.y
                calculatePoint2Move(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                point2Move.x = (event.x - latestDownX) + point2Move.x
                point2Move.y = (event.y - latestDownY) + point2Move.y
                movePoints()
                latestDownY = event.y
                latestDownX = event.x
            }
        }
        return true
    }

    private fun calculatePoint2Move(downX: Float, downY: Float) {
        val points = listOf(tl, tr, br, bl)
        point2Move = points.minByOrNull { abs((it.x - downX).times(it.y - downY)) } ?: tl
    }

    private fun movePoints() {
        path.reset()
        path.close()
        path2.reset()
        path2.close()
        path2.moveTo(tl.x.toFloat(), tl.y.toFloat())
        path2.lineTo(tr.x.toFloat(), tr.y.toFloat())
        path2.lineTo(br.x.toFloat(), br.y.toFloat())
        path2.lineTo(bl.x.toFloat(), bl.y.toFloat())
        path2.close()
        invalidate()
    }

    private fun resize() {
        tl.x = tl.x.div(ratioX)
        tl.y = tl.y.div(ratioY)
        tr.x = tr.x.div(ratioX)
        tr.y = tr.y.div(ratioY)
        br.x = br.x.div(ratioX)
        br.y = br.y.div(ratioY)
        bl.x = bl.x.div(ratioX)
        bl.y = bl.y.div(ratioY)
    }
}
