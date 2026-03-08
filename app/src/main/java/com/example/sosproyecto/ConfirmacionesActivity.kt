package com.example.sosproyecto

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.Confirmacion
import com.example.sosproyecto.model.Reporte
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ConfirmacionesActivity : AppCompatActivity() {
    private lateinit var rvConfirmaciones: RecyclerView
    private val reportes = mutableListOf<Reporte>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmaciones)

        rvConfirmaciones = findViewById(R.id.rvConfirmaciones)
        rvConfirmaciones.layoutManager = LinearLayoutManager(this)

        loadReportes()
        val tipos = loadTiposMap()
        rvConfirmaciones.adapter = ConfirmacionAdapter(
            reportes,
            tipos,
            ::countConfirmacionesForReporte,
            ::hasUserConfirmed,
            { reporte -> confirmarReporte(reporte) },
            { reporte -> showMyConfirmacionesForReporte(reporte) }
        )
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
        val json = prefs.getString("reportes", null) ?: return
        val arr = JSONArray(json)
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
            reportes.add(Reporte(idReporte, idUsuario, idTipo, descripcion, lat, lng, fecha, nivel, estado, anon))
        }
    }

    private fun countConfirmacionesForReporte(idReporte: UUID): Int {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("confirmaciones", null) ?: return 0
        val arr = JSONArray(json)
        var count = 0
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val rid = UUID.fromString(obj.getString("idReporte"))
            if (rid == idReporte) count++
        }
        return count
    }

    private fun hasUserConfirmed(idReporte: UUID): Boolean {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val userIdStr = prefs.getString("user_id", null) ?: return false
        val userId = UUID.fromString(userIdStr)
        val json = prefs.getString("confirmaciones", null) ?: return false
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val rid = UUID.fromString(obj.getString("idReporte"))
            val uid = UUID.fromString(obj.getString("idUsuario"))
            if (rid == idReporte && uid == userId) return true
        }
        return false
    }

    private fun confirmarReporte(r: Reporte) {
        if (hasUserConfirmed(r.idReporte)) {
            Toast.makeText(this, "Ya confirmaste este reporte", Toast.LENGTH_SHORT).show()
            return
        }
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val userIdStr = prefs.getString("user_id", null)
        val userId = if (userIdStr != null) UUID.fromString(userIdStr) else UUID.randomUUID()
        val c = Confirmacion(UUID.randomUUID(), r.idReporte, userId)

        val existing = prefs.getString("confirmaciones", null)
        val arr = if (existing == null) JSONArray() else JSONArray(existing)
        val obj = JSONObject()
        obj.put("idConfirmacion", c.idConfirmacion.toString())
        obj.put("idReporte", c.idReporte.toString())
        obj.put("idUsuario", c.idUsuario.toString())
        obj.put("fechaConfirmacion", c.fechaConfirmacion)
        arr.put(obj)
        prefs.edit().putString("confirmaciones", arr.toString()).apply()
        Toast.makeText(this, getString(R.string.msg_reporte_confirmado), Toast.LENGTH_SHORT).show()

        loadReportes()
        (rvConfirmaciones.adapter as? ConfirmacionAdapter)?.notifyDataSetChanged()
    }

    private fun showMyConfirmacionesForReporte(r: Reporte) {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val userIdStr = prefs.getString("user_id", null) ?: return
        val userId = UUID.fromString(userIdStr)
        val json = prefs.getString("confirmaciones", null) ?: return
        val arr = JSONArray(json)
        val myList = mutableListOf<JSONObject>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val rid = UUID.fromString(obj.getString("idReporte"))
            val uid = UUID.fromString(obj.getString("idUsuario"))
            if (rid == r.idReporte && uid == userId) myList.add(obj)
        }
        if (myList.isEmpty()) {
            Toast.makeText(this, "No tienes confirmaciones para este reporte", Toast.LENGTH_SHORT).show()
            return
        }
        val items = myList.map { it.getString("fechaConfirmacion") }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Mis confirmaciones")
            .setItems(items) { dialog, which ->
                // permitir eliminar la confirmación seleccionada
                val objToRemove = myList[which]
                confirmDelete(objToRemove.getString("idConfirmacion"))
            }
            .setNegativeButton(getString(R.string.cancelar_btn), null)
            .show()
    }

    private fun confirmDelete(idConfirmacion: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar confirmación")
            .setMessage("¿Eliminar esta confirmación?")
            .setPositiveButton(getString(R.string.accion_crear)) { d, _ ->
                deleteConfirmacion(idConfirmacion)
            }
            .setNegativeButton(getString(R.string.cancelar_btn), null)
            .show()
    }

    private fun deleteConfirmacion(idConfirmacion: String) {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("confirmaciones", null) ?: return
        val arr = JSONArray(existing)
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("idConfirmacion") != idConfirmacion) newArr.put(obj)
        }
        prefs.edit().putString("confirmaciones", newArr.toString()).apply()
        Toast.makeText(this, "Confirmación eliminada", Toast.LENGTH_SHORT).show()
        loadReportes()
        (rvConfirmaciones.adapter as? ConfirmacionAdapter)?.notifyDataSetChanged()
    }
}
