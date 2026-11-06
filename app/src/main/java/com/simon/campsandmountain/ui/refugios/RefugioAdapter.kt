package com.simon.campsandmountain.ui.refugios

import android.content.Intent
import android.net.Uri
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
import com.simon.campsandmountain.data.repository.AuthRepository

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

        holder.tvNombre.text = item.nombre
        holder.tvUbicacion.text = "📍 ${item.ubicacionSector} 🗺️"
        holder.tvAltitud.text = "⛰️ ${item.altitudMsnm} m.s.n.m."
        holder.tvCamas.text = "🛏️ ${item.capacidadCamas} camas"
        holder.tvPrecio.text = "🆓 Gratuito (Sin Costo)"
        holder.tvPrecio.setTextColor(ContextCompat.getColor(context, R.color.primary_green))

        holder.tvEstado.text = item.estado.displayName
        val colorRes = when (item.estado) {
            EstadoRefugio.ABIERTO -> R.color.status_open
            EstadoRefugio.CERRADO -> R.color.status_closed
            EstadoRefugio.MANTENIMIENTO -> R.color.status_maintenance
        }
        val color = ContextCompat.getColor(context, colorRes)
        holder.tvEstado.setTextColor(color)

        // Al presionar la ubicación se abre Google Maps / Mapas del teléfono
        holder.tvUbicacion.setOnClickListener {
            val query = if (item.latitud != 0.0 && item.longitud != 0.0) {
                "geo:${item.latitud},${item.longitud}?q=${item.latitud},${item.longitud}(${Uri.encode(item.nombre)})"
            } else {
                "geo:0,0?q=${Uri.encode(item.ubicacionSector)}"
            }
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(query))
                context.startActivity(intent)
            } catch (_: Exception) {
                val webQuery = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(item.ubicacionSector)}"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webQuery))
                context.startActivity(webIntent)
            }
        }

        val canManage = AuthRepository.canManageRefugiosAndCampamentos()
        if (canManage) {
            holder.btnEditar.visibility = View.VISIBLE
            holder.btnEliminar.visibility = View.VISIBLE
            holder.btnEditar.setOnClickListener { onEditClick(item) }
            holder.btnEliminar.setOnClickListener { onDeleteClick(item) }
        } else {
            holder.btnEditar.visibility = View.GONE
            holder.btnEliminar.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Refugio>) {
        list = newList
        notifyDataSetChanged()
    }
}
