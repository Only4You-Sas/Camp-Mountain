package com.simon.campsandmountain.data.importer

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.opencsv.CSVReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Servicio profesional para importar archivos CSV de forma masiva a Cloud Firestore
 * utilizando OpenCSV y operaciones por lotes de alto rendimiento (WriteBatch).
 */
object CsvBatchImporter {

    private const val TAG = "CsvBatchImporter"
    private const val BATCH_SIZE_LIMIT = 500 // Límite máximo permitido por Firestore en WriteBatch

    /**
     * Lee un archivo CSV desde la carpeta 'assets' o un InputStream y sube los registros
     * eficientemente a Cloud Firestore en lotes de hasta 500 documentos por batch.
     */
    suspend fun importCsvFromAssetsToFirestore(
        context: Context,
        assetFileName: String,
        collectionName: String,
        onProgress: ((importedCount: Int) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = context.assets.open(assetFileName)
            importCsvInputStreamToFirestore(inputStream, collectionName, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir el archivo CSV desde assets: $assetFileName", e)
            Result.failure(e)
        }
    }

    /**
     * Procesa un InputStream de un archivo CSV y ejecuta lotes masivos de escritura en Firestore.
     */
    suspend fun importCsvInputStreamToFirestore(
        inputStream: InputStream,
        collectionName: String,
        onProgress: ((importedCount: Int) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        var totalImported = 0
        val db = FirebaseFirestore.getInstance()

        try {
            val reader = CSVReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val header = reader.readNext()

            if (header == null) {
                return@withContext Result.failure(Exception("El archivo CSV está vacío o no tiene encabezado."))
            }

            var nextLine: Array<String>?
            var currentBatch = db.batch()
            var currentBatchCount = 0

            while (reader.readNext().also { nextLine = it } != null) {
                val row = nextLine ?: continue
                if (row.isEmpty() || (row.size == 1 && row[0].isBlank())) continue

                val docData = hashMapOf<String, Any>()
                for (i in header.indices) {
                    if (i < row.size) {
                        val key = header[i].trim()
                        val rawValue = row[i].trim()
                        
                        // Conversión automática de tipos comunes (Long, Double, Boolean)
                        val convertedValue: Any = when {
                            rawValue.equals("true", ignoreCase = true) -> true
                            rawValue.equals("false", ignoreCase = true) -> false
                            rawValue.toLongOrNull() != null -> rawValue.toLong()
                            rawValue.toDoubleOrNull() != null -> rawValue.toDouble()
                            else -> rawValue
                        }
                        docData[key] = convertedValue
                    }
                }

                val docRef = db.collection(collectionName).document()
                currentBatch.set(docRef, docData)

                currentBatchCount++
                totalImported++

                // Cuando se alcanza el límite de 500 documentos, se hace commit del batch actual y se inicia uno nuevo
                if (currentBatchCount >= BATCH_SIZE_LIMIT) {
                    currentBatch.commit().await()
                    Log.d(TAG, "Batch de $currentBatchCount documentos confirmado en Firestore.")
                    onProgress?.invoke(totalImported)

                    currentBatch = db.batch()
                    currentBatchCount = 0
                }
            }

            // Commit del lote final restante si contiene documentos
            if (currentBatchCount > 0) {
                currentBatch.commit().await()
                Log.d(TAG, "Lote final de $currentBatchCount documentos confirmado en Firestore.")
                onProgress?.invoke(totalImported)
            }

            reader.close()
            inputStream.close()

            Log.d(TAG, "Importación masiva completada exitosamente. Total registros: $totalImported")
            Result.success(totalImported)
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la importación masiva en lote", e)
            Result.failure(e)
        }
    }
}
