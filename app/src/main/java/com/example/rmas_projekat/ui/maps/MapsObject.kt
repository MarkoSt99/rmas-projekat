package com.example.rmas_projekat.ui.maps

import com.google.android.gms.maps.model.LatLng

import java.time.LocalDateTime
import java.util.Date

data class MapObject(
    val id: String,
    val name: String,
    val description: String,
    val location: LatLng,
    val icon: Int,
    val imageUri: String? = null,
    val creatorId: String,
    val category: String,
    val ride: Boolean = false,
    val start: Date? = null, // LocalDateTime to hold date and time
    val riders: List<String> = listOf() // List of rider IDs
)


