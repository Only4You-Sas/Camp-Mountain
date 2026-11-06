package com.simon.campsandmountain.data.model

enum class EstadoRefugio(val displayName: String) {
    ABIERTO("Abierto"),
    CERRADO("Cerrado"),
    MANTENIMIENTO("En Mantenimiento")
}

data class Refugio(
    val id: String = "",
    val nombre: String = "",
    val altitudMsnm: Int = 0,
    val capacidadCamas: Int = 0,
    val estado: EstadoRefugio = EstadoRefugio.ABIERTO,
    val precioNocheClp: Int = 0,
    val ubicacionSector: String = "",
    val telefonoContacto: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
)
