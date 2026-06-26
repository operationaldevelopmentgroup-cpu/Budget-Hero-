package com.example.budgethero.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class BillFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val frequency: BillFrequency,
    val category: String = "General",
    val endDate: LocalDate? = null
)
