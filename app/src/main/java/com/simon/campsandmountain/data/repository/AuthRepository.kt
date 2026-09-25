package com.simon.campsandmountain.data.repository

import com.simon.campsandmountain.data.model.User

object AuthRepository {
    private val users = mutableListOf(
        User("admin", "admin123", "Administrador Principal", "Jefe de Parque"),
        User("guardaparque", "1234", "Carlos Mendoza", "Guardaparque Seccional"),
        User("demo", "demo", "Usuario Demostración", "Visitante")
    )

    var currentUser: User? = null
        private set

    fun login(usernameInput: String, passwordInput: String): Boolean {
        val user = users.find {
            it.username.equals(usernameInput.trim(), ignoreCase = true) &&
                    it.passwordHash == passwordInput
        }
        return if (user != null) {
            currentUser = user
            true
        } else {
            false
        }
    }

    fun logout() {
        currentUser = null
    }

    fun isLoggedIn(): Boolean = currentUser != null
}
