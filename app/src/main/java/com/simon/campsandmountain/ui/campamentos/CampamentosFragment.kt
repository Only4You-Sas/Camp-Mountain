package com.simon.campsandmountain.ui.campamentos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.importer.CsvBatchImporter
import com.simon.campsandmountain.data.model.Campamento
import com.simon.campsandmountain.data.repository.AuthRepository
import com.simon.campsandmountain.data.repository.CampamentoRepository
import com.simon.campsandmountain.location.AddressValidationManager
import kotlinx.coroutines.launch

class CampamentosFragment : Fragment() {

    private lateinit var adapter: CampamentoAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private var firebaseAnalytics: FirebaseAnalytics? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_campamentos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let {
            firebaseAnalytics = FirebaseAnalytics.getInstance(it)
        }

        tvEmpty = view.findViewById(R.id.tvEmptyCampamentos)
        etSearch = view.findViewById(R.id.etSearchCampamento)
        val btnCsv: MaterialButton = view.findViewById(R.id.btnImportarCsvCampamentos)
        val rv: RecyclerView = view.findViewById(R.id.rvCampamentos)
        val fab: FloatingActionButton = view.findViewById(R.id.fabAddCampamento)

        val canManage = AuthRepository.canManageRefugiosAndCampamentos()
        if (!canManage) {
            fab.visibility = View.GONE
            btnCsv.visibility = View.GONE
        } else {
            fab.visibility = View.VISIBLE
            btnCsv.visibility = View.VISIBLE
            fab.setOnClickListener { showCampamentoFormDialog(null) }
            btnCsv.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("📥 Importar Catálogo de Campamentos (CSV)")
                    .setMessage("¿Deseas cargar masivamente los campamentos del dataset 'campamentos_oficiales_chile.csv' a Cloud Firestore usando OpenCSV y WriteBatch?")
                    .setPositiveButton("Importar") { _, _ ->
                        lifecycleScope.launch {
                            Toast.makeText(requireContext(), "Procesando lote de escritura en Firestore...", Toast.LENGTH_SHORT).show()
                            val res = CsvBatchImporter.importCsvFromAssetsToFirestore(
                                context = requireContext(),
                                assetFileName = "campamentos_oficiales_chile.csv",
                                collectionName = "Campamentos"
                            )
                            if (res.isSuccess) {
                                val count = res.getOrDefault(0)
                                Toast.makeText(requireContext(), "¡Se importaron $count campamentos exitosamente a Cloud Firestore!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(requireContext(), "Error al importar CSV: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        adapter = CampamentoAdapter(
            list = emptyList(),
            onEditClick = { camp -> showCampamentoFormDialog(camp) },
            onDeleteClick = { camp -> showDeleteConfirmation(camp) }
        )
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        CampamentoRepository.listen(firebaseAnalytics) { _ ->
            filterList(etSearch.text.toString())
        }
    }

    private fun filterList(query: String) {
        val filtered = CampamentoRepository.search(query)
        adapter.updateData(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showCampamentoFormDialog(campToEdit: Campamento?) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_form_campamento, null)

        val tvTitle: TextView = dialogView.findViewById(R.id.tvFormTitleCamp)
        val etNombre: EditText = dialogView.findViewById(R.id.etNombreCamp)
        val etSector: EditText = dialogView.findViewById(R.id.etSectorCamp)
        val btnValidarUbicacionCamp: MaterialButton = dialogView.findViewById(R.id.btnValidarUbicacionCamp)
        val etCapacidad: EditText = dialogView.findViewById(R.id.etCapacidadCarpas)
        val etTarifa: EditText = dialogView.findViewById(R.id.etTarifa)
        val etTerreno: EditText = dialogView.findViewById(R.id.etTipoTerreno)
        val switchAgua: MaterialSwitch = dialogView.findViewById(R.id.switchAguaPotable)
        val switchFogata: MaterialSwitch = dialogView.findViewById(R.id.switchPermiteFogata)
        val btnCancelar: MaterialButton = dialogView.findViewById(R.id.btnCancelarCamp)
        val btnGuardar: MaterialButton = dialogView.findViewById(R.id.btnGuardarCamp)

        var currentLat = campToEdit?.latitud ?: 0.0
        var currentLon = campToEdit?.longitud ?: 0.0

        btnValidarUbicacionCamp.setOnClickListener {
            val rawAddress = etSector.text.toString().trim()
            if (rawAddress.isEmpty()) {
                Toast.makeText(context, "Ingrese un sector o ubicación para validar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                Toast.makeText(context, "Validando dirección con Retrofit (Nominatim)...", Toast.LENGTH_SHORT).show()
                AddressValidationManager.validateAddressAndSaveToFirestore(rawAddress) { success, msg, data ->
                    activity?.runOnUiThread {
                        if (success && data != null) {
                            val full = data["nombreCompleto"] as? String ?: rawAddress
                            currentLat = (data["latitud"] as? Double) ?: 0.0
                            currentLon = (data["longitud"] as? Double) ?: 0.0
                            etSector.setText(full)
                            Toast.makeText(context, "✅ Ubicación validada con Retrofit (Lat: $currentLat, Lon: $currentLon) y guardada en Firestore", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, msg ?: "No se encontraron resultados", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        if (campToEdit != null) {
            tvTitle.text = "Editar Campamento"
            etNombre.setText(campToEdit.nombre)
            etSector.setText(campToEdit.sector)
            etCapacidad.setText(campToEdit.capacidadCarpas.toString())
            etTarifa.setText(campToEdit.tarifaDiariaClp.toString())
            etTerreno.setText(campToEdit.tipoTerreno)
            switchAgua.isChecked = campToEdit.aguaPotable
            switchFogata.isChecked = campToEdit.permiteFogata
        } else {
            tvTitle.text = "Nuevo Campamento"
        }

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnCancelar.setOnClickListener { alertDialog.dismiss() }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val sector = etSector.text.toString().trim()
            val capacidadStr = etCapacidad.text.toString().trim()
            val tarifaStr = etTarifa.text.toString().trim()
            val terreno = etTerreno.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(context, "Debe ingresar el nombre del campamento", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sector.isEmpty()) {
                Toast.makeText(context, "Debe ingresar el sector o ubicación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val capacidad = capacidadStr.toIntOrNull()
            if (capacidad == null || capacidad <= 0) {
                Toast.makeText(context, "Ingrese una capacidad de carpas válida (mayor a 0)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val tarifa = tarifaStr.toIntOrNull()
            if (tarifa == null || tarifa <= 0) {
                Toast.makeText(context, "Ingrese una tarifa diaria válida (mayor a 0)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (terreno.isEmpty()) {
                Toast.makeText(context, "Debe ingresar el tipo de terreno", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val actionName = if (campToEdit != null) "actualizar" else "crear"
            AlertDialog.Builder(context)
                .setTitle("Confirmar Guardado")
                .setMessage("¿Deseas $actionName el campamento '$nombre'?")
                .setPositiveButton("Guardar") { _, _ ->
                    if (campToEdit != null) {
                        val updated = campToEdit.copy(
                            nombre = nombre,
                            sector = sector,
                            capacidadCarpas = capacidad,
                            tarifaDiariaClp = tarifa,
                            tipoTerreno = terreno,
                            aguaPotable = switchAgua.isChecked,
                            permiteFogata = switchFogata.isChecked,
                            latitud = currentLat,
                            longitud = currentLon
                        )
                        CampamentoRepository.update(updated, firebaseAnalytics) { success, error ->
                            if (success) {
                                Toast.makeText(context, "Campamento actualizado exitosamente", Toast.LENGTH_SHORT).show()
                                alertDialog.dismiss()
                            } else {
                                FirebaseCrashlytics.getInstance().recordException(Exception("Error al actualizar campamento: $error"))
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        val newCampamento = Campamento(
                            id = "",
                            nombre = nombre,
                            sector = sector,
                            capacidadCarpas = capacidad,
                            tipoTerreno = terreno,
                            permiteFogata = switchFogata.isChecked,
                            aguaPotable = switchAgua.isChecked,
                            tarifaDiariaClp = tarifa,
                            latitud = currentLat,
                            longitud = currentLon
                        )
                        CampamentoRepository.add(newCampamento, firebaseAnalytics) { success, error ->
                            if (success) {
                                Toast.makeText(context, "Campamento guardado exitosamente", Toast.LENGTH_SHORT).show()
                                alertDialog.dismiss()
                            } else {
                                FirebaseCrashlytics.getInstance().recordException(Exception("Error al crear campamento: $error"))
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

    private fun showDeleteConfirmation(campamento: Campamento) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar permanentemente '${campamento.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                CampamentoRepository.delete(campamento.id, firebaseAnalytics) { success, error ->
                    if (success) {
                        Toast.makeText(requireContext(), "Campamento eliminado exitosamente", Toast.LENGTH_SHORT).show()
                    } else {
                        FirebaseCrashlytics.getInstance().recordException(Exception("Error al eliminar campamento: $error"))
                        Toast.makeText(requireContext(), "Error al eliminar: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        CampamentoRepository.stopListening()
    }
}
