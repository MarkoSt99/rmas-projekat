package com.example.rmas_projekat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rmas_projekat.ui.navigation.BottomNavigationBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LeaderboardScreen(navController: NavController, auth: FirebaseAuth) {
    val firestore = FirebaseFirestore.getInstance()
    var users by remember { mutableStateOf(listOf<UserScore>()) }

    LaunchedEffect(Unit) {
        firestore.collection("users")
            .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                users = snapshot.documents.mapNotNull { document ->
                    val name = document.getString("fullName")
                    val score = document.getLong("score")?.toInt() ?: 0
                    if (name != null) UserScore(name, score) else null
                }
            }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Rider", style = MaterialTheme.typography.labelLarge)
                Text(text = "Score", style = MaterialTheme.typography.labelLarge)
            }

            users.forEachIndexed { index, user ->
                val backgroundColor = if (index % 2 == 0) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = user.name, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "${user.score}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

data class UserScore(val name: String, val score: Int)
