package com.example.sosproyecto

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.ContactoEmergencia
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ContactosEmergenciaActivity : AppCompatActivity() {
    private lateinit var btnNuevo: Button
    private lateinit var rvContactos: RecyclerView
    private val contactos = mutableListOf<ContactoEmergencia>()
    private lateinit var adapter: ContactoAdapter

    private val requestLocationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(this, getString(R.string.msg_permiso_ubicacion_denegado), Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos_emergencia)

        btnNuevo = findViewById(R.id.btnNuevoContacto)
        rvContactos = findViewById(R.id.rvContactos)
        rvContactos.layoutManager = LinearLayoutManager(this)

        adapter = ContactoAdapter(contactos) { contacto ->
            // llamado cuando se pulsa 'Enviar ubicación'
            sendLocationToContacto(contacto)
        }
        rvContactos.adapter = adapter

        loadContactos()

        btnNuevo.setOnClickListener {
            showAgregarContactoDialog()
        }
    }

    private fun loadContactos() {
        contactos.clear()
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val userIdStr = prefs.getString("user_id", null)
        val currentUserId = if (userIdStr != null) UUID.fromString(userIdStr) else null
        val json = prefs.getString("contactos_emergencia", null) ?: return
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = UUID.fromString(obj.getString("idContacto"))
            val idUsuario = UUID.fromString(obj.getString("idUsuario"))
            // Mostrar solo contactos del usuario logueado
            if (currentUserId != null && idUsuario != currentUserId) continue
            val nombre = obj.getString("nombre")
            val telefono = obj.getString("telefono")
            contactos.add(ContactoEmergencia(id, idUsuario, nombre, telefono))
        }
        adapter.notifyDataSetChanged()
    }

    private fun showAgregarContactoDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val nombreInput = EditText(this)
        nombreInput.hint = getString(R.string.hint_nombre_contacto)
        val telefonoInput = EditText(this)
        telefonoInput.hint = getString(R.string.hint_telefono_contacto)
        layout.addView(nombreInput)
        layout.addView(telefonoInput)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_nuevo_contacto))
            .setView(layout)
            .setPositiveButton(getString(R.string.btn_agregar)) { d, _ ->
                val nombre = nombreInput.text.toString().trim()
                val telefono = telefonoInput.text.toString().trim()
                if (nombre.isEmpty() || telefono.isEmpty()) {
                    Toast.makeText(this, getString(R.string.msg_completa_nombre_telefono), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
                val userIdStr = prefs.getString("user_id", null)
                val userId = if (userIdStr != null) UUID.fromString(userIdStr) else UUID.randomUUID()
                val contacto = ContactoEmergencia(UUID.randomUUID(), userId, nombre, telefono)
                saveContacto(contacto)
                loadContactos()
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar_btn)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun saveContacto(c: ContactoEmergencia) {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("contactos_emergencia", null)
        val arr = if (existing == null) JSONArray() else JSONArray(existing)
        val obj = JSONObject()
        obj.put("idContacto", c.idContacto.toString())
        obj.put("idUsuario", c.idUsuario.toString())
        obj.put("nombre", c.nombre)
        obj.put("telefono", c.telefono)
        arr.put(obj)
        prefs.edit().putString("contactos_emergencia", arr.toString()).apply()
    }

    private fun sendLocationToContacto(contacto: ContactoEmergencia) {
        requestLocationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                val mapsLink = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
                val smsText = getString(R.string.sms_mensaje_ubicacion, mapsLink)

                val uri = Uri.parse("smsto:${contacto.telefono}")
                val intent = Intent(Intent.ACTION_SENDTO, uri)
                intent.putExtra("sms_body", smsText)
                startActivity(intent)
                Toast.makeText(this, getString(R.string.msg_enviando_ubicacion), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.msg_no_archivo_seleccionado), Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, getString(R.string.msg_no_archivo_seleccionado), Toast.LENGTH_SHORT).show()
        }
    }
}
