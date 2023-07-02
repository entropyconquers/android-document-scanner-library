package net.vishesh.scanner.display

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import net.vishesh.scanner.R
import net.vishesh.scanner.model.OpenCVLoader
import net.vishesh.scanner.databinding.ActivityScannerBinding
import net.vishesh.scanner.plugins.outputDirectory

abstract class BaseScannerActivity : AppCompatActivity() {
    private lateinit var viewModel: ScannerViewModel
    private lateinit var binding: ActivityScannerBinding

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmapUri =
                    result.data?.extras?.getString("croppedPath") ?: error("invalid path")

                //val image = File(bitmapUri)
                //val bmOptions = BitmapFactory.Options()
                //val bitmap = BitmapFactory.decodeFile(image.absolutePath, bmOptions)
                onDocumentAccepted(bitmapUri)

                //image.delete()
            } else {
                viewModel.onViewCreated(OpenCVLoader(this), this, binding.viewFinder)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        /*window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)*/
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //triggerFullscreen()

        binding = ActivityScannerBinding.inflate(layoutInflater)

        setContentView(binding.root)
        binding.root.getLayoutTransition()
            .enableTransitionType(LayoutTransition.CHANGING);


        val viewModel: ScannerViewModel by viewModels()
        var message = binding.message
        viewModel.isBusy.observe(this, { isBusy ->
            binding.progress.visibility = if (isBusy) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        })

        viewModel.lastUri.observe(this) {
            val intent = Intent(this, CropperActivity::class.java)
            intent.putExtra("lastUri", it.toString())
            var bundle: Bundle

            intent.putExtra(
                "detectedCorners",
                if (viewModel.corners.value != null) ArrayList(viewModel.corners.value!!.pointsToPair()) else null
            )
            var oldSizeWidth: Double? = if (viewModel.corners.value != null) viewModel.corners.value!!.size.width else null
            intent.putExtra(
                "oldSizeWidth",
                oldSizeWidth
            )
            var oldSizeHeight: Double? = if (viewModel.corners.value != null) viewModel.corners.value!!.size.height else null
            intent.putExtra(
                "oldSizeHeight",
                oldSizeHeight
            )

            resultLauncher.launch(intent)
        }

        viewModel.errors.observe(this, {
            onError(it)
            Log.e(ScannerActivity::class.java.simpleName, it.message, it)
        })
        viewModel.mlCorners.observe(this) {
            var counter = 0;
            var lastPropChange = R.string.biv
            Log.d(ScannerActivity::class.java.simpleName, "mlCorners: ${getResources().getString(lastPropChange)}")
            it?.let { cornersX ->

                viewModel.corners.observe(this) {
                    it?.let { corners ->
                        message.visibility = View.VISIBLE
                        if(corners.calculateSize().width > 0.7 * binding.root.width) {
                            if(lastPropChange != R.string.farther) {
                                counter = 1
                                lastPropChange = R.string.farther
                            }
                            else {
                                counter++
                            }
                            if(counter > 5) {
                                message.setText(R.string.farther)
                                lastPropChange = R.string.farther
                                counter = 0
                            }

                        }
                        else if(corners.calculateSize().width < 0.4 * binding.root.width) {
                            if(lastPropChange != R.string.closer) {
                                counter = 1
                                lastPropChange = R.string.closer
                            }
                            else {
                                counter++
                            }
                            if(counter > 5) {
                                message.setText(R.string.closer)
                                lastPropChange = R.string.closer
                                counter = 0
                            }

                        }
                        else{
                            if(corners.isTooSteep()){
                                if(lastPropChange != R.string.angle) {
                                    counter = 1
                                    lastPropChange = R.string.angle
                                }
                                else {
                                    counter++
                                }
                                if(counter > 5) {
                                    message.setText(R.string.angle)
                                    lastPropChange = R.string.angle
                                    counter = 0
                                }
                            }
                            else{
                                if(lastPropChange != R.string.ready) {
                                    counter = 1
                                    lastPropChange = R.string.ready
                                }
                                else {
                                    counter++
                                }
                                if(counter > 5) {
                                    message.setText(R.string.ready)
                                    lastPropChange = R.string.ready
                                    counter = 0
                                }

                            }

                        }
                    } ?: run {
                        message.visibility = View.VISIBLE
                        if(cornersX.calculateSize().width > 0.8 * binding.root.width) {
                            if (lastPropChange != R.string.farther) {
                                counter = 1
                                lastPropChange = R.string.farther

                            } else {
                                counter++
                            }
                            if (counter > 10) {
                                message.setText(R.string.farther)
                                lastPropChange = R.string.farther
                                counter = 0
                            }

                        }
                        else if(cornersX.calculateSize().width < 0.4 * binding.root.width) {
                            if (lastPropChange != R.string.closer) {
                                counter = 1
                                lastPropChange = R.string.closer
                            } else {
                                counter++
                            }
                            if (counter > 10) {
                                message.setText(R.string.closer)
                                lastPropChange = R.string.closer
                                counter = 0
                            }

                        }
                        else{
                            if (lastPropChange != R.string.finding) {
                                counter = 1
                                lastPropChange = R.string.finding
                            } else {
                                counter++
                            }
                            if (counter > 10) {
                                message.setText(R.string.finding)
                                lastPropChange = R.string.finding
                                counter = 0
                            }
                            //message.visibility = View.INVISIBLE
                        }

                    }
                }
            } ?: run {
                if (lastPropChange != R.string.dnf) {
                    counter = 1
                    lastPropChange = R.string.dnf
                } else {
                    counter++
                }
                if (counter > 10) {
                    message.setText(R.string.dnf)
                    lastPropChange = R.string.dnf
                    counter = 0
                }
            }
        }
        viewModel.corners.observe(this) {
            it?.let { corners ->
                binding.hud.onCornersDetected(corners)
            } ?: run {
                binding.hud.onCornersNotDetected()
            }
        }
        viewModel.mlCorners.observe(this) {
            it?.let { corners ->
                binding.hud.onMlCornersDetected(corners)
            } ?: run {
                binding.hud.onMlCornersNotDetected()
            }
        }

        viewModel.flashStatus.observe(this, { status ->
            binding.flashToggle.setImageResource(
                when (status) {
                    FlashStatus.ON -> R.drawable.ic_flash_on
                    FlashStatus.OFF -> R.drawable.ic_flash_off
                    else -> R.drawable.ic_flash_off
                }
            )
        })

        binding.flashToggle.setOnClickListener {
            viewModel.onFlashToggle()
        }

        binding.shutter.setOnClickListener {
            viewModel.onTakePicture(outputDirectory, this)
        }

        binding.closeScanner.setOnClickListener {
            closePreview()
        }
        this.viewModel = viewModel
    }

    override fun onResume() {
        super.onResume()
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        viewModel.onViewCreated(OpenCVLoader(this), this, binding.viewFinder)
    }

    private fun closePreview() {
        binding.rootView.visibility = View.GONE
        viewModel.onClosePreview()
        finish()
    }

    abstract fun onError(throwable: Throwable)
    abstract fun onDocumentAccepted(bitmap: String)
    abstract fun onClose()
}


