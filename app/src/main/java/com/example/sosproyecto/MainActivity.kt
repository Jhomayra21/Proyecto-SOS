package com.example.sosproyecto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.etLoginEmail)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnLoginMain)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoToRegisterMain)

        btnLogin.setOnClickListener {
            val emailInput = etEmail.text.toString().trim()
            val passwordInput = etPassword.text.toString()
            if (emailInput.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_ingresa_email), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (passwordInput.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_ingresa_password), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("sos_prefs", MODE_PRIVATE)
            val savedEmail = prefs.getString("user_email", null)
            val savedPassword = prefs.getString("user_password", null)

            if (savedEmail != null && savedPassword != null && savedEmail == emailInput && savedPassword == passwordInput) {
                Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MapaActivity::class.java))
            } else {
                Toast.makeText(this, getString(R.string.msg_credenciales_invalidas), Toast.LENGTH_LONG).show()
            }
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, UsuariosActivity::class.java))
        }
    }
}