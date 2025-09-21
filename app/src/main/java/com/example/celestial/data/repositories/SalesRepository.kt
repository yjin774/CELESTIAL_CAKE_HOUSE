package com.example.celestial.data.repositories

import android.content.Context
import android.util.Log
import com.example.celestial.data.models.Cake
import com.example.celestial.data.models.Sale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class SalesRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid
    private val TAG = "SalesRepository"

    private var salesListener: ListenerRegistration? = null

    fun listenSalesChanges(onUpdate: (List<Sale>) -> Unit) {
        salesListener?.remove()
        val coll = FirebaseFirestore.getInstance()
            .collection("users")
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .collection("sales")
        salesListener = coll.addSnapshotListener { snaps, _ ->
            val list = snaps?.mapNotNull { it.toObject(Sale::class.java) } ?: emptyList()
            onUpdate(list)
        }
    }

    fun removeSalesListener() {
        salesListener?.remove()
    }

    suspend fun addSale(sale: Sale) {
        if (userId == null) {
            Log.e(TAG, "Cannot add sale: User not authenticated")
            return
        }

        try {
            val batch = db.batch()
            // Loop through each SoldCake in the sale
            for (soldCake in sale.items) {
                // For each sold cake, fetch the cake to get its ingredient breakdown
                val cakeSnapshot = db.collection("users")
                    .document(userId!!)
                    .collection("cakes")
                    .whereEqualTo("type", soldCake.cakeName)
                    .get()
                    .await()
                val cake = cakeSnapshot.documents.firstOrNull()?.toObject(Cake::class.java)
                    ?: throw IllegalStateException("Cake not found: ${soldCake.cakeName}")

                // Deduct ingredients for whole cakes
                if (soldCake.wholeCakeQty > 0) {
                    cake.ingredients.forEach { entry ->
                        val ingredientRef = db.collection("users")
                            .document(userId!!)
                            .collection("ingredients")
                            .document(entry.key)
                        batch.update(
                            ingredientRef, "quantity",
                            com.google.firebase.firestore.FieldValue.increment(-entry.value * soldCake.wholeCakeQty)
                        )
                    }
                }
                // Deduct ingredients for slice cakes (proportional)
                if (soldCake.sliceQty > 0) {
                    cake.ingredients.forEach { entry ->
                        val ingredientRef = db.collection("users")
                            .document(userId!!)
                            .collection("ingredients")
                            .document(entry.key)
                        // If a whole cake is 8 slices, this is the partial deduction
                        batch.update(
                            ingredientRef, "quantity",
                            com.google.firebase.firestore.FieldValue.increment(
                                -entry.value * (soldCake.sliceQty / 8.0)
                            )
                        )
                    }
                }
            }
            // Add sale record to Firestore
            val saleRef = db.collection("users")
                .document(userId!!)
                .collection("sales")
                .document(sale.id)
            batch.set(saleRef, sale)
            // Commit all updates as one atomic batch
            batch.commit().await()
            Log.d(TAG, "Added sale: id=${sale.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding sale: ${e.message}", e)
            throw e
        }
    }


    suspend fun getSales(): List<Sale> {
        if (userId == null) {
            Log.e(TAG, "Cannot fetch sales: User not authenticated")
            return emptyList()
        }
        return try {
            val snapshot = db.collection("users")
                .document(userId!!)
                .collection("sales")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Sale::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sales: ${e.message}", e)
            emptyList()
        }
    }
}