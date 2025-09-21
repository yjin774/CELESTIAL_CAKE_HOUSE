package com.example.celestial.data.models

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Ingredient(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("quantity") val quantity: Double = 0.0,
    @PropertyName("unit") val unit: String? = null, // Made optional
    @PropertyName("expiryDate") val expiryDate: String? = null,
    @PropertyName("disabled") val disabled: Boolean = false,
    @PropertyName("imageUrl") val imageUrl: String? = null
) : Serializable