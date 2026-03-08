package com.example.sosproyecto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.Comentario

class ComentarioAdapter(private val items: List<Comentario>) : RecyclerView.Adapter<ComentarioAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsuario: TextView = view.findViewById(R.id.tvComentarioUsuario)
        val tvTexto: TextView = view.findViewById(R.id.tvComentarioTexto)
        val tvFecha: TextView = view.findViewById(R.id.tvComentarioFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comentario, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = items[position]
        holder.tvUsuario.text = c.idUsuario.toString().take(8)
        holder.tvTexto.text = c.comentario
        holder.tvFecha.text = c.fecha
    }
}

