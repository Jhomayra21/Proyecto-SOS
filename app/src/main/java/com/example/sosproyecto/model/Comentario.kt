package com.example.sosproyecto.model

import java.util.UUID
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

data class Comentario(
    val idComentario: UUID = UUID.randomUUID(),
    val idReporte: UUID,
    val idUsuario: UUID,
    val comentario: String,
    val fecha: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
)
