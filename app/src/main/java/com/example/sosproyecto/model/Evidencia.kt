package com.example.sosproyecto.model

import java.util.UUID

data class Evidencia(
    val idEvidencia: UUID = UUID.randomUUID(),
    val idReporte: UUID,
    val tipoArchivo: String? = null,
    val urlArchivo: String? = null
)

