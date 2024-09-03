@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.rmas_projekat.ui.maps

import android.app.TimePickerDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObjectDialog(
    currentLocation: LatLng?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Uri?, Boolean, Date?) -> Unit
) {
    var objectName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var objectDescription by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(R.drawable.default_pin) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isRide by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )

    val calendar = Calendar.getInstance()

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            selectedDate = calendar.time
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
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

                OutlinedTextField(
                    value = if (isRide) "Ride" else category,
                    onValueChange = { if (!isRide) category = it },
                    label = { Text("Category") },
                    enabled = !isRide,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = objectDescription,
                    onValueChange = { objectDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = isRide,
                        onCheckedChange = { isChecked ->
                            isRide = isChecked
                            if (isChecked) {
                                category = "Ride"
                            } else {
                                category = ""
                                selectedDate = null
                            }
                        }
                    )
                    Text("This is a Ride")
                }

                if (isRide) {
                    Button(onClick = { datePickerDialog.show() }) {
                        Text("Select Date")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { timePickerDialog.show() }) {
                        Text("Select Time")
                    }

                    selectedDate?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Selected Date & Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)}")
                    }
                }

                IconPicker(selectedIcon) { selectedIcon = it }

                Button(onClick = { launcher.launch("image/*") }) {
                    Text(text = "Choose Photo")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                currentUserId?.let { userId ->
                    onSave(objectName, category, objectDescription, selectedIcon, imageUri, isRide, selectedDate)

                    firestore.collection("users").document(userId)
                        .update("score", FieldValue.increment(5))
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




@Composable
fun IconPicker(selectedIcon: Int, onIconSelected: (Int) -> Unit) {
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
