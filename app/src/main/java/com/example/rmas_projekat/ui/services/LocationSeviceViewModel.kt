package com.example.rmas_projekat.ui.services

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationServiceViewModel(context: Context) : ViewModel() {
    private val sharedPreferences = context.getSharedPreferences("LocationServicePrefs", Context.MODE_PRIVATE)
    private val _isLocationServiceEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("locationServiceEnabled", false)
    )
    val isLocationServiceEnabled: StateFlow<Boolean> = _isLocationServiceEnabled

    fun toggleLocationService() {
        val newState = !_isLocationServiceEnabled.value
        _isLocationServiceEnabled.value = newState
        sharedPreferences.edit().putBoolean("locationServiceEnabled", newState).apply()
    }
}
