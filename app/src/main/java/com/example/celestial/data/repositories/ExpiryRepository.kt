package com.example.celestial.data.repositories

import android.content.Context
import android.util.Log
import com.example.celestial.data.models.Ingredient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExpiryRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid
    private val TAG = "ExpiryRepository"

    suspend fun getExpiringIngredients(): List<Ingredient> {
        if (userId == null) {
            Log.e(TAG, "Cannot fetch expiring ingredients: User not authenticated")
            return emptyList()
        }

        val currentDate = LocalDate.now()
        val expiryThreshold = currentDate.plusDays(7)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        return try {
            val snapshot = db.collection("users")
                .document(userId!!)
                .collection("ingredients")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val ingredient = doc.toObject(Ingredient::class.java)
                ingredient?.let {
                    val expiryDateStr = it.expiryDate
                    if (expiryDateStr != null) {
                        try {
                            val expiryDate = LocalDate.parse(expiryDateStr, formatter)
                            if (expiryDate.isBefore(expiryThreshold) && expiryDate.isAfter(currentDate)) {
                                it
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Invalid date format for ingredient ${it.id}: ${e.message}")
                            null
                        }
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching expiring ingredients: ${e.message}", e)
            emptyList()
        }
    }
}