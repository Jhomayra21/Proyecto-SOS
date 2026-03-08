package com.example.sosproyecto.model

import java.util.UUID
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

data class Emergencia(
    val idEmergencia: UUID = UUID.randomUUID(),
    val idUsuario: UUID,
    val latitud: Double,
    val longitud: Double,
    val fechaEmergencia: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
    val estado: String = "activa"
)
