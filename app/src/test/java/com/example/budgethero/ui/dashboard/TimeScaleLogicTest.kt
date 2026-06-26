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
import java.time.LocalTime
import java.time.ZoneId
import java.lang.reflect.Method

@OptIn(ExperimentalCoroutinesApi::class)
class TimeScaleLogicTest {

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

    private fun invokeCalculateDashboardState(
        viewModel: DashboardViewModel,
        timeScale: TimeScale,
        now: LocalDate,
        nowTime: LocalTime
    ): DashboardState {
        val method: Method = DashboardViewModel::class.java.getDeclaredMethods().find { it.name == "calculateDashboardState" }!!
        method.isAccessible = true
        // List parameters are: bills, workDays, lineItems, incomeEntries
        return method.invoke(
            viewModel, 
            emptyList<Bill>(), 
            emptyList<WorkDay>(), 
            emptyList<LineItem>(), 
            emptyList<Income>(), 
            timeScale, 
            now, 
            nowTime
        ) as DashboardState
    }

    @Test
    fun `Yearly view in June shows reasonable remaining months`() = runTest {
        val mockNow = LocalDate.of(2026, 6, 15)
        val mockNowTime = LocalTime.of(12, 0)

        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(repository)
        val state = invokeCalculateDashboardState(viewModel, TimeScale.YEARLY, mockNow, mockNowTime)
        val formattedTime = state.formattedWorkTime
        
        // In June 15, we should have roughly 6.5 months remaining of the year.
        // With 2026 logic (261 work days):
        // June 15 is 114th work day. 261 - 114 = 147 work days remaining.
        // 147 / (21.75 * 8) = 6.75 months? No, 147 * 8 = 1176 hours.
        // 1176 / (21.75 * 8) = 147 / 21.75 = 6.75 months.
        assertTrue("Expected months (m) in: $formattedTime", formattedTime.contains("m"))
        assertTrue("Expected ~6m, but got: $formattedTime", formattedTime.startsWith("6m"))
    }

    @Test
    fun `Yearly view on Jan 1st shows full year remaining`() = runTest {
        val mockNow = LocalDate.of(2026, 1, 1)
        val mockNowTime = LocalTime.of(9, 0) // Start of work day

        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(repository)
        val state = invokeCalculateDashboardState(viewModel, TimeScale.YEARLY, mockNow, mockNowTime)
        val formattedTime = state.formattedWorkTime
        
        // Full year remaining: 12 months (or 11m 4w ... depending on exact day count)
        // With 261 work days / 12 = 21.75 avg days/month
        // Jan 1 2026 is Thursday.
        // Total working hours for 2026 (Mon-Fri) = 261 * 8 = 2088.
        // Hours passed on Jan 1 9am = 0.
        // 2088 / (21.75 * 8) = 12.0 months.
        assertTrue("Expected ~12m, but got: $formattedTime", formattedTime.startsWith("12m") || formattedTime.startsWith("11m"))
    }

    @Test
    fun `Daily view uses 24-hour window`() = runTest {
        val mockNow = LocalDate.of(2026, 6, 15) // Monday
        val mockNowTime = LocalTime.of(12, 0) // Noon

        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(repository)
        val state = invokeCalculateDashboardState(viewModel, TimeScale.DAILY, mockNow, mockNowTime)
        val formattedTime = state.formattedWorkTime
        
        // At Noon, 12 hours passed, 12 hours remaining in a 24h window
        assertEquals("12.0h", formattedTime)
    }

    @Test
    fun `Weekly view has day-level precision (ignores today's hours)`() = runTest {
        val mockNow = LocalDate.of(2026, 6, 15) // Monday
        val mockNowTime = LocalTime.of(16, 0) // 4:00 PM

        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(repository)
        val state = invokeCalculateDashboardState(viewModel, TimeScale.WEEKLY, mockNow, mockNowTime)
        val formattedTime = state.formattedWorkTime
        
        // On Monday 4pm, 1h remains today + 4 days (Tue-Fri) = 33h
        // With June 2026 logic (22 work days / month):
        // 33.0 hours = 4d
        
        assertEquals("4d", formattedTime)
    }

    @Test
    fun `Running Delta correctly identifies surplus over target`() = runTest {
        val mockNow = LocalDate.of(2026, 6, 15)
        val mockNowTime = LocalTime.of(12, 0)

        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        // Mock income of 150, target so far would be 100 (if monthly was 300)
        // Wait, calculateDashboardState needs actual bills and income to compute onTrackAmount properly.
        val bills = listOf(Bill(name = "Test", amount = 304.375, frequency = BillFrequency.MONTHLY))
        val income = listOf(Income(date = mockNow, amount = 150.0, description = "Salary"))
        
        val viewModel = DashboardViewModel(repository)
        
        val method: Method = DashboardViewModel::class.java.getDeclaredMethods().find { it.name == "calculateDashboardState" }!!
        method.isAccessible = true
        val state = method.invoke(
            viewModel, 
            bills, 
            emptyList<WorkDay>(), 
            emptyList<LineItem>(), 
            income, 
            TimeScale.MONTHLY, 
            mockNow, 
            mockNowTime
        ) as DashboardState
        
        // June 2026 has 22 working days (Mon-Fri).
        // June 1 to June 15:
        // Week 1: 1, 2, 3, 4, 5 (5)
        // Week 2: 8, 9, 10, 11, 12 (5)
        // Week 3: 15 (1)
        // Total: 11 working days passed.
        // workingHoursPassed = 10 * 8 + (12-9) = 83.
        // earnedIncome = 150.0
        
        assertTrue("Monthly Delta should be positive surplus, but was: ${state.monthlyDelta}", state.monthlyDelta > 0)
    }

    @Test
    fun `Daily Delta tracks weekly progress even in Monthly view`() = runTest {
        val mockNow = LocalDate.of(2026, 6, 15) // Monday
        val mockNowTime = LocalTime.of(12, 0)

        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val bills = listOf(Bill(name = "Weekly Target", amount = 70.0, frequency = BillFrequency.WEEKLY))
        val income = listOf(Income(date = mockNow, amount = 20.0, description = "Part-time"))
        
        val viewModel = DashboardViewModel(repository)
        val method: Method = DashboardViewModel::class.java.getDeclaredMethods().find { it.name == "calculateDashboardState" }!!
        method.isAccessible = true
        val state = method.invoke(viewModel, bills, emptyList<WorkDay>(), emptyList<LineItem>(), income, TimeScale.MONTHLY, mockNow, mockNowTime) as DashboardState
        
        // Monday Noon in Weekly view:
        // June 2026 has 22 working days.
        // Monthly equivalent for $70/week = 70 * (30/7) = 300.0
        // Weekly target for 5-day week = 300 * (5/22) = 68.1818
        // Hours passed: (12-9) = 3 hours
        // Pro-rated target = 68.1818 * (3 / 40) = 5.1136
        // Earned = 20
        // weeklyDelta = 20 - 5.1136 = 14.8864
        
        assertEquals(14.88636, state.weeklyDelta, 0.001)
    }

    @Test
    fun `Modifying work schedule in calendar accurately adjusts Dashboard metrics`() = runTest {
        val mockNow = LocalDate.of(2026, 6, 15) // Monday
        val mockNowTime = LocalTime.of(12, 0) // Noon

        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        val bills = listOf(Bill(name = "Target", amount = 2200.0, frequency = BillFrequency.MONTHLY))
        
        val viewModel = DashboardViewModel(repository)
        val method: Method = DashboardViewModel::class.java.getDeclaredMethods().find { it.name == "calculateDashboardState" }!!
        method.isAccessible = true

        // 1. No overrides
        val state1 = method.invoke(viewModel, bills, emptyList<WorkDay>(), emptyList<LineItem>(), emptyList<Income>(), TimeScale.MONTHLY, mockNow, mockNowTime) as DashboardState
        val onTrack1 = state1.onTrackAmount
        assertTrue("OnTrack should be > 0", onTrack1 > 0)
        
        // 2. Add an "Off" day in the past (June 10)
        val workDays = listOf(WorkDay(date = LocalDate.of(2026, 6, 10), isWorking = false))
        val state2 = method.invoke(viewModel, bills, workDays, emptyList<LineItem>(), emptyList<Income>(), TimeScale.MONTHLY, mockNow, mockNowTime) as DashboardState
        val onTrack2 = state2.onTrackAmount
        // Just verify it's different and reasonable
        assertTrue("OnTrack amount should change when schedule changes, but was same: $onTrack2", onTrack2 != onTrack1)
    }

    @Test
    fun `Daily Average Goal updates when work schedule changes`() = runTest {
        val mockNow = LocalDate.of(2026, 6, 15)
        val mockNowTime = LocalTime.of(12, 0)

        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        // $2200 monthly bill. 
        // June 2026 has 22 working days (Mon-Fri).
        // Expected daily goal: 2200 / 22 = 100.0
        val bills = listOf(Bill(name = "Target", amount = 2200.0, frequency = BillFrequency.MONTHLY))
        
        val viewModel = DashboardViewModel(repository)
        val method: Method = DashboardViewModel::class.java.getDeclaredMethods().find { it.name == "calculateDashboardState" }!!
        method.isAccessible = true

        // 1. No overrides (22 working days)
        val state1 = method.invoke(viewModel, bills, emptyList<WorkDay>(), emptyList<LineItem>(), emptyList<Income>(), TimeScale.MONTHLY, mockNow, mockNowTime) as DashboardState
        assertEquals(100.0, state1.dailyGoalAverage, 0.001)
        
        // 2. Add an "Off" day on a weekday (June 10) -> 21 working days
        // Expected daily goal: 2200 / 21 = 104.76
        val workDays = listOf(WorkDay(date = LocalDate.of(2026, 6, 10), isWorking = false))
        val state2 = method.invoke(viewModel, bills, workDays, emptyList<LineItem>(), emptyList<Income>(), TimeScale.MONTHLY, mockNow, mockNowTime) as DashboardState
        assertEquals(2200.0 / 21, state2.dailyGoalAverage, 0.001)
        
        // 3. Add a "Work" day on a weekend (June 14, Sunday) -> 23 working days (compared to 22)
        // Expected daily goal: 2200 / 23 = 95.65
        val workDays2 = listOf(WorkDay(date = LocalDate.of(2026, 6, 14), isWorking = true))
        val state3 = method.invoke(viewModel, bills, workDays2, emptyList<LineItem>(), emptyList<Income>(), TimeScale.MONTHLY, mockNow, mockNowTime) as DashboardState
        assertEquals(2200.0 / 23, state3.dailyGoalAverage, 0.001)
    }

    @Test
    fun `Target Billing recalibrates correctly for all timescales`() = runTest {
        val mockNow = LocalDate.of(2026, 6, 15)
        val mockNowTime = LocalTime.of(12, 0)

        every { repository.getAllBills() } returns flowOf(emptyList())
        every { repository.getAllWorkDays() } returns flowOf(emptyList())
        every { repository.getAllLineItems() } returns flowOf(emptyList())
        every { repository.getAllIncome() } returns flowOf(emptyList())

        // $2200 monthly bill. June 2026 has 22 working days.
        val bills = listOf(Bill(name = "Target", amount = 2200.0, frequency = BillFrequency.MONTHLY))
        val viewModel = DashboardViewModel(repository)
        val method: Method = DashboardViewModel::class.java.getDeclaredMethods().find { it.name == "calculateDashboardState" }!!
        method.isAccessible = true

        // 1. Monthly - should be $2200
        val stateM = method.invoke(viewModel, bills, emptyList<WorkDay>(), emptyList<LineItem>(), emptyList<Income>(), TimeScale.MONTHLY, mockNow, mockNowTime) as DashboardState
        assertEquals(2200.0, stateM.targetAmount, 0.001)

        // 2. Weekly - June 15-21 week has 5 working days (Mon-Fri)
        // 2200 * (5/22) = 500
        val stateW = method.invoke(viewModel, bills, emptyList<WorkDay>(), emptyList<LineItem>(), emptyList<Income>(), TimeScale.WEEKLY, mockNow, mockNowTime) as DashboardState
        assertEquals(500.0, stateW.targetAmount, 0.001)

        // 3. Daily - June 15 is Monday (working day)
        // 2200 * (1/22) = 100
        val stateD = method.invoke(viewModel, bills, emptyList<WorkDay>(), emptyList<LineItem>(), emptyList<Income>(), TimeScale.DAILY, mockNow, mockNowTime) as DashboardState
        assertEquals(100.0, stateD.targetAmount, 0.001)

        // 4. Daily on Weekend (June 14 Sunday) - should be 0
        val stateD2 = method.invoke(viewModel, bills, emptyList<WorkDay>(), emptyList<LineItem>(), emptyList<Income>(), TimeScale.DAILY, LocalDate.of(2026, 6, 14), mockNowTime) as DashboardState
        assertEquals(0.0, stateD2.targetAmount, 0.001)
        
        // 5. Daily on Weekend MADE WORKING (June 14 Sunday)
        val workDays = listOf(WorkDay(date = LocalDate.of(2026, 6, 14), isWorking = true))
        val stateD3 = method.invoke(viewModel, bills, workDays, emptyList<LineItem>(), emptyList<Income>(), TimeScale.DAILY, LocalDate.of(2026, 6, 14), mockNowTime) as DashboardState
        // New working days in month = 23.
        // Target = 2200 * (1/23) = 95.65
        assertEquals(2200.0 / 23, stateD3.targetAmount, 0.001)
    }
}
