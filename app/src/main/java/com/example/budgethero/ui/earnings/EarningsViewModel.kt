package com.example.budgethero.ui.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgethero.data.model.Income
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
class EarningsViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val deviceZone = ZoneId.systemDefault()

    val incomeItems: StateFlow<List<Income>> = repository.getAllIncome().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addIncome(description: String, amount: Double, date: LocalDate? = null, imageUri: String? = null) {
        viewModelScope.launch {
            repository.insertIncome(
                Income(
                    date = date ?: LocalDate.now(deviceZone),
                    description = description,
                    amount = amount,
                    imageUri = imageUri
                )
            )
        }
    }

    fun updateIncome(income: Income) {
        viewModelScope.launch {
            repository.insertIncome(income)
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            repository.deleteIncome(income)
        }
    }
}
