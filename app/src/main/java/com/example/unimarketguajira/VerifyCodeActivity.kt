package com.example.unimarketguajira

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class VerifyCodeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verify_code)

        val btnVerify = findViewById<Button>(R.id.btnVerify)

        btnVerify.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
    }
}