package com.simon.campsandmountain.data.repository

import com.simon.campsandmountain.data.model.EstadoReserva
import com.simon.campsandmountain.data.model.Reserva
import com.simon.campsandmountain.data.model.TipoAlojamiento
import java.util.UUID

object ReservaRepository {
    private val list = mutableListOf(
        Reserva(
            id = "RES-301",
            nombreVisitante = "María Paz Gonzalez",
            identificacionContacto = "+56 9 8811 2233",
            tipoAlojamiento = TipoAlojamiento.REFUGIO,
            idAlojamiento = "REF-001",
            nombreAlojamiento = "Refugio El Cóndor",
            fechaEntrada = "2026-10-12",
            fechaSalida = "2026-10-15",
            cantidadPersonas = 2,
            estado = EstadoReserva.CONFIRMADA
        ),
        Reserva(
            id = "RES-302",
            nombreVisitante = "Andrés Silva",
            identificacionContacto = "andres.silva@email.com",
            tipoAlojamiento = TipoAlojamiento.CAMPAMENTO,
            idAlojamiento = "CMP-101",
            nombreAlojamiento = "Campamento Valle del Sol",
            fechaEntrada = "2026-10-18",
            fechaSalida = "2026-10-20",
            cantidadPersonas = 4,
            estado = EstadoReserva.PENDIENTE
        ),
        Reserva(
            id = "RES-303",
            nombreVisitante = "Expedición Andina",
            identificacionContacto = "+56 9 7766 5544",
            tipoAlojamiento = TipoAlojamiento.REFUGIO,
            idAlojamiento = "REF-002",
            nombreAlojamiento = "Refugio Volcán Osorno",
            fechaEntrada = "2026-11-01",
            fechaSalida = "2026-11-03",
            cantidadPersonas = 6,
            estado = EstadoReserva.CONFIRMADA
        )
    )

    fun getAll(): List<Reserva> = list.toList()

    fun getById(id: String): Reserva? = list.find { it.id == id }

    fun add(
        nombreVisitante: String,
        identificacionContacto: String,
        tipoAlojamiento: TipoAlojamiento,
        idAlojamiento: String,
        nombreAlojamiento: String,
        fechaEntrada: String,
        fechaSalida: String,
        cantidadPersonas: Int,
        estado: EstadoReserva
    ): Reserva {
        val newReserva = Reserva(
            id = "RES-" + UUID.randomUUID().toString().take(6).uppercase(),
            nombreVisitante = nombreVisitante,
            identificacionContacto = identificacionContacto,
            tipoAlojamiento = tipoAlojamiento,
            idAlojamiento = idAlojamiento,
            nombreAlojamiento = nombreAlojamiento,
            fechaEntrada = fechaEntrada,
            fechaSalida = fechaSalida,
            cantidadPersonas = cantidadPersonas,
            estado = estado
        )
        list.add(0, newReserva)
        return newReserva
    }

    fun update(reserva: Reserva): Boolean {
        val index = list.indexOfFirst { it.id == reserva.id }
        return if (index != -1) {
            list[index] = reserva
            true
        } else {
            false
        }
    }

    fun delete(id: String): Boolean {
        return list.removeIf { it.id == id }
    }

    fun search(query: String): List<Reserva> {
        if (query.isBlank()) return getAll()
        val q = query.trim().lowercase()
        return list.filter {
            it.nombreVisitante.lowercase().contains(q) ||
                    it.nombreAlojamiento.lowercase().contains(q) ||
                    it.id.lowercase().contains(q)
        }
    }
}
