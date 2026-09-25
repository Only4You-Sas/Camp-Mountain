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

        db.collection("users").document(fbUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fullName = document.getString("fullName") ?: fbUser.displayName ?: "Usuario"
                    val email = document.getString("email") ?: fbUser.email ?: ""
                    val role = document.getString("role") ?: "Guardaparque"
                    val user = User(
                        uid = fbUser.uid,
                        fullName = fullName,
                        email = email,
                        role = role
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

    fun saveUserDataToFirestore(uid: String, fullName: String, email: String, onComplete: (Boolean) -> Unit) {
        val userMap = hashMapOf(
            "fullName" to fullName,
            "email" to email,
            "role" to "Guardaparque",
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                currentUser = User(uid = uid, fullName = fullName, email = email, role = "Guardaparque")
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}
