package com.example.celestial.data.models

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Stock(
    @get:PropertyName("stock_id") @set:PropertyName("stock_id")
    var stockId: String = "",
    @get:PropertyName("quantity") @set:PropertyName("quantity")
    var quantity: Double = 0.0,
    @get:PropertyName("unit") @set:PropertyName("unit")
    var unit: String = "GRAM",
    @get:PropertyName("expiry_date") @set:PropertyName("expiry_date")
    var expiryDate: String? = null,
    @get:PropertyName("ordered_date") @set:PropertyName("ordered_date")
    var orderedDate: String? = null,
    // --- NEW FIELDS ---
    @get:PropertyName("ordered_amount_gram") @set:PropertyName("ordered_amount_gram")
    var orderedAmountGram: Double = 0.0,
    @get:PropertyName("ordered_amount_kg") @set:PropertyName("ordered_amount_kg")
    var orderedAmountKg: Double = 0.0
) : Serializable
