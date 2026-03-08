package com.example.sosproyecto.model

import java.util.UUID

data class ContactoEmergencia(
    val idContacto: UUID = UUID.randomUUID(),
    val idUsuario: UUID,
    val nombre: String,
    val telefono: String
)

