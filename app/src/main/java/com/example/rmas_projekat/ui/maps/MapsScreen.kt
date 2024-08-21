package com.example.rmas_projekat.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.rmas_projekat.ui.navigation.BottomNavigationBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapsScreen(navController: NavController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val firestore = FirebaseFirestore.getInstance()

    // Permissions
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var mapObjects by remember { mutableStateOf(listOf<MapObject>()) }

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        currentLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            }
        } else {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation ?: LatLng(0.0, 0.0), 15f)
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
        }
    }

    // Listen to Firestore changes and update mapObjects
    LaunchedEffect(Unit) {
        firestore.collection("objects").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                mapObjects = snapshot.documents.mapNotNull { document ->
                    val geoPoint = document.getGeoPoint("location")
                    val name = document.getString("name")
                    val type = document.getString("type")
                    val description = document.getString("description")
                    if (geoPoint != null && name != null && type != null && description != null) {
                        MapObject(
                            id = document.id,
                            name = name,
                            type = type,
                            description = description,
                            location = LatLng(geoPoint.latitude, geoPoint.longitude)
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && locationPermissionState.allPermissionsGranted) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        location?.let {
                            currentLocation = LatLng(it.latitude, it.longitude)
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Object")
            }
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLongClick = { latLng ->
                    selectedLocation = latLng
                    showDialog = true
                }
            ) {
                currentLocation?.let {
                    Marker(
                        state = rememberMarkerState(position = it),
                        title = "You are here"
                    )
                }

                // Add markers for all objects fetched from Firestore
                mapObjects.forEach { mapObject ->
                    Marker(
                        state = rememberMarkerState(position = mapObject.location),
                        title = mapObject.name,
                        snippet = mapObject.description
                    )
                }
            }
        }
    }

    // Dialog to add a new object
    if (showDialog) {
        AddObjectDialog(
            currentLocation = selectedLocation ?: currentLocation,
            onDismiss = { showDialog = false },
            onSave = { objectName, objectType, objectDescription ->
                if (selectedLocation != null) {
                    // Save object to Firestore
                    val newObject = mapOf(
                        "name" to objectName,
                        "type" to objectType,
                        "description" to objectDescription,
                        "location" to GeoPoint(selectedLocation!!.latitude, selectedLocation!!.longitude)
                    )
                    firestore.collection("objects").add(newObject).addOnSuccessListener { documentReference ->
                        // Refresh the mapObjects list to include the newly added object
                        firestore.collection("objects").get().addOnSuccessListener { snapshot ->
                            mapObjects = snapshot.documents.mapNotNull { document ->
                                val geoPoint = document.getGeoPoint("location")
                                val name = document.getString("name")
                                val type = document.getString("type")
                                val description = document.getString("description")
                                if (geoPoint != null && name != null && type != null && description != null) {
                                    MapObject(
                                        id = document.id,
                                        name = name,
                                        type = type,
                                        description = description,
                                        location = LatLng(geoPoint.latitude, geoPoint.longitude)
                                    )
                                } else {
                                    null
                                }
                            }
                        }
                        showDialog = false
                    }.addOnFailureListener {
                        // Handle failure
                    }
                }
            },firestore=firestore
        )
    }
}

data class MapObject(
    val id: String,
    val name: String,
    val type: String,
    val description: String,
    val location: LatLng
)