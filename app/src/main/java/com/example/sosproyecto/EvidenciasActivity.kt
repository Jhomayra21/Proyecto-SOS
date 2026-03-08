package com.example.sosproyecto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.Evidencia
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class EvidenciasActivity : AppCompatActivity() {
    private lateinit var btnAgregar: Button
    private lateinit var rvEvidencias: RecyclerView
    private val evidencias = mutableListOf<Evidencia>()
    private var idReporteExtra: UUID? = null

    private lateinit var pickFileLauncher: ActivityResultLauncher<Array<String>>
    private var pendingSelectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evidencias)

        btnAgregar = findViewById(R.id.btnNuevaEvidencia)
        rvEvidencias = findViewById(R.id.rvEvidencias)
        rvEvidencias.layoutManager = LinearLayoutManager(this)
        rvEvidencias.adapter = EvidenciaAdapter(evidencias)

        intent.getStringExtra("idReporte")?.let {
            try {
                idReporteExtra = UUID.fromString(it)
            } catch (_: Exception) {  }
        }

        if (idReporteExtra != null) {
            loadEvidenciasForReporte(idReporteExtra!!)
        }

        pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                pendingSelectedUri = uri
                val name = uri.lastPathSegment ?: uri.toString()
                Toast.makeText(this, getString(R.string.msg_archivo_seleccionado, name), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.msg_no_archivo_seleccionado), Toast.LENGTH_SHORT).show()
            }
        }

        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val reportesJson = prefs.getString("reportes", null)
        var reportesList = listOf<Pair<UUID, String>>()
        if (!reportesJson.isNullOrEmpty()) {
            val arr = JSONArray(reportesJson)
            val tmp = mutableListOf<Pair<UUID, String>>()
            val userIdStr = prefs.getString("user_id", null)
            val currentUserId = if (userIdStr != null) UUID.fromString(userIdStr) else null
             for (i in 0 until arr.length()) {
                 val obj = arr.getJSONObject(i)
                 val id = UUID.fromString(obj.getString("idReporte"))
                val idUsuario = if (obj.has("idUsuario") && !obj.isNull("idUsuario")) UUID.fromString(obj.getString("idUsuario")) else null

                if (currentUserId != null && idUsuario != null && idUsuario != currentUserId) continue
                 val desc = obj.optString("descripcion")
                 tmp.add(Pair(id, "#${id.toString().take(8)} - ${desc}"))
             }
             reportesList = tmp
         }

        if (reportesList.isNotEmpty() && idReporteExtra == null) {
            val spinner = findViewById<Spinner>(R.id.spinnerReportesEvidencias)
            spinner.visibility = android.view.View.VISIBLE
            val options = mutableListOf("-- ${getString(R.string.agregar_evidencias)} --")
            options.addAll(reportesList.map { it.second })
            val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            spinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                    idReporteExtra = if (position == 0) null else reportesList[position - 1].first
                    if (idReporteExtra != null) {
                        loadEvidenciasForReporte(idReporteExtra!!)
                    } else {
                        evidencias.clear()
                        rvEvidencias.adapter?.notifyDataSetChanged()
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                    // no-op
                }
            })
        }

        btnAgregar.setOnClickListener {
            showAgregarDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        idReporteExtra?.let { loadEvidenciasForReporte(it) }
    }

    private fun loadEvidenciasForReporte(idReporte: UUID) {
        evidencias.clear()
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("evidencias", null) ?: return
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val rid = UUID.fromString(obj.getString("idReporte"))
            if (rid == idReporte) {
                val idE = UUID.fromString(obj.getString("idEvidencia"))
                val tipo = if (obj.has("tipoArchivo") && !obj.isNull("tipoArchivo")) obj.getString("tipoArchivo") else null
                val url = if (obj.has("urlArchivo") && !obj.isNull("urlArchivo")) obj.getString("urlArchivo") else null
                evidencias.add(Evidencia(idE, rid, tipo, url))
            }
        }
        rvEvidencias.adapter?.notifyDataSetChanged()
    }

    private fun showAgregarDialog() {
        val tipoInput = EditText(this)
        tipoInput.hint = getString(R.string.hint_tipo_evidencia)
        val urlInput = EditText(this)
        urlInput.hint = getString(R.string.hint_url_evidencia)

        val selectFileBtn = Button(this)
        selectFileBtn.text = getString(R.string.elegir_ubicacion_btn)
        selectFileBtn.setOnClickListener {
            pickFileLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
        }

        val layout = LinearLayoutCompat(this)
        layout.orientation = LinearLayoutCompat.VERTICAL
        layout.setPadding(16,16,16,16)
        layout.addView(tipoInput)
        layout.addView(urlInput)
        layout.addView(selectFileBtn)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.agregar_evidencia_title))
            .setView(layout)
            .setPositiveButton(getString(R.string.accion_crear)) { d, _ ->
                val tipo = tipoInput.text.toString().trim().ifEmpty { null }
                var url = urlInput.text.toString().trim().ifEmpty { null }
                if (pendingSelectedUri != null) {
                    url = pendingSelectedUri.toString()
                }
                if (tipo.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.msg_ingresa_tipo), Toast.LENGTH_SHORT).show()
                } else {
                    val reporteId = idReporteExtra ?: UUID.randomUUID()
                    val ev = Evidencia(UUID.randomUUID(), reporteId, tipo, url)
                    if (idReporteExtra != null) {
                        saveEvidenciaToPrefs(ev)
                        loadEvidenciasForReporte(idReporteExtra!!)
                    } else {
                        evidencias.add(ev)
                        rvEvidencias.adapter?.notifyDataSetChanged()
                    }
                    pendingSelectedUri = null
                    Toast.makeText(this, getString(R.string.msg_evidencias_agregadas), Toast.LENGTH_SHORT).show()
                }
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar_btn)) { d, _ -> d.dismiss() }
            .create()
        dialog.show()
    }

    private fun saveEvidenciaToPrefs(e: Evidencia) {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("evidencias", null)
        val arrExisting = if (existing == null) JSONArray() else JSONArray(existing)
        val obj = JSONObject()
        obj.put("idEvidencia", e.idEvidencia.toString())
        obj.put("idReporte", e.idReporte.toString())
        obj.put("tipoArchivo", e.tipoArchivo)
        obj.put("urlArchivo", e.urlArchivo)
        arrExisting.put(obj)
        prefs.edit().putString("evidencias", arrExisting.toString()).apply()
    }

    override fun finish() {
        val arr = JSONArray()
        for (e in evidencias) {
            val obj = JSONObject()
            obj.put("idEvidencia", e.idEvidencia.toString())
            obj.put("idReporte", e.idReporte.toString())
            obj.put("tipoArchivo", e.tipoArchivo)
            obj.put("urlArchivo", e.urlArchivo)
            arr.put(obj)
        }
        val data = Intent()
        data.putExtra("evidencias_json", arr.toString())
        setResult(Activity.RESULT_OK, data)
        super.finish()
    }
}
