package com.example.rmas_projekat.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.rmas_projekat.R
import com.example.rmas_projekat.ui.navigation.BottomNavigationBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, auth: FirebaseAuth, storage: FirebaseStorage) {
    val user = auth.currentUser
    //val context = LocalContext.current
    //val coroutineScope = rememberCoroutineScope()

    // Mutable state to hold display name, phone number, and profile image URL
    val displayName = remember { mutableStateOf("Unknown User") }
    val profileImageUrl = remember { mutableStateOf("") }
    val phoneNumber = remember { mutableStateOf("Unknown Number") }

    // Fetch the profile image URL, display name, and phone number when the screen is displayed
    LaunchedEffect(user) {
        user?.let {
            val firestore = FirebaseFirestore.getInstance()
            val userDoc = firestore.collection("users").document(user.uid)

            // Fetch user details from Firestore
            userDoc.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    displayName.value = document.getString("fullName") ?: "Unknown User"
                    phoneNumber.value = document.getString("phoneNumber") ?: "Unknown Number"
                }
            }.addOnFailureListener {
                // Handle error if needed
            }

            // Fetch the profile image URL from Firebase Storage
            val storageReference = storage.reference.child("userPhotos/${user.uid}.jpg")
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                profileImageUrl.value = uri.toString()
            }.addOnFailureListener {
                profileImageUrl.value = "" // Optionally set a default image or leave empty
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("profile") { inclusive = true }
                        }
                    }) {
                        Text("Sign Out")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("editProfile")
                    }) {
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
            // Profile Image
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
                // Placeholder image
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

            // Display Name
            Text(
                text = displayName.value,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Phone Number
            Text(
                text = "Phone: ${phoneNumber.value}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Space for further app development
        }
    }
}
