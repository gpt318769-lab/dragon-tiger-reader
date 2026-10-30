package com.example.dragontigerreader

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val captureRequest = 7101
    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
        }
        root.addView(TextView(this).apply {
            text = "🐉 Dragon Tiger Floating Reader"
            textSize = 23f
        })
        root.addView(TextView(this).apply {
            text = "Screen reading is user-started. Keep Phoenix visible after granting capture permission. Recognition is experimental and may miss or misread cards."
            textSize = 15f
            setPadding(0, 18, 0, 20)
        })
        root.addView(Button(this).apply {
            text = "1. Allow display over other apps"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                } else toast("Overlay permission is enabled.")
            }
        })
        root.addView(Button(this).apply {
            text = "2. Start screen reader"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    toast("Enable display over other apps first.")
                } else {
                    startActivityForResult(projectionManager.createScreenCaptureIntent(), captureRequest)
                }
            }
        })
        root.addView(Button(this).apply {
            text = "Stop floating reader"
            setOnClickListener {
                stopService(Intent(this@MainActivity, CaptureOverlayService::class.java))
            }
        })
        setContentView(root)
    }

    @Deprecated("Deprecated by platform API; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureRequest && resultCode == RESULT_OK && data != null) {
            val service = Intent(this, CaptureOverlayService::class.java).apply {
                putExtra(CaptureOverlayService.EXTRA_RESULT_CODE, resultCode)
                putExtra(CaptureOverlayService.EXTRA_RESULT_DATA, data)
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(service)
            else startService(service)
            toast("Screen capture started. Switch to Phoenix.")
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
