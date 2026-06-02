package com.example.vibecheck_dev.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.UpdateProfileImage -> _uiState.update { it.copy(profileImageUri = event.uri) }
            is HomeEvent.UpdateUsername -> _uiState.update { it.copy(username = event.username) }
            is HomeEvent.UpdatePassword -> _uiState.update { it.copy(password = event.password) }
            is HomeEvent.SaveProfile -> {
                _uiState.update { it.copy(isSaving = true) }
                viewModelScope.launch {
                    // Simulasi delay nyimpen data ke lokal/database
                    delay(800)
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }
}