package com.example.rmas_projekat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.rmas_projekat.ui.navigation.SetupNavGraph
import com.example.rmas_projekat.ui.theme.RmasProjekatTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()  // Initialize FirebaseAuth
        setContent {
            RmasProjekatTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    navController = rememberNavController()
                    SetupNavGraph(navController, auth)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Handle any necessary logic when activity is starting
    }

    override fun onResume() {
        super.onResume()
        // Handle any necessary logic when activity is resuming
    }

    override fun onPause() {
        super.onPause()
        // Handle any necessary logic when activity is pausing
    }

    override fun onStop() {
        super.onStop()
        // Handle any necessary logic when activity is stopping
    }

    override fun onDestroy() {
        super.onDestroy()
        // Handle any necessary logic when activity is destroyed
    }

    override fun onRestart() {
        super.onRestart()
        // Handle any necessary logic when activity is restarting
    }
}
