package com.example.rmas_projekat.ui.profile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

@Composable
fun YouScreen(navController: NavController, auth: FirebaseAuth, firestore: FirebaseFirestore, storage: FirebaseStorage) {
    val user = auth.currentUser
    val context = LocalContext.current

    var displayName by remember { mutableStateOf(TextFieldValue("")) }
    var phoneNumber by remember { mutableStateOf(TextFieldValue("")) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    // Load user data
    LaunchedEffect(user) {
        user?.let {
            displayName = TextFieldValue(it.displayName ?: "")
            phoneNumber = TextFieldValue(it.phoneNumber ?: "")

            // Load profile image URL from Firebase Storage
            val storageRef = storage.reference.child("users/${it.uid}/profile.jpg")
            try {
                profileImageUrl = storageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                // Handle error if profile image is not found
                profileImageUrl = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image
        profileImageUrl?.let {
            Image(
                painter = rememberImagePainter(it),
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable {
                        // Handle image click to update profile picture
                    },
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Name
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Phone Number
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Update user data in Firebase
                user?.let {
                    val updates = hashMapOf(
                        "displayName" to displayName.text,
                        "phoneNumber" to phoneNumber.text
                    )
                    firestore.collection("users").document(it.uid).set(updates)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Profile")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewYouScreen() {
    YouScreen(navController = NavController(LocalContext.current), auth = FirebaseAuth.getInstance(), firestore = FirebaseFirestore.getInstance(), storage = FirebaseStorage.getInstance())
}
