package com.example.rmas_projekat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.rmas_projekat.R
import com.example.rmas_projekat.ui.auth.LoginScreen
import com.example.rmas_projekat.ui.auth.RegisterScreen
import com.example.rmas_projekat.ui.screens.*
import com.example.rmas_projekat.ui.home.HomeScreen
import com.example.rmas_projekat.ui.maps.MapObject
import com.example.rmas_projekat.ui.maps.MapsScreen
import com.example.rmas_projekat.ui.profile.EditProfileScreen
import com.example.rmas_projekat.ui.profile.ProfileScreen
import com.example.rmas_projekat.ui.maps.ObjectDetailsScreen
import com.google.android.gms.maps.model.LatLng
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
            MapsScreen(navController = navController, auth = auth)
        }
        composable("record") {
            RecordScreen(navController = navController, auth = auth)
        }
        composable("groups") {
            GroupsScreen(navController = navController, auth = auth)
        }
        composable("profile") {
            ProfileScreen(navController = navController, auth = auth, storage = FirebaseStorage.getInstance())
        }
        composable("editProfile") {
            EditProfileScreen(navController = navController, auth = auth, storage = FirebaseStorage.getInstance(), firestore = FirebaseFirestore.getInstance())
        }
        composable("objectDetails/{objectId}") { backStackEntry ->
            val objectId = backStackEntry.arguments?.getString("objectId")
            if (objectId != null) {
                // Fetch the MapObject using objectId from Firestore and pass it to ObjectDetailsScreen
                val firestore = FirebaseFirestore.getInstance()
                val mapObject = remember { mutableStateOf<MapObject?>(null) }

                LaunchedEffect(objectId) {
                    firestore.collection("objects").document(objectId).get().addOnSuccessListener { document ->
                        if (document != null) {
                            val geoPoint = document.getGeoPoint("location")
                            val name = document.getString("name") ?: ""
                            val description = document.getString("description") ?: ""
                            val icon = document.getLong("icon")?.toInt() ?: R.drawable.default_pin
                            val imageUri = document.getString("imageUri")
                            val creatorId = document.getString("creatorId") ?: ""
                            val category = document.getString("category") ?: "" // Add this line

                            if (geoPoint != null) {
                                mapObject.value = MapObject(
                                    id = document.id,
                                    name = name,
                                    description = description,
                                    location = LatLng(geoPoint.latitude, geoPoint.longitude),
                                    icon = icon,
                                    imageUri = imageUri,
                                    creatorId = creatorId,
                                    category = category // Add this line to the MapObject
                                )
                            }
                        }
                    }
                }

                mapObject.value?.let {
                    ObjectDetailsScreen(mapObject = it, navController = navController, onDelete = {
                        firestore.collection("objects").document(objectId).delete()
                        navController.popBackStack()
                    })
                }
            }
        }

    }
}