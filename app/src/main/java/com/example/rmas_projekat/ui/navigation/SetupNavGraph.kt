package com.example.rmas_projekat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.rmas_projekat.ui.auth.LoginScreen
import com.example.rmas_projekat.ui.auth.RegisterScreen
import com.example.rmas_projekat.ui.screens.*
import com.example.rmas_projekat.ui.home.HomeScreen
import com.example.rmas_projekat.ui.maps.MapsScreen
import com.example.rmas_projekat.ui.profile.EditProfileScreen
import com.example.rmas_projekat.ui.profile.ProfileScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun SetupNavGraph(navController: NavHostController, auth: FirebaseAuth) {
    NavHost(navController = navController, startDestination = "login") {
        composable("register") {
            RegisterScreen(navController = navController, auth = auth, storage = FirebaseStorage.getInstance())
        }
        composable("login") {
            LoginScreen(navController = navController, auth = auth)
        }
        composable("home") {
            HomeScreen(navController = navController, auth = auth)
        }
        composable("maps") {
            MapsScreen(navController = navController, auth=auth)
        }
        composable("record") {
            RecordScreen(navController = navController, auth = auth)
        }
        composable("groups") {
            GroupsScreen(navController = navController, auth = auth)
        }
        composable("profile") {
            ProfileScreen(navController = navController,auth = auth, storage = FirebaseStorage.getInstance())
        }
        composable("editProfile") {
            EditProfileScreen(navController = navController, auth = auth, storage = FirebaseStorage.getInstance(), firestore = FirebaseFirestore.getInstance())
        }


    }
}
