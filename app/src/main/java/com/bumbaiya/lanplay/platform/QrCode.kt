package com.bumbaiya.lanplay.platform

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCode {
    /** Draws [text] as a QR code. Black on white at any theme, because scanners need the contrast. */
    fun generate(text: String, sizePx: Int = 768): ImageBitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, mapOf(EncodeHintType.MARGIN to 1))
        val bmp = createBitmap(sizePx, sizePx)
        for (x in 0 until sizePx) for (y in 0 until sizePx) bmp[x, y] =
            if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        return bmp.asImageBitmap()
    }

    /**
     * Opens Google's scanner screen (it brings its own camera UI, so the app needs no camera
     * permission). Calls [onResult] with the scanned text, or [onError] with a message.
     */
    fun scan(context: Context, onResult: (String) -> Unit, onError: (String) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { code -> code.rawValue?.let(onResult) ?: onError("Empty QR code") }
            .addOnCanceledListener { }
            .addOnFailureListener { onError(it.message ?: "Scanner not available") }
    }
}
