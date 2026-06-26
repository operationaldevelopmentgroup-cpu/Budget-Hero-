package com.example.budgethero.ui.notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgethero.data.model.LineItem
import com.example.budgethero.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class NotebookViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val deviceZone = ZoneId.systemDefault()

    val lineItems: StateFlow<List<LineItem>> = repository.getAllLineItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addLineItem(
        description: String,
        amount: Double,
        date: LocalDate? = null,
        imageUri: String? = null,
        isBudgetItem: Boolean = false
    ) {
        viewModelScope.launch {
            repository.insertLineItem(
                LineItem(
                    date = date ?: LocalDate.now(deviceZone),
                    description = description,
                    amount = amount,
                    imageUri = imageUri,
                    isBudgetItem = isBudgetItem
                )
            )
        }
    }

    fun updateLineItem(lineItem: LineItem) {
        viewModelScope.launch {
            repository.insertLineItem(lineItem)
        }
    }

    fun deleteLineItem(lineItem: LineItem) {
        viewModelScope.launch {
            repository.deleteLineItem(lineItem)
        }
    }
}
