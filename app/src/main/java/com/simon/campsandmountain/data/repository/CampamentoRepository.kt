package com.simon.campsandmountain.data.repository

import com.simon.campsandmountain.data.model.Campamento
import java.util.UUID

object CampamentoRepository {
    private val list = mutableListOf(
        Campamento(
            id = "CMP-101",
            nombre = "Campamento Valle del Sol",
            sector = "Paso Fronterizo, Km 14",
            capacidadCarpas = 30,
            tipoTerreno = "Pradera / Pasto",
            permiteFogata = true,
            aguaPotable = true,
            tarifaDiariaClp = 12000
        ),
        Campamento(
            id = "CMP-102",
            nombre = "Campamento Río Blanco",
            sector = "Ribera Sur, Bosque Nativo",
            capacidadCarpas = 15,
            tipoTerreno = "Arena y Humus",
            permiteFogata = false,
            aguaPotable = true,
            tarifaDiariaClp = 10000
        ),
        Campamento(
            id = "CMP-103",
            nombre = "Campamento Base Las Torres",
            sector = "Valle del Silencio",
            capacidadCarpas = 50,
            tipoTerreno = "Gravilla y Roca",
            permiteFogata = false,
            aguaPotable = false,
            tarifaDiariaClp = 18500
        )
    )

    fun getAll(): List<Campamento> = list.toList()

    fun getById(id: String): Campamento? = list.find { it.id == id }

    fun add(
        nombre: String,
        sector: String,
        capacidadCarpas: Int,
        tipoTerreno: String,
        permiteFogata: Boolean,
        aguaPotable: Boolean,
        tarifaDiariaClp: Int
    ): Campamento {
        val newCamp = Campamento(
            id = "CMP-" + UUID.randomUUID().toString().take(6).uppercase(),
            nombre = nombre,
            sector = sector,
            capacidadCarpas = capacidadCarpas,
            tipoTerreno = tipoTerreno,
            permiteFogata = permiteFogata,
            aguaPotable = aguaPotable,
            tarifaDiariaClp = tarifaDiariaClp
        )
        list.add(0, newCamp)
        return newCamp
    }

    fun update(campamento: Campamento): Boolean {
        val index = list.indexOfFirst { it.id == campamento.id }
        return if (index != -1) {
            list[index] = campamento
            true
        } else {
            false
        }
    }

    fun delete(id: String): Boolean {
        return list.removeIf { it.id == id }
    }

    fun search(query: String): List<Campamento> {
        if (query.isBlank()) return getAll()
        val q = query.trim().lowercase()
        return list.filter {
            it.nombre.lowercase().contains(q) ||
                    it.sector.lowercase().contains(q) ||
                    it.tipoTerreno.lowercase().contains(q)
        }
    }
}
