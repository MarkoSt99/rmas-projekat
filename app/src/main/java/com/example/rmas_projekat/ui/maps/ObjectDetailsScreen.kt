package com.example.rmas_projekat.ui.maps

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailsScreen(
    mapObject: MapObject,
    navController: NavController,
    onDelete: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance() // Initialize Firestore instance
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Get the current user's UID

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(mapObject.name) })
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp, start = 16.dp, end = 16.dp) // Add padding from the top to move entire content down
        ) {
            // Display image
            if (mapObject.imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(mapObject.imageUri),
                    contentDescription = "Object Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Display other details
            Text("Category: ${mapObject.category}")
            Text("Description: ${mapObject.description}")

            Spacer(modifier = Modifier.height(16.dp))

            // Check if the current user is the creator of the object
            if (currentUserId == mapObject.creatorId) {
                Button(onClick = {
                    // Delete the object
                    firestore.collection("objects").document(mapObject.id).delete().addOnSuccessListener {
                        onDelete()
                        navController.popBackStack()
                    }
                }) {
                    Text("Delete Object")
                }
            }
        }
    }
}
