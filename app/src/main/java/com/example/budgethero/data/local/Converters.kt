package com.example.budgethero.data.local

import androidx.room.TypeConverter
import com.example.budgethero.data.model.BillFrequency
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromFrequency(value: String?): BillFrequency? {
        return value?.let { BillFrequency.valueOf(it) }
    }

    @TypeConverter
    fun frequencyToString(frequency: BillFrequency?): String? {
        return frequency?.name
    }
}
