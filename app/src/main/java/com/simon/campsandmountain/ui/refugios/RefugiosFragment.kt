package com.simon.campsandmountain.ui.refugios

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.model.EstadoRefugio
import com.simon.campsandmountain.data.model.Refugio
import com.simon.campsandmountain.data.repository.RefugioRepository

class RefugiosFragment : Fragment() {

    private lateinit var adapter: RefugioAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_refugios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvEmpty = view.findViewById(R.id.tvEmptyRefugios)
        etSearch = view.findViewById(R.id.etSearchRefugio)
        val rv: RecyclerView = view.findViewById(R.id.rvRefugios)
        val fab: FloatingActionButton = view.findViewById(R.id.fabAddRefugio)

        adapter = RefugioAdapter(
            list = RefugioRepository.getAll(),
            onEditClick = { refugio -> showRefugioFormDialog(refugio) },
            onDeleteClick = { refugio -> showDeleteConfirmation(refugio) }
        )
        rv.adapter = adapter

        fab.setOnClickListener { showRefugioFormDialog(null) }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        refreshData()
    }

    private fun filterList(query: String) {
        val filtered = RefugioRepository.search(query)
        adapter.updateData(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    fun refreshData() {
        val currentQuery = etSearch.text.toString()
        filterList(currentQuery)
    }

    private fun showRefugioFormDialog(refugioToEdit: Refugio?) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_form_refugio, null)

        val tvTitle: TextView = dialogView.findViewById(R.id.tvFormTitle)
        val etNombre: EditText = dialogView.findViewById(R.id.etNombre)
        val etUbicacion: EditText = dialogView.findViewById(R.id.etUbicacion)
        val etAltitud: EditText = dialogView.findViewById(R.id.etAltitud)
        val etCamas: EditText = dialogView.findViewById(R.id.etCamas)
        val etPrecio: EditText = dialogView.findViewById(R.id.etPrecio)
        val etTelefono: EditText = dialogView.findViewById(R.id.etTelefono)
        val rgEstado: RadioGroup = dialogView.findViewById(R.id.rgEstado)
        val rbAbierto: RadioButton = dialogView.findViewById(R.id.rbAbierto)
        val rbCerrado: RadioButton = dialogView.findViewById(R.id.rbCerrado)
        val rbMantenimiento: RadioButton = dialogView.findViewById(R.id.rbMantenimiento)
        val btnCancelar: MaterialButton = dialogView.findViewById(R.id.btnCancelar)
        val btnGuardar: MaterialButton = dialogView.findViewById(R.id.btnGuardar)

        if (refugioToEdit != null) {
            tvTitle.text = "Editar Refugio"
            etNombre.setText(refugioToEdit.nombre)
            etUbicacion.setText(refugioToEdit.ubicacionSector)
            etAltitud.setText(refugioToEdit.altitudMsnm.toString())
            etCamas.setText(refugioToEdit.capacidadCamas.toString())
            etPrecio.setText(refugioToEdit.precioNocheClp.toString())
            etTelefono.setText(refugioToEdit.telefonoContacto)
            when (refugioToEdit.estado) {
                EstadoRefugio.ABIERTO -> rbAbierto.isChecked = true
                EstadoRefugio.CERRADO -> rbCerrado.isChecked = true
                EstadoRefugio.MANTENIMIENTO -> rbMantenimiento.isChecked = true
            }
        } else {
            tvTitle.text = "Nuevo Refugio"
        }

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnCancelar.setOnClickListener { alertDialog.dismiss() }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val ubicacion = etUbicacion.text.toString().trim()
            val altitudStr = etAltitud.text.toString().trim()
            val camasStr = etCamas.text.toString().trim()
            val precioStr = etPrecio.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(context, "Debe ingresar el nombre del refugio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ubicacion.isEmpty()) {
                Toast.makeText(context, "Debe ingresar la ubicación o sector", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val altitud = altitudStr.toIntOrNull()
            if (altitud == null || altitud <= 0) {
                Toast.makeText(context, "Ingrese una altitud válida (mayor a 0)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val camas = camasStr.toIntOrNull()
            if (camas == null || camas <= 0) {
                Toast.makeText(context, "Ingrese una cantidad de camas válida (mayor a 0)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val precio = precioStr.toIntOrNull()
            if (precio == null || precio <= 0) {
                Toast.makeText(context, "Ingrese un precio por noche válido (mayor a 0)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (telefono.isEmpty()) {
                Toast.makeText(context, "Debe ingresar un teléfono de contacto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val estado = when (rgEstado.checkedRadioButtonId) {
                R.id.rbCerrado -> EstadoRefugio.CERRADO
                R.id.rbMantenimiento -> EstadoRefugio.MANTENIMIENTO
                else -> EstadoRefugio.ABIERTO
            }

            if (refugioToEdit != null) {
                val updated = refugioToEdit.copy(
                    nombre = nombre,
                    ubicacionSector = ubicacion,
                    altitudMsnm = altitud,
                    capacidadCamas = camas,
                    precioNocheClp = precio,
                    telefonoContacto = telefono,
                    estado = estado
                )
                RefugioRepository.update(updated)
                Toast.makeText(context, "Refugio actualizado", Toast.LENGTH_SHORT).show()
            } else {
                RefugioRepository.add(
                    nombre = nombre,
                    altitudMsnm = altitud,
                    capacidadCamas = camas,
                    estado = estado,
                    precioNocheClp = precio,
                    ubicacionSector = ubicacion,
                    telefonoContacto = telefono
                )

                val analyticsBundle = Bundle().apply {
                    putString("item_name", nombre)
                    putInt("altitud", altitud)
                    putString("category", "refugio")
                }
                FirebaseAnalytics.getInstance(context).logEvent("refugio_creado", analyticsBundle)

                Toast.makeText(context, "Refugio creado exitosamente", Toast.LENGTH_SHORT).show()
            }

            alertDialog.dismiss()
            refreshData()
        }

        alertDialog.show()
    }

    private fun showDeleteConfirmation(refugio: Refugio) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Refugio")
            .setMessage("¿Estás seguro de que deseas eliminar '${refugio.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                RefugioRepository.delete(refugio.id)
                Toast.makeText(requireContext(), "Refugio eliminado", Toast.LENGTH_SHORT).show()
                refreshData()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
