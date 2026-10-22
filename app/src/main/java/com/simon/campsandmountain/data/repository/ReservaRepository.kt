package com.simon.campsandmountain.data.repository

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.simon.campsandmountain.data.model.EstadoReserva
import com.simon.campsandmountain.data.model.Reserva
import com.simon.campsandmountain.data.model.TipoAlojamiento

object ReservaRepository {

    private const val TAG = "ReservaRepository"
    private const val COLLECTION = "Reservas"

    private fun getDb(): FirebaseFirestore = AuthRepository.getFirestore()

    private var cachedList = mutableListOf<Reserva>()
    private var listenerRegistration: ListenerRegistration? = null

    fun listen(
        analytics: FirebaseAnalytics? = null,
        onUpdate: (List<Reserva>) -> Unit
    ) {
        listenerRegistration?.remove()
        val db = getDb()

        listenerRegistration = db.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for $COLLECTION", error)
                    onUpdate(cachedList.toList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newList = mutableListOf<Reserva>()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.id
                            val nombreVisitante = doc.getString("nombreVisitante") ?: ""
                            val identificacionContacto = doc.getString("identificacionContacto") ?: ""
                            val tipoAlojamientoStr = doc.getString("tipoAlojamiento") ?: "REFUGIO"
                            val tipoAlojamiento = try { TipoAlojamiento.valueOf(tipoAlojamientoStr) } catch (_: Exception) { TipoAlojamiento.REFUGIO }
                            val idAlojamiento = doc.getString("idAlojamiento") ?: ""
                            val nombreAlojamiento = doc.getString("nombreAlojamiento") ?: ""
                            val fechaEntrada = doc.getString("fechaEntrada") ?: ""
                            val fechaSalida = doc.getString("fechaSalida") ?: ""
                            val cantidadPersonas = (doc.getLong("cantidadPersonas") ?: 1L).toInt()
                            val estadoStr = doc.getString("estado") ?: "CONFIRMADA"
                            val estado = try { EstadoReserva.valueOf(estadoStr) } catch (_: Exception) { EstadoReserva.CONFIRMADA }
                            val userId = doc.getString("userId") ?: ""
                            val userEmail = doc.getString("userEmail") ?: ""

                            val item = Reserva(
                                id = id,
                                nombreVisitante = nombreVisitante,
                                identificacionContacto = identificacionContacto,
                                tipoAlojamiento = tipoAlojamiento,
                                idAlojamiento = idAlojamiento,
                                nombreAlojamiento = nombreAlojamiento,
                                fechaEntrada = fechaEntrada,
                                fechaSalida = fechaSalida,
                                cantidadPersonas = cantidadPersonas,
                                estado = estado,
                                userId = userId,
                                userEmail = userEmail
                            )
                            newList.add(item)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing reserva doc: ${doc.id}", e)
                        }
                    }
                    cachedList = newList

                    analytics?.let {
                        val bundle = Bundle().apply { putString("tipo_entidad", "reserva") }
                        it.logEvent("listado_consultado", bundle)
                    }

                    onUpdate(cachedList.toList())
                }
            }
    }

    fun getAllCached(): List<Reserva> = cachedList.toList()

    fun add(
        reserva: Reserva,
        analytics: FirebaseAnalytics? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val db = getDb()
        val currentUid = AuthRepository.currentUser?.uid ?: ""
        val currentEmail = AuthRepository.currentUser?.email ?: ""

        val map = hashMapOf(
            "nombreVisitante" to reserva.nombreVisitante,
            "identificacionContacto" to reserva.identificacionContacto,
            "tipoAlojamiento" to reserva.tipoAlojamiento.name,
            "idAlojamiento" to reserva.idAlojamiento,
            "nombreAlojamiento" to reserva.nombreAlojamiento,
            "fechaEntrada" to reserva.fechaEntrada,
            "fechaSalida" to reserva.fechaSalida,
            "cantidadPersonas" to reserva.cantidadPersonas,
            "estado" to reserva.estado.name,
            "userId" to currentUid,
            "userEmail" to currentEmail
        )

        db.collection(COLLECTION).add(map)
            .addOnSuccessListener { docRef ->
                analytics?.let {
                    val bundle = Bundle().apply { putString("tipo_entidad", "reserva") }
                    it.logEvent("registro_creado", bundle)
                }
                onComplete(true, docRef.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding reserva", e)
                onComplete(false, e.localizedMessage)
            }
    }

    fun update(
        reserva: Reserva,
        analytics: FirebaseAnalytics? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val db = getDb()
        val map = hashMapOf(
            "nombreVisitante" to reserva.nombreVisitante,
            "identificacionContacto" to reserva.identificacionContacto,
            "tipoAlojamiento" to reserva.tipoAlojamiento.name,
            "idAlojamiento" to reserva.idAlojamiento,
            "nombreAlojamiento" to reserva.nombreAlojamiento,
            "fechaEntrada" to reserva.fechaEntrada,
            "fechaSalida" to reserva.fechaSalida,
            "cantidadPersonas" to reserva.cantidadPersonas,
            "estado" to reserva.estado.name,
            "userId" to reserva.userId,
            "userEmail" to reserva.userEmail
        )

        db.collection(COLLECTION).document(reserva.id).set(map)
            .addOnSuccessListener {
                analytics?.let {
                    val bundle = Bundle().apply { putString("tipo_entidad", "reserva") }
                    it.logEvent("registro_actualizado", bundle)
                }
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating reserva ${reserva.id}", e)
                onComplete(false, e.localizedMessage)
            }
    }

    fun delete(
        id: String,
        analytics: FirebaseAnalytics? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val db = getDb()
        db.collection(COLLECTION).document(id).delete()
            .addOnSuccessListener {
                analytics?.let {
                    val bundle = Bundle().apply { putString("tipo_entidad", "reserva") }
                    it.logEvent("registro_eliminado", bundle)
                }
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting reserva $id", e)
                onComplete(false, e.localizedMessage)
            }
    }

    fun search(query: String): List<Reserva> {
        if (query.isBlank()) return getAllCached()
        val q = query.trim().lowercase()
        return cachedList.filter {
            it.nombreVisitante.lowercase().contains(q) ||
                    it.nombreAlojamiento.lowercase().contains(q) ||
                    it.id.lowercase().contains(q)
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
