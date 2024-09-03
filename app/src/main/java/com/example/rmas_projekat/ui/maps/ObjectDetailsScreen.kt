package com.example.rmas_projekat.ui.maps

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

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
    var riders by remember { mutableStateOf(listOf<String>()) }
    var listenerRegistration: ListenerRegistration? by remember { mutableStateOf(null) }

    LaunchedEffect(mapObject.id) {
        listenerRegistration = firestore.collection("users")
            .whereArrayContains("joinedRides", mapObject.id)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    riders = snapshot.documents.mapNotNull { document -> document.id }
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
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
                    Text(userMap[riderId] ?: riderId)
                }

                if (riders.contains(currentUserId)) {
                    Button(onClick = {
                        firestore.collection("users").document(currentUserId!!)
                            .update("joinedRides", FieldValue.arrayRemove(mapObject.id))
                            .addOnSuccessListener {
                                firestore.collection("users").document(currentUserId)
                                    .update("score", FieldValue.increment(-10)) // Decrease score
                            }
                    }) {
                        Text("Unjoin Ride")
                    }
                } else {
                    Button(onClick = {
                        firestore.collection("users").document(currentUserId!!)
                            .update("joinedRides", FieldValue.arrayUnion(mapObject.id))
                            .addOnSuccessListener {
                                firestore.collection("users").document(currentUserId)
                                    .update("score", FieldValue.increment(10)) // Increase score
                            }
                    }) {
                        Text("Join Ride")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentUserId == mapObject.creatorId) {
                Button(onClick = {
                    firestore.collection("users")
                        .whereArrayContains("joinedRides", mapObject.id)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val batch = firestore.batch()

                            snapshot.documents.forEach { document ->
                                batch.update(
                                    firestore.collection("users").document(document.id),
                                    "joinedRides", FieldValue.arrayRemove(mapObject.id)
                                )

                                batch.update(
                                    firestore.collection("users").document(document.id),
                                    "score", FieldValue.increment(-10)
                                )
                            }

                            batch.commit().addOnSuccessListener {
                                firestore.collection("objects").document(mapObject.id).delete()
                                    .addOnSuccessListener {
                                        firestore.collection("users").document(currentUserId!!)
                                            .update("score", FieldValue.increment(-5)) // Decrease creator's score
                                        onDelete()
                                        navController.popBackStack("map", inclusive = false)
                                    }
                            }
                        }
                }) {
                    Text("Delete Object")
                }
            }
        }
    }
}


