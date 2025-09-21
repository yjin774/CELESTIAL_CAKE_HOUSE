// Sale.kt
package com.example.celestial.data.models

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class SoldCake(
    @PropertyName("cakeName") val cakeName: String = "",
    @PropertyName("wholeCakeQty") val wholeCakeQty: Int = 0,
    @PropertyName("sliceQty") val sliceQty: Int = 0,
    @PropertyName("totalSale") val totalSale: Double = 0.0  // NEW FIELD
) : Serializable

data class Sale(
    @PropertyName("id") val id: String = "",
    @PropertyName("custName") val custName: String = "",
    @PropertyName("items") val items: List<SoldCake> = emptyList(),
    @PropertyName("date") val date: String = "" // store as formatted String: "YYYY-MM-DD HH:MM"
) : Serializable
