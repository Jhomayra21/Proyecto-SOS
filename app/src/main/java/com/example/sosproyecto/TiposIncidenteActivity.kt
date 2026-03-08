package com.example.sosproyecto

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.TipoIncidente
import org.json.JSONArray
import org.json.JSONObject

class TiposIncidenteActivity : AppCompatActivity() {
    private lateinit var rvTipos: RecyclerView
    private lateinit var btnNuevoTipo: Button
    private val tipos = mutableListOf<TipoIncidente>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tipos_incidente)

        rvTipos = findViewById(R.id.rvTipos)
        btnNuevoTipo = findViewById(R.id.btnNuevoTipo)

        rvTipos.layoutManager = LinearLayoutManager(this)

        loadTipos()
        rvTipos.adapter = TipoAdapter(tipos)

        btnNuevoTipo.setOnClickListener {
            showCrearTipoDialog()
        }
    }

    private fun loadTipos() {
        tipos.clear()
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("tipos_incidente", null)
        if (json == null) {
            val defaults = listOf("Acoso", "Robo", "Intento de secuestro", "Zona peligrosa", "Lugar oscuro")
            var id = 1
            val arr = JSONArray()
            for (d in defaults) {
                val obj = JSONObject()
                obj.put("idTipo", id)
                obj.put("nombreTipo", d)
                arr.put(obj)
                tipos.add(TipoIncidente(id, d))
                id++
            }
            prefs.edit().putString("tipos_incidente", arr.toString()).apply()
        } else {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getInt("idTipo")
                val nombre = obj.getString("nombreTipo")
                tipos.add(TipoIncidente(id, nombre))
            }
        }
    }

    private fun showCrearTipoDialog() {
        val input = EditText(this)
        input.hint = "Nombre del tipo"
        val dialog = AlertDialog.Builder(this)
            .setTitle("Nuevo tipo de incidente")
            .setView(input)
            .setPositiveButton("Crear") { d, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    addTipo(nombre)
                }
                d.dismiss()
            }
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .create()
        dialog.show()
    }

    private fun addTipo(nombre: String) {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("tipos_incidente", null)
        val arr = if (json == null) JSONArray() else JSONArray(json)
        var maxId = 0
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.getInt("idTipo")
            if (id > maxId) maxId = id
        }
        val nuevoId = maxId + 1
        val nuevoObj = JSONObject()
        nuevoObj.put("idTipo", nuevoId)
        nuevoObj.put("nombreTipo", nombre)
        arr.put(nuevoObj)
        prefs.edit().putString("tipos_incidente", arr.toString()).apply()

        tipos.add(TipoIncidente(nuevoId, nombre))
        rvTipos.adapter?.notifyDataSetChanged()
    }
}
