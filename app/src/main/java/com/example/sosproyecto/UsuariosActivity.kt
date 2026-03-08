package com.example.sosproyecto

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sosproyecto.model.Usuario
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsuariosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usuarios)

        val etNombre = findViewById<EditText>(R.id.etNombreUsuario)
        val etEmail = findViewById<EditText>(R.id.etEmailUsuario)
        val etTelefono = findViewById<EditText>(R.id.etTelefonoUsuario)
        val etPassword = findViewById<EditText>(R.id.etPasswordUsuario)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarUsuario)

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val password = etPassword.text.toString()

            if (nombre.isEmpty() || email.isEmpty() || telefono.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_completa_campos), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val fechaIso = formatter.format(Date())

            val nuevoUsuario = Usuario(
                idUsuario = UUID.randomUUID(),
                nombre = nombre,
                email = email,
                telefono = telefono,
                password = password,
                fechaRegistro = fechaIso,
                nivelConfianza = 0
            )

            val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("user_id", nuevoUsuario.idUsuario.toString())
            editor.putString("user_nombre", nuevoUsuario.nombre)
            editor.putString("user_email", nuevoUsuario.email)
            editor.putString("user_telefono", nuevoUsuario.telefono)
            editor.putString("user_password", nuevoUsuario.password)
            editor.putString("user_fecha", nuevoUsuario.fechaRegistro)
            editor.putInt("user_nivel", nuevoUsuario.nivelConfianza)
            editor.apply()

            Toast.makeText(this, getString(R.string.btn_registrar_exito), Toast.LENGTH_LONG).show()

            etNombre.text.clear()
            etEmail.text.clear()
            etTelefono.text.clear()
            etPassword.text.clear()
        }
    }
}
