package com.example.sosproyecto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.Emergencia
import java.util.Locale

class EmergenciaAdapter(private val items: List<Emergencia>) : RecyclerView.Adapter<EmergenciaAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvId: TextView = view.findViewById(R.id.tvEmergenciaId)
        val tvUbic: TextView = view.findViewById(R.id.tvEmergenciaUbic)
        val tvFecha: TextView = view.findViewById(R.id.tvEmergenciaFecha)
        val tvEstado: TextView = view.findViewById(R.id.tvEmergenciaEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emergencia, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val e = items[position]
        holder.tvId.text = e.idEmergencia.toString().take(8)
        holder.tvUbic.text = String.format(Locale.getDefault(), "%.6f, %.6f", e.latitud, e.longitud)
        holder.tvFecha.text = e.fechaEmergencia.toString()
        holder.tvEstado.text = e.estado
    }
}
