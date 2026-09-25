package com.simon.campsandmountain.data.repository

import com.simon.campsandmountain.data.model.EstadoRefugio
import com.simon.campsandmountain.data.model.Refugio
import java.util.UUID

object RefugioRepository {
    private val list = mutableListOf(
        Refugio(
            id = "REF-001",
            nombre = "Refugio El Cóndor",
            altitudMsnm = 2450,
            capacidadCamas = 24,
            estado = EstadoRefugio.ABIERTO,
            precioNocheClp = 35000,
            ubicacionSector = "Valle de la Luna, Sector Norte",
            telefonoContacto = "+56 9 8765 4321"
        ),
        Refugio(
            id = "REF-002",
            nombre = "Refugio Volcán Osorno",
            altitudMsnm = 1820,
            capacidadCamas = 18,
            estado = EstadoRefugio.ABIERTO,
            precioNocheClp = 42500,
            ubicacionSector = "Ladera Este, Tramo Altura",
            telefonoContacto = "+56 9 1234 5678"
        ),
        Refugio(
            id = "REF-003",
            nombre = "Refugio Alturas del Paine",
            altitudMsnm = 3100,
            capacidadCamas = 12,
            estado = EstadoRefugio.MANTENIMIENTO,
            precioNocheClp = 50000,
            ubicacionSector = "Paso Los Vientos",
            telefonoContacto = "+56 9 9988 7766"
        )
    )

    fun getAll(): List<Refugio> = list.toList()

    fun getById(id: String): Refugio? = list.find { it.id == id }

    fun add(
        nombre: String,
        altitudMsnm: Int,
        capacidadCamas: Int,
        estado: EstadoRefugio,
        precioNocheClp: Int,
        ubicacionSector: String,
        telefonoContacto: String
    ): Refugio {
        val newRefugio = Refugio(
            id = "REF-" + UUID.randomUUID().toString().take(6).uppercase(),
            nombre = nombre,
            altitudMsnm = altitudMsnm,
            capacidadCamas = capacidadCamas,
            estado = estado,
            precioNocheClp = precioNocheClp,
            ubicacionSector = ubicacionSector,
            telefonoContacto = telefonoContacto
        )
        list.add(0, newRefugio)
        return newRefugio
    }

    fun update(refugio: Refugio): Boolean {
        val index = list.indexOfFirst { it.id == refugio.id }
        return if (index != -1) {
            list[index] = refugio
            true
        } else {
            false
        }
    }

    fun delete(id: String): Boolean {
        return list.removeIf { it.id == id }
    }

    fun search(query: String): List<Refugio> {
        if (query.isBlank()) return getAll()
        val q = query.trim().lowercase()
        return list.filter {
            it.nombre.lowercase().contains(q) ||
                    it.ubicacionSector.lowercase().contains(q) ||
                    it.altitudMsnm.toString().contains(q)
        }
    }
}
