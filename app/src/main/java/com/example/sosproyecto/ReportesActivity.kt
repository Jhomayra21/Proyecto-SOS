package com.example.sosproyecto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.Reporte
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ReportesActivity : AppCompatActivity() {
    private lateinit var rvReportes: RecyclerView
    private lateinit var btnNuevoReporte: Button
    private val reportes = mutableListOf<Reporte>()
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    private lateinit var selectLocationLauncher: ActivityResultLauncher<Intent>
    private lateinit var evidenciasLauncher: ActivityResultLauncher<Intent>
    private var pendingReporteId: UUID? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        rvReportes = findViewById(R.id.rvReportes)
        btnNuevoReporte = findViewById(R.id.btnNuevoReporte)
        rvReportes.layoutManager = LinearLayoutManager(this)

        selectLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                selectedLat = result.data!!.getDoubleExtra("lat", 0.0)
                selectedLng = result.data!!.getDoubleExtra("lng", 0.0)
                Toast.makeText(this, getString(R.string.ubicacion_format).format(selectedLat, selectedLng), Toast.LENGTH_SHORT).show()
            }
        }

        evidenciasLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val json = result.data!!.getStringExtra("evidencias_json")
                if (pendingReporteId != null && !json.isNullOrEmpty()) {
                    saveEvidenciasForReporte(pendingReporteId!!, json)
                    Toast.makeText(this, "Evidencias agregadas", Toast.LENGTH_SHORT).show()
                    pendingReporteId = null
                    loadReportes()
                    rvReportes.adapter = ReporteAdapter(reportes, loadTiposMap()) { reporte ->
                        val intent = Intent(this, EvidenciasActivity::class.java).putExtra("idReporte", reporte.idReporte.toString())
                        evidenciasLauncher.launch(intent)
                    }
                }
            }
        }

        loadReportes()
        rvReportes.adapter = ReporteAdapter(reportes, loadTiposMap()) { reporte ->
            // abrir EvidenciasActivity para este reporte
            val intent = Intent(this, EvidenciasActivity::class.java).putExtra("idReporte", reporte.idReporte.toString())
            evidenciasLauncher.launch(intent)
        }

        btnNuevoReporte.setOnClickListener {
            showCrearReporteDialog()
        }
    }

    private fun loadTiposMap(): Map<Int, String> {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("tipos_incidente", null) ?: return emptyMap()
        val arr = JSONArray(json)
        val map = mutableMapOf<Int, String>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            map[obj.getInt("idTipo")] = obj.getString("nombreTipo")
        }
        return map
    }

    private fun loadReportes() {
        reportes.clear()
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val userIdStr = prefs.getString("user_id", null)
        val userId = if (userIdStr != null) UUID.fromString(userIdStr) else null
        val json = prefs.getString("reportes", null) ?: return
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val idReporte = UUID.fromString(obj.getString("idReporte"))
            val idUsuario = if (obj.has("idUsuario") && !obj.isNull("idUsuario")) UUID.fromString(obj.getString("idUsuario")) else null

            if (userId != null && idUsuario != null && idUsuario != userId) continue
            val idTipo = obj.getInt("idTipo")
            val descripcion = if (obj.has("descripcion") && !obj.isNull("descripcion")) obj.getString("descripcion") else null
            val lat = obj.getDouble("latitud")
            val lng = obj.getDouble("longitud")
            val fecha = obj.getString("fechaReporte")
            val nivel = obj.getInt("nivelConfianza")
            val estado = obj.getString("estadoVerificacion")
            val anon = obj.getBoolean("esAnonimo")
            reportes.add(Reporte(idReporte, idUsuario, idTipo, descripcion, lat, lng, fecha, nivel, estado, anon))
        }
    }

    private fun showCrearReporteDialog() {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val tiposJson = prefs.getString("tipos_incidente", null)
        val tiposList = mutableListOf<Pair<Int, String>>()
        if (tiposJson != null) {
            val arr = JSONArray(tiposJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                tiposList.add(Pair(obj.getInt("idTipo"), obj.getString("nombreTipo")))
            }
        }

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(16,16,16,16)

        val descripcionInput = EditText(this)
        descripcionInput.hint = getString(R.string.descripcion_hint)
        container.addView(descripcionInput)

        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposList.map { it.second })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        container.addView(spinner)

        val btnElegirUbic = Button(this)
        btnElegirUbic.text = getString(R.string.elegir_ubicacion_btn)
        container.addView(btnElegirUbic)

        val chkAnonimo = CheckBox(this)
        chkAnonimo.text = getString(R.string.reportar_anonimo)
        container.addView(chkAnonimo)

        btnElegirUbic.setOnClickListener {
            selectedLat = null; selectedLng = null
            val intent = Intent(this, MapaActivity::class.java).putExtra("select_mode", true)
            selectLocationLauncher.launch(intent)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.nuevo_reporte_title))
            .setView(container)
            .setPositiveButton(getString(R.string.crear_btn)) { d, _ ->
                val descripcion = descripcionInput.text.toString().trim()
                val tipoIndex = spinner.selectedItemPosition
                if (tiposList.isEmpty() || tipoIndex < 0) {
                    Toast.makeText(this, getString(R.string.no_tipos_disponibles), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val idTipo = tiposList[tipoIndex].first

                val lat = selectedLat
                val lng = selectedLng
                if (lat == null || lng == null) {
                    Toast.makeText(this, getString(R.string.selecciona_ubicacion_mapa), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val userIdStr = prefs.getString("user_id", null)
                val userId = if (userIdStr != null) UUID.fromString(userIdStr) else null
                val nivel = prefs.getInt("user_nivel", 0)

                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val fechaIso = formatter.format(Date())

                val nuevoReporte = Reporte(UUID.randomUUID(), userId, idTipo, descripcion, lat, lng, fechaIso, nivel, "pendiente", chkAnonimo.isChecked)

                saveReporte(nuevoReporte)

                pendingReporteId = nuevoReporte.idReporte
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.reporte_creado))
                    .setMessage(getString(R.string.msg_debes_agregar_evidencia))
                    .setPositiveButton(getString(R.string.agregar_evidencias)) { _, _ ->
                        val intent = Intent(this, EvidenciasActivity::class.java).putExtra("idReporte", nuevoReporte.idReporte.toString())
                        evidenciasLauncher.launch(intent)
                    }
                    .setNegativeButton(getString(R.string.cancelar_btn), null)
                    .show()

                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar_btn)) { d, _ -> d.dismiss() }
            .create()
        dialog.show()
    }

    private fun saveEvidenciasForReporte(idReporte: UUID, evidenciasJson: String) {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("evidencias", null)
        val arrExisting = if (existing == null) JSONArray() else JSONArray(existing)

        val arrNew = JSONArray(evidenciasJson)
        for (i in 0 until arrNew.length()) {
            val obj = arrNew.getJSONObject(i)
            obj.put("idReporte", idReporte.toString())
            arrExisting.put(obj)
        }
        prefs.edit().putString("evidencias", arrExisting.toString()).apply()
    }

    private fun saveReporte(r: Reporte) {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("reportes", null)
        val arr = if (json == null) JSONArray() else JSONArray(json)
        val obj = JSONObject()
        obj.put("idReporte", r.idReporte.toString())
        obj.put("idUsuario", r.idUsuario?.toString())
        obj.put("idTipo", r.idTipo)
        obj.put("descripcion", r.descripcion)
        obj.put("latitud", r.latitud)
        obj.put("longitud", r.longitud)
        obj.put("fechaReporte", r.fechaReporte)
        obj.put("nivelConfianza", r.nivelConfianza)
        obj.put("estadoVerificacion", r.estadoVerificacion)
        obj.put("esAnonimo", r.esAnonimo)
        arr.put(obj)
        prefs.edit().putString("reportes", arr.toString()).apply()
        Toast.makeText(this, getString(R.string.reporte_creado), Toast.LENGTH_SHORT).show()
    }
}
