package com.simon.campsandmountain.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

/**
 * Gestor profesional de autenticación nativa con Passkeys y Biometría
 * utilizando la API androidx.credentials (Credential Manager).
 */
object PasskeyAuthManager {

    private const val TAG = "PasskeyAuthManager"
    private const val RP_ID = "campandmountain-86f56.firebaseapp.com"
    private const val RP_NAME = "Camp & Mountain"

    /**
     * Genera un desafío criptográfico aleatorio de prueba (Challenge Base64).
     */
    fun generateRandomChallenge(): String {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Crea una Passkey en el dispositivo del usuario utilizando Credential Manager y Biometría.
     */
    suspend fun createPasskey(
        context: Context,
        username: String,
        displayName: String,
        challengeBase64: String = generateRandomChallenge()
    ): Result<CreatePublicKeyCredentialResponse> {
        val credentialManager = CredentialManager.create(context)

        return try {
            val userHandle = Base64.encodeToString(username.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

            val requestJson = JSONObject().apply {
                put("rp", JSONObject().apply {
                    put("name", RP_NAME)
                    put("id", RP_ID)
                })
                put("user", JSONObject().apply {
                    put("id", userHandle)
                    put("name", username)
                    put("displayName", displayName)
                })
                put("challenge", challengeBase64)
                put("pubKeyCredParams", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "public-key")
                        put("alg", -7) // ES256
                    })
                    put(JSONObject().apply {
                        put("type", "public-key")
                        put("alg", -257) // RS256
                    })
                })
                put("timeout", 60000)
                put("attestation", "none")
                put("authenticatorSelection", JSONObject().apply {
                    put("authenticatorAttachment", "platform")
                    put("userVerification", "required")
                    put("residentKey", "required")
                })
            }.toString()

            val createRequest = CreatePublicKeyCredentialRequest(requestJson)
            val response = credentialManager.createCredential(context, createRequest) as CreatePublicKeyCredentialResponse
            
            Log.d(TAG, "Passkey creada exitosamente en el dispositivo.")
            Result.success(response)
        } catch (e: CreateCredentialCancellationException) {
            Log.w(TAG, "El usuario canceló la creación de la Passkey.")
            Result.failure(Exception("Creación de Passkey cancelada por el usuario."))
        } catch (e: CreateCredentialException) {
            Log.e(TAG, "Error al crear la Passkey", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al crear Passkey", e)
            Result.failure(e)
        }
    }

    /**
     * Autentica al usuario consultando la Passkey guardada localmente mediante Biometría.
     */
    suspend fun authenticateWithPasskey(
        context: Context,
        challengeBase64: String = generateRandomChallenge()
    ): Result<String> {
        val credentialManager = CredentialManager.create(context)

        return try {
            val getJson = JSONObject().apply {
                put("challenge", challengeBase64)
                put("rpId", RP_ID)
                put("userVerification", "required")
            }.toString()

            val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(getJson)
            val getCredentialRequest = GetCredentialRequest(listOf(getPublicKeyCredentialOption))

            val response: GetCredentialResponse = credentialManager.getCredential(context, getCredentialRequest)

            val credential = response.credential
            if (credential is PublicKeyCredential) {
                val authenticationResponseJson = credential.authenticationResponseJson
                Log.d(TAG, "Autenticación exitosa con Passkey: $authenticationResponseJson")
                Result.success(authenticationResponseJson)
            } else {
                Result.failure(Exception("El tipo de credencial retornado no es PublicKeyCredential."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "El usuario canceló la autenticación con Passkey.")
            Result.failure(Exception("Autenticación cancelada."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Error al consultar la Passkey", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado en autenticación Passkey", e)
            Result.failure(e)
        }
    }
}
