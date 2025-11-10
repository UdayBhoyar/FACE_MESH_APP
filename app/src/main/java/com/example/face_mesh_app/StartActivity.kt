package com.example.face_mesh_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val startBtn = findViewById<Button>(R.id.btn_start)
        startBtn.setOnClickListener {
            // When user taps Start, proceed to the existing MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
