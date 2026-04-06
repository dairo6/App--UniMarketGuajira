package com.example.unimarketguajira

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        tvGoToLogin.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            // Lógica de registro
            finish()
        }
    }
}