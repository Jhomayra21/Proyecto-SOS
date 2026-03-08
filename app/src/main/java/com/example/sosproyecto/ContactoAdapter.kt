package com.example.sosproyecto

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.ContactoEmergencia

class ContactoAdapter(
    private val items: List<ContactoEmergencia>,
    private val onSendLocation: (ContactoEmergencia) -> Unit
) : RecyclerView.Adapter<ContactoAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvContactoNombre)
        val tvTelefono: TextView = view.findViewById(R.id.tvContactoTelefono)
        val btnLlamar: Button = view.findViewById(R.id.btnLlamarContacto)
        val btnEnviarUbic: Button = view.findViewById(R.id.btnEnviarUbic)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contacto_emergencia, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = items[position]
        holder.tvNombre.text = c.nombre
        holder.tvTelefono.text = c.telefono
        holder.btnLlamar.setOnClickListener {
            val ctx: Context = holder.itemView.context
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.telefono}"))
            ctx.startActivity(intent)
        }
        holder.btnEnviarUbic.setOnClickListener {
            onSendLocation(c)
        }
    }
}
