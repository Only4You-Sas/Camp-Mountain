package com.simon.campsandmountain.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
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
import androidx.fragment.app.FragmentActivity
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

/**
 * Gestor profesional de autenticación nativa con Passkeys y Biometría (Huella / Rostro)
 * utilizando las APIs androidx.credentials y androidx.biometric de Android.
 */
object PasskeyAuthManager {

    private const val TAG = "PasskeyAuthManager"
    private const val RP_ID = "campandmountain-86f56.firebaseapp.com"
    private const val RP_NAME = "Camp & Mountain"

    /**
     * Genera un desafío criptográfico aleatorio Base64.
     */
    fun generateRandomChallenge(): String {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Autentica al usuario de forma 100% nativa con la Huella Digital / Rostro / PIN del dispositivo
     * usando BiometricPrompt de Android.
     */
    fun authenticateWithBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Autenticación Biométrica",
        subtitle: String = "Usa tu huella digital o rostro para iniciar sesión",
        onResult: (Boolean, String?) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onResult(false, "El dispositivo no tiene biometría configurada o activa.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Autenticación biométrica exitosa.")
                    onResult(true, "Autenticación biométrica exitosa.")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.w(TAG, "Error de biometría ($errorCode): $errString")
                    onResult(false, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Fallo de lectura biométrica.")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Crea una Passkey WebAuthn en el dispositivo mediante Credential Manager.
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
                        put("alg", -7)
                    })
                    put(JSONObject().apply {
                        put("type", "public-key")
                        put("alg", -257)
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
            
            Log.d(TAG, "Passkey creada exitosamente.")
            Result.success(response)
        } catch (e: CreateCredentialCancellationException) {
            Result.failure(Exception("Creación de Passkey cancelada por el usuario."))
        } catch (e: CreateCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Autentica con Passkey WebAuthn mediante Credential Manager.
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
                Result.success(credential.authenticationResponseJson)
            } else {
                Result.failure(Exception("Tipo de credencial no soportado."))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Autenticación cancelada."))
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
