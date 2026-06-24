package com.example.unimarketguajira.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unimarketguajira.R
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val btnSendCode = findViewById<Button>(R.id.btnSendCode)

        btnSendCode.setOnClickListener {
            val email = tilEmail.editText?.text.toString().trim()

            if (email.isEmpty() || !email.endsWith("@uniguajira.edu.co")) {
                Toast.makeText(this, "Debes ingresar un correo institucional válido.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            btnSendCode.isEnabled = false
            lifecycleScope.launch {
                try {
                    val auth = FirebaseAuth.getInstance()
                    auth.sendPasswordResetEmail(email).await()
                    Toast.makeText(this@ForgotPasswordActivity, "Se ha enviado un enlace de recuperación a tu correo institucional.", Toast.LENGTH_LONG).show()
                    finish()
                } catch (e: Exception) {
                    btnSendCode.isEnabled = true
                    Toast.makeText(this@ForgotPasswordActivity, e.message ?: "Error al enviar el enlace de recuperación.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}