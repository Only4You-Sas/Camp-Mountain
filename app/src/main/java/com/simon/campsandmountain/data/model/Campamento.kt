package com.simon.campsandmountain.data.model

data class Campamento(
    val id: String = "",
    val nombre: String = "",
    val sector: String = "",
    val capacidadCarpas: Int = 0,
    val tipoTerreno: String = "",
    val permiteFogata: Boolean = false,
    val aguaPotable: Boolean = false,
    val tarifaDiariaClp: Int = 0,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
)
