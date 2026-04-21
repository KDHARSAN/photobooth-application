package com.example.photobooth.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    val authManager = AuthManager(application)
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(authManager.auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    fun handleSignInResult(idToken: String?) {
        if (idToken == null) return
        viewModelScope.launch {
            val success = authManager.signInWithGoogleToken(idToken)
            if (success) {
                _currentUser.value = authManager.auth.currentUser
            }
        }
    }

    fun signOut() {
        authManager.signOut()
        _currentUser.value = null
    }
}
