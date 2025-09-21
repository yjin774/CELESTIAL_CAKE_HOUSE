package com.example.celestial.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class OpenFoodFactsProduct(
    val name: String,
    val brand: String,
    val quantity: String,
    val packaging: String,
    val expiryDate: String
)

object OpenFoodFactsUtils {
    suspend fun lookupProduct(barcode: String): OpenFoodFactsProduct? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://world.openfoodfacts.org/api/v0/product/${barcode}.json")
            val json = url.readText()
            val obj = JSONObject(json)
            if (obj.optInt("status", 0) == 1) {
                val prod = obj.getJSONObject("product")
                OpenFoodFactsProduct(
                    name = prod.optString("product_name", ""),
                    brand = prod.optString("brands", ""),
                    quantity = prod.optString("quantity", ""),
                    packaging = prod.optString("packaging", ""),
                    expiryDate = prod.optString("expiration_date", "")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

