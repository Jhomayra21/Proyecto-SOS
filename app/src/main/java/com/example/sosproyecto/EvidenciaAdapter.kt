package com.example.sosproyecto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sosproyecto.model.Evidencia
import java.util.Locale

class EvidenciaAdapter(private val items: List<Evidencia>) : RecyclerView.Adapter<EvidenciaAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPreview: ImageView = view.findViewById(R.id.ivEvidenciaPreview)
        val tvTipo: TextView = view.findViewById(R.id.tvEvidenciaTipo)
        val tvUrl: TextView = view.findViewById(R.id.tvEvidenciaUrl)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evidencia, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val e = items[position]
        holder.tvTipo.text = e.tipoArchivo ?: ""
        holder.tvUrl.text = e.urlArchivo ?: "(no disponible)"

        val ctx: Context = holder.itemView.context
        val url = e.urlArchivo
        if (url != null && url.startsWith("content://")) {
            try {
                val uri = Uri.parse(url)
                val input = ctx.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(input)
                input?.close()
                if (bitmap != null) {
                    holder.ivPreview.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 64, 64, true))
                    return
                }
            } catch (t: Throwable) {
            }
        }

        if (e.tipoArchivo != null) {
            val type = e.tipoArchivo.lowercase(Locale.getDefault())
            when {
                type.contains("image") || type.contains("foto") -> holder.ivPreview.setImageResource(android.R.drawable.ic_menu_gallery)
                type.contains("video") -> holder.ivPreview.setImageResource(android.R.drawable.ic_media_play)
                type.contains("audio") -> holder.ivPreview.setImageResource(android.R.drawable.ic_media_ff)
                else -> holder.ivPreview.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        } else {
            holder.ivPreview.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }
}
