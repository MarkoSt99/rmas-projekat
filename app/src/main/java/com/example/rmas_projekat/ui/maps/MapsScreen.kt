package com.example.rmas_projekat.ui.maps

import android.util.Log
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.rmas_projekat.R
import com.example.rmas_projekat.ui.navigation.BottomNavigationBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(navController: NavController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

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
    var filteredObjects by remember { mutableStateOf(listOf<MapObject>()) }
    var categories by remember { mutableStateOf(listOf<String>()) }
    var creators by remember { mutableStateOf(listOf<String>()) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedCreator by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isCreatorDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firestore.collection("objects").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                mapObjects = snapshot.documents.mapNotNull { document ->
                    val geoPoint = document.getGeoPoint("location")
                    val name = document.getString("name")
                    val description = document.getString("description")
                    val icon = document.getLong("icon")?.toInt() ?: R.drawable.default_pin
                    val imageUri = document.getString("imageUri")
                    val creatorId = document.getString("creatorId")
                    val category = document.getString("category").orEmpty()

                    if (geoPoint != null && name != null && description != null && creatorId != null) {
                        MapObject(
                            id = document.id,
                            name = name,
                            description = description,
                            location = LatLng(geoPoint.latitude, geoPoint.longitude),
                            icon = icon,
                            imageUri = imageUri,
                            creatorId = creatorId,
                            category = category
                        )
                    } else {
                        null
                    }
                }
                categories = mapObjects.map { it.category }.distinct()
                creators = mapObjects.map { it.creatorId }.distinct()
                filteredObjects = mapObjects.sortedBy { it.category }
            }
        }
    }

    LaunchedEffect(selectedCategory, selectedCreator, searchQuery) {
        // Filtering logic
        filteredObjects = mapObjects.filter { mapObject ->
            val matchesCategory = selectedCategory.isEmpty() || mapObject.category.equals(selectedCategory.trim(), ignoreCase = true)
            val matchesCreator = selectedCreator.isEmpty() || mapObject.creatorId == selectedCreator
            val matchesSearchQuery = searchQuery.isEmpty() || mapObject.name.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesCreator && matchesSearchQuery
        }.sortedBy { it.category }

        // Log filtered objects to ensure correct filtering
        filteredObjects.forEach { obj ->
            Log.d("MapsScreen", "Filtered Object: ${obj.name}, Category: ${obj.category}, Location: ${obj.location.latitude}, ${obj.location.longitude}")
        }
    }




    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation ?: LatLng(0.0, 0.0), 15f)
    }

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

    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedLocation = currentLocation
                showDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Object")
            }
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Objects") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedCategory.ifEmpty { "All Categories" },
                        onValueChange = { /* No-op */ },
                        label = { Text("Category") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = {
                                selectedCategory = ""
                                isCategoryDropdownExpanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category.trim()  // Ensure no leading/trailing spaces
                                    isCategoryDropdownExpanded = false
                                    Log.d("MapsScreen", "Selected Category: $selectedCategory")
                                }
                            )

                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                ExposedDropdownMenuBox(
                    expanded = isCreatorDropdownExpanded,
                    onExpandedChange = { isCreatorDropdownExpanded = !isCreatorDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedCreator.ifEmpty { "All Creators" },
                        onValueChange = { /* No-op */ },
                        label = { Text("Creator") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCreatorDropdownExpanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCreatorDropdownExpanded,
                        onDismissRequest = { isCreatorDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Creators") },
                            onClick = {
                                selectedCreator = ""
                                isCreatorDropdownExpanded = false
                            }
                        )
                        creators.forEach { creator ->
                            DropdownMenuItem(
                                text = { Text(creator) },
                                onClick = {
                                    selectedCreator = creator
                                    isCreatorDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

                filteredObjects.forEach { mapObject ->
                    // Log to confirm correct position is used
                    Log.d("MapsScreen", "Rendering Marker: ${mapObject.name}, Location: ${mapObject.location.latitude}, ${mapObject.location.longitude}")

                    // Use mapObject.id with remember to retain individual marker states
                    val markerState = remember(mapObject.id) {
                        MarkerState(position = LatLng(mapObject.location.latitude, mapObject.location.longitude))
                    }

                    Marker(
                        state = markerState,
                        title = mapObject.name,
                        snippet = mapObject.description,
                        onClick = {
                            navController.navigate("objectDetails/${mapObject.id}")
                            true
                        },
                        icon = bitmapDescriptorFromVector(context, mapObject.icon, 0.1f)
                    )
                }
            }


        }
    }

    if (showDialog) {
        AddObjectDialog(
            currentLocation = selectedLocation ?: currentLocation,
            onDismiss = { showDialog = false },
            onSave = { objectName, category, objectDescription, selectedIcon, imageUri ->
                if (selectedLocation != null) {
                    val newObject = mapOf(
                        "name" to objectName,
                        "description" to objectDescription,
                        "location" to GeoPoint(selectedLocation!!.latitude, selectedLocation!!.longitude),
                        "icon" to selectedIcon,
                        "imageUri" to imageUri.toString(),
                        "creatorId" to FirebaseAuth.getInstance().currentUser?.uid,
                        "category" to category
                    )
                    firestore.collection("objects").add(newObject).addOnSuccessListener {
                        scope.launch {
                            firestore.collection("objects").get().addOnSuccessListener { snapshot ->
                                mapObjects = snapshot.documents.mapNotNull { document ->
                                    val geoPoint = document.getGeoPoint("location")
                                    val name = document.getString("name")
                                    val description = document.getString("description")
                                    val icon = document.getLong("icon")?.toInt() ?: R.drawable.default_pin
                                    val imageUri = document.getString("imageUri")
                                    val creatorId = document.getString("creatorId")
                                    val category = document.getString("category").orEmpty()

                                    if (geoPoint != null && name != null && description != null && creatorId != null) {
                                        MapObject(
                                            id = document.id,
                                            name = name,
                                            description = description,
                                            location = LatLng(geoPoint.latitude, geoPoint.longitude),
                                            icon = icon,
                                            imageUri = imageUri,
                                            creatorId = creatorId,
                                            category = category
                                        )
                                    } else {
                                        null
                                    }
                                }
                                filteredObjects = mapObjects.sortedBy { it.category }
                            }
                        }
                        showDialog = false
                    }
                }
            }
        )
    }
}

fun bitmapDescriptorFromVector(context: Context, vectorResId: Int, scaleFactor: Float = 0.1f): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
    vectorDrawable?.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)

    val bitmap = Bitmap.createBitmap(
        (vectorDrawable!!.intrinsicWidth * scaleFactor).toInt(),
        (vectorDrawable.intrinsicHeight * scaleFactor).toInt(),
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    canvas.scale(scaleFactor, scaleFactor)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

