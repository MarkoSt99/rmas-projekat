package com.example.rmas_projekat.ui.auth

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun RegisterScreen(navController: NavController, auth: FirebaseAuth, storage: FirebaseStorage) {
    val firestore = FirebaseFirestore.getInstance() // Firestore instance

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

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

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Choose Photo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (password == confirmPassword) {
                    val userPhoto = imageUri
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val userId = user?.uid ?: ""

                                // Set the display name
                                val profileUpdates = userProfileChangeRequest {
                                    displayName = fullName
                                }

                                user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        // Upload the photo to Firebase Storage
                                        val storageRef = storage.reference.child("userPhotos/$userId.jpg")
                                        userPhoto?.let {
                                            storageRef.putFile(it)
                                                .addOnSuccessListener {
                                                    // Save additional user data in Firestore
                                                    val userData = hashMapOf(
                                                        "fullName" to fullName,
                                                        "phoneNumber" to phoneNumber,
                                                        "email" to email,
                                                        "photoUrl" to storageRef.path
                                                    )
                                                    firestore.collection("users").document(userId)
                                                        .set(userData)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                                                            navController.navigate("login")
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Toast.makeText(context, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "Photo Upload Failed", Toast.LENGTH_SHORT).show()
                                                }
                                        } ?: run {
                                            // Save additional user data without photo
                                            val userData = hashMapOf(
                                                "fullName" to fullName,
                                                "phoneNumber" to phoneNumber,
                                                "email" to email
                                            )
                                            firestore.collection("users").document(userId)
                                                .set(userData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                                                    navController.navigate("login")
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(context, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to update profile: ${profileTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                navController.navigate("login") // Navigate to login screen
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Already Registered? Login")
        }
    }
}
