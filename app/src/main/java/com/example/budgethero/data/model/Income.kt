package com.example.budgethero.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "income_entries")
data class Income(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val amount: Double,
    val description: String,
    val category: String = "General",
    val imageUri: String? = null
)
