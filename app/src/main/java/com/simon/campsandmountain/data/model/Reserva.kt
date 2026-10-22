package com.simon.campsandmountain.data.model

enum class TipoAlojamiento(val label: String) {
    REFUGIO("Refugio"),
    CAMPAMENTO("Campamento")
}

enum class EstadoReserva(val label: String) {
    CONFIRMADA("Confirmada"),
    PENDIENTE("Pendiente"),
    CANCELADA("Cancelada")
}

data class Reserva(
    val id: String = "",
    val nombreVisitante: String = "",
    val identificacionContacto: String = "",
    val tipoAlojamiento: TipoAlojamiento = TipoAlojamiento.REFUGIO,
    val idAlojamiento: String = "",
    val nombreAlojamiento: String = "",
    val fechaEntrada: String = "",
    val fechaSalida: String = "",
    val cantidadPersonas: Int = 1,
    val estado: EstadoReserva = EstadoReserva.CONFIRMADA,
    val userId: String = "",
    val userEmail: String = ""
)
