package com.example.rmas_projekat.ui.maps

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onDelete: () -> Unit,
    userMap: Map<String, String>
) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var riders by remember { mutableStateOf(listOf<String>()) } // List of rider IDs

    LaunchedEffect(mapObject.id) {
        firestore.collection("rides").document(mapObject.id).get().addOnSuccessListener { document ->
            riders = document.get("riders") as? List<String> ?: listOf(mapObject.creatorId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(mapObject.name) })
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp, start = 16.dp, end = 16.dp)
        ) {
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

            Text("Category: ${mapObject.category}")
            Text("Description: ${mapObject.description}")

            if (mapObject.ride) {
                Text("Start Time: ${mapObject.start}")

                Text("Riders:")
                riders.forEach { riderId ->
                    Text(userMap[riderId] ?: riderId) // Display rider's full name
                }

                if (riders.contains(currentUserId)) {
                    Button(onClick = {
                        riders = riders - currentUserId!!
                        firestore.collection("rides").document(mapObject.id)
                            .update("riders", riders)
                    }) {
                        Text("Unjoin Ride")
                    }
                } else {
                    Button(onClick = {
                        riders = riders + currentUserId!!
                        firestore.collection("rides").document(mapObject.id)
                            .update("riders", riders)
                    }) {
                        Text("Join Ride")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentUserId == mapObject.creatorId) {
                Button(onClick = {
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
