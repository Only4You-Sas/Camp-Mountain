package com.simon.campsandmountain.location

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class NominatimResponseItem(
    @SerializedName("place_id") val placeId: Long,
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("type") val type: String?,
    @SerializedName("importance") val importance: Double?
)

interface NominatimApiService {
    @GET("search")
    suspend fun searchAddress(
        @Query("q") address: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 3,
        @Header("User-Agent") userAgent: String = "CampAndMountainApp/1.0"
    ): List<NominatimResponseItem>
}

object AddressValidationManager {

    private const val TAG = "AddressValidation"
    private const val BASE_URL = "https://nominatim.openstreetmap.org/"
    private const val COLLECTION_NAME = "DireccionesValidadas"

    private val retrofitService: NominatimApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApiService::class.java)
    }

    suspend fun validateAddressAndSaveToFirestore(
        addressText: String,
        onResult: (Boolean, String?, Map<String, Any>?) -> Unit
    ) {
        val query = addressText.trim()
        if (query.isBlank()) {
            withContext(Dispatchers.Main) {
                onResult(false, "La dirección no puede estar vacía.", null)
            }
            return
        }

        withContext(Dispatchers.IO) {
            try {
                var results = retrofitService.searchAddress(query)

                if (results.isEmpty() && !query.lowercase().contains("chile")) {
                    results = retrofitService.searchAddress("$query, Chile")
                }

                if (results.isNotEmpty()) {
                    val topResult = results[0]
                    val latNum = topResult.lat.toDoubleOrNull() ?: 0.0
                    val lonNum = topResult.lon.toDoubleOrNull() ?: 0.0

                    val recordMap = hashMapOf<String, Any>(
                        "direccionConsultada" to query,
                        "nombreCompleto" to topResult.displayName,
                        "latitud" to latNum,
                        "longitud" to lonNum,
                        "placeId" to topResult.placeId,
                        "fechaValidacion" to FieldValue.serverTimestamp()
                    )

                    val db = FirebaseFirestore.getInstance()
                    db.collection(COLLECTION_NAME).add(recordMap)
                        .addOnSuccessListener { docRef ->
                            Log.d(TAG, "Dirección guardada en Firestore ID: ${docRef.id}")
                        }

                    withContext(Dispatchers.Main) {
                        onResult(true, "Ubicación real confirmada.", recordMap)
                    }
                } else {
                    Log.w(TAG, "No se encontraron resultados para la dirección: $query")
                    withContext(Dispatchers.Main) {
                        onResult(false, "No se encontraron coordenadas reales para '$query'. Escriba la ciudad/comuna (ej. 'San José de Maipo, Chile').", null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al consultar la API de Nominatim", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "Error de conexión al validar dirección: ${e.localizedMessage}", null)
                }
            }
        }
    }
}
