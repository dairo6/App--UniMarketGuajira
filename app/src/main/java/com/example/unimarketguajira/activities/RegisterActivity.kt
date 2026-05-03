package com.example.unimarketguajira.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
            val name = tilFullName.editText?.text.toString()
            val email = tilEmail.editText?.text.toString()
            val pass = tilPassword.editText?.text.toString()
            val confirmPass = tilConfirmPassword.editText?.text.toString()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newUser = User(name, email, pass)
            if (UserManager.registerUser(this, newUser)) {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "El usuario ya existe", Toast.LENGTH_SHORT).show()
            }
        }
    }
}