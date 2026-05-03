package com.example.unimarketguajira.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.unimarketguajira.R

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        val btnSendCode = findViewById<Button>(R.id.btnSendCode)

        btnSendCode.setOnClickListener {
            startActivity(Intent(this, VerifyCodeActivity::class.java))
        }
    }
}