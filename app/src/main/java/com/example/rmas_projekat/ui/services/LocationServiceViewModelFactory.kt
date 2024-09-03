package com.example.rmas_projekat.ui.services

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class LocationServiceViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationServiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocationServiceViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
