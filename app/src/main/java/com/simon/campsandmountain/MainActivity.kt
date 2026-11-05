package com.simon.campsandmountain

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import com.simon.campsandmountain.auth.PasskeyAuthManager
import com.simon.campsandmountain.data.importer.CsvBatchImporter
import com.simon.campsandmountain.data.repository.AuthRepository
import com.simon.campsandmountain.location.AddressValidationManager
import com.simon.campsandmountain.ui.campamentos.CampamentosFragment
import com.simon.campsandmountain.ui.login.LoginActivity
import com.simon.campsandmountain.ui.refugios.RefugiosFragment
import com.simon.campsandmountain.ui.reservas.ReservasFragment
import com.simon.campsandmountain.ui.usuarios.UsuariosFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var topAppBar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("FCM", "POST_NOTIFICATIONS permission granted")
        } else {
            Log.w("FCM", "POST_NOTIFICATIONS permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthRepository.isLoggedIn()) {
            navigateToLogin()
            return
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        topAppBar = findViewById(R.id.topAppBar)
        bottomNav = findViewById(R.id.bottomNavigation)

        val currentUser = AuthRepository.currentUser
        topAppBar.subtitle = "Usuario: ${currentUser?.fullName ?: "Guardaparque"}"

        val screenBundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity")
            putString("usuario_activo", currentUser?.email ?: "Guardaparque")
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenBundle)

        // Solicitar permiso de notificaciones para Android 13+ (Slide 4)
        askNotificationPermission()

        // Obtener el token de FCM para probar notificaciones Push desde Firebase Console
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCMToken", "FCM Registration Token: $token")
            }
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_tools -> {
                    showAdvancedToolsDialog()
                    true
                }
                R.id.action_logout -> {
                    AuthRepository.logout()
                    Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                    true
                }
                else -> false
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_refugios -> {
                    topAppBar.title = "Refugios de Montaña"
                    loadFragment(RefugiosFragment())
                    firebaseAnalytics.logEvent("ver_refugios", null)
                    true
                }
                R.id.nav_campamentos -> {
                    topAppBar.title = "Zonas de Campamento"
                    loadFragment(CampamentosFragment())
                    firebaseAnalytics.logEvent("ver_campamentos", null)
                    true
                }
                R.id.nav_reservas -> {
                    topAppBar.title = "Gestión de Reservas"
                    loadFragment(ReservasFragment())
                    firebaseAnalytics.logEvent("ver_reservas", null)
                    true
                }
                R.id.nav_usuarios -> {
                    topAppBar.title = "Usuarios Firestore"
                    loadFragment(UsuariosFragment())
                    firebaseAnalytics.logEvent("ver_usuarios", null)
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_refugios
        }
    }

    private fun showAdvancedToolsDialog() {
        val options = arrayOf(
            "🔑 Probar Passkeys & Biometría",
            "🗺️ Validar Dirección con Retrofit & Nominatim",
            "📊 Importar CSV Masivo a Firestore (OpenCSV)"
        )

        AlertDialog.Builder(this)
            .setTitle("🛠️ Herramientas Avanzadas")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> testPasskeys()
                    1 -> testAddressValidation()
                    2 -> testCsvImport()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun testPasskeys() {
        val currentUser = AuthRepository.currentUser
        val username = currentUser?.email ?: "usuario@campandmountain.com"
        val displayName = currentUser?.fullName ?: "Usuario Camp & Mountain"

        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Iniciando creación de Passkey...", Toast.LENGTH_SHORT).show()
            val result = PasskeyAuthManager.createPasskey(this@MainActivity, username, displayName)
            if (result.isSuccess) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("🔑 Passkey Creada Exitosamente")
                    .setMessage("Se ha guardado la Passkey vinculada a '$username' mediante autenticación biométrica de Android.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                Toast.makeText(this@MainActivity, "Resultado Passkey: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun testAddressValidation() {
        val input = EditText(this)
        input.hint = "Ej: Plaza de Armas, Santiago, Chile"
        input.setText("Parque Nacional Torres del Paine, Chile")

        AlertDialog.Builder(this)
            .setTitle("🗺️ Validar Dirección (Retrofit / Nominatim)")
            .setView(input)
            .setPositiveButton("Validar y Guardar en Firestore") { _, _ ->
                val address = input.text.toString().trim()
                if (address.isNotEmpty()) {
                    lifecycleScope.launch {
                        Toast.makeText(this@MainActivity, "Consultando API de Nominatim con Retrofit...", Toast.LENGTH_SHORT).show()
                        AddressValidationManager.validateAddressAndSaveToFirestore(address) { success, message, data ->
                            runOnUiThread {
                                if (success && data != null) {
                                    val lat = data["latitud"]
                                    val lon = data["longitud"]
                                    val fullName = data["nombreCompleto"]
                                    AlertDialog.Builder(this@MainActivity)
                                        .setTitle("✅ Dirección Validada y Guardada")
                                        .setMessage("Lugar: $fullName\n\nLatitud: $lat\nLongitud: $lon\n\nGuardado en la colección 'DireccionesValidadas' de Cloud Firestore.")
                                        .setPositiveButton("OK", null)
                                        .show()
                                } else {
                                    Toast.makeText(this@MainActivity, message ?: "Error al validar dirección", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun testCsvImport() {
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Importando 'refugios_muestra.csv' a Firestore con OpenCSV y WriteBatch...", Toast.LENGTH_SHORT).show()
            val result = CsvBatchImporter.importCsvFromAssetsToFirestore(
                context = this@MainActivity,
                assetFileName = "refugios_muestra.csv",
                collectionName = "RefugiosPrueba"
            )
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("📊 Importación Masiva Completada")
                    .setMessage("Se importaron exitosamente $count registros desde el archivo CSV a la colección 'RefugiosPrueba' en Cloud Firestore usando lotes WriteBatch.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                Toast.makeText(this@MainActivity, "Error en importación: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("FCM", "Permission already granted")
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
