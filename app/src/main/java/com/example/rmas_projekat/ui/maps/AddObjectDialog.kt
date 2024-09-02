@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.rmas_projekat.ui.maps

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.rmas_projekat.R
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObjectDialog(
    currentLocation: LatLng?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Uri?) -> Unit
) {
    var objectName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }  // Use a simple text box for category
    var objectDescription by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(R.drawable.default_pin) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )

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

                // Simple text field for category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = objectDescription,
                    onValueChange = { objectDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Icon Picker
                IconPicker(selectedIcon) { selectedIcon = it }

                // Image Picker
                Button(onClick = { launcher.launch("image/*") }) {
                    Text(text = "Choose Photo")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(objectName, category, objectDescription, selectedIcon, imageUri)
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


@Composable
fun IconPicker(selectedIcon: Int, onIconSelected: (Int) -> Unit) {
    // Example of displaying icons (assuming they're drawable resources)
    val icons = listOf(R.drawable.food, R.drawable.service_center, R.drawable.water, R.drawable.shop)
    Row(horizontalArrangement = Arrangement.SpaceAround) {
        icons.forEach { icon ->
            IconButton(onClick = { onIconSelected(icon) }) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
