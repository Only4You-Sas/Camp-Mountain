package com.simon.campsandmountain.ui.registro

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
import com.simon.campsandmountain.R
import com.simon.campsandmountain.data.repository.AuthRepository
import com.simon.campsandmountain.ui.login.LoginActivity

class RegistroActivity : AppCompatActivity() {

    private lateinit var oFirebaseAnalytics: FirebaseAnalytics
    private lateinit var oFirebaseAuth: FirebaseAuth

    private lateinit var txtNombreR: EditText
    private lateinit var txtCorreoR: EditText
    private lateinit var txtPass1: EditText
    private lateinit var tvErrorRegistro: TextView

    companion object {
        private const val TAG = "RegistroActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        oFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle().apply { putString("Mensaje", "Entro_al_registro") }
        oFirebaseAnalytics.logEvent("Formulario_registro", bundle)
        title = "Formulario Registro"

        oFirebaseAuth = FirebaseAuth.getInstance()

        txtNombreR = findViewById(R.id.txtNombreR)
        txtCorreoR = findViewById(R.id.txtCorreoR)
        txtPass1 = findViewById(R.id.txtPass1)
        tvErrorRegistro = findViewById(R.id.tvErrorRegistro)

        val btnCrearRegistro: MaterialButton = findViewById(R.id.btnCrearRegistro)
        val btnVolverLogin: MaterialButton = findViewById(R.id.btnVolverLogin)

        btnCrearRegistro.setOnClickListener { crearRegistro() }
        btnVolverLogin.setOnClickListener { finish() }
    }

    private fun crearRegistro() {
        val nombre = txtNombreR.text.toString().trim()
        val correo = txtCorreoR.text.toString().trim()
        val pass = txtPass1.text.toString().trim()

        if (nombre.isEmpty() || correo.isEmpty() || pass.isEmpty()) {
            tvErrorRegistro.text = "Por favor completa todos los campos"
            tvErrorRegistro.visibility = View.VISIBLE
            return
        }

        if (pass.length < 6) {
            tvErrorRegistro.text = "La contraseña debe tener al menos 6 caracteres"
            tvErrorRegistro.visibility = View.VISIBLE
            return
        }

        tvErrorRegistro.visibility = View.GONE

        oFirebaseAuth.createUserWithEmailAndPassword(correo, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = oFirebaseAuth.currentUser
                    if (user != null) {
                        AuthRepository.saveUserDataToFirestore(user.uid, nombre, correo) { _, _ ->
                            finalizarRegistroNologeado(correo)
                        }
                    } else {
                        finalizarRegistroNologeado(correo)
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    val errorDetail = task.exception?.localizedMessage ?: "Fallo la autenticación"
                    tvErrorRegistro.text = errorDetail
                    tvErrorRegistro.visibility = View.VISIBLE
                    Toast.makeText(this, errorDetail, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun finalizarRegistroNologeado(correo: String) {
        oFirebaseAuth.signOut()
        AuthRepository.logout()
        Toast.makeText(this, "usuario ha sido creado. Por favor inicie sesión", Toast.LENGTH_LONG).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("PREFILLED_EMAIL", correo)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
