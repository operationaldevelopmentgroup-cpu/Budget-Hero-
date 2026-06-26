package com.example.budgethero.ui.dashboard

import com.example.budgethero.data.model.Bill
import com.example.budgethero.data.model.BillFrequency
import com.example.budgethero.data.model.Income
import com.example.budgethero.data.repository.BudgetRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.lang.reflect.Method
import com.example.budgethero.data.model.LineItem
import com.example.budgethero.data.model.WorkDay

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardHistoricalMetricsTest {

    private val repository: BudgetRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dashboardState shows pro-rated metrics for past weeks when traveler is mid-week`() = runTest {
        val today = LocalDate.of(2026, 6, 24) // Wednesday
        val startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastWeekStart = startOfThisWeek.minusWeeks(1) // June 15
        
        val lastFriday = lastWeekStart.plusDays(4)
        val income = listOf(Income(date = lastFriday, amount = 1000.0, description = "Last week pay"))
        val bills = listOf(Bill(name = "Weekly Bill", amount = 500.0, frequency = BillFrequency.WEEKLY))
        
        every { repository.getAllBills() } returns flowOf(bills)
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(income)

        val viewModel = DashboardViewModel(repository)
        val method: Method = DashboardViewModel::class.java.declaredMethods.find { it.name == "calculateDashboardState" }!!
        method.isAccessible = true
        
        val lastWeekWednesday = today.minusWeeks(1)
        val state = method.invoke(viewModel, bills, emptyList<WorkDay>(), emptyList<LineItem>(), income, TimeScale.WEEKLY, lastWeekWednesday, LocalTime.of(12, 0)) as DashboardState
        
        assertEquals("Earned income should be pro-rated (0) for a traveler mid-past-week", 0.0, state.earnedIncome, 0.01)
        assertEquals("Target amount should be pro-rated (231.33) for a traveler mid-past-week", 231.33, state.onTrackAmount, 0.01)
    }

    @Test
    fun `dashboardState shows full totals for past weeks when traveler is beyond the week`() = runTest {
        val realToday = LocalDate.of(2026, 6, 24) // Wednesday
        val lastWeekStart = LocalDate.of(2026, 6, 15)
        val lastWeekEnd = lastWeekStart.plusDays(6)
        
        val lastFriday = lastWeekStart.plusDays(4)
        val income = listOf(Income(date = lastFriday, amount = 1000.0, description = "Last week pay"))
        val bills = listOf(Bill(name = "Weekly Bill", amount = 700.0, frequency = BillFrequency.WEEKLY))
        
        every { repository.getAllBills() } returns flowOf(bills)
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(income)

        val viewModel = DashboardViewModel(repository)
        
        // Use reflection to find the method on the object DashboardStateUtils
        // Using getMethods() instead of getDeclaredMethods() to find the static-like method on the singleton object
        val calculateWithRef: Method = DashboardStateUtils::class.java.methods.find { 
            it.name.startsWith("calculateDashboardStateWithReference") && it.parameterCount == 8
        }!!
        calculateWithRef.isAccessible = true
        
        val state = calculateWithRef.invoke(
            DashboardStateUtils, 
            bills, emptyList<WorkDay>(), emptyList<LineItem>(), income, 
            TimeScale.WEEKLY, lastWeekStart, LocalTime.of(12, 0), realToday
        ) as DashboardState
        
        assertEquals("Earned income should be FULL (1000) for a past week", 1000.0, state.earnedIncome, 0.01)
        assertEquals("Target amount should be FULL (~681.82) for a past week", 681.82, state.targetAmount, 0.01)
        assertEquals("Weekly delta should be ~318.18 (1000 - 681.82)", 318.18, state.weeklyDelta, 0.01)
    }

    @Test
    fun `weeklyDelta for a past Wednesday should be running delta, not full week surplus`() = runTest {
        val today = LocalDate.of(2026, 6, 24)
        val startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastWeekStart = startOfThisWeek.minusWeeks(1)
        
        val lastFriday = lastWeekStart.plusDays(4)
        val lastWednesday = lastWeekStart.plusDays(2)
        
        val income = listOf(Income(date = lastFriday, amount = 1000.0, description = "Last week Friday pay"))
        val bills = listOf(Bill(name = "Weekly Bill", amount = 700.0, frequency = BillFrequency.WEEKLY))
        
        every { repository.getAllBills() } returns flowOf(bills)
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(income)

        val viewModel = DashboardViewModel(repository)
        val method: Method = DashboardViewModel::class.java.declaredMethods.find { it.name == "calculateDashboardState" }!!
        method.isAccessible = true

        val state = method.invoke(viewModel, bills, emptyList<WorkDay>(), emptyList<LineItem>(), income, TimeScale.DAILY, lastWednesday, LocalTime.of(12, 0)) as DashboardState
        
        assertEquals("Should be running delta for last Wednesday (-323.86)", -323.86, state.weeklyDelta, 0.01)
    }

    @Test
    fun `DAILY view target at noon should be exactly half of day target`() = runTest {
        val today = LocalDate.of(2026, 6, 24) // Wednesday
        val bills = listOf(Bill(name = "Monthly", amount = 2200.0, frequency = BillFrequency.MONTHLY))
        
        every { repository.getAllBills() } returns flowOf(bills)
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(repository)
        val method: Method = DashboardViewModel::class.java.declaredMethods.find { it.name == "calculateDashboardState" }!!
        method.isAccessible = true
        
        val state = method.invoke(viewModel, bills, emptyList<WorkDay>(), emptyList<LineItem>(), emptyList<Income>(), TimeScale.DAILY, today, LocalTime.of(12, 0)) as DashboardState
        
        assertEquals("Target for DAILY view at noon should be 50", 50.0, state.onTrackAmount, 0.01)
    }
}
