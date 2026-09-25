package com.simon.campsandmountain.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simon.campsandmountain.data.model.User

object AuthRepository {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private fun getFirestore(): FirebaseFirestore {
        return try {
            FirebaseFirestore.getInstance("usuarios")
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    var currentUser: User? = null

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun logout() {
        auth.signOut()
        currentUser = null
    }

    fun fetchUserData(onComplete: (User?) -> Unit) {
        val fbUser = auth.currentUser
        if (fbUser == null) {
            currentUser = null
            onComplete(null)
            return
        }

        val db = getFirestore()
        db.collection("Usuarios").document(fbUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: fbUser.displayName ?: "Usuario"
                    val correo = document.getString("correo") ?: fbUser.email ?: ""
                    val user = User(
                        uid = fbUser.uid,
                        fullName = nombre,
                        email = correo,
                        role = "Guardaparque"
                    )
                    currentUser = user
                    onComplete(user)
                } else {
                    val fallbackUser = User(
                        uid = fbUser.uid,
                        fullName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "Usuario",
                        email = fbUser.email ?: "",
                        role = "Guardaparque"
                    )
                    currentUser = fallbackUser
                    onComplete(fallbackUser)
                }
            }
            .addOnFailureListener {
                val fallbackUser = User(
                    uid = fbUser.uid,
                    fullName = fbUser.email?.substringBefore("@") ?: "Usuario",
                    email = fbUser.email ?: "",
                    role = "Guardaparque"
                )
                currentUser = fallbackUser
                onComplete(fallbackUser)
            }
    }

    fun saveUserDataToFirestore(uid: String, nombre: String, correo: String, onComplete: (Boolean, String?) -> Unit) {
        val userMap = hashMapOf(
            "nombre" to nombre,
            "correo" to correo
        )

        currentUser = User(uid = uid, fullName = nombre, email = correo, role = "Guardaparque")

        val namedDb = try { FirebaseFirestore.getInstance("usuarios") } catch (_: Exception) { null }
        val defaultDb = FirebaseFirestore.getInstance()

        val targetDb = namedDb ?: defaultDb

        targetDb.collection("Usuarios").document(uid).set(userMap)
            .addOnSuccessListener {
                Log.d("AuthRepository", "Firestore guardado exitosamente en Usuarios/$uid")
                onComplete(true, null)
            }
            .addOnFailureListener { error1 ->
                Log.w("AuthRepository", "Fallo primera BD Firestore: ${error1.message}. Intentando BD por defecto...")
                defaultDb.collection("Usuarios").document(uid).set(userMap)
                    .addOnSuccessListener {
                        Log.d("AuthRepository", "Firestore guardado exitosamente en BD defecto Usuarios/$uid")
                        onComplete(true, null)
                    }
                    .addOnFailureListener { error2 ->
                        Log.e("AuthRepository", "Error al guardar en Firestore: ${error2.message}")
                        onComplete(false, error2.localizedMessage ?: error1.localizedMessage)
                    }
            }
    }
}
