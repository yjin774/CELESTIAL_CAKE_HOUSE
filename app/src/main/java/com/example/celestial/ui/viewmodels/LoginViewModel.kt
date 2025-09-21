package com.example.celestial.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.celestial.data.repositories.AuthRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class LoginViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val TAG = "LoginViewModel"

    fun login(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "Starting login coroutine for email: $email")
            try {
                Log.d(TAG, "Attempting login with email: $email, password length: ${password.length}")
                val result = authRepo.login(email, password)
                Log.d(TAG, "Login result received: isSuccess=${result.isSuccess}")
                if (result.isSuccess) {
                    Log.d(TAG, "Login successful, notifying success")
                    onSuccess()
                } else {
                    val message = result.exceptionOrNull()?.message ?: "Login failed"
                    Log.d(TAG, "Login failed: $message")
                    onFailure(message)
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    val message = "Login failed due to an internal error: ${e.message}"
                    Log.e(TAG, message, e)
                    onFailure(message)
                } else {
                    Log.w(TAG, "Login cancelled: ${e.message}")
                }
            } finally {
                Log.d(TAG, "Login coroutine completed")
            }
        }
    }

    fun register(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "Starting register coroutine for email: $email")
            try {
                Log.d(TAG, "Attempting register with email: $email, password length: ${password.length}")
                val result = authRepo.register(email, password)
                Log.d(TAG, "Register result received: isSuccess=${result.isSuccess}")
                if (result.isSuccess) {
                    Log.d(TAG, "Registration successful, notifying success")
                    onSuccess()
                } else {
                    val message = result.exceptionOrNull()?.message ?: "Registration failed"
                    Log.d(TAG, "Registration failed: $message")
                    onFailure(message)
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    val message = "Registration failed due to an internal error: ${e.message}"
                    Log.e(TAG, message, e)
                    onFailure(message)
                } else {
                    Log.w(TAG, "Registration cancelled: ${e.message}")
                }
            } finally {
                Log.d(TAG, "Register coroutine completed")
            }
        }
    }
}