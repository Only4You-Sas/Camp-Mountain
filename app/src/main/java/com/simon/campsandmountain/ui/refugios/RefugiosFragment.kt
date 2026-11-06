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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.importer.CsvBatchImporter
import com.simon.campsandmountain.data.model.EstadoRefugio
import com.simon.campsandmountain.data.model.Refugio
import com.simon.campsandmountain.data.repository.AuthRepository
import com.simon.campsandmountain.data.repository.RefugioRepository
import com.simon.campsandmountain.location.AddressValidationManager
import kotlinx.coroutines.launch

class RefugiosFragment : Fragment() {

    private lateinit var adapter: RefugioAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private var firebaseAnalytics: FirebaseAnalytics? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_refugios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let {
            firebaseAnalytics = FirebaseAnalytics.getInstance(it)
        }

        tvEmpty = view.findViewById(R.id.tvEmptyRefugios)
        etSearch = view.findViewById(R.id.etSearchRefugio)
        val btnCsv: MaterialButton = view.findViewById(R.id.btnImportarCsvRefugios)
        val rv: RecyclerView = view.findViewById(R.id.rvRefugios)
        val fab: FloatingActionButton = view.findViewById(R.id.fabAddRefugio)

        val canManage = AuthRepository.canManageRefugiosAndCampamentos()
        if (!canManage) {
            fab.visibility = View.GONE
            btnCsv.visibility = View.GONE
        } else {
            fab.visibility = View.VISIBLE
            btnCsv.visibility = View.VISIBLE
            fab.setOnClickListener { showRefugioFormDialog(null) }
            btnCsv.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("📥 Importar Catálogo de Refugios (CSV)")
                    .setMessage("¿Deseas cargar masivamente los refugios del dataset oficial 'refugios_oficiales_chile.csv' a Cloud Firestore usando OpenCSV y WriteBatch?")
                    .setPositiveButton("Importar") { _, _ ->
                        lifecycleScope.launch {
                            Toast.makeText(requireContext(), "Procesando lote de escritura en Firestore...", Toast.LENGTH_SHORT).show()
                            val res = CsvBatchImporter.importCsvFromAssetsToFirestore(
                                context = requireContext(),
                                assetFileName = "refugios_oficiales_chile.csv",
                                collectionName = "Refugios"
                            )
                            if (res.isSuccess) {
                                val count = res.getOrDefault(0)
                                Toast.makeText(requireContext(), "¡Se importaron $count refugios exitosamente a Cloud Firestore!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(requireContext(), "Error al importar CSV: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        adapter = RefugioAdapter(
            list = emptyList(),
            onEditClick = { refugio -> showRefugioFormDialog(refugio) },
            onDeleteClick = { refugio -> showDeleteConfirmation(refugio) }
        )
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        RefugioRepository.listen(firebaseAnalytics) { _ ->
            filterList(etSearch.text.toString())
        }
    }

    private fun filterList(query: String) {
        val filtered = RefugioRepository.search(query)
        adapter.updateData(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showRefugioFormDialog(refugioToEdit: Refugio?) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_form_refugio, null)

        val tvTitle: TextView = dialogView.findViewById(R.id.tvFormTitle)
        val etNombre: EditText = dialogView.findViewById(R.id.etNombre)
        val etUbicacion: EditText = dialogView.findViewById(R.id.etUbicacion)
        val btnValidarUbicacion: MaterialButton = dialogView.findViewById(R.id.btnValidarUbicacion)
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

        var currentLat = refugioToEdit?.latitud ?: 0.0
        var currentLon = refugioToEdit?.longitud ?: 0.0

        // Los refugios son de uso gratuito (sin costo)
        etPrecio.setText("0")
        etPrecio.isEnabled = false

        btnValidarUbicacion.setOnClickListener {
            val rawAddress = etUbicacion.text.toString().trim()
            if (rawAddress.isEmpty()) {
                Toast.makeText(context, "Debe ingresar una ubicación para validar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(context, "Validando ubicación...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                AddressValidationManager.validateAddressAndSaveToFirestore(rawAddress) { success, msg, data ->
                    activity?.runOnUiThread {
                        if (success && data != null) {
                            val full = data["nombreCompleto"] as? String ?: rawAddress
                            currentLat = (data["latitud"] as? Double) ?: 0.0
                            currentLon = (data["longitud"] as? Double) ?: 0.0
                            etUbicacion.setText(full)
                            Toast.makeText(context, "✅ Validación correcta. Ubicación real confirmada (Lat: $currentLat, Lon: $currentLon)", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "❌ No se pudo validar. Verifique la dirección o intente incluir la comuna/país.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        if (refugioToEdit != null) {
            tvTitle.text = "Editar Refugio"
            etNombre.setText(refugioToEdit.nombre)
            etUbicacion.setText(refugioToEdit.ubicacionSector)
            etAltitud.setText(refugioToEdit.altitudMsnm.toString())
            etCamas.setText(refugioToEdit.capacidadCamas.toString())
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
            if (telefono.isEmpty()) {
                Toast.makeText(context, "Debe ingresar un teléfono de contacto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val estado = when (rgEstado.checkedRadioButtonId) {
                R.id.rbCerrado -> EstadoRefugio.CERRADO
                R.id.rbMantenimiento -> EstadoRefugio.MANTENIMIENTO
                else -> EstadoRefugio.ABIERTO
            }

            val actionName = if (refugioToEdit != null) "actualizar" else "crear"
            AlertDialog.Builder(context)
                .setTitle("Confirmar Guardado")
                .setMessage("¿Deseas $actionName el refugio '$nombre' (Gratuito)?")
                .setPositiveButton("Guardar") { _, _ ->
                    if (refugioToEdit != null) {
                        val updated = refugioToEdit.copy(
                            nombre = nombre,
                            ubicacionSector = ubicacion,
                            altitudMsnm = altitud,
                            capacidadCamas = camas,
                            precioNocheClp = 0,
                            telefonoContacto = telefono,
                            estado = estado,
                            latitud = currentLat,
                            longitud = currentLon
                        )
                        RefugioRepository.update(updated, firebaseAnalytics) { success, error ->
                            if (success) {
                                Toast.makeText(context, "Refugio actualizado exitosamente", Toast.LENGTH_SHORT).show()
                                alertDialog.dismiss()
                            } else {
                                FirebaseCrashlytics.getInstance().recordException(Exception("Error al actualizar refugio: $error"))
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        val newRefugio = Refugio(
                            id = "",
                            nombre = nombre,
                            altitudMsnm = altitud,
                            capacidadCamas = camas,
                            estado = estado,
                            precioNocheClp = 0,
                            ubicacionSector = ubicacion,
                            telefonoContacto = telefono,
                            latitud = currentLat,
                            longitud = currentLon
                        )
                        RefugioRepository.add(newRefugio, firebaseAnalytics) { success, error ->
                            if (success) {
                                Toast.makeText(context, "Refugio guardado exitosamente", Toast.LENGTH_SHORT).show()
                                alertDialog.dismiss()
                            } else {
                                FirebaseCrashlytics.getInstance().recordException(Exception("Error al crear refugio: $error"))
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        alertDialog.show()
    }

    private fun showDeleteConfirmation(refugio: Refugio) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar permanentemente '${refugio.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                RefugioRepository.delete(refugio.id, firebaseAnalytics) { success, error ->
                    if (success) {
                        Toast.makeText(requireContext(), "Refugio eliminado exitosamente", Toast.LENGTH_SHORT).show()
                    } else {
                        FirebaseCrashlytics.getInstance().recordException(Exception("Error al eliminar refugio: $error"))
                        Toast.makeText(requireContext(), "Error al eliminar: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        RefugioRepository.stopListening()
    }
}
