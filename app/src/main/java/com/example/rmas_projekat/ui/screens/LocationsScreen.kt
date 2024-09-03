package com.example.rmas_projekat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rmas_projekat.R
import com.example.rmas_projekat.ui.maps.MapObject
import com.example.rmas_projekat.ui.navigation.BottomNavigationBar
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(navController: NavController, auth: FirebaseAuth) {
    val firestore = FirebaseFirestore.getInstance()
    var mapObjects by remember { mutableStateOf(listOf<MapObject>()) }

    // Date format to parse the "start" field
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // Fetch the objects from Firestore
    LaunchedEffect(Unit) {
        firestore.collection("objects").get().addOnSuccessListener { snapshot ->
            mapObjects = snapshot.documents.mapNotNull { document ->
                val geoPoint = document.getGeoPoint("location")
                val name = document.getString("name")
                val description = document.getString("description")
                val icon = document.getLong("icon")?.toInt() ?: R.drawable.default_pin
                val imageUri = document.getString("imageUri")
                val creatorId = document.getString("creatorId")
                val category = document.getString("category").orEmpty()
                val ride = document.getBoolean("ride") ?: false
                val start = document.getString("start")?.let {
                    try {
                        dateFormat.parse(it)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (geoPoint != null && name != null && description != null && creatorId != null) {
                    MapObject(
                        id = document.id,
                        name = name,
                        description = description,
                        location = LatLng(geoPoint.latitude, geoPoint.longitude),
                        icon = icon,
                        imageUri = imageUri,
                        creatorId = creatorId,
                        category = category,
                        ride = ride,
                        start = start
                    )
                } else {
                    null
                }
            }
        }
    }

    // Display the table of objects
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Locations") })
        },
        bottomBar = {
            BottomNavigationBar(navController = navController) // Integrated BottomNavigationBar
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(mapObjects) { mapObject ->
                ObjectRow(mapObject = mapObject, onClick = {
                    navController.navigate("objectDetails/${mapObject.id}")
                })
                Divider()
            }
        }
    }
}

@Composable
fun ObjectRow(mapObject: MapObject, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Name: ${mapObject.name}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Category: ${mapObject.category}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Description: ${mapObject.description}", style = MaterialTheme.typography.bodySmall)
            mapObject.start?.let {
                Text(text = "Start: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

