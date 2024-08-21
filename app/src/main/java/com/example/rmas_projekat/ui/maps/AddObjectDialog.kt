@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.rmas_projekat.ui.maps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.Modifier
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.FirebaseFirestore



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObjectDialog(
    currentLocation: LatLng?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    firestore: FirebaseFirestore // Add this parameter
) {
    var objectName by remember { mutableStateOf("") }
    var objectType by remember { mutableStateOf("") }
    var objectDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Object") },
        text = {
            Column {
                OutlinedTextField(
                    value = objectName,
                    onValueChange = { objectName = it },
                    label = { Text("Object Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = objectType,
                    onValueChange = { objectType = it },
                    label = { Text("Object Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = objectDescription,
                    onValueChange = { objectDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (currentLocation != null) {
                    // Save object to Firestore with GeoPoint for the location
                    val newObject = mapOf(
                        "name" to objectName,
                        "type" to objectType,
                        "description" to objectDescription,
                        "location" to GeoPoint(currentLocation.latitude, currentLocation.longitude) // Ensure this is GeoPoint
                    )
                    firestore.collection("objects").add(newObject).addOnSuccessListener {
                        onDismiss() // Close the dialog on success
                    }.addOnFailureListener {
                        // Handle failure
                    }
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
