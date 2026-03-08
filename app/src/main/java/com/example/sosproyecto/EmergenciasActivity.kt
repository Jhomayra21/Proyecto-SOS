package com.example.sosproyecto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.Emergencia
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class EmergenciasActivity : AppCompatActivity() {
    private lateinit var tvEstadoActual: TextView
    private lateinit var tvUbicacionActual: TextView
    private lateinit var btnToggle: View
    private lateinit var rvEmergencias: RecyclerView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var currentEstado: String = "desactivado"
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) getLastLocation() else Toast.makeText(this, getString(R.string.msg_permiso_ubicacion_denegado), Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergencias)

        tvEstadoActual = findViewById(R.id.tvEstadoActual)
        tvUbicacionActual = findViewById(R.id.tvUbicacionActual)
        btnToggle = findViewById(R.id.btnToggleEmergencia)
        rvEmergencias = findViewById(R.id.rvEmergencias)

        rvEmergencias.layoutManager = LinearLayoutManager(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnToggle.setOnClickListener {
            toggleEmergencia()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        loadEmergencias()
    }

    private fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    tvUbicacionActual.text = String.format(Locale.getDefault(), "Lat: %.6f , Lng: %.6f", currentLat, currentLng)
                } else {
                    tvUbicacionActual.text = getString(R.string.latlng_placeholder)
                }
            }
        } catch (e: SecurityException) {
            // ignore
        }
    }

    private fun toggleEmergencia() {
        if (currentEstado == "desactivado") {
            currentEstado = "activa"
            tvEstadoActual.text = getString(R.string.estado_activa)

            val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
            val userIdStr = prefs.getString("user_id", null)
            val userId = if (userIdStr != null) UUID.fromString(userIdStr) else UUID.randomUUID()
            val lat = currentLat ?: 0.0
            val lng = currentLng ?: 0.0
            val fecha = dateFormatter.format(Date())
            val emergencia = Emergencia(UUID.randomUUID(), userId, lat, lng, fecha, "activa")
            saveEmergencia(emergencia)
            loadEmergencias()
            Toast.makeText(this, getString(R.string.msg_emergencia_activada), Toast.LENGTH_SHORT).show()
        } else {
            currentEstado = "desactivado"
            tvEstadoActual.text = getString(R.string.estado_desactivado)
            Toast.makeText(this, getString(R.string.msg_emergencia_desactivada), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveEmergencia(e: Emergencia) {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("emergencias", null)
        val arr = if (existing == null) JSONArray() else JSONArray(existing)
        val obj = JSONObject()
        obj.put("idEmergencia", e.idEmergencia.toString())
        obj.put("idUsuario", e.idUsuario.toString())
        obj.put("latitud", e.latitud)
        obj.put("longitud", e.longitud)
        obj.put("fechaEmergencia", e.fechaEmergencia)
        obj.put("estado", e.estado)
        arr.put(obj)
        prefs.edit().putString("emergencias", arr.toString()).apply()
    }

    private fun loadEmergencias() {
        val list = mutableListOf<Emergencia>()
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val userIdStr = prefs.getString("user_id", null)
        val currentUserId = if (userIdStr != null) UUID.fromString(userIdStr) else null
        val json = prefs.getString("emergencias", null) ?: "[]"
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = UUID.fromString(obj.getString("idEmergencia"))
            val idUsuario = UUID.fromString(obj.getString("idUsuario"))
            if (currentUserId != null && idUsuario != currentUserId) continue
            val lat = obj.getDouble("latitud")
            val lng = obj.getDouble("longitud")
            val fecha = obj.getString("fechaEmergencia")
            val estado = obj.getString("estado")
            list.add(Emergencia(id, idUsuario, lat, lng, fecha, estado))
        }
        rvEmergencias.adapter = EmergenciaAdapter(list)
    }
}
