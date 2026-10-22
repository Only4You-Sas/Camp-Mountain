package com.simon.campsandmountain.data.repository

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.simon.campsandmountain.data.model.EstadoRefugio
import com.simon.campsandmountain.data.model.Refugio

object RefugioRepository {

    private const val TAG = "RefugioRepository"
    private const val COLLECTION = "Refugios"

    private fun getDb(): FirebaseFirestore = AuthRepository.getFirestore()

    private var cachedList = mutableListOf<Refugio>()
    private var listenerRegistration: ListenerRegistration? = null

    private val defaultRefugiosChilenos = listOf(
        Refugio(
            id = "",
            nombre = "Refugio Los Cuernos",
            altitudMsnm = 120,
            capacidadCamas = 36,
            estado = EstadoRefugio.ABIERTO,
            precioNocheClp = 98000,
            ubicacionSector = "Circuito W, Parque Nacional Torres del Paine",
            telefonoContacto = "+56 61 222 5986"
        ),
        Refugio(
            id = "",
            nombre = "Refugio Lo Valdés",
            altitudMsnm = 1900,
            capacidadCamas = 28,
            estado = EstadoRefugio.ABIERTO,
            precioNocheClp = 45000,
            ubicacionSector = "Valle del Volcán, San José de Maipo",
            telefonoContacto = "+56 2 2861 1234"
        ),
        Refugio(
            id = "",
            nombre = "Refugio Teski Volcán Osorno",
            altitudMsnm = 1240,
            capacidadCamas = 40,
            estado = EstadoRefugio.ABIERTO,
            precioNocheClp = 55000,
            ubicacionSector = "Ladera Sur Volcán Osorno, Puerto Varas",
            telefonoContacto = "+56 65 223 3445"
        )
    )

    fun listen(
        analytics: FirebaseAnalytics? = null,
        onUpdate: (List<Refugio>) -> Unit
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
                    if (snapshot.isEmpty) {
                        seedInitialRefugios()
                        return@addSnapshotListener
                    }

                    val newList = mutableListOf<Refugio>()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.id
                            val nombre = doc.getString("nombre") ?: ""
                            val altitudMsnm = (doc.getLong("altitudMsnm") ?: 0L).toInt()
                            val capacidadCamas = (doc.getLong("capacidadCamas") ?: 0L).toInt()
                            val estadoStr = doc.getString("estado") ?: "ABIERTO"
                            val estado = try { EstadoRefugio.valueOf(estadoStr) } catch (_: Exception) { EstadoRefugio.ABIERTO }
                            val precioNocheClp = (doc.getLong("precioNocheClp") ?: 0L).toInt()
                            val ubicacionSector = doc.getString("ubicacionSector") ?: ""
                            val telefonoContacto = doc.getString("telefonoContacto") ?: ""

                            val item = Refugio(
                                id = id,
                                nombre = nombre,
                                altitudMsnm = altitudMsnm,
                                capacidadCamas = capacidadCamas,
                                estado = estado,
                                precioNocheClp = precioNocheClp,
                                ubicacionSector = ubicacionSector,
                                telefonoContacto = telefonoContacto
                            )
                            newList.add(item)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing refugio doc: ${doc.id}", e)
                        }
                    }
                    cachedList = newList

                    analytics?.let {
                        val bundle = Bundle().apply { putString("tipo_entidad", "refugio") }
                        it.logEvent("listado_consultado", bundle)
                    }

                    onUpdate(cachedList.toList())
                }
            }
    }

    private fun seedInitialRefugios() {
        val db = getDb()
        for (refugio in defaultRefugiosChilenos) {
            val map = hashMapOf(
                "nombre" to refugio.nombre,
                "altitudMsnm" to refugio.altitudMsnm,
                "capacidadCamas" to refugio.capacidadCamas,
                "estado" to refugio.estado.name,
                "precioNocheClp" to refugio.precioNocheClp,
                "ubicacionSector" to refugio.ubicacionSector,
                "telefonoContacto" to refugio.telefonoContacto
            )
            db.collection(COLLECTION).add(map)
        }
    }

    fun getAllCached(): List<Refugio> = cachedList.toList()

    fun add(
        refugio: Refugio,
        analytics: FirebaseAnalytics? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val db = getDb()
        val map = hashMapOf(
            "nombre" to refugio.nombre,
            "altitudMsnm" to refugio.altitudMsnm,
            "capacidadCamas" to refugio.capacidadCamas,
            "estado" to refugio.estado.name,
            "precioNocheClp" to refugio.precioNocheClp,
            "ubicacionSector" to refugio.ubicacionSector,
            "telefonoContacto" to refugio.telefonoContacto
        )

        db.collection(COLLECTION).add(map)
            .addOnSuccessListener { docRef ->
                analytics?.let {
                    val bundle = Bundle().apply { putString("tipo_entidad", "refugio") }
                    it.logEvent("registro_creado", bundle)
                }
                onComplete(true, docRef.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding refugio", e)
                onComplete(false, e.localizedMessage)
            }
    }

    fun update(
        refugio: Refugio,
        analytics: FirebaseAnalytics? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val db = getDb()
        val map = hashMapOf(
            "nombre" to refugio.nombre,
            "altitudMsnm" to refugio.altitudMsnm,
            "capacidadCamas" to refugio.capacidadCamas,
            "estado" to refugio.estado.name,
            "precioNocheClp" to refugio.precioNocheClp,
            "ubicacionSector" to refugio.ubicacionSector,
            "telefonoContacto" to refugio.telefonoContacto
        )

        db.collection(COLLECTION).document(refugio.id).set(map)
            .addOnSuccessListener {
                analytics?.let {
                    val bundle = Bundle().apply { putString("tipo_entidad", "refugio") }
                    it.logEvent("registro_actualizado", bundle)
                }
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating refugio ${refugio.id}", e)
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
                    val bundle = Bundle().apply { putString("tipo_entidad", "refugio") }
                    it.logEvent("registro_eliminado", bundle)
                }
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting refugio $id", e)
                onComplete(false, e.localizedMessage)
            }
    }

    fun search(query: String): List<Refugio> {
        if (query.isBlank()) return getAllCached()
        val q = query.trim().lowercase()
        return cachedList.filter {
            it.nombre.lowercase().contains(q) ||
                    it.ubicacionSector.lowercase().contains(q) ||
                    it.altitudMsnm.toString().contains(q)
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
