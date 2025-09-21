package com.example.celestial.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.celestial.data.models.Sale
import com.example.celestial.data.repositories.SalesRepository
import kotlinx.coroutines.launch

class SalesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SalesRepository(application.applicationContext)
    private val _sales = MutableLiveData<List<Sale>>(emptyList())
    val sales: LiveData<List<Sale>> = _sales
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadSales()
    }

    fun startSalesListener() {
        repo.listenSalesChanges { list ->
            _sales.postValue(list)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.removeSalesListener()
    }

    private fun loadSales() {
        viewModelScope.launch {
            try {
                _sales.value = repo.getSales()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load sales: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}