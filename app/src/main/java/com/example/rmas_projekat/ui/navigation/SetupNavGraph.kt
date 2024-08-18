package com.example.rmas_projekat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.rmas_projekat.ui.auth.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SetupNavGraph(navController: NavHostController, auth: FirebaseAuth) {
    NavHost(navController = navController, startDestination = "register") {
        composable("register") {
            RegisterScreen(navController = navController, auth = auth)
        }

        // Add more composable destinations for other screens here
    }
}
