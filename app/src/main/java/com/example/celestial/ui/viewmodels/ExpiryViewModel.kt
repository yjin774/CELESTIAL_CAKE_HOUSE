package com.example.celestial.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.celestial.data.models.Ingredient
import com.example.celestial.data.models.Stock
import com.example.celestial.data.repositories.ExpiryRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExpiryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ExpiryRepository(application.applicationContext)  // Pass context here
    private val _expiringIngredients = MutableLiveData<List<Ingredient>>()
    val expiringIngredients: LiveData<List<Ingredient>> = _expiringIngredients

    data class ExpiringStockInfo(
        val ingredientName: String,
        val stock: Stock
    )


    private val _expiringStocks = MutableLiveData<List<ExpiringStockInfo>>(emptyList())
    val expiringStocks: LiveData<List<ExpiringStockInfo>> = _expiringStocks

    fun computeExpiringStocks(ingredients: List<Ingredient>, stocksByIngredient: Map<String, List<Stock>>) {
        val today = LocalDate.now()
        val threshold = today.plusDays(7)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val result = mutableListOf<ExpiringStockInfo>()
        for (ingredient in ingredients) {
            val stocks = stocksByIngredient[ingredient.id] ?: continue
            for (stock in stocks) {
                val expiry = stock.expiryDate?.let {
                    try { LocalDate.parse(it, formatter) } catch (e: Exception) { null }
                }
                if (expiry != null &&
                    expiry.isAfter(today.minusDays(1)) &&
                    expiry.isBefore(threshold.plusDays(1))) {
                    result.add(ExpiringStockInfo(ingredient.name, stock))
                }
            }
        }
        _expiringStocks.value = result
    }


    init {
        loadExpiringIngredients()
    }

    private fun loadExpiringIngredients() {
        viewModelScope.launch {
            _expiringIngredients.value = repo.getExpiringIngredients()
        }
    }
}
