package com.simon.campsandmountain.data.model

enum class EstadoRefugio(val displayName: String) {
    ABIERTO("Abierto"),
    CERRADO("Cerrado"),
    MANTENIMIENTO("En Mantenimiento")
}

data class Refugio(
    val id: String,
    val nombre: String,
    val altitudMsnm: Int,
    val capacidadCamas: Int,
    val estado: EstadoRefugio,
    val precioNocheClp: Int,
    val ubicacionSector: String,
    val telefonoContacto: String
)
