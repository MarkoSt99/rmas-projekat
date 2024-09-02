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
    onSave: (String, String, String, Int, Uri?) -> Unit,
    onAddCategory: (String) -> Unit
) {
    var objectName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var objectDescription by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(R.drawable.default_pin) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val mutex = Mutex()
    var categories by remember { mutableStateOf(listOf<String>()) }

    // Fetch categories from Firestore
    LaunchedEffect(Unit) {
        firestore.collection("categories")
            .get()
            .addOnSuccessListener { result ->
                categories = result.documents.mapNotNull { it.getString("name") }
                if (categories.isNotEmpty()) {
                    selectedCategory = categories.first()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AddObjectDialog", "Error getting categories", exception)
            }
    }

    // Image picker launcher
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

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = { /* No-op */ },
                        label = { Text("Object Type") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Add New Object Type/Category") },
                            onClick = {
                                showAddCategoryDialog = true
                                expanded = false
                            }
                        )
                    }
                }

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
                onSave(objectName, selectedCategory, objectDescription, selectedIcon, imageUri)
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

    // Add category dialog if showAddCategoryDialog is true
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            mutex.withLock {
                                val newCategory = newCategoryName.trim()

                                if (newCategory.isNotEmpty()) {
                                    try {
                                        Log.d("AddObjectDialog", "Checking if category already exists: $newCategory")
                                        val categoryDocRef = firestore.collection("categories").document(newCategory)
                                        val existingCategoryDoc = categoryDocRef.get().await()

                                        if (!existingCategoryDoc.exists()) {
                                            Log.d("AddObjectDialog", "Category doesn't exist, adding: $newCategory")
                                            categoryDocRef.set(mapOf("name" to newCategory)).await()
                                            Log.d("AddObjectDialog", "Category added successfully: $newCategory")
                                            categories = categories + newCategory
                                            selectedCategory = newCategory
                                            onAddCategory(newCategory)
                                        } else {
                                            Log.d("AddObjectDialog", "Category already exists: $newCategory")
                                            selectedCategory = newCategory
                                            onAddCategory(newCategory)
                                        }
                                        showAddCategoryDialog = false
                                    } catch (e: Exception) {
                                        Log.e("AddObjectDialog", "Error during category check/add", e)
                                    }
                                } else {
                                    Log.w("AddObjectDialog", "Attempted to add an empty category")
                                }
                            }
                        }
                    }
                ) {
                    Text("Add Category")
                }
            },
            dismissButton = {
                Button(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
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
