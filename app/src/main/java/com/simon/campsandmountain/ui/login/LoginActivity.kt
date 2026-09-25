package com.simon.campsandmountain.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.simon.campsandmountain.MainActivity
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.repository.AuthRepository
import com.simon.campsandmountain.ui.registro.RegistroActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var txtCorreo: EditText
    private lateinit var txtPass: EditText
    private lateinit var tvError: TextView

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        if (mAuth.currentUser != null) {
            AuthRepository.fetchUserData {
                navigateToMain()
            }
            return
        }

        setContentView(R.layout.activity_login)
        title = "Inicio de sesión"

        txtCorreo = findViewById(R.id.txtCorreo)
        txtPass = findViewById(R.id.txtPass)
        tvError = findViewById(R.id.tvError)

        val prefilledEmail = intent.getStringExtra("PREFILLED_EMAIL")
        if (!prefilledEmail.isNullOrBlank()) {
            txtCorreo.setText(prefilledEmail)
        }

        val btnIngresar = findViewById<MaterialButton>(R.id.btnIngresar)
        val btnRegistrar = findViewById<MaterialButton>(R.id.btnRegistrar)

        btnIngresar.setOnClickListener {
            val email = txtCorreo.text.toString().trim()
            val password = txtPass.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                tvError.text = "Por favor ingrese correo y contraseña"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            ingresar(email, password)
        }

        btnRegistrar.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun ingresar(email: String, password: String) {
        tvError.visibility = View.GONE

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = mAuth.currentUser

                    AuthRepository.fetchUserData { userData ->
                        val bundle = Bundle().apply {
                            putString("username", userData?.email ?: email)
                            putString("user_fullname", userData?.fullName ?: "")
                            putString("user_role", userData?.role ?: "Guardaparque")
                            putString(FirebaseAnalytics.Param.METHOD, "firebase_email")
                        }
                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)

                        Toast.makeText(this, "¡Bienvenido, ${userData?.fullName ?: user?.email}!", Toast.LENGTH_SHORT).show()
                        updateUI(user)
                    }
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    tvError.text = "Credenciales incorrectas"
                    tvError.visibility = View.VISIBLE
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
