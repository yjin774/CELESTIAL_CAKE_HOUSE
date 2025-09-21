package com.example.celestial.data.repositories

import com.example.celestial.data.models.Wastage
import android.content.Context
import android.util.Log
import androidx.lifecycle.get
import com.google.firebase.Timestamp
import com.example.celestial.data.models.Cake
import com.example.celestial.data.models.Ingredient
import com.example.celestial.data.models.Sale
import com.example.celestial.data.models.Stock
import com.example.celestial.ui.viewmodels.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class InventoryRepository(private val context: Context) {

    private val TAG = "InventoryRepository"
    private val db = FirebaseFirestore.getInstance()
    private val stockListeners = mutableMapOf<String, ListenerRegistration>()
    private var ingredientsListener: ListenerRegistration? = null
    private var cakesListener: ListenerRegistration? = null
    private val wastageListeners = mutableMapOf<String, ListenerRegistration?>()
    private val wastageCache = mutableMapOf<String, List<Wastage>>()

    suspend fun addSaleToUserSubcollection(userId: String, sale: Sale) {
        val saleRef = db.collection("users")
            .document(userId)
            .collection("sales")
            .document(sale.id)
        saleRef.set(sale).await()
    }


    fun removeWastageListeners() {
        wastageListeners.values.forEach { it?.remove() }
        wastageListeners.clear()
    }
    // All collection accessors are now NULLABLE and guarded!
    fun ingredientsCollection(): CollectionReference? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return db.collection("users").document(uid).collection("ingredients")
    }

    fun cakesCollection(): CollectionReference? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return db.collection("users").document(uid).collection("cakes")
    }

    suspend fun seedDataIfEmpty() {
        val cakesColl = cakesCollection() ?: return
        val ingredientsColl = ingredientsCollection() ?: return
        try {
            val cakesSnapshot = cakesColl.get().await()
            val ingredientsSnapshot = ingredientsColl.get().await()
            if (cakesSnapshot.isEmpty && ingredientsSnapshot.isEmpty) {
                Log.d(TAG, "Seeding initial cakes and ingredients data for user")
                val gson = Gson()
                val cakesJson = context.assets.open("cakes.json").bufferedReader().use { it.readText() }
                val cakes = gson.fromJson(cakesJson, Array<Cake>::class.java).toList()
                val ingredientsJson = context.assets.open("ingredients.json").bufferedReader().use { it.readText() }
                val ingredients = gson.fromJson(ingredientsJson, Array<Ingredient>::class.java).toList()
                val batch: WriteBatch = db.batch()
                cakes.forEach { cake ->
                    cakesColl.document(cake.id).let { batch.set(it, cake) }
                }
                ingredients.forEach { ingredient ->
                    ingredientsColl.document(ingredient.id).let { batch.set(it, ingredient) }
                }
                batch.commit().await()
                Log.d(TAG, "Seeding completed successfully for user")
            } else {
                Log.d(TAG, "Data already exists for user; skipping seed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during seeding data for user: ${e.message}", e)
            throw e
        }
    }

    fun listenIngredientsChanges(onUpdate: (List<Ingredient>) -> Unit) {
        ingredientsListener?.remove()
        val coll = ingredientsCollection() ?: return
        ingredientsListener = coll.addSnapshotListener { snapshots, _ ->
            val ingredients = snapshots?.mapNotNull { it.toObject(Ingredient::class.java) } ?: emptyList()
            onUpdate(ingredients)
        }
    }

    fun listenCakesChanges(onUpdate: (List<Cake>) -> Unit) {
        cakesListener?.remove()
        val coll = cakesCollection() ?: return
        cakesListener = coll.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e(TAG, "Error listening cakes: ${error.message}")
                onUpdate(emptyList())
                return@addSnapshotListener
            }
            val cakes = snapshots?.mapNotNull { it.toObject(Cake::class.java) } ?: emptyList()
            onUpdate(cakes)
        }
    }

    fun listenWastageChanges(onUpdate: (List<Pair<String, Wastage>>) -> Unit) {
        // Clear previous listeners and cache
        wastageListeners.values.forEach { it?.remove() }
        wastageListeners.clear()
        wastageCache.clear()

        val coll = ingredientsCollection() ?: return

        coll.get().addOnSuccessListener { ingredientSnapshot ->
            ingredientSnapshot.documents.forEach { ingredientDoc ->
                val ingredientId = ingredientDoc.id
                val wastageColl = coll.document(ingredientId).collection("wastages")
                val listener = wastageColl.addSnapshotListener { snapshots, _ ->
                    val wastages = snapshots?.mapNotNull { it.toObject(Wastage::class.java) } ?: emptyList()
                    wastageCache[ingredientId] = wastages

                    // Combine all cached wastage lists into one list
                    val combined = wastageCache.flatMap { entry ->
                        entry.value.map { wastage -> entry.key to wastage }
                    }
                    onUpdate(combined)
                }
                wastageListeners[ingredientId] = listener
            }
        }
    }

    suspend fun getWastagesForIngredient(ingredientId: String): List<Wastage> {
        val coll = ingredientsCollection() ?: return emptyList()
        return try {
            val snapshot = coll.document(ingredientId).collection("wastages").get().await()
            snapshot.documents.mapNotNull { it.toObject(Wastage::class.java) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getIngredients(): List<Ingredient> {
        val coll = ingredientsCollection() ?: return emptyList()
        return try {
            val snapshot = coll.get().await()
            snapshot.documents.mapNotNull { it.toObject(Ingredient::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ingredients: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getCakes(): List<Cake> {
        val coll = cakesCollection() ?: return emptyList()
        return try {
            val snapshot = coll.get().await()
            snapshot.documents.mapNotNull { it.toObject(Cake::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cakes: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addStock(ingredientId: String, stock: Stock) {
        val ingredientColl = ingredientsCollection()
        Log.d("AddStockRepo", "ingredientsCollection(): $ingredientColl") // Shows if collection is null (bad auth)
        if (ingredientColl == null) {
            Log.e("AddStockRepo", "ingredientsCollection() is null. User may not be authenticated.")
            throw IllegalStateException("User not authenticated")
        }
        try {
            val stockRef = ingredientColl.document(ingredientId).collection("stocks").document(stock.stockId)
            val ingredientRef = ingredientColl.document(ingredientId)
            Log.d("AddStockRepo", "About to run transaction for ingredient: $ingredientId, stockId: ${stock.stockId}")
            db.runTransaction { transaction ->
                transaction.set(stockRef, stock)
                Log.d("AddStockRepo", "Transaction: set stockRef done.")
                val quantityToAdd = if (stock.unit == "KG") stock.quantity * 1000 else stock.quantity
                transaction.update(ingredientRef, "quantity", FieldValue.increment(quantityToAdd))
                Log.d("AddStockRepo", "Transaction: updated ingredient quantity by $quantityToAdd.")
            }.await()
            Log.d("AddStockRepo", "addStock succeeded for ingredientId=$ingredientId, stockId=${stock.stockId}")
        } catch (e: Exception) {
            Log.e("AddStockRepo", "Error adding stock", e)
            throw e
        }
    }



    suspend fun updateStockQuantity(ingredientId: String, stockId: String, newQuantity: Double) {
        val ingredientColl = ingredientsCollection() ?: return
        val stockRef = ingredientColl.document(ingredientId).collection("stocks").document(stockId)
        stockRef.update("quantity", newQuantity).await()
    }

    suspend fun updateIngredientTotalQuantity(ingredientId: String, deltaQuantity: Double) {
        val ingredientColl = ingredientsCollection() ?: return
        val ingredientRef = ingredientColl.document(ingredientId)
        ingredientRef.update("quantity", FieldValue.increment(deltaQuantity)).await()
    }

    suspend fun deleteStock(ingredientId: String, stockId: String) {
        val ingredientColl = ingredientsCollection() ?: return
        val stockRef = ingredientColl.document(ingredientId).collection("stocks").document(stockId)
        stockRef.delete().await()
    }

    suspend fun updateStockExpiry(ingredientId: String, stockId: String, newExpiryDate: String) {
        val ingredientColl = ingredientsCollection() ?: return
        val stockRef = ingredientColl.document(ingredientId).collection("stocks").document(stockId)
        stockRef.update("expiry_date", newExpiryDate).await()
    }

    suspend fun updateCakeQuantities(cakeId: String, wholeCakeQty: Int, sliceQty: Int) {
        val cakesColl = cakesCollection() ?: return
        cakesColl.document(cakeId)
            .update(mapOf("wholeCakeQuantity" to wholeCakeQty, "sliceQuantity" to sliceQty))
            .await()
    }

    suspend fun getStocksForIngredient(ingredientId: String): List<Stock> {
        val ingredientColl = ingredientsCollection() ?: return emptyList()
        return try {
            val snapshot = ingredientColl.document(ingredientId).collection("stocks").get().await()
            snapshot.documents.mapNotNull { doc -> doc.toObject(Stock::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stocks for ingredient $ingredientId: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addSaleRecord(
        orderId: String,
        customerName: String,
        customerAddress: String,
        cartItems: List<CartItem>,
        totalPrice: Double
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val saleRef = db.collection("users").document(uid).collection("sales").document(orderId)
        val saleRefGlobal = db.collection("sales").document(orderId)
        val batch: WriteBatch = db.batch()
        val saleData = mapOf(
            "id" to orderId,
            "customerName" to customerName,
            "customerAddress" to customerAddress,
            "totalPrice" to totalPrice,
            "items" to cartItems.map {
                mapOf(
                    "cakeId" to it.cake.id,
                    "wholeCakeQty" to it.wholeCakeQuantity,
                    "sliceQty" to it.sliceQuantity
                )
            },
            "date" to Timestamp.now()
        )
        batch.set(saleRef, saleData)
        batch.set(saleRefGlobal, saleData)
        batch.commit().await()
    }

    suspend fun deductStocksInOrder(ingredientId: String, deductionAmount: Double) {
        val ingredientColl = ingredientsCollection() ?: return
        val stocks = getStocksForIngredient(ingredientId)
            .sortedBy { stock ->
                try {
                    stock.expiryDate?.let { LocalDate.parse(it) } ?: LocalDate.MAX
                } catch (e: Exception) { LocalDate.MAX }
            }
            .toMutableList()
        var remaining = deductionAmount
        for (stock in stocks) {
            if (remaining <= 0) break
            val deductQty = minOf(stock.quantity, remaining)
            val newQty = stock.quantity - deductQty
            updateStockQuantity(ingredientId, stock.stockId, newQty)
            remaining -= deductQty
        }
    }

    suspend fun produceCakeOnFirestore(cakeId: String, quantity: Int) {
        val cakesColl = cakesCollection() ?: return
        val cakeDoc = cakesColl.document(cakeId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(cakeDoc)
            val currentWhole = snapshot.getLong("wholeCakeQuantity") ?: 0L
            val currentSlice = snapshot.getLong("sliceQuantity") ?: 0L
            transaction.update(cakeDoc, "wholeCakeQuantity", currentWhole + quantity)
            transaction.update(cakeDoc, "sliceQuantity", currentSlice + quantity * 8)
        }.await()
    }

    fun listenStocksForIngredient(
        ingredientId: String,
        onUpdate: (List<Stock>) -> Unit
    ): ListenerRegistration? {
        val ingredientColl = ingredientsCollection() ?: return null
        val stockColl = ingredientColl.document(ingredientId).collection("stocks")
        return stockColl.addSnapshotListener { snapshots, _ ->
            val stocks = snapshots?.mapNotNull { it.toObject(Stock::class.java) } ?: emptyList()
            onUpdate(stocks)
        }
    }

    suspend fun addWastageRecord(ingredientId: String, wastage: Wastage) {
        val ingredientColl = ingredientsCollection() ?: return
        val wastageColl = ingredientColl.document(ingredientId).collection("wastages")
        wastageColl.document(wastage.stockId).set(wastage).await()
    }

    suspend fun addSaleRecordObject(sale: Sale) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val saleRefUser = db.collection("users").document(uid).collection("sales").document(sale.id)
        val saleRefGlobal = db.collection("sales").document(sale.id)
        saleRefUser.set(sale).await()
        saleRefGlobal.set(sale).await()
    }

    fun removeAllStockListeners() {
        stockListeners.values.forEach { it?.remove() }
        stockListeners.clear()
    }

    suspend fun updateCakeAvailableProduce(cakeId: String, available: Int) {
        val cakesColl = cakesCollection() ?: return
        cakesColl.document(cakeId)
            .update("availableProduce", available)
            .await()
    }

    suspend fun getAvailableCakes(cake: Cake): Int {
        return try {
            val ingredientsList = getIngredients()
            if (ingredientsList.isEmpty()) return 0
            var minPossible = Int.MAX_VALUE
            for ((ingredientId, qtyPerCake) in cake.ingredients) {
                val ingredient = ingredientsList.find { it.id == ingredientId } ?: return 0
                if (ingredient.disabled || ingredient.quantity <= 0.0) return 0
                val available = (ingredient.quantity / qtyPerCake).toInt()
                minPossible = minOf(minPossible, available)
            }
            minPossible
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating available cakes: ${e.message}", e)
            0
        }
    }

    fun removeListeners() {
        ingredientsListener?.remove()
        cakesListener?.remove()
        ingredientsListener = null
        cakesListener = null
        stockListeners.values.forEach { it?.remove() }
        stockListeners.clear()
    }
}
