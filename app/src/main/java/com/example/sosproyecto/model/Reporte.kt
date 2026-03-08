package com.example.sosproyecto.model

import java.util.UUID

data class Reporte(
    val idReporte: UUID = UUID.randomUUID(),
    val idUsuario: UUID? = null,
    val idTipo: Int,
    val descripcion: String? = null,
    val latitud: Double,
    val longitud: Double,
    // Usar String ISO para compatibilidad
    val fechaReporte: String = "",
    val nivelConfianza: Int = 0,
    val estadoVerificacion: String = "pendiente",
    val esAnonimo: Boolean = false
)
