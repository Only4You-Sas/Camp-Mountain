package com.simon.campsandmountain.ui.usuarios

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.model.User

class UsuariosFragment : Fragment() {

    private lateinit var adapter: UsuarioFirestoreAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private var allUsersList = mutableListOf<User>()
    private var firestoreListener: ListenerRegistration? = null

    private fun getFirestore(): FirebaseFirestore {
        return try {
            FirebaseFirestore.getInstance("usuarios")
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    companion object {
        private const val TAG = "UsuariosFragment"
        val ROLES = arrayOf("Guardaparque", "Administrador", "Guía", "Visitante")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_usuarios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvEmpty = view.findViewById(R.id.tvEmptyUsuarios)
        etSearch = view.findViewById(R.id.etSearchUsuario)
        val rv: RecyclerView = view.findViewById(R.id.rvUsuariosFirestore)
        val fab: FloatingActionButton = view.findViewById(R.id.fabAddUsuario)

        adapter = UsuarioFirestoreAdapter(
            list = emptyList(),
            onEditClick = { user -> showUserFormDialog(user) },
            onDeleteClick = { user -> showDeleteConfirmation(user) }
        )
        rv.adapter = adapter

        fab.setOnClickListener { showUserFormDialog(null) }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadUsersFromFirestore()
    }

    private fun loadUsersFromFirestore() {
        val db = getFirestore()
        firestoreListener?.remove()

        firestoreListener = db.collection("Usuarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    allUsersList.clear()
                    for (doc in snapshot.documents) {
                        val nombre = doc.getString("nombre") ?: doc.getString("fullName") ?: ""
                        val correo = doc.getString("correo") ?: doc.getString("email") ?: ""
                        val rol = doc.getString("rol") ?: doc.getString("role") ?: "Guardaparque"
                        val user = User(
                            uid = doc.id,
                            fullName = nombre,
                            email = correo,
                            role = rol
                        )
                        allUsersList.add(user)
                    }
                    filterList(etSearch.text.toString())
                }
            }
    }

    private fun filterList(query: String) {
        val filtered = if (query.isBlank()) {
            allUsersList.toList()
        } else {
            val q = query.trim().lowercase()
            allUsersList.filter {
                it.fullName.lowercase().contains(q) || it.email.lowercase().contains(q) || it.role.lowercase().contains(q)
            }
        }
        adapter.updateData(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showUserFormDialog(userToEdit: User?) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_form_usuario_firestore, null)

        val tvTitle: TextView = dialogView.findViewById(R.id.tvFormTitleUser)
        val etNombre: EditText = dialogView.findViewById(R.id.etNombreUsuario)
        val etCorreo: EditText = dialogView.findViewById(R.id.etCorreoUsuario)
        val spinnerRol: AutoCompleteTextView = dialogView.findViewById(R.id.spinnerRolUsuario)
        val btnCancelar: MaterialButton = dialogView.findViewById(R.id.btnCancelarUser)
        val btnGuardar: MaterialButton = dialogView.findViewById(R.id.btnGuardarUser)

        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, ROLES)
        spinnerRol.setAdapter(spinnerAdapter)

        if (userToEdit != null) {
            tvTitle.text = "Editar Usuario"
            etNombre.setText(userToEdit.fullName)
            etCorreo.setText(userToEdit.email)
            spinnerRol.setText(userToEdit.role.ifEmpty { ROLES[0] }, false)
        } else {
            tvTitle.text = "Nuevo Usuario"
            spinnerRol.setText(ROLES[0], false)
        }

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnCancelar.setOnClickListener { alertDialog.dismiss() }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val rol = spinnerRol.text.toString().trim().ifEmpty { "Guardaparque" }

            if (nombre.isEmpty() || correo.isEmpty()) {
                Toast.makeText(context, "Por favor ingrese nombre y correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = getFirestore()
            val userMap = hashMapOf(
                "nombre" to nombre,
                "correo" to correo,
                "rol" to rol
            )

            if (userToEdit != null) {
                db.collection("Usuarios").document(userToEdit.uid).set(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Usuario actualizado en Firestore", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            } else {
                db.collection("Usuarios").add(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Usuario creado exitosamente", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
        }

        alertDialog.show()
    }

    private fun showDeleteConfirmation(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de eliminar a '${user.fullName.ifEmpty { user.email }}' de Cloud Firestore?")
            .setPositiveButton("Eliminar") { _, _ ->
                getFirestore().collection("Usuarios").document(user.uid).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Usuario eliminado de Firestore", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error al eliminar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
    }
}
