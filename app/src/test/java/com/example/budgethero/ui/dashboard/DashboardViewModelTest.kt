package com.example.budgethero.ui.dashboard

import com.example.budgethero.data.model.Bill
import com.example.budgethero.data.model.BillFrequency
import com.example.budgethero.data.model.Income
import com.example.budgethero.data.model.LineItem
import com.example.budgethero.data.model.WorkDay
import com.example.budgethero.data.repository.BudgetRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val repository: BudgetRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private val deviceZone = ZoneId.systemDefault()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `calculateDashboardState correctly handles surplus income`() = runTest {
        val now = LocalDate.now(deviceZone)
        
        // $1200 monthly income
        val income = listOf(Income(date = now, amount = 1200.0, description = "Salary"))
        
        // $1000 monthly bill (the Target)
        val bills = listOf(Bill(name = "Rent", amount = 1000.0, frequency = BillFrequency.MONTHLY))
        
        every { repository.getAllBills() } returns flowOf(bills)
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(income)

        val viewModel = DashboardViewModel(repository)
        viewModel.setTimeScale(TimeScale.MONTHLY)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.dashboardState.value
        
        // Target should be 1000
        assertEquals(1000.0, state.targetAmount, 0.01)
        assertEquals(1200.0, state.earnedIncome, 0.01)
        
        // Remaining should be -200 (Surplus)
        assertEquals(-200.0, state.remainingAmount, 0.01)
    }

    @Test
    fun `onTrackAmount calculation based on working hours in PST`() = runTest {
        val now = LocalDate.now(deviceZone)
        val startOfMonth = now.withDayOfMonth(1)
        
        // $600 monthly bill
        val bills = listOf(Bill(name = "Bill", amount = 600.0, frequency = BillFrequency.MONTHLY))
        
        every { repository.getAllBills() } returns flowOf(bills)
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(repository)
        viewModel.setTimeScale(TimeScale.MONTHLY)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.dashboardState.value
        
        // If we are at the start of the month, onTrack should be small or 0
        // If we are at the end, it should be near 600
        assertTrue(state.onTrackAmount >= 0.0)
        assertTrue(state.onTrackAmount <= 600.0)
    }

    @Test
    fun `formattedWorkTime excludes specific units per timescale`() = runTest {
        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(repository)
        
        // Yearly: Skip Weeks
        viewModel.setTimeScale(TimeScale.YEARLY)
        testDispatcher.scheduler.advanceUntilIdle()
        val yearlyTime = viewModel.dashboardState.value.formattedWorkTime
        if (yearlyTime != "0h") {
            assertTrue("Yearly should not have 'w', but was: $yearlyTime", !yearlyTime.contains("w"))
        }

        // Monthly: Skip Days
        viewModel.setTimeScale(TimeScale.MONTHLY)
        testDispatcher.scheduler.advanceUntilIdle()
        val monthlyTime = viewModel.dashboardState.value.formattedWorkTime
        if (monthlyTime != "0h") {
            assertTrue("Monthly should not have 'd', but was: $monthlyTime", !monthlyTime.contains("d"))
        }

        // Weekly: Skip Hours
        viewModel.setTimeScale(TimeScale.WEEKLY)
        testDispatcher.scheduler.advanceUntilIdle()
        val weeklyTime = viewModel.dashboardState.value.formattedWorkTime
        if (weeklyTime != "0h") {
            assertTrue("Weekly should not have 'h', but was: $weeklyTime", !weeklyTime.contains("h"))
        }
    }

    @Test
    fun `period navigation updates correctly for different timescales`() = runTest {
        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Test Weekly
        viewModel.setTimeScale(TimeScale.WEEKLY)
        testDispatcher.scheduler.advanceUntilIdle()
        val initialWeekStart = viewModel.weeklyDailyData.value.first().date
        viewModel.decrementPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        val newWeekStart = viewModel.weeklyDailyData.value.first().date
        assertEquals("New start should be 1 week before", initialWeekStart.minusWeeks(1), newWeekStart)
        
        // Test Monthly
        viewModel.setTimeScale(TimeScale.MONTHLY)
        testDispatcher.scheduler.advanceUntilIdle()
        val initialMonthRef = LocalDate.now(deviceZone)
        viewModel.decrementPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        val newMonthStart = viewModel.weeklyDailyData.value.first().date
        // The new start should be the Monday of the week containing (now - 1 month)
        val expectedRef = initialMonthRef.minusMonths(1)
        val expectedStart = expectedRef.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        assertEquals(expectedStart, newMonthStart)

        // Test Daily
        viewModel.setTimeScale(TimeScale.DAILY)
        testDispatcher.scheduler.advanceUntilIdle()
        val initialDayRef = LocalDate.now(deviceZone)
        viewModel.decrementPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        val newDayStart = viewModel.weeklyDailyData.value.first().date
        val expectedDayStart = initialDayRef.minusDays(1).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        assertEquals(expectedDayStart, newDayStart)

        // Test Yearly
        viewModel.setTimeScale(TimeScale.YEARLY)
        testDispatcher.scheduler.advanceUntilIdle()
        val initialYearRef = LocalDate.now(deviceZone)
        viewModel.decrementPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        val newYearStart = viewModel.weeklyDailyData.value.first().date
        val expectedYearStart = initialYearRef.minusYears(1).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        assertEquals(expectedYearStart, newYearStart)
    }

    @Test
    fun `timeScale cycling works correctly`() = runTest {
        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(repository)
        assertEquals(TimeScale.DAILY, viewModel.timeScale.value)
        
        viewModel.nextTimeScale()
        assertEquals(TimeScale.WEEKLY, viewModel.timeScale.value)
        
        viewModel.nextTimeScale()
        assertEquals(TimeScale.MONTHLY, viewModel.timeScale.value)
        
        viewModel.nextTimeScale()
        assertEquals(TimeScale.YEARLY, viewModel.timeScale.value)
        
        viewModel.nextTimeScale()
        assertEquals(TimeScale.DAILY, viewModel.timeScale.value)
    }
}
