package com.simon.campsandmountain.ui.crud

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simon.campsandmountain.R

class FirestoreCrudActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "FirestoreCrudActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firestore_crud)

        title = "CRUD Firestore Usuarios"

        val btnCrear = findViewById<Button>(R.id.btnCrear)
        val btnVer = findViewById<Button>(R.id.btnVer)
        val btnActualizar = findViewById<Button>(R.id.btnActualizar)
        val btnBorrar = findViewById<Button>(R.id.btnBorrar)

        btnCrear.setOnClickListener {
            crearUsuario()
        }

        btnVer.setOnClickListener {
            verUsuario()
        }

        btnActualizar.setOnClickListener {
            actualizar()
        }

        btnBorrar.setOnClickListener {
            borrarUsuario()
        }

        verUsuario()
    }

    private fun getUsuarioActual(): String {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val name = user.displayName
            val uid = user.uid
        }
        return user?.email ?: ""
    }

    private fun crearUsuario() {
        val txtNombre = findViewById<EditText>(R.id.txtCrear)
        val user = hashMapOf(
            "nombre" to txtNombre.text.toString(),
            "correo" to getUsuarioActual()
        )

        db.collection("Usuarios")
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                Toast.makeText(this, "Usuario creado exitosamente", Toast.LENGTH_SHORT).show()
                verUsuario()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error cargando documento", e)
                Toast.makeText(this, "Error cargando documento", Toast.LENGTH_SHORT).show()
            }
    }

    private fun verUsuario() {
        val datos = findViewById<TextView>(R.id.txtVer)
        val tabla = findViewById<TextView>(R.id.txtTabla)

        db.collection("Usuarios")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    datos.text = ""
                    for (document in task.result) {
                        Log.d(TAG, "${document.id} => ${document.data}")
                        datos.append(" correo: ${document.get("correo")} nombre:${document.get("nombre")}\n")
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }
    }

    fun actualizar() {
        val txtNombre = findViewById<EditText>(R.id.txtCrear)
        val user = hashMapOf(
            "nombre" to txtNombre.text.toString(),
            "correo" to getUsuarioActual()
        )

        val currentUser = FirebaseAuth.getInstance().currentUser
        val docId = currentUser?.uid ?: getUsuarioActual()

        db.collection("Usuarios").document(docId).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show()
                verUsuario()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al actualizar documento", e)
            }
    }

    private fun borrarUsuario() {
        val txtBorrar = findViewById<EditText>(R.id.txtCrear)
        val idABorrar = txtBorrar.text.toString().trim()

        if (idABorrar.isNotEmpty()) {
            db.collection("Usuarios").document(idABorrar).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Registro borrado", Toast.LENGTH_SHORT).show()
                    verUsuario()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al borrar", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Ingrese un id a borrar", Toast.LENGTH_SHORT).show()
        }
    }
}
