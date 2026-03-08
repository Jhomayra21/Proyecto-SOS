package com.example.sosproyecto.model

import java.util.UUID

data class Usuario(
    val idUsuario: UUID = UUID.randomUUID(),
    val nombre: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val password: String? = null,
    val fechaRegistro: String = "",
    val nivelConfianza: Int = 0
)
