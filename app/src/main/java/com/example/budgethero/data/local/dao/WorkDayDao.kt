package com.example.budgethero.data.local.dao

import androidx.room.*
import com.example.budgethero.data.model.WorkDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WorkDayDao {
    @Query("SELECT * FROM work_days")
    fun getAllWorkDays(): Flow<List<WorkDay>>

    @Query("SELECT * FROM work_days WHERE date = :date")
    suspend fun getWorkDay(date: LocalDate): WorkDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDay(workDay: WorkDay)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDays(workDays: List<WorkDay>)
}
