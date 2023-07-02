package com.entropylabs.dscantest

import android.content.ContentValues
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import net.vishesh.scanner.display.BaseScannerActivity
import net.vishesh.scanner.errors.NullCorners


class MainActivity : BaseScannerActivity() {
    override fun onError(throwable: Throwable) {
        Log.d(ContentValues.TAG, "ERROR A")
        when (throwable) {
            is NullCorners -> Toast.makeText(
                this,
                "R.string.null_corners", Toast.LENGTH_LONG
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