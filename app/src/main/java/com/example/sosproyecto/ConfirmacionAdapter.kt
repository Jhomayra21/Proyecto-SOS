package com.example.sosproyecto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.Reporte
import java.util.UUID

class ConfirmacionAdapter(
    private val items: List<Reporte>,
    private val tiposMap: Map<Int, String>,
    private val confirmCountProvider: (UUID) -> Int,
    private val hasUserConfirmedProvider: (UUID) -> Boolean,
    private val onConfirm: (Reporte) -> Unit,
    private val onViewMyConfirms: (Reporte) -> Unit
) : RecyclerView.Adapter<ConfirmacionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTipo: TextView = view.findViewById(R.id.tvItemReporteTipo)
        val tvDesc: TextView = view.findViewById(R.id.tvItemReporteDesc)
        val tvUsuario: TextView = view.findViewById(R.id.tvItemReporteUsuario)
        val tvFecha: TextView = view.findViewById(R.id.tvItemReporteFecha)
        val tvCount: TextView = view.findViewById(R.id.tvConfirmCount)
        val btnConfirm: Button = view.findViewById(R.id.btnConfirmarReporte)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_confirmacion, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val r = items[position]
        holder.tvTipo.text = tiposMap[r.idTipo] ?: "Tipo ${r.idTipo}"
        holder.tvDesc.text = r.descripcion ?: ""
        holder.tvFecha.text = r.fechaReporte
        holder.tvUsuario.text = if (r.esAnonimo) "Anónimo" else (r.idUsuario?.toString()?.take(8) ?: "Desconocido")

        val count = confirmCountProvider(r.idReporte)
        holder.tvCount.text = "$count confirmaciones"

        val hasConfirmed = hasUserConfirmedProvider(r.idReporte)
        holder.btnConfirm.isEnabled = !hasConfirmed
        holder.btnConfirm.setOnClickListener { onConfirm(r) }

        holder.itemView.setOnLongClickListener {
            onViewMyConfirms(r)
            true
        }
    }
}
