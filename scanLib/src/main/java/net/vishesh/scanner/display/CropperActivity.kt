package net.vishesh.scanner.display



import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import net.vishesh.scanner.model.Corners
import net.vishesh.scanner.databinding.ActivityCropperBinding
import net.vishesh.scanner.plugins.outputDirectory
import net.vishesh.scanner.plugins.toByteArray
import net.vishesh.scanner.plugins.waitForLayout
import org.opencv.core.Point
import org.opencv.core.Size
import java.io.File
import java.io.FileOutputStream
import java.util.*


class CropperActivity : AppCompatActivity() {
    private lateinit var cropModel: CropperModel
    private lateinit var bitmapUri: Uri
    private lateinit var binding: ActivityCropperBinding
    private lateinit var points: List<Point>
    private lateinit var oldCorners: Corners


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //triggerFullscreen()
        binding = ActivityCropperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        /*window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)*/
        val extras = intent.extras
        if (extras != null) {
            bitmapUri = intent.extras?.getString("lastUri")?.toUri() ?: error("invalid uri")

            val list = if(intent.extras?.getSerializable("detectedCorners")!=null) intent.extras?.getSerializable("detectedCorners") as List<Pair<Double, Double>> else null
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, bitmapUri)

            if(list!=null){
                val sizeX = intent.extras?.getDouble("oldSizeWidth")
                val sizeY = intent.extras?.getDouble("oldSizeHeight")
                val size = Size(sizeX!!, sizeY!!)
                points = listOf(Point(list[0].first, list[0].second),Point(list[1].first, list[1].second),Point(list[2].first, list[2].second),Point(list[3].first, list[3].second))
                oldCorners = Corners(points, size)
                //oldCorners.log()
            }
            else{
                val emptyPoints : List<Point> = listOf(Point(0.0, 0.0),Point(0.0, 0.0),Point(0.0, 0.0),Point(0.0, 0.0),Point(0.0, 0.0))
                oldCorners = Corners(emptyPoints, Size(0.0,0.0))
            }



        }

        val cropModel: CropperModel by viewModels()

        // Picture taken from User
        cropModel.original.observe(this, {

            binding.cropPreview.setImageBitmap(cropModel.original.value)
            binding.cropWrap.visibility = View.VISIBLE

            // Wait for bitmap to be loaded on view, then draw corners
            binding.cropWrap.waitForLayout {
                binding.cropHud.onCorners(
                    corners = cropModel.corners.value ?: error("invalid Corners"),
                    height = binding.cropPreview.measuredHeight,
                    width = binding.cropPreview.measuredWidth
                )
            }
        })

        cropModel.bitmapToCrop.observe(this, {
            binding.cropResultPreview.setImageBitmap(cropModel.bitmapToCrop.value)
        })

        binding.closeResultPreview.setOnClickListener {
            closeActivity()
        }

        binding.closeCropPreview.setOnClickListener {
            closeActivity()
        }

        binding.confirmCropPreview.setOnClickListener {
            binding.cropWrap.visibility = View.GONE
            binding.cropHud.visibility = View.GONE
            loadBitmapFromView(binding.cropPreview)?.let { bitmapToCrop ->
                cropModel.onCornersAccepted(
                    bitmapToCrop, this
                )
            }
            cropModel.bitmapToCrop.observe(this) {
                it?.let { bitmap ->
                    val file = File(outputDirectory, "${UUID.randomUUID()}.jpg")
                    val outputStream = FileOutputStream(file)
                    outputStream.write(cropModel.bitmapToCrop.value?.toByteArray())
                    outputStream.close()

                    val resultIntent = Intent()
                    resultIntent.putExtra("croppedPath", file.absolutePath)
                    setResult(RESULT_OK, resultIntent)

                    finish()

                }}

            //binding.cropResultWrap.visibility = View.VISIBLE
        }

        binding.confirmCropResult.setOnClickListener {


        }

        binding.cropPreview.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            view.performClick()
            binding.cropHud.onTouch(motionEvent)
        }

        this.cropModel = cropModel
    }

    override fun onResume() {
        super.onResume()
        cropModel.onViewCreated(bitmapUri, contentResolver,oldCorners, this)
    }

    private fun closeActivity() {
        this.setResult(Activity.RESULT_CANCELED)
        finish()
    }
}

private fun loadBitmapFromView(v: View): Bitmap? {
    val b = Bitmap.createBitmap(
        v.measuredWidth,
        v.measuredHeight,
        Bitmap.Config.ARGB_8888
    )
    val c = Canvas(b)
    v.layout(v.left, v.top, v.right, v.bottom)
    v.draw(c)
    return b
}
