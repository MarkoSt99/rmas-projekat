package com.example.rmas_projekat.ui.profile

import com.example.rmas_projekat.ui.services.LocationService
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.rmas_projekat.R
import com.example.rmas_projekat.ui.navigation.BottomNavigationBar
import com.example.rmas_projekat.ui.services.LocationServiceViewModel
import com.example.rmas_projekat.ui.services.LocationServiceViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    auth: FirebaseAuth,
    storage: FirebaseStorage
) {
    val context = LocalContext.current
    val locationServiceViewModel: LocationServiceViewModel = viewModel(
        factory = LocationServiceViewModelFactory(context)
    )

    val user = auth.currentUser

    val displayName = remember { mutableStateOf("Unknown User") }
    val profileImageUrl = remember { mutableStateOf("") }
    val phoneNumber = remember { mutableStateOf("Unknown Number") }

    val isLocationServiceEnabled by locationServiceViewModel.isLocationServiceEnabled.collectAsState()

    LaunchedEffect(isLocationServiceEnabled) {
        if (isLocationServiceEnabled) {
            startLocationService(context)
        } else {
            stopLocationService(context)
        }
    }

    LaunchedEffect(user) {
        user?.let {
            val firestore = FirebaseFirestore.getInstance()
            val userDoc = firestore.collection("users").document(user.uid)

            userDoc.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    displayName.value = document.getString("fullName") ?: "Unknown User"
                    phoneNumber.value = document.getString("phoneNumber") ?: "Unknown Number"
                }
            }

            val storageReference = storage.reference.child("userPhotos/${user.uid}.jpg")
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                profileImageUrl.value = uri.toString()
            }.addOnFailureListener {
                profileImageUrl.value = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo("profile") { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Sign Out")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            navController.navigate("editProfile")
                        },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Edit")
                    }
                }
            )

        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (profileImageUrl.value.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(profileImageUrl.value),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.placeholder_profile),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = displayName.value,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Phone: ${phoneNumber.value}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Location Service")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isLocationServiceEnabled,
                    onCheckedChange = { locationServiceViewModel.toggleLocationService() }
                )
            }
        }
    }
}

private fun startLocationService(context: Context) {
    val serviceIntent = Intent(context, LocationService::class.java)
    ContextCompat.startForegroundService(context, serviceIntent)
}

private fun stopLocationService(context: Context) {
    val serviceIntent = Intent(context, LocationService::class.java)
    context.stopService(serviceIntent)
}
