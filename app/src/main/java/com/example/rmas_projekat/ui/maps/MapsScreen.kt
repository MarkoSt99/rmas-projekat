package com.example.rmas_projekat.ui.maps

import android.util.Log
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(navController: NavController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val userMap = remember { mutableStateMapOf<String, String>() }
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

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
    var searchRadiusSliderValue by remember { mutableStateOf(10f) }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isCreatorDropdownExpanded by remember { mutableStateOf(false) }

    val searchRadius = when (searchRadiusSliderValue.toInt()) {
        0 -> 100f
        1 -> 200f
        2 -> 500f
        3 -> 1000f
        4 -> 2000f
        5 -> 3000f
        6 -> 4000f
        7 -> 5000f
        8 -> 7500f
        9 -> 10000f
        else -> Float.MAX_VALUE
    }

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
                    val ride = document.getBoolean("ride") ?: false
                    val start = document.getString("start")?.let { formatter.parse(it) }

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
                categories = mapObjects.map { it.category }.distinct()
                creators = mapObjects.map { it.creatorId }.distinct()
                filteredObjects = mapObjects.sortedBy { it.category }
            }
        }
    }

    LaunchedEffect(Unit) {
        firestore.collection("users").get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                snapshot.documents.forEach { document ->
                    val userId = document.id
                    val fullName = document.getString("fullName")

                    if (fullName != null) {
                        userMap[userId] = fullName
                        Log.d("MapsScreen", "Mapped User: $userId -> $fullName")
                    } else {
                        Log.e("MapsScreen", "Missing fullName for userId: $userId")
                    }
                }
            } else {
                Log.e("MapsScreen", "Failed to fetch users")
            }
        }
    }

    LaunchedEffect(selectedCategory, selectedCreator, searchQuery, searchRadiusSliderValue) {
        filteredObjects = mapObjects.filter { mapObject ->
            val matchesCategory = selectedCategory.isEmpty() || mapObject.category.equals(selectedCategory.trim(), ignoreCase = true)
            val matchesCreator = selectedCreator.isEmpty() || mapObject.creatorId == selectedCreator
            val matchesSearchQuery = searchQuery.isEmpty() || mapObject.name.contains(searchQuery, ignoreCase = true)
            val withinRadius = currentLocation?.let {
                if (searchRadius != Float.MAX_VALUE) {
                    val objectLocation = Location("").apply {
                        latitude = mapObject.location.latitude
                        longitude = mapObject.location.longitude
                    }
                    val distance = Location("").apply {
                        latitude = it.latitude
                        longitude = it.longitude
                    }.distanceTo(objectLocation)
                    distance <= searchRadius
                } else {
                    true
                }
            } ?: true

            matchesCategory && matchesCreator && matchesSearchQuery && withinRadius
        }.sortedBy { it.category }

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
            FloatingActionButton(
                onClick = {
                    selectedLocation = currentLocation
                    showDialog = true
                },
                modifier = Modifier.offset(x = -50.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Object", tint = MaterialTheme.colorScheme.onPrimary) // Set the icon color to match
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
                                    selectedCategory = category.trim()
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
                    val selectedFullName = if (selectedCreator.isNotEmpty()) {
                        userMap[selectedCreator] ?: "All Creators"
                    } else {
                        "All Creators"
                    }

                    OutlinedTextField(
                        value = selectedFullName,
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
                        creators.forEach { creatorId ->
                            val fullName = userMap[creatorId] ?: creatorId
                            DropdownMenuItem(
                                text = { Text(fullName) },
                                onClick = {
                                    selectedCreator = creatorId
                                    isCreatorDropdownExpanded = false
                                    Log.d("MapsScreen", "Selected Creator: $fullName ($creatorId)")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = "Search Radius: ${if (searchRadius == Float.MAX_VALUE) "Max" else "${searchRadius.toInt()} meters"}")
                Slider(
                    value = searchRadiusSliderValue,
                    onValueChange = { searchRadiusSliderValue = it },
                    valueRange = 0f..10f,
                    steps = 9
                )
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
                    Log.d("MapsScreen", "Rendering Marker: ${mapObject.name}, Location: ${mapObject.location.latitude}, ${mapObject.location.longitude}")

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
            onSave = { objectName, category, objectDescription, selectedIcon, imageUri, isRide, start ->
                if (selectedLocation != null) {
                    val newObject = mutableMapOf(
                        "name" to objectName,
                        "description" to objectDescription,
                        "location" to GeoPoint(selectedLocation!!.latitude, selectedLocation!!.longitude),
                        "icon" to selectedIcon,
                        "imageUri" to imageUri.toString(),
                        "creatorId" to FirebaseAuth.getInstance().currentUser?.uid,
                        "category" to category,
                        "ride" to isRide
                    )

                    if (start != null) {
                        newObject["start"] = formatter.format(start)
                    }

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
                                    val ride = document.getBoolean("ride") ?: false
                                    val startDate = document.getString("start")?.let { formatter.parse(it) }

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
                                            start = startDate
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




fun bitmapDescriptorFromVector(
    context: Context,
    vectorResId: Int,
    scaleFactor: Float = 1f
): BitmapDescriptor? {
    // Get the vector drawable
    val vectorDrawable: Drawable? = ContextCompat.getDrawable(context, vectorResId)
    if (vectorDrawable == null) {
        return null
    }

    val width = (vectorDrawable.intrinsicWidth * scaleFactor).takeIf { it > 0 } ?: return null
    val height = (vectorDrawable.intrinsicHeight * scaleFactor).takeIf { it > 0 } ?: return null

    vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)

    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)

    val scaledBitmap = Bitmap.createScaledBitmap(
        bitmap,
        (vectorDrawable.intrinsicWidth * scaleFactor).toInt(),
        (vectorDrawable.intrinsicHeight * scaleFactor).toInt(),
        false
    )

    return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
}

