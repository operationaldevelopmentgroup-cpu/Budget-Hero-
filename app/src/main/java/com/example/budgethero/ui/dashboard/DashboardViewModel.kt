package com.example.budgethero.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgethero.data.model.Bill
import com.example.budgethero.data.model.BillFrequency
import com.example.budgethero.data.model.Income
import com.example.budgethero.data.model.LineItem
import com.example.budgethero.data.model.WorkDay
import com.example.budgethero.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

enum class TimeScale {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val deviceZone = ZoneId.systemDefault()

    private val _timeScale = MutableStateFlow(TimeScale.DAILY)
    val timeScale: StateFlow<TimeScale> = _timeScale

    private val _currentTimeOffset = MutableStateFlow(0L)
    val currentTimeOffset: StateFlow<Long> = _currentTimeOffset

    val bills = repository.getAllBills().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val workDays = repository.getAllWorkDays().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val lineItems = repository.getAllLineItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val incomeEntries = repository.getAllIncome().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val weeklyDailyData: StateFlow<List<DailyMetrics>> = combine(
        incomeEntries, 
        lineItems, 
        bills,
        workDays,
        _timeScale, 
        _currentTimeOffset
    ) { flows: Array<Any> ->
        val income = flows[0] as List<Income>
        val spending = flows[1] as List<LineItem>
        val b = flows[2] as List<Bill>
        val wd = flows[3] as List<WorkDay>
        val scale = flows[4] as TimeScale
        val offset = flows[5] as Long

        val now = LocalDate.now(deviceZone)
        val referenceDate = when (scale) {
            TimeScale.DAILY -> now.plusDays(offset)
            TimeScale.WEEKLY -> now.plusWeeks(offset)
            TimeScale.MONTHLY -> now.plusMonths(offset)
            TimeScale.YEARLY -> now.plusYears(offset)
        }
        
        val startOfWeek = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        
        (0..6).map { daysToAdd ->
            val date = startOfWeek.plusDays(daysToAdd.toLong())
            val dayIncome = income.filter { it.date == date }.sumOf { it.amount }
            val dayBudgetSpending = spending.filter { it.date == date && it.isBudgetItem }.sumOf { it.amount }
            val dayNonBudgetSpending = spending.filter { it.date == date && !it.isBudgetItem }.sumOf { it.amount }
            
            // Calculate daily metrics relative to the current real "now"
            val dayState = DashboardStateUtils.calculateDashboardStateWithReference(
                bills = b,
                workDays = wd,
                lineItems = spending,
                incomeEntries = income,
                timeScale = TimeScale.DAILY,
                now = date,
                nowTime = LocalTime.MAX,
                referenceNow = now
            )
            
            val effectiveOnTrackAmount = if (date.isBefore(now)) {
                DashboardStateUtils.calculateDashboardStateWithReference(
                    bills = b,
                    workDays = wd,
                    lineItems = spending,
                    incomeEntries = income,
                    timeScale = TimeScale.DAILY,
                    now = date,
                    nowTime = LocalTime.MAX,
                    referenceNow = date // Treat this date as 'now' to get full target for that day
                ).onTrackAmount
            } else {
                dayState.onTrackAmount
            }

            DailyMetrics(
                date = date,
                income = dayIncome,
                budgetSpending = dayBudgetSpending,
                nonBudgetSpending = dayNonBudgetSpending,
                delta = (dayIncome - dayNonBudgetSpending) - effectiveOnTrackAmount
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = run {
            val now = LocalDate.now()
            val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (0..6).map { DailyMetrics(startOfWeek.plusDays(it.toLong()), 0.0, 0.0, 0.0, 0.0) }
        }
    )

    val dashboardState = combine(
        bills,
        workDays,
        lineItems,
        incomeEntries,
        _timeScale,
        _currentTimeOffset
    ) { flows: Array<Any> ->
        val b = flows[0] as List<Bill>
        val wd = flows[1] as List<WorkDay>
        val li = flows[2] as List<LineItem>
        val inc = flows[3] as List<Income>
        val ts = flows[4] as TimeScale
        val offset = flows[5] as Long

        val now = LocalDate.now(deviceZone)
        val referenceDate = when (ts) {
            TimeScale.DAILY -> now.plusDays(offset)
            TimeScale.WEEKLY -> now.plusWeeks(offset)
            TimeScale.MONTHLY -> now.plusMonths(offset)
            TimeScale.YEARLY -> now.plusYears(offset)
        }
        DashboardStateUtils.calculateDashboardStateWithReference(b, wd, li, inc, ts, now = referenceDate, nowTime = LocalTime.now(deviceZone), referenceNow = now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DashboardState()
    )

    fun setTimeScale(scale: TimeScale) {
        _timeScale.value = scale
        _currentTimeOffset.value = 0 // Reset offset when changing scale
    }

    fun nextTimeScale() {
        val current = _timeScale.value
        val entries = TimeScale.entries
        val nextIndex = (current.ordinal + 1) % entries.size
        setTimeScale(entries[nextIndex])
    }

    fun incrementPeriod() {
        _currentTimeOffset.value += 1
    }

    fun decrementPeriod() {
        _currentTimeOffset.value -= 1
    }

    // Used by tests via reflection
    private fun calculateDashboardState(
        bills: List<Bill>,
        workDays: List<WorkDay>,
        lineItems: List<LineItem>,
        incomeEntries: List<Income>,
        timeScale: TimeScale,
        now: LocalDate,
        nowTime: LocalTime
    ): DashboardState {
        // For tests that use time travel, we treat 'now' as the reference point for pro-rating logic
        return DashboardStateUtils.calculateDashboardStateWithReference(bills, workDays, lineItems, incomeEntries, timeScale, now, nowTime, referenceNow = now)
    }

    fun addBill(name: String, amount: Double, frequency: BillFrequency, endDate: LocalDate? = null) {
        viewModelScope.launch {
            repository.insertBill(Bill(name = name, amount = amount, frequency = frequency, endDate = endDate))
        }
    }

    fun addLineItem(description: String, amount: Double, date: LocalDate? = null, imageUri: String? = null, isBudgetItem: Boolean = false) {
        viewModelScope.launch {
            repository.insertLineItem(
                LineItem(
                    date = date ?: LocalDate.now(deviceZone),
                    amount = amount,
                    description = description,
                    imageUri = imageUri,
                    isBudgetItem = isBudgetItem
                )
            )
        }
    }

    fun updateBill(bill: Bill) {
        viewModelScope.launch {
            repository.insertBill(bill)
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    fun addIncome(amount: Double, description: String, date: LocalDate? = null, imageUri: String? = null) {
        viewModelScope.launch {
            repository.insertIncome(
                Income(
                    date = date ?: LocalDate.now(deviceZone),
                    amount = amount,
                    description = description,
                    imageUri = imageUri
                )
            )
        }
    }

    fun getExportData(): ExportUtils.ExportData {
        val state = dashboardState.value
        val now = LocalDate.now(deviceZone)
        val daysInMonth = now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()).dayOfMonth
        
        return ExportUtils.ExportData(
            bills = bills.value,
            income = incomeEntries.value,
            lineItems = lineItems.value,
            dailyGoal = state.dailyGoalAverage,
            totalMonthlyBilling = state.totalBilling,
            daysInMonth = daysInMonth,
            referenceDate = now
        )
    }
}

data class DailyMetrics(
    val date: LocalDate,
    val income: Double,
    val budgetSpending: Double,
    val nonBudgetSpending: Double,
    val delta: Double
)

data class DashboardState(
    val totalBilling: Double = 0.0,
    val billingFulfilled: Double = 0.0,
    val targetAmount: Double = 0.0,
    val earnedIncome: Double = 0.0,
    val notebookSpentAmount: Double = 0.0,
    val budgetSpentAmount: Double = 0.0,
    val timeScale: TimeScale = TimeScale.DAILY,
    val remainingWorkHours: Double = 0.0,
    val formattedWorkTime: String = "",
    val onTrackAmount: Double = 0.0,
    val weeklyDelta: Double = 0.0,
    val monthlyDelta: Double = 0.0,
    val cumulativeDelta: Double = 0.0,
    val dailyGoalAverage: Double = 0.0,
    val periodStart: LocalDate = LocalDate.now(),
    val periodEnd: LocalDate = LocalDate.now()
) {
    val totalSpending: Double get() = budgetSpentAmount + notebookSpentAmount
    val netEarned: Double get() = earnedIncome - notebookSpentAmount
    val totalDelta: Double get() = earnedIncome - (onTrackAmount + notebookSpentAmount)
    val remainingAmount: Double get() = targetAmount - netEarned
    val remainingBilling: Double get() = (totalBilling - billingFulfilled).coerceAtLeast(0.0)
    val progress: Float get() = when {
        onTrackAmount > 0 -> (netEarned / onTrackAmount).toFloat()
        targetAmount > 0 -> (netEarned / targetAmount).toFloat()
        else -> 0f
    }
}
