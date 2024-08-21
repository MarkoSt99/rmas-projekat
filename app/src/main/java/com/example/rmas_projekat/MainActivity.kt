package com.example.rmas_projekat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    navController = rememberNavController()

                    // Set up the navigation graph
                    SetupNavGraph(navController = navController, auth = auth)

                    // Check if the user is already logged in
                    LaunchedEffect(auth.currentUser) {
                        if (auth.currentUser != null) {
                            // If the user is logged in, navigate directly to the home screen
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Lifecycle methods (if needed)
    override fun onStart() { super.onStart() }
    override fun onResume() { super.onResume() }
    override fun onPause() { super.onPause() }
    override fun onStop() { super.onStop() }
    override fun onDestroy() { super.onDestroy() }
    override fun onRestart() { super.onRestart() }
}
