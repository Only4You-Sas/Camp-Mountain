package com.simon.campsandmountain.ui.refugios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.model.EstadoRefugio
import com.simon.campsandmountain.data.model.Refugio
import java.text.NumberFormat
import java.util.Locale

class RefugioAdapter(
    private var list: List<Refugio>,
    private val onEditClick: (Refugio) -> Unit,
    private val onDeleteClick: (Refugio) -> Unit
) : RecyclerView.Adapter<RefugioAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreRefugio)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoRefugio)
        val tvUbicacion: TextView = itemView.findViewById(R.id.tvUbicacion)
        val tvAltitud: TextView = itemView.findViewById(R.id.tvAltitud)
        val tvCamas: TextView = itemView.findViewById(R.id.tvCamas)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_refugio, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context
        val clpFormat = NumberFormat.getInstance(Locale("es", "CL")).format(item.precioNocheClp)

        holder.tvNombre.text = item.nombre
        holder.tvUbicacion.text = "📍 ${item.ubicacionSector}"
        holder.tvAltitud.text = "⛰️ ${item.altitudMsnm} m.s.n.m."
        holder.tvCamas.text = "🛏️ ${item.capacidadCamas} camas"
        holder.tvPrecio.text = "💵 $$clpFormat / noche"

        holder.tvEstado.text = item.estado.displayName
        val colorRes = when (item.estado) {
            EstadoRefugio.ABIERTO -> R.color.status_open
            EstadoRefugio.CERRADO -> R.color.status_closed
            EstadoRefugio.MANTENIMIENTO -> R.color.status_maintenance
        }
        val color = ContextCompat.getColor(context, colorRes)
        holder.tvEstado.setTextColor(color)

        holder.btnEditar.setOnClickListener { onEditClick(item) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Refugio>) {
        list = newList
        notifyDataSetChanged()
    }
}
