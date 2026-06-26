package com.example.budgethero.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "line_items")
data class LineItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val amount: Double,
    val description: String,
    val category: String = "General",
    val imageUri: String? = null,
    val isBudgetItem: Boolean = false
)
