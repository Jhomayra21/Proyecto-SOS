package com.example.sosproyecto

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.Comentario
import com.example.sosproyecto.model.Reporte
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ComentariosActivity : AppCompatActivity() {
    private lateinit var spinnerReportes: Spinner
    private lateinit var rvComentarios: RecyclerView
    private lateinit var btnNuevoComentario: Button

    private val reportes = mutableListOf<Reporte>()
    private val comentarios = mutableListOf<Comentario>()
    private lateinit var adapter: ComentarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comentarios)

        spinnerReportes = findViewById(R.id.spinnerReportesParaComentarios)
        rvComentarios = findViewById(R.id.rvComentarios)
        btnNuevoComentario = findViewById(R.id.btnNuevoComentario)

        rvComentarios.layoutManager = LinearLayoutManager(this)
        adapter = ComentarioAdapter(comentarios)
        rvComentarios.adapter = adapter

        loadReportesIntoSpinner()

        spinnerReportes.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val reporte = reportes[position]
                loadComentariosForReporte(reporte.idReporte)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        btnNuevoComentario.setOnClickListener {
            val pos = spinnerReportes.selectedItemPosition
            if (pos < 0 || pos >= reportes.size) {
                Toast.makeText(this, "Selecciona un reporte", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val reporte = reportes[pos]
            showAgregarComentarioDialog(reporte)
        }
    }

    private fun loadReportesIntoSpinner() {
        reportes.clear()
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("reportes", null) ?: return
        val arr = JSONArray(json)
        val labels = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val idReporte = UUID.fromString(obj.getString("idReporte"))
            val idUsuario = if (obj.has("idUsuario") && !obj.isNull("idUsuario")) UUID.fromString(obj.getString("idUsuario")) else null
            val idTipo = obj.getInt("idTipo")
            val descripcion = if (obj.has("descripcion") && !obj.isNull("descripcion")) obj.getString("descripcion") else null
            val lat = obj.getDouble("latitud")
            val lng = obj.getDouble("longitud")
            val fecha = obj.getString("fechaReporte")
            val nivel = obj.getInt("nivelConfianza")
            val estado = obj.getString("estadoVerificacion")
            val anon = obj.getBoolean("esAnonimo")
            val r = Reporte(idReporte, idUsuario, idTipo, descripcion, lat, lng, fecha, nivel, estado, anon)
            reportes.add(r)
            labels.add("#${idReporte.toString().take(8)} - ${descripcion}")
        }
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReportes.adapter = adapterSpinner
        if (reportes.isNotEmpty()) loadComentariosForReporte(reportes[0].idReporte)
    }

    private fun loadComentariosForReporte(idReporte: UUID) {
        comentarios.clear()
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("comentarios", null) ?: "[]"
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val rid = UUID.fromString(obj.getString("idReporte"))
            if (rid == idReporte) {
                val idComentario = UUID.fromString(obj.getString("idComentario"))
                val idUsuario = UUID.fromString(obj.getString("idUsuario"))
                val texto = obj.getString("comentario")
                val fecha = obj.getString("fecha")
                comentarios.add(Comentario(idComentario, rid, idUsuario, texto, fecha))
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun showAgregarComentarioDialog(reporte: Reporte) {
        val input = EditText(this)
        input.hint = getString(R.string.hint_escribe_comentario)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_nuevo_comentario))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_agregar)) { d, _ ->
                val texto = input.text.toString().trim()
                if (texto.isEmpty()) {
                    Toast.makeText(this, getString(R.string.msg_comentario_vacio), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
                val userIdStr = prefs.getString("user_id", null)
                val userId = if (userIdStr != null) UUID.fromString(userIdStr) else UUID.randomUUID()
                val c = Comentario(UUID.randomUUID(), reporte.idReporte, userId, texto)
                saveComentario(c)
                loadComentariosForReporte(reporte.idReporte)
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar_btn), null)
            .show()
    }

    private fun saveComentario(c: Comentario) {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("comentarios", null)
        val arr = if (existing == null) JSONArray() else JSONArray(existing)
        val obj = JSONObject()
        obj.put("idComentario", c.idComentario.toString())
        obj.put("idReporte", c.idReporte.toString())
        obj.put("idUsuario", c.idUsuario.toString())
        obj.put("comentario", c.comentario)
        obj.put("fecha", c.fecha)
        arr.put(obj)
        prefs.edit().putString("comentarios", arr.toString()).apply()
    }
}
