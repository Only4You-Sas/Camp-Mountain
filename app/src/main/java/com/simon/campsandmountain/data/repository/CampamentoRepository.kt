package com.simon.campsandmountain.data.repository

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.simon.campsandmountain.data.model.Campamento

object CampamentoRepository {

    private const val TAG = "CampamentoRepository"
    private const val COLLECTION = "Campamentos"

    private fun getDb(): FirebaseFirestore = AuthRepository.getFirestore()

    private var cachedList = mutableListOf<Campamento>()
    private var listenerRegistration: ListenerRegistration? = null

    fun listen(
        analytics: FirebaseAnalytics? = null,
        onUpdate: (List<Campamento>) -> Unit
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
                    val newList = mutableListOf<Campamento>()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.id
                            val nombre = doc.getString("nombre") ?: ""
                            val sector = doc.getString("sector") ?: ""
                            val capacidadCarpas = (doc.getLong("capacidadCarpas") ?: 0L).toInt()
                            val tipoTerreno = doc.getString("tipoTerreno") ?: ""
                            val permiteFogata = doc.getBoolean("permiteFogata") ?: false
                            val aguaPotable = doc.getBoolean("aguaPotable") ?: false
                            val tarifaDiariaClp = (doc.getLong("tarifaDiariaClp") ?: 0L).toInt()

                            val item = Campamento(
                                id = id,
                                nombre = nombre,
                                sector = sector,
                                capacidadCarpas = capacidadCarpas,
                                tipoTerreno = tipoTerreno,
                                permiteFogata = permiteFogata,
                                aguaPotable = aguaPotable,
                                tarifaDiariaClp = tarifaDiariaClp
                            )
                            newList.add(item)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing campamento doc: ${doc.id}", e)
                        }
                    }
                    cachedList = newList

                    analytics?.let {
                        val bundle = Bundle().apply { putString("tipo_entidad", "campamento") }
                        it.logEvent("listado_consultado", bundle)
                    }

                    onUpdate(cachedList.toList())
                }
            }
    }

    fun getAllCached(): List<Campamento> = cachedList.toList()

    fun add(
        campamento: Campamento,
        analytics: FirebaseAnalytics? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val db = getDb()
        val map = hashMapOf(
            "nombre" to campamento.nombre,
            "sector" to campamento.sector,
            "capacidadCarpas" to campamento.capacidadCarpas,
            "tipoTerreno" to campamento.tipoTerreno,
            "permiteFogata" to campamento.permiteFogata,
            "aguaPotable" to campamento.aguaPotable,
            "tarifaDiariaClp" to campamento.tarifaDiariaClp
        )

        db.collection(COLLECTION).add(map)
            .addOnSuccessListener { docRef ->
                analytics?.let {
                    val bundle = Bundle().apply { putString("tipo_entidad", "campamento") }
                    it.logEvent("registro_creado", bundle)
                }
                onComplete(true, docRef.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding campamento", e)
                onComplete(false, e.localizedMessage)
            }
    }

    fun update(
        campamento: Campamento,
        analytics: FirebaseAnalytics? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val db = getDb()
        val map = hashMapOf(
            "nombre" to campamento.nombre,
            "sector" to campamento.sector,
            "capacidadCarpas" to campamento.capacidadCarpas,
            "tipoTerreno" to campamento.tipoTerreno,
            "permiteFogata" to campamento.permiteFogata,
            "aguaPotable" to campamento.aguaPotable,
            "tarifaDiariaClp" to campamento.tarifaDiariaClp
        )

        db.collection(COLLECTION).document(campamento.id).set(map)
            .addOnSuccessListener {
                analytics?.let {
                    val bundle = Bundle().apply { putString("tipo_entidad", "campamento") }
                    it.logEvent("registro_actualizado", bundle)
                }
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating campamento ${campamento.id}", e)
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
                    val bundle = Bundle().apply { putString("tipo_entidad", "campamento") }
                    it.logEvent("registro_eliminado", bundle)
                }
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting campamento $id", e)
                onComplete(false, e.localizedMessage)
            }
    }

    fun search(query: String): List<Campamento> {
        if (query.isBlank()) return getAllCached()
        val q = query.trim().lowercase()
        return cachedList.filter {
            it.nombre.lowercase().contains(q) ||
                    it.sector.lowercase().contains(q) ||
                    it.tipoTerreno.lowercase().contains(q)
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
