package com.example.sosproyecto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.Reporte

class ReporteAdapter(
    private val items: List<Reporte>,
    private val tiposMap: Map<Int, String>,
    private val onItemClick: (Reporte) -> Unit
) : RecyclerView.Adapter<ReporteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTipo: TextView = view.findViewById(R.id.tvReporteTipo)
        val tvDesc: TextView = view.findViewById(R.id.tvReporteDescripcion)
        val tvUbic: TextView = view.findViewById(R.id.tvReporteUbicacion)
        val tvEstado: TextView = view.findViewById(R.id.tvReporteEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reporte, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val r = items[position]
        holder.tvTipo.text = tiposMap[r.idTipo] ?: "Tipo ${r.idTipo}"
        holder.tvDesc.text = r.descripcion ?: ""
        holder.tvUbic.text = String.format(holder.itemView.context.getString(R.string.ubicacion_format), r.latitud, r.longitud)
        holder.tvEstado.text = r.estadoVerificacion

        holder.itemView.setOnClickListener { onItemClick(r) }
    }
}
