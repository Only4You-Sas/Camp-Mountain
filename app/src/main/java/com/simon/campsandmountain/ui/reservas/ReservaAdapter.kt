package com.simon.campsandmountain.ui.reservas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.model.EstadoReserva
import com.simon.campsandmountain.data.model.Reserva
import com.simon.campsandmountain.data.model.TipoAlojamiento

class ReservaAdapter(
    private var list: List<Reserva>,
    private val onEditClick: (Reserva) -> Unit,
    private val onDeleteClick: (Reserva) -> Unit
) : RecyclerView.Adapter<ReservaAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvVisitante: TextView = itemView.findViewById(R.id.tvNombreVisitante)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoReserva)
        val tvAlojamiento: TextView = itemView.findViewById(R.id.tvAlojamiento)
        val tvFechas: TextView = itemView.findViewById(R.id.tvFechas)
        val tvPersonas: TextView = itemView.findViewById(R.id.tvPersonas)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditarReserva)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarReserva)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reserva, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvVisitante.text = item.nombreVisitante
        val iconType = if (item.tipoAlojamiento == TipoAlojamiento.REFUGIO) "🏠 Refugio" else "⛺ Campamento"
        holder.tvAlojamiento.text = "$iconType: ${item.nombreAlojamiento}"
        holder.tvFechas.text = "📅 ${item.fechaEntrada} ➔ ${item.fechaSalida}"
        holder.tvPersonas.text = "👥 ${item.cantidadPersonas} personas (${item.identificacionContacto})"

        holder.tvEstado.text = item.estado.label
        val colorRes = when (item.estado) {
            EstadoReserva.CONFIRMADA -> R.color.status_open
            EstadoReserva.PENDIENTE -> R.color.status_pending
            EstadoReserva.CANCELADA -> R.color.status_closed
        }
        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        holder.tvEstado.setTextColor(color)

        holder.btnEditar.setOnClickListener { onEditClick(item) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Reserva>) {
        list = newList
        notifyDataSetChanged()
    }
}
