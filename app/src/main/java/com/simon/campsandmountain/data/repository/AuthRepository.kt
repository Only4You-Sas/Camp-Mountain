package com.simon.campsandmountain.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simon.campsandmountain.data.model.User
import java.util.Locale

object AuthRepository {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun getFirestore(): FirebaseFirestore {
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

    fun isAdmin(): Boolean {
        val email = currentUser?.email?.trim()?.lowercase() ?: auth.currentUser?.email?.trim()?.lowercase() ?: ""
        if (email.contains("saimondaza@hotmail.com") || email.contains("admin")) {
            return true
        }
        return currentUser?.role.equals("Administrador", ignoreCase = true)
    }

    fun fetchUserData(onComplete: (User?) -> Unit) {
        val fbUser = auth.currentUser
        if (fbUser == null) {
            currentUser = null
            onComplete(null)
            return
        }

        val email = fbUser.email ?: ""
        val isSaimonAdmin = email.trim().equals("saimondaza@hotmail.com", ignoreCase = true) || email.contains("admin")

        val db = getFirestore()
        db.collection("Usuarios").document(fbUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val rawNombre = document.getString("nombre") ?: document.getString("fullName")
                    val nombre = if (rawNombre.isNullOrBlank() || rawNombre.startsWith("Usuario google") || rawNombre.startsWith("Usuario microsoft")) {
                        fbUser.displayName ?: fbUser.email?.substringBefore("@")
                            ?.split(".")
                            ?.joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
                            ?: "Usuario"
                    } else {
                        rawNombre
                    }
                    val correo = document.getString("correo") ?: document.getString("email") ?: email
                    val savedRole = document.getString("rol") ?: document.getString("role") ?: "Guardaparque"
                    val finalRole = if (isSaimonAdmin) "Administrador" else savedRole

                    val user = User(
                        uid = fbUser.uid,
                        fullName = nombre,
                        email = correo,
                        role = finalRole
                    )
                    currentUser = user

                    if (isSaimonAdmin && savedRole != "Administrador") {
                        saveUserDataToFirestore(fbUser.uid, nombre, correo, "Administrador") { _, _ -> }
                    }

                    onComplete(user)
                } else {
                    val fallbackName = fbUser.displayName
                        ?: fbUser.email?.substringBefore("@")
                            ?.split(".")
                            ?.joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
                        ?: "Usuario"
                    val finalRole = if (isSaimonAdmin) "Administrador" else "Guardaparque"
                    val fallbackUser = User(
                        uid = fbUser.uid,
                        fullName = fallbackName,
                        email = email,
                        role = finalRole
                    )
                    currentUser = fallbackUser

                    saveUserDataToFirestore(fbUser.uid, fallbackName, email, finalRole) { _, _ -> }

                    onComplete(fallbackUser)
                }
            }
            .addOnFailureListener {
                val fallbackName = fbUser.email?.substringBefore("@")
                    ?.split(".")
                    ?.joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
                    ?: "Usuario"
                val finalRole = if (isSaimonAdmin) "Administrador" else "Guardaparque"
                val fallbackUser = User(
                    uid = fbUser.uid,
                    fullName = fallbackName,
                    email = email,
                    role = finalRole
                )
                currentUser = fallbackUser
                onComplete(fallbackUser)
            }
    }

    fun saveUserDataToFirestore(
        uid: String,
        nombre: String,
        correo: String,
        role: String = "Guardaparque",
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val isSaimonAdmin = correo.trim().equals("saimondaza@hotmail.com", ignoreCase = true) || correo.contains("admin")
        val finalRole = if (isSaimonAdmin) "Administrador" else role

        val userMap = hashMapOf(
            "nombre" to nombre,
            "correo" to correo,
            "rol" to finalRole
        )

        currentUser = User(uid = uid, fullName = nombre, email = correo, role = finalRole)

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
