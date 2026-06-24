package com.example.unimarketguajira.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.unimarketguajira.R
import com.example.unimarketguajira.models.User
import com.example.unimarketguajira.services.UserManager
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        val tilFullName = findViewById<TextInputLayout>(R.id.tilFullName)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        tvGoToLogin.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            val name = tilFullName.editText?.text.toString().trim()
            val email = tilEmail.editText?.text.toString().trim()
            val pass = tilPassword.editText?.text.toString()
            val confirmPass = tilConfirmPassword.editText?.text.toString()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Validar dominio institucional
            if (!email.endsWith("@uniguajira.edu.co")) {
                Toast.makeText(this, "Solo se permiten correos institucionales de Uniguajira.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 2. Verificar coincidencia de contraseñas
            if (pass != confirmPass) {
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Contraseña mínima de 8 caracteres
            if (pass.length < 8) {
                Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newUser = User(name, email, pass)
            lifecycleScope.launch {
                try {
                    val success = UserManager.registerUser(this@RegisterActivity, newUser)
                    if (success) {
                        Toast.makeText(this@RegisterActivity, "Registro exitoso. Se ha enviado un correo de verificación.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, "El usuario ya existe.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    val errorMsg = e.message ?: "Error al registrar el usuario"
                    if (errorMsg.contains("Collision", ignoreCase = true) || errorMsg.contains("already in use", ignoreCase = true)) {
                        Toast.makeText(this@RegisterActivity, "El usuario ya existe.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}