package com.simon.campsandmountain.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simon.campsandmountain.data.model.User

object AuthRepository {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

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
        db.collection("Usuarios").document(uid).set(userMap)
            .addOnSuccessListener {
                currentUser = User(uid = uid, fullName = nombre, email = correo, role = "Guardaparque")
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage)
            }
    }
}
