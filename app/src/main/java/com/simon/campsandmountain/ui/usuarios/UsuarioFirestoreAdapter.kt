package com.simon.campsandmountain.ui.usuarios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.model.User

class UsuarioFirestoreAdapter(
    private var list: List<User>,
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UsuarioFirestoreAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreUsuario)
        val tvCorreo: TextView = itemView.findViewById(R.id.tvCorreoUsuario)
        val tvRol: TextView = itemView.findViewById(R.id.tvRolUsuario)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditarUsuario)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarUsuario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario_firestore, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvNombre.text = item.fullName.ifEmpty { "Sin nombre" }
        holder.tvCorreo.text = "✉️ ${item.email}"
        holder.tvRol.text = item.role.ifEmpty { "Guardaparque" }

        holder.btnEditar.setOnClickListener { onEditClick(item) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<User>) {
        list = newList
        notifyDataSetChanged()
    }
}
