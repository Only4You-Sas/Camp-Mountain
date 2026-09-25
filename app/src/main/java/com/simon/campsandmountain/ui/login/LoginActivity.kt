package com.simon.campsandmountain.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.simon.campsandmountain.MainActivity
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.repository.AuthRepository

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        if (AuthRepository.isLoggedIn()) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvError = findViewById<TextView>(R.id.tvError)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnQuickDemo = findViewById<MaterialButton>(R.id.btnQuickDemo)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (username.isBlank() || password.isBlank()) {
                tvError.text = "Por favor ingrese usuario y contraseña"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val success = AuthRepository.login(username, password)
            if (success) {
                tvError.visibility = View.GONE

                val currentUser = AuthRepository.currentUser
                val bundle = Bundle().apply {
                    putString("username", currentUser?.username ?: username)
                    putString("user_fullname", currentUser?.fullName ?: "")
                    putString("user_role", currentUser?.role ?: "Guardaparque")
                    putString(FirebaseAnalytics.Param.METHOD, "local_auth")
                }
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)

                Toast.makeText(this, "¡Bienvenido, ${currentUser?.fullName}!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } else {
                tvError.text = "Credenciales incorrectas. Intente con admin / admin123"
                tvError.visibility = View.VISIBLE
            }
        }

        btnQuickDemo.setOnClickListener {
            etUsername.setText("admin")
            etPassword.setText("admin123")
            btnLogin.performClick()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
