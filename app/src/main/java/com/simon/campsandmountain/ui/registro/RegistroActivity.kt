package com.simon.campsandmountain.ui.registro

import android.content.Context
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
import com.simon.campsandmountain.auth.PasskeyAuthManager
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

        fun saveBiometricEmail(context: Context, email: String) {
            val prefs = context.getSharedPreferences("CampMountainPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("BIOMETRIC_LINKED_EMAIL", email.trim()).apply()
        }

        fun getBiometricEmail(context: Context): String? {
            val prefs = context.getSharedPreferences("CampMountainPrefs", Context.MODE_PRIVATE)
            return prefs.getString("BIOMETRIC_LINKED_EMAIL", null)
        }
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
        val btnVincularBiometria: MaterialButton = findViewById(R.id.btnVincularBiometria)
        val btnVolverLogin: MaterialButton = findViewById(R.id.btnVolverLogin)

        btnCrearRegistro.setOnClickListener { crearRegistro() }
        btnVolverLogin.setOnClickListener { finish() }

        btnVincularBiometria.setOnClickListener {
            val correo = txtCorreoR.text.toString().trim()
            if (correo.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese su correo electrónico antes de vincular su huella", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PasskeyAuthManager.authenticateWithBiometricPrompt(
                activity = this@RegistroActivity,
                title = "Vincular Huella Dactilar",
                subtitle = "Escanea tu huella para asociarla a $correo"
            ) { success, message ->
                if (success) {
                    saveBiometricEmail(this@RegistroActivity, correo)
                    Toast.makeText(this@RegistroActivity, "✅ Huella dactilar vinculada exitosamente con $correo", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@RegistroActivity, message ?: "Error al vincular huella dactilar", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

        saveBiometricEmail(this, correo)

        val isSaimon = correo.equals("saimondaza@hotmail.com", ignoreCase = true) || correo.contains("admin")
        val role = if (isSaimon) "Administrador" else "Visitante"

        oFirebaseAuth.createUserWithEmailAndPassword(correo, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = oFirebaseAuth.currentUser
                    if (user != null) {
                        AuthRepository.saveUserDataToFirestore(user.uid, nombre, correo, role) { _, _ ->
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
        Toast.makeText(this, "Cuenta creada y huella vinculada a $correo. ¡Inicie sesión!", Toast.LENGTH_LONG).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("PREFILLED_EMAIL", correo)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
