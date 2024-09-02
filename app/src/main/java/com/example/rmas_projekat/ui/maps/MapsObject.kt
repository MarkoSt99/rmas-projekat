package com.example.rmas_projekat.ui.maps

import com.google.android.gms.maps.model.LatLng

data class MapObject(
    val id: String,
    val name: String,
    val description: String,
    val location: LatLng,
    val icon: Int,
    val imageUri: String? = null,
    val creatorId: String,
    val category: String // Add this field for the object's category
)
