package com.example.budgethero.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.budgethero.data.local.dao.BillDao
import com.example.budgethero.data.local.dao.IncomeDao
import com.example.budgethero.data.local.dao.LineItemDao
import com.example.budgethero.data.local.dao.WorkDayDao
import com.example.budgethero.data.model.*

@Database(entities = [Bill::class, WorkDay::class, LineItem::class, Income::class], version = 13, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun workDayDao(): WorkDayDao
    abstract fun lineItemDao(): LineItemDao
    abstract fun incomeDao(): IncomeDao
}
