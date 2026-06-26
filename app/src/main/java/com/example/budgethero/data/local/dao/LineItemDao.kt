package com.example.budgethero.data.local.dao

import androidx.room.*
import com.example.budgethero.data.model.LineItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface LineItemDao {
    @Query("SELECT * FROM line_items ORDER BY date DESC")
    fun getAllLineItems(): Flow<List<LineItem>>

    @Query("SELECT * FROM line_items WHERE date = :date")
    fun getLineItemsByDate(date: LocalDate): Flow<List<LineItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItem(lineItem: LineItem)

    @Delete
    suspend fun deleteLineItem(lineItem: LineItem)
}
