package net.vishesh.scanner.display

import android.widget.Toast
import net.vishesh.scanner.R
import net.vishesh.scanner.errors.NullCorners

class ScannerActivity : BaseScannerActivity() {
    override fun onError(throwable: Throwable) {
        when (throwable) {
            is NullCorners -> Toast.makeText(
                this,
                R.string.null_corners, Toast.LENGTH_LONG
            )
                .show()
            else -> Toast.makeText(this, throwable.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDocumentAccepted(bitmap: String) {
    }

    override fun onClose() {
        finish()
    }
}
