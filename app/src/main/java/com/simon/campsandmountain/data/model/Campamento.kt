package com.simon.campsandmountain.data.model

data class Campamento(
    val id: String,
    val nombre: String,
    val sector: String,
    val capacidadCarpas: Int,
    val tipoTerreno: String,
    val permiteFogata: Boolean,
    val aguaPotable: Boolean,
    val tarifaDiariaClp: Int
)
