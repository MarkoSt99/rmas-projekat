package com.example.rmas_projekat.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController, auth: FirebaseAuth, storage: FirebaseStorage, firestore: FirebaseFirestore) {
    val user = auth.currentUser
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf(user?.displayName ?: "") }
    var phoneNumber by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Load current user data from Firestore
    LaunchedEffect(user) {
        user?.let {
            val userDocument = firestore.collection("users").document(user.uid).get().await()
            phoneNumber = userDocument.getString("phoneNumber") ?: ""
        }
    }

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    Button(onClick = {
                        navController.popBackStack()
                    }) {
                        Text("Cancel")
                    }
                },
                actions = {
                    Button(onClick = {
                        coroutineScope.launch {
                            // Save the updated information
                            val userPhoto = imageUri
                            user?.let {
                                val profileUpdates = userProfileChangeRequest {
                                    displayName = fullName
                                }
                                user.updateProfile(profileUpdates).await()

                                // Save the phone number and full name to Firestore
                                firestore.collection("users").document(user.uid).update(
                                    mapOf(
                                        "phoneNumber" to phoneNumber,
                                        "fullName" to fullName
                                    )
                                ).await()

                                userPhoto?.let { uri ->
                                    val storageRef = storage.reference.child("userPhotos/${user.uid}.jpg")
                                    storageRef.putFile(uri).addOnSuccessListener {
                                        Toast.makeText(context, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }.addOnFailureListener {
                                        Toast.makeText(context, "Failed to Update Photo", Toast.LENGTH_SHORT).show()
                                    }
                                } ?: run {
                                    Toast.makeText(context, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        }
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Choose New Profile Photo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display current profile image
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder image if no image is chosen
                Image(
                    painter = rememberAsyncImagePainter(user?.photoUrl),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
