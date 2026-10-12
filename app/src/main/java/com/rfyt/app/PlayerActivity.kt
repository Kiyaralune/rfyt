package com.rfyt.app

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rfyt.app.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var videoId: String = ""
    private var videoTitle: String = ""
    private var videoAuthor: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoId = intent.getStringExtra("VIDEO_ID") ?: ""
        videoTitle = intent.getStringExtra("VIDEO_TITLE") ?: "YouTube Video"
        videoAuthor = intent.getStringExtra("VIDEO_AUTHOR") ?: "Creator"

        binding.tvPlayerTitle.text = videoTitle
        binding.tvPlayerChannel.text = videoAuthor

        setupPlayer()

        binding.btnPip.setOnClickListener {
            enterPipMode()
        }

        binding.btnDownload.setOnClickListener {
            val downloadUrl = "https://www.y2mate.com/youtube/$videoId"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
            startActivity(browserIntent)
            Toast.makeText(this, "Membuka opsi download MP4...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPlayer() {
        val webView = binding.webViewPlayer
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress >= 90) {
                    binding.playerLoading.visibility = View.GONE
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {}

        // Embed YouTube video with ad-blocking parameters & autoplay
        val embedHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                    iframe { width: 100%; height: 100%; border: none; }
                </style>
            </head>
            <body>
                <iframe 
                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0&iv_load_policy=3&playsinline=1"
                    allow="autoplay; encrypted-media; picture-in-picture"
                    allowfullscreen>
                </iframe>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", null)
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } else {
            Toast.makeText(this, "PIP Mode didukung di Android 8.0+", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPipMode()
        }
    }

    override fun onDestroy() {
        binding.webViewPlayer.destroy()
        super.onDestroy()
    }
}
