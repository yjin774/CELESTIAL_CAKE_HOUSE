package com.example.celestial.data.models

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Cake(
    @PropertyName("id") val id: String = "",
    @PropertyName("type") val type: String = "",
    @PropertyName("wholeCakeQuantity") val wholeCakeQuantity: Int = 0,
    @PropertyName("sliceQuantity") val sliceQuantity: Int = 0,
    @PropertyName("ingredients") val ingredients: Map<String, Double> = emptyMap(),
    @PropertyName("wholeCakePrice") val wholeCakePrice: Double = 60.0,
    @PropertyName("sliceCakePrice") val sliceCakePrice: Double = 9.0,
    @PropertyName("availableProduce") val availableProduce: Int = 0,
    @PropertyName("imageUrl") val imageUrl: String? = null
) : Serializable
