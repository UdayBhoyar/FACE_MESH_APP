package com.example.face_mesh_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.Looper

class SplashActivity : AppCompatActivity() {
    private val splashDelay = 1000L
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        // After the splash, navigate to the Start screen (user presses Start to continue)
        startActivity(Intent(this, StartActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        handler.postDelayed(runnable, splashDelay)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}
