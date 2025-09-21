package com.example.celestial.data.models

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class IngredientEditRecord(
    val ingredientName: String = "",
    val timestamp: String = "",
    val reason: String = "",
    val quantityDiff: Double = 0.0,
    val stockId: String = "",
    val expiryChange: ExpiryChange? = null,
    val imageChange: String = "NOT CHANGE" // set your default here!
) : Serializable

data class ExpiryChange(
    val before: String? = null,
    val after: String? = null
) : Serializable




