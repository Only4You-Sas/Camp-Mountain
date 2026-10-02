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
import com.google.firebase.auth.OAuthProvider
import com.simon.campsandmountain.MainActivity
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.repository.AuthRepository
import com.simon.campsandmountain.ui.crud.FirestoreCrudActivity
import com.simon.campsandmountain.ui.registro.RegistroActivity
import java.util.Locale

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
        val btnGoogleLogin = findViewById<MaterialButton>(R.id.btnGoogleLogin)
        val btnMicrosoftLogin = findViewById<MaterialButton>(R.id.btnMicrosoftLogin)
        val btnCrudFirestore = findViewById<MaterialButton>(R.id.btnCrudFirestore)

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

        btnGoogleLogin.setOnClickListener {
            iniciarSesionConProveedor("google.com")
        }

        btnMicrosoftLogin.setOnClickListener {
            iniciarSesionConProveedor("microsoft.com")
        }

        btnCrudFirestore?.setOnClickListener {
            val intent = Intent(this, FirestoreCrudActivity::class.java)
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

    private fun iniciarSesionConProveedor(providerId: String) {
        tvError.visibility = View.GONE

        val provider = OAuthProvider.newBuilder(providerId)
        provider.addCustomParameter("prompt", "select_account")

        mAuth.startActivityForSignInWithProvider(this, provider.build())
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    val profileMap = authResult.additionalUserInfo?.profile
                    val profileName = profileMap?.get("name") as? String
                        ?: profileMap?.get("given_name") as? String
                        ?: user.displayName
                        ?: user.email?.substringBefore("@")
                            ?.split(".")
                            ?.joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
                        ?: "Usuario"

                    val correo = user.email ?: ""

                    AuthRepository.saveUserDataToFirestore(user.uid, profileName, correo) { _, _ ->
                        val bundle = Bundle().apply {
                            putString("username", correo)
                            putString("user_fullname", profileName)
                            putString(FirebaseAnalytics.Param.METHOD, providerId)
                        }
                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)

                        Toast.makeText(this, "¡Bienvenido, $profileName!", Toast.LENGTH_SHORT).show()
                        updateUI(user)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "signInWithProvider:failure", e)
                val errorMsg = e.localizedMessage ?: "Error al iniciar sesión con proveedor"
                tvError.text = errorMsg
                tvError.visibility = View.VISIBLE
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
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
