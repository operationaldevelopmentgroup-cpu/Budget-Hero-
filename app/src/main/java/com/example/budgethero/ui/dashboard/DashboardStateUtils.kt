package com.example.budgethero.ui.dashboard

import com.example.budgethero.data.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DashboardStateUtils {

    data class Quad<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    fun formatWorkTime(totalHours: Double, timeScale: TimeScale, avgWorkDaysPerWeek: Double, avgWorkDaysPerMonth: Double): String {
        if (timeScale == TimeScale.DAILY) {
            return String.format(Locale.US, "%.1fh", totalHours)
        }

        val hoursPerDay = 8.0
        val effectiveDaysPerMonth = if (avgWorkDaysPerMonth > 0) avgWorkDaysPerMonth else 21.75
        val effectiveDaysPerWeek = if (avgWorkDaysPerWeek > 0) avgWorkDaysPerWeek else 5.0
        val epsilon = 0.05

        var rem = totalHours + epsilon
        val months = (rem / (effectiveDaysPerMonth * hoursPerDay)).toInt()
        rem -= months * (effectiveDaysPerMonth * hoursPerDay)
        
        val weeks = (rem / (effectiveDaysPerWeek * hoursPerDay)).toInt()
        rem -= weeks * (effectiveDaysPerWeek * hoursPerDay)
        
        val days = (rem / hoursPerDay).toInt()
        val hours = rem % hoursPerDay

        return buildString {
            if (months > 0) append("${months}m")
            
            if (weeks > 0 && timeScale != TimeScale.YEARLY) {
                if (isNotEmpty()) append(" ")
                append("${weeks}w")
            }
            
            if (days > 0 && timeScale != TimeScale.MONTHLY) {
                if (isNotEmpty()) append(" ")
                append("${days}d")
            }
            
            if (hours > epsilon && (timeScale == TimeScale.DAILY || (months == 0 && (weeks == 0 || timeScale == TimeScale.YEARLY) && (days == 0 || timeScale == TimeScale.MONTHLY)))) {
                if (isNotEmpty()) append(" ")
                append(String.format(Locale.US, "%.1fh", hours))
            }
        }.ifEmpty { "0h" }
    }

    fun calculateDashboardState(
        bills: List<Bill>,
        workDays: List<WorkDay>,
        lineItems: List<LineItem>,
        incomeEntries: List<Income>,
        timeScale: TimeScale,
        now: LocalDate = LocalDate.now(),
        nowTime: LocalTime = LocalTime.now()
    ): DashboardState {
        // Legacy entry point for external callers (and some tests)
        return calculateDashboardStateWithReference(
            bills, workDays, lineItems, incomeEntries, timeScale, now, nowTime, now
        )
    }

    internal fun calculateDashboardStateWithReference(
        bills: List<Bill>,
        workDays: List<WorkDay>,
        lineItems: List<LineItem>,
        incomeEntries: List<Income>,
        timeScale: TimeScale,
        now: LocalDate,
        nowTime: LocalTime,
        referenceNow: LocalDate
    ): DashboardState {
        val firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
        val daysInMonth = lastDayOfMonth.dayOfMonth

        fun countWorkDaysInRange(start: LocalDate, end: LocalDate): Int {
            val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
            return (0 until totalDays).count { offset ->
                val date = start.plusDays(offset.toLong())
                val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                val workDayOverride = workDays.find { it.date == date }
                workDayOverride?.isWorking ?: !isWeekend
            }
        }

        val workingDaysInMonth = countWorkDaysInRange(firstDayOfMonth, lastDayOfMonth)
        // Centralized logic: Target for a single day based on billing sum smoothed over working days
        fun calculateDayTarget(date: LocalDate): Double {
            val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            val workDayOverride = workDays.find { it.date == date }
            val isWorking = workDayOverride?.isWorking ?: !isWeekend
            if (!isWorking) return 0.0

            val monthStart = date.with(TemporalAdjusters.firstDayOfMonth())
            val monthEnd = date.with(TemporalAdjusters.lastDayOfMonth())
            val workingDaysInThisMonth = countWorkDaysInRange(monthStart, monthEnd)
            if (workingDaysInThisMonth == 0) return 0.0

            // Use bills that were active on the specific date being calculated
            val activeBillsAtDate = bills.filter { it.endDate == null || !it.endDate.isBefore(date) }
            val monthTarget = activeBillsAtDate.sumOf { bill ->
                when (bill.frequency) {
                    BillFrequency.DAILY -> bill.amount * monthEnd.dayOfMonth
                    BillFrequency.WEEKLY -> bill.amount * (monthEnd.dayOfMonth / 7.0)
                    BillFrequency.MONTHLY -> bill.amount
                    BillFrequency.YEARLY -> bill.amount / 12.0
                }
            }
            return monthTarget / workingDaysInThisMonth
        }

        fun calculatePeriodTarget(pStart: LocalDate, pEnd: LocalDate): Double {
            var total = 0.0
            var curr = pStart
            while (!curr.isAfter(pEnd)) {
                total += calculateDayTarget(curr)
                curr = curr.plusDays(1)
            }
            return total
        }

        val totalMonthlyBilling = calculatePeriodTarget(firstDayOfMonth, lastDayOfMonth)

        /**
         * Returns (Earned, NotebookSpent, BudgetSpent, ProratedTarget)
         */
        fun calculatePeriodMetrics(
            pStart: LocalDate,
            pEnd: LocalDate,
            pNow: LocalDate,
            pNowTime: LocalTime,
            pScale: TimeScale? = null,
            realNow: LocalDate = referenceNow
        ): Quad<Double, Double, Double, Double> {
            val totalTarget = calculatePeriodTarget(pStart, pEnd)

            // If this whole period is in the absolute past relative to real life, show full totals.
            if (pEnd.isBefore(realNow)) {
                val earned = incomeEntries.filter { !it.date.isBefore(pStart) && !it.date.isAfter(pEnd) }.sumOf { it.amount }
                val notebookSpent = lineItems.filter { !it.isBudgetItem && !it.date.isBefore(pStart) && !it.date.isAfter(pEnd) }.sumOf { it.amount }
                val budgetSpent = lineItems.filter { it.isBudgetItem && !it.date.isBefore(pStart) && !it.date.isAfter(pEnd) }.sumOf { it.amount }
                return Quad(earned, notebookSpent, budgetSpent, totalTarget)
            }

            // If the traveler is beyond the end of this period, show full totals for THIS period relative to pNow.
            if (pNow.isAfter(pEnd)) {
                val earned = incomeEntries.filter { !it.date.isBefore(pStart) && !it.date.isAfter(pEnd) }.sumOf { it.amount }
                val notebookSpent = lineItems.filter { !it.isBudgetItem && !it.date.isBefore(pStart) && !it.date.isAfter(pEnd) }.sumOf { it.amount }
                val budgetSpent = lineItems.filter { it.isBudgetItem && !it.date.isBefore(pStart) && !it.date.isAfter(pEnd) }.sumOf { it.amount }
                return Quad(earned, notebookSpent, budgetSpent, totalTarget)
            }
            
            // If the traveler hasn't reached this period yet, show zeros.
            if (pNow.isBefore(pStart)) {
                return Quad(0.0, 0.0, 0.0, 0.0)
            }

            // Otherwise, it's the current period relative to the traveler -> use pro-rating logic based on pNow.
            // 1. Full days passed
            var proratedTarget = 0.0
            var curr = pStart
            while (curr.isBefore(pNow)) {
                proratedTarget += calculateDayTarget(curr)
                curr = curr.plusDays(1)
            }
            
            // 2. Pro-rate today's target if it's a work day
            val todayTarget = calculateDayTarget(pNow)
            if (todayTarget > 0) {
                val hoursPerDay = if (pScale == TimeScale.DAILY) 24.0 else 8.0
                val h = pNowTime.hour
                val m = pNowTime.minute
                
                val todayHoursPassed = if (pScale == TimeScale.DAILY) {
                    h.toDouble() + m.toDouble() / 60.0
                } else {
                    // Standard 9 AM - 5 PM work day
                    when { 
                        h < 9 -> 0.0 
                        h >= 17 -> 8.0 
                        else -> (h - 9).toDouble() + m.toDouble() / 60.0
                    }
                }
                proratedTarget += todayTarget * (todayHoursPassed / hoursPerDay)
            }

            val earned = incomeEntries.filter { !it.date.isBefore(pStart) && !it.date.isAfter(pNow) }.sumOf { it.amount }
            val notebookSpent = lineItems.filter { !it.isBudgetItem && !it.date.isBefore(pStart) && !it.date.isAfter(pNow) }.sumOf { it.amount }
            val budgetSpent = lineItems.filter { it.isBudgetItem && !it.date.isBefore(pStart) && !it.date.isAfter(pNow) }.sumOf { it.amount }
            return Quad(earned, notebookSpent, budgetSpent, proratedTarget)
        }


        val periodStart: LocalDate
        val periodEnd: LocalDate
        when (timeScale) {
            TimeScale.DAILY -> { periodStart = now; periodEnd = now }
            TimeScale.WEEKLY -> { periodStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); periodEnd = periodStart.plusDays(6) }
            TimeScale.MONTHLY -> { periodStart = firstDayOfMonth; periodEnd = lastDayOfMonth }
            TimeScale.YEARLY -> { periodStart = now.with(TemporalAdjusters.firstDayOfYear()); periodEnd = now.with(TemporalAdjusters.lastDayOfYear()) }
        }

        val workingDaysInPeriod = countWorkDaysInRange(periodStart, periodEnd)
        val targetBilling = calculatePeriodTarget(periodStart, periodEnd)

        val (earnedIncome, notebookSpending, budgetSpending, onTrackAmount) = calculatePeriodMetrics(periodStart, periodEnd, now, nowTime, timeScale)
        
        val hoursPerDay = if (timeScale == TimeScale.DAILY) 24.0 else 8.0
        val totalWorkingHours = workingDaysInPeriod * hoursPerDay
        
        val remainingWorkHours = when {
            periodEnd.isBefore(referenceNow) -> 0.0
            periodStart.isAfter(referenceNow) -> totalWorkingHours
            else -> {
                val fullWorkDaysPassed = if (ChronoUnit.DAYS.between(periodStart, now) > 0) countWorkDaysInRange(periodStart, now.minusDays(1)) else 0
                val todayIsWorkDay = run {
                    val isWeekend = now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY
                    val workDayOverride = workDays.find { it.date == now }
                    workDayOverride?.isWorking ?: !isWeekend
                }
                val todayWorkHoursPassed = if (todayIsWorkDay) {
                    if (timeScale == TimeScale.DAILY) nowTime.hour.toDouble() + nowTime.minute.toDouble() / 60.0
                    else {
                        val currentHour = nowTime.hour
                        when { currentHour < 9 -> 0.0; currentHour >= 17 -> 8.0; else -> (currentHour - 9).toDouble() }
                    }
                } else 0.0
                val workingHoursPassed = fullWorkDaysPassed * hoursPerDay + todayWorkHoursPassed
                (totalWorkingHours - workingHoursPassed).coerceAtLeast(0.0)
            }
        }

        val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val (wEarned, wSpent, wBSpent, wTarget) = calculatePeriodMetrics(weekStart, weekStart.plusDays(6), now, nowTime, TimeScale.WEEKLY)
        val weeklyDelta = (wEarned - wSpent) - wTarget
        
        val (mEarned, mSpent, mBSpent, mTarget) = calculatePeriodMetrics(firstDayOfMonth, lastDayOfMonth, now, nowTime, TimeScale.MONTHLY)
        val monthlyDelta = (mEarned - mSpent) - mTarget
        
        val ytdStart = now.with(TemporalAdjusters.firstDayOfYear())
        val (yEarned, ySpent, yBSpent, yTarget) = calculatePeriodMetrics(ytdStart, now.with(TemporalAdjusters.lastDayOfYear()), now, nowTime, TimeScale.YEARLY)
        val cumulativeDelta = (yEarned - ySpent) - yTarget

        val billingFulfilled = lineItems.filter { it.isBudgetItem && !it.date.isBefore(periodStart) && !it.date.isAfter(periodEnd) }.sumOf { it.amount }
        
        // Total monthly billing for the month containing 'now'
        val totalMonthlyBillingForMonthOfNow = calculatePeriodTarget(firstDayOfMonth, lastDayOfMonth)
        val dailyGoalAverage = if (workingDaysInMonth > 0) totalMonthlyBillingForMonthOfNow / workingDaysInMonth else totalMonthlyBillingForMonthOfNow / daysInMonth

        val workingDaysInCurrentMonth = workingDaysInMonth.toDouble()
        val avgWorkDaysPerMonth = if (timeScale == TimeScale.YEARLY) {
            val totalWorkingDaysInYear = countWorkDaysInRange(now.with(TemporalAdjusters.firstDayOfYear()), now.with(TemporalAdjusters.lastDayOfYear()))
            totalWorkingDaysInYear / 12.0
        } else workingDaysInCurrentMonth

        val avgWorkDaysPerWeek = avgWorkDaysPerMonth / (if (timeScale == TimeScale.YEARLY) 4.345 else (daysInMonth / 7.0))
        
        return DashboardState(
            totalBilling = totalMonthlyBilling,
            billingFulfilled = billingFulfilled,
            targetAmount = targetBilling,
            earnedIncome = earnedIncome,
            notebookSpentAmount = notebookSpending,
            budgetSpentAmount = budgetSpending,
            timeScale = timeScale,
            remainingWorkHours = remainingWorkHours,
            formattedWorkTime = formatWorkTime(remainingWorkHours, timeScale, avgWorkDaysPerWeek, avgWorkDaysPerMonth),
            onTrackAmount = onTrackAmount,
            weeklyDelta = weeklyDelta,
            monthlyDelta = monthlyDelta,
            cumulativeDelta = cumulativeDelta,
            dailyGoalAverage = dailyGoalAverage,
            periodStart = periodStart,
            periodEnd = periodEnd
        )
    }
}
