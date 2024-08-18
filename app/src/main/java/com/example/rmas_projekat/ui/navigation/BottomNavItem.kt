package com.example.rmas_projekat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val screenRoute: String
) {
    object Home : BottomNavItem("Home", Icons.Filled.Home, "home")
    object Maps : BottomNavItem("Maps", Icons.Filled.Map, "maps")
    object Record : BottomNavItem("Record", Icons.Filled.Mic, "record")
    object Groups : BottomNavItem("Groups", Icons.Filled.Group, "groups")
    object You : BottomNavItem("You", Icons.Filled.Person, "you")
}
