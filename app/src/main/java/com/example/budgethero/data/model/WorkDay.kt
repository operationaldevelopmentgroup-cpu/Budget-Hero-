package com.example.budgethero.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "work_days")
data class WorkDay(
    @PrimaryKey
    val date: LocalDate,
    val isWorking: Boolean
)
