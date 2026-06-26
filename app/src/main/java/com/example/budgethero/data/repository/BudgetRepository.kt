package com.example.budgethero.data.repository

import com.example.budgethero.data.local.dao.BillDao
import com.example.budgethero.data.local.dao.IncomeDao
import com.example.budgethero.data.local.dao.LineItemDao
import com.example.budgethero.data.local.dao.WorkDayDao
import com.example.budgethero.data.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val billDao: BillDao,
    private val workDayDao: WorkDayDao,
    private val lineItemDao: LineItemDao,
    private val incomeDao: IncomeDao
) {
    fun getAllBills(): Flow<List<Bill>> = billDao.getAllBills()
    
    suspend fun insertBill(bill: Bill) = billDao.insertBill(bill)
    
    suspend fun deleteBill(bill: Bill) = billDao.deleteBill(bill)
    
    fun getAllWorkDays(): Flow<List<WorkDay>> = workDayDao.getAllWorkDays()
    
    suspend fun insertWorkDay(workDay: WorkDay) = workDayDao.insertWorkDay(workDay)
    
    suspend fun insertWorkDays(workDays: List<WorkDay>) = workDayDao.insertWorkDays(workDays)
    
    suspend fun getWorkDay(date: LocalDate) = workDayDao.getWorkDay(date)

    fun getAllLineItems(): Flow<List<LineItem>> = lineItemDao.getAllLineItems()

    fun getLineItemsByDate(date: LocalDate): Flow<List<LineItem>> = lineItemDao.getLineItemsByDate(date)

    suspend fun insertLineItem(lineItem: LineItem) = lineItemDao.insertLineItem(lineItem)

    suspend fun deleteLineItem(lineItem: LineItem) = lineItemDao.deleteLineItem(lineItem)

    fun getAllIncome(): Flow<List<Income>> = incomeDao.getAllIncome()

    suspend fun insertIncome(income: Income) = incomeDao.insertIncome(income)

    suspend fun deleteIncome(income: Income) = incomeDao.deleteIncome(income)
}
