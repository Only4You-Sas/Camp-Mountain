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

/**
 * Modelo de respuesta de la API pública OpenStreetMap Nominatim.
 */
data class NominatimResponseItem(
    @SerializedName("place_id") val placeId: Long,
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("type") val type: String?,
    @SerializedName("importance") val importance: Double?
)

/**
 * Interfaz Retrofit para consultar OpenStreetMap Nominatim API.
 */
interface NominatimApiService {
    @GET("search")
    suspend fun searchAddress(
        @Query("q") address: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Header("User-Agent") userAgent: String = "CampAndMountainApp/1.0 (contact@campandmountain.com)"
    ): List<NominatimResponseItem>
}

/**
 * Servicio profesional para validar direcciones con Retrofit/Nominatim y guardarlas en Cloud Firestore.
 */
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

    /**
     * Valida una dirección en formato texto mediante Nominatim (Retrofit)
     * y guarda el resultado geocodificado en Cloud Firestore.
     */
    suspend fun validateAddressAndSaveToFirestore(
        addressText: String,
        onResult: (Boolean, String?, Map<String, Any>?) -> Unit
    ) {
        if (addressText.isBlank()) {
            onResult(false, "La dirección no puede estar vacía.", null)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val results = retrofitService.searchAddress(addressText.trim())

                if (results.isNotEmpty()) {
                    val topResult = results[0]
                    val latNum = topResult.lat.toDoubleOrNull() ?: 0.0
                    val lonNum = topResult.lon.toDoubleOrNull() ?: 0.0

                    val recordMap = hashMapOf<String, Any>(
                        "direccionConsultada" to addressText.trim(),
                        "nombreCompleto" to topResult.displayName,
                        "latitud" to latNum,
                        "longitud" to lonNum,
                        "placeId" to topResult.placeId,
                        "fechaValidacion" to FieldValue.serverTimestamp()
                    )

                    val db = FirebaseFirestore.getInstance()
                    db.collection(COLLECTION_NAME).add(recordMap)
                        .addOnSuccessListener { docRef ->
                            Log.d(TAG, "Dirección guardada en Firestore con ID: ${docRef.id}")
                            onResult(true, "Dirección validada y guardada exitosamente.", recordMap)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al guardar dirección en Firestore", e)
                            onResult(false, "Válida en API pero fallo al guardar en Firestore: ${e.localizedMessage}", recordMap)
                        }
                } else {
                    Log.w(TAG, "No se encontraron resultados para la dirección: $addressText")
                    onResult(false, "No se encontraron resultados válidos para la dirección especificada.", null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al consultar la API de Nominatim con Retrofit", e)
                onResult(false, "Error de red/API: ${e.localizedMessage}", null)
            }
        }
    }
}
