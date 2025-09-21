package com.example.celestial.data.models

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Wastage(
    @get:PropertyName("stock_id") @set:PropertyName("stock_id")
    var stockId: String = "",

    @get:PropertyName("expired_quantity_gram") @set:PropertyName("expired_quantity_gram")
    var expiredQuantityGram: Double = 0.0,

    @get:PropertyName("expired_quantity_kg") @set:PropertyName("expired_quantity_kg")
    var expiredQuantityKg: Double = 0.0,

    @get:PropertyName("expiry_date") @set:PropertyName("expiry_date")
    var expiryDate: String? = null,

    @get:PropertyName("ordered_date") @set:PropertyName("ordered_date")
    var orderedDate: String? = null
) : Serializable
