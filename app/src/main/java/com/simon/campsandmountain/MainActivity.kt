package com.simon.campsandmountain

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.simon.campsandmountain.data.repository.AuthRepository
import com.simon.campsandmountain.ui.campamentos.CampamentosFragment
import com.simon.campsandmountain.ui.login.LoginActivity
import com.simon.campsandmountain.ui.refugios.RefugiosFragment
import com.simon.campsandmountain.ui.reservas.ReservasFragment

class MainActivity : AppCompatActivity() {

    private lateinit var topAppBar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthRepository.isLoggedIn()) {
            navigateToLogin()
            return
        }

        // Inicialización de Firebase Analytics
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

        // Registrar evento de apertura en Firebase Analytics
        val screenBundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity")
            putString("usuario_activo", currentUser?.username ?: "Guardaparque")
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenBundle)

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
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
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_refugios
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
