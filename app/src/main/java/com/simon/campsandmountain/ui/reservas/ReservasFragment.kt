package com.simon.campsandmountain.ui.reservas

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.simon.campsandmountain.data.model.EstadoReserva
import com.simon.campsandmountain.data.model.Reserva
import com.simon.campsandmountain.data.model.TipoAlojamiento
import com.simon.campsandmountain.data.repository.CampamentoRepository
import com.simon.campsandmountain.data.repository.RefugioRepository
import com.simon.campsandmountain.data.repository.ReservaRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReservasFragment : Fragment() {

    private lateinit var adapter: ReservaAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reservas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvEmpty = view.findViewById(R.id.tvEmptyReservas)
        etSearch = view.findViewById(R.id.etSearchReserva)
        val rv: RecyclerView = view.findViewById(R.id.rvReservas)
        val fab: FloatingActionButton = view.findViewById(R.id.fabAddReserva)

        adapter = ReservaAdapter(
            list = ReservaRepository.getAll(),
            onEditClick = { reserva -> showReservaFormDialog(reserva) },
            onDeleteClick = { reserva -> showDeleteConfirmation(reserva) }
        )
        rv.adapter = adapter

        fab.setOnClickListener { showReservaFormDialog(null) }

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
        val filtered = ReservaRepository.search(query)
        adapter.updateData(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    fun refreshData() {
        val currentQuery = etSearch.text.toString()
        filterList(currentQuery)
    }

    private fun showReservaFormDialog(reservaToEdit: Reserva?) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_form_reserva, null)

        val tvTitle: TextView = dialogView.findViewById(R.id.tvFormTitleReserva)
        val etNombre: EditText = dialogView.findViewById(R.id.etNombreVisitante)
        val etContacto: EditText = dialogView.findViewById(R.id.etContactoReserva)
        val rgTipo: RadioGroup = dialogView.findViewById(R.id.rgTipoAlojamiento)
        val rbRefugio: RadioButton = dialogView.findViewById(R.id.rbRefugio)
        val rbCampamento: RadioButton = dialogView.findViewById(R.id.rbCampamento)
        val spinnerLugar: AutoCompleteTextView = dialogView.findViewById(R.id.spinnerLugar)
        val etEntrada: EditText = dialogView.findViewById(R.id.etFechaEntrada)
        val etSalida: EditText = dialogView.findViewById(R.id.etFechaSalida)
        val etPersonas: EditText = dialogView.findViewById(R.id.etCantidadPersonas)
        val rgEstado: RadioGroup = dialogView.findViewById(R.id.rgEstadoReserva)
        val rbConfirmada: RadioButton = dialogView.findViewById(R.id.rbConfirmada)
        val rbPendiente: RadioButton = dialogView.findViewById(R.id.rbPendiente)
        val btnCancelar: MaterialButton = dialogView.findViewById(R.id.btnCancelarReserva)
        val btnGuardar: MaterialButton = dialogView.findViewById(R.id.btnGuardarReserva)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun showDatePicker(targetEditText: EditText) {
            val y = calendar.get(Calendar.YEAR)
            val m = calendar.get(Calendar.MONTH)
            val d = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth)
                targetEditText.setText(dateFormat.format(selectedCal.time))
            }, y, m, d).show()
        }

        etEntrada.setOnClickListener { showDatePicker(etEntrada) }
        etSalida.setOnClickListener { showDatePicker(etSalida) }

        val lugaresList = mutableListOf<Pair<String, String>>()
        var selectedLugarIndex = 0

        fun updateSpinnerOptions(isRefugio: Boolean) {
            lugaresList.clear()
            if (isRefugio) {
                RefugioRepository.getAll().forEach {
                    lugaresList.add(Pair(it.id, it.nombre))
                }
            } else {
                CampamentoRepository.getAll().forEach {
                    lugaresList.add(Pair(it.id, it.nombre))
                }
            }
            val displayNames = lugaresList.map { it.second }
            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, displayNames)
            spinnerLugar.setAdapter(spinnerAdapter)
            if (displayNames.isNotEmpty()) {
                spinnerLugar.setText(displayNames[0], false)
                selectedLugarIndex = 0
            } else {
                spinnerLugar.setText("", false)
                selectedLugarIndex = -1
            }
        }

        spinnerLugar.setOnItemClickListener { _, _, position, _ ->
            selectedLugarIndex = position
        }

        rgTipo.setOnCheckedChangeListener { _, checkedId ->
            updateSpinnerOptions(checkedId == R.id.rbRefugio)
        }

        updateSpinnerOptions(true)

        if (reservaToEdit != null) {
            tvTitle.text = "Editar Reserva"
            etNombre.setText(reservaToEdit.nombreVisitante)
            etContacto.setText(reservaToEdit.identificacionContacto)
            etEntrada.setText(reservaToEdit.fechaEntrada)
            etSalida.setText(reservaToEdit.fechaSalida)
            etPersonas.setText(reservaToEdit.cantidadPersonas.toString())

            if (reservaToEdit.tipoAlojamiento == TipoAlojamiento.REFUGIO) {
                rbRefugio.isChecked = true
                updateSpinnerOptions(true)
            } else {
                rbCampamento.isChecked = true
                updateSpinnerOptions(false)
            }

            val selectedIndex = lugaresList.indexOfFirst { it.first == reservaToEdit.idAlojamiento }
            if (selectedIndex != -1) {
                spinnerLugar.setText(lugaresList[selectedIndex].second, false)
                selectedLugarIndex = selectedIndex
            }

            if (reservaToEdit.estado == EstadoReserva.CONFIRMADA) {
                rbConfirmada.isChecked = true
            } else {
                rbPendiente.isChecked = true
            }
        } else {
            tvTitle.text = "Nueva Reserva"
        }

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnCancelar.setOnClickListener { alertDialog.dismiss() }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val contacto = etContacto.text.toString().trim()
            val entrada = etEntrada.text.toString().trim()
            val salida = etSalida.text.toString().trim()
            val personasStr = etPersonas.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(context, "Debe ingresar el nombre del visitante", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (contacto.isEmpty()) {
                Toast.makeText(context, "Debe ingresar teléfono o email de contacto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lugaresList.isEmpty() || selectedLugarIndex < 0 || selectedLugarIndex >= lugaresList.size) {
                Toast.makeText(context, "Debe seleccionar un lugar de alojamiento", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (entrada.isEmpty()) {
                Toast.makeText(context, "Debe seleccionar la fecha de entrada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (salida.isEmpty()) {
                Toast.makeText(context, "Debe seleccionar la fecha de salida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dateEntrada = try { dateFormat.parse(entrada) } catch (e: Exception) { null }
            val dateSalida = try { dateFormat.parse(salida) } catch (e: Exception) { null }

            if (dateEntrada == null || dateSalida == null) {
                Toast.makeText(context, "Formato de fecha inválido. Use AAAA-MM-DD", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dateSalida.before(dateEntrada)) {
                Toast.makeText(context, "La fecha de salida no puede ser anterior a la fecha de entrada", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val personas = personasStr.toIntOrNull()
            if (personas == null || personas <= 0) {
                Toast.makeText(context, "La cantidad de personas debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedLugarPair = lugaresList[selectedLugarIndex]
            val isRefugio = rgTipo.checkedRadioButtonId == R.id.rbRefugio
            val tipoAlojamiento = if (isRefugio) TipoAlojamiento.REFUGIO else TipoAlojamiento.CAMPAMENTO
            val estadoReserva = if (rgEstado.checkedRadioButtonId == R.id.rbConfirmada) EstadoReserva.CONFIRMADA else EstadoReserva.PENDIENTE

            if (reservaToEdit != null) {
                val updated = reservaToEdit.copy(
                    nombreVisitante = nombre,
                    identificacionContacto = contacto,
                    tipoAlojamiento = tipoAlojamiento,
                    idAlojamiento = selectedLugarPair.first,
                    nombreAlojamiento = selectedLugarPair.second,
                    fechaEntrada = entrada,
                    fechaSalida = salida,
                    cantidadPersonas = personas,
                    estado = estadoReserva
                )
                ReservaRepository.update(updated)
                Toast.makeText(context, "Reserva actualizada", Toast.LENGTH_SHORT).show()
            } else {
                ReservaRepository.add(
                    nombreVisitante = nombre,
                    identificacionContacto = contacto,
                    tipoAlojamiento = tipoAlojamiento,
                    idAlojamiento = selectedLugarPair.first,
                    nombreAlojamiento = selectedLugarPair.second,
                    fechaEntrada = entrada,
                    fechaSalida = salida,
                    cantidadPersonas = personas,
                    estado = estadoReserva
                )

                val analyticsBundle = Bundle().apply {
                    putString("nombre_visitante", nombre)
                    putString("tipo_alojamiento", tipoAlojamiento.label)
                    putString("nombre_alojamiento", selectedLugarPair.second)
                    putInt("cantidad_personas", personas)
                    putString("fecha_entrada", entrada)
                    putString("fecha_salida", salida)
                    putString("estado", estadoReserva.label)
                }
                FirebaseAnalytics.getInstance(context).logEvent("reserva_creada", analyticsBundle)

                Toast.makeText(context, "Reserva creada exitosamente", Toast.LENGTH_SHORT).show()
            }

            alertDialog.dismiss()
            refreshData()
        }

        alertDialog.show()
    }

    private fun showDeleteConfirmation(reserva: Reserva) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Reserva")
            .setMessage("¿Estás seguro de que deseas eliminar la reserva de '${reserva.nombreVisitante}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                ReservaRepository.delete(reserva.id)
                Toast.makeText(requireContext(), "Reserva eliminada", Toast.LENGTH_SHORT).show()
                refreshData()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
