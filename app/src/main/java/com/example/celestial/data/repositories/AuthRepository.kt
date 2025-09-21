package com.example.celestial.data.repositories

import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import kotlin.Result
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "AuthRepository"

    suspend fun login(email: String, password: String): Result<Unit> {
        Log.d(TAG, "Initiating login for email: $email")
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            if (authResult.user != null) {
                Log.d(TAG, "Login succeeded for email: $email")
                Result.success(Unit)
            } else {
                Log.d(TAG, "Login failed: No user returned")
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String): Result<Unit> {
        Log.d(TAG, "Initiating registration for email: $email")
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            if (authResult.user != null) {
                Log.d(TAG, "Registration succeeded for email: $email")
                Result.success(Unit)
            } else {
                Log.d(TAG, "Registration failed: No user returned")
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration error: ${e.message}", e)
            Result.failure(e)
        }
    }


    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}