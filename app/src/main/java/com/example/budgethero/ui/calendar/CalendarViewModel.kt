package com.example.budgethero.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgethero.data.model.WorkDay
import com.example.budgethero.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class WorkDayPreset {
    WEEKDAYS, WEEKENDS, ALL_DAYS, SUNDAYS_OFF, SATURDAYS_OFF
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    val workDays: StateFlow<List<WorkDay>> = repository.getAllWorkDays().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleWorkDay(date: LocalDate) {
        viewModelScope.launch {
            val existing = repository.getWorkDay(date)
            val updated = WorkDay(date, !(existing?.isWorking ?: true))
            repository.insertWorkDay(updated)
        }
    }

    fun setWorkDayPreset(preset: WorkDayPreset) {
        viewModelScope.launch {
            val currentMonth = YearMonth.now()
            val daysInMonth = (1..currentMonth.lengthOfMonth()).map { currentMonth.atDay(it) }
            
            val workDaysToInsert = daysInMonth.map { date ->
                val isWorking = when (preset) {
                    WorkDayPreset.WEEKDAYS -> date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
                    WorkDayPreset.WEEKENDS -> date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                    WorkDayPreset.ALL_DAYS -> true
                    WorkDayPreset.SUNDAYS_OFF -> date.dayOfWeek != DayOfWeek.SUNDAY
                    WorkDayPreset.SATURDAYS_OFF -> date.dayOfWeek != DayOfWeek.SATURDAY
                }
                WorkDay(date, isWorking)
            }
            repository.insertWorkDays(workDaysToInsert)
        }
    }
}
