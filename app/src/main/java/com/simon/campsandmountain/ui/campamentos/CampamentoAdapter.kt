package com.simon.campsandmountain.ui.campamentos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.model.Campamento
import java.text.NumberFormat
import java.util.Locale

class CampamentoAdapter(
    private var list: List<Campamento>,
    private val onEditClick: (Campamento) -> Unit,
    private val onDeleteClick: (Campamento) -> Unit
) : RecyclerView.Adapter<CampamentoAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreCampamento)
        val tvSector: TextView = itemView.findViewById(R.id.tvSector)
        val tvCapacidad: TextView = itemView.findViewById(R.id.tvCapacidadCarpas)
        val tvTerreno: TextView = itemView.findViewById(R.id.tvTipoTerreno)
        val tvAgua: TextView = itemView.findViewById(R.id.tvAguaPotable)
        val tvFogata: TextView = itemView.findViewById(R.id.tvFogata)
        val tvTarifa: TextView = itemView.findViewById(R.id.tvTarifa)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditarCampamento)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarCampamento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_campamento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context
        val clpFormat = NumberFormat.getInstance(Locale("es", "CL")).format(item.tarifaDiariaClp)

        holder.tvNombre.text = item.nombre
        holder.tvSector.text = "📍 ${item.sector}"
        holder.tvCapacidad.text = "⛺ ${item.capacidadCarpas} carpas"
        holder.tvTerreno.text = "🌱 ${item.tipoTerreno}"
        holder.tvTarifa.text = "💵 $$clpFormat / día"

        if (item.aguaPotable) {
            holder.tvAgua.text = "🚰 Agua Potable: Sí"
            holder.tvAgua.setTextColor(ContextCompat.getColor(context, R.color.primary_green))
        } else {
            holder.tvAgua.text = "🚰 Agua Potable: No"
            holder.tvAgua.setTextColor(ContextCompat.getColor(context, R.color.status_closed))
        }

        if (item.permiteFogata) {
            holder.tvFogata.text = "🔥 Fogatas: Permitidas"
            holder.tvFogata.setTextColor(ContextCompat.getColor(context, R.color.primary_green))
        } else {
            holder.tvFogata.text = "🔥 Fogatas: Prohibidas"
            holder.tvFogata.setTextColor(ContextCompat.getColor(context, R.color.status_closed))
        }

        holder.btnEditar.setOnClickListener { onEditClick(item) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Campamento>) {
        list = newList
        notifyDataSetChanged()
    }
}
