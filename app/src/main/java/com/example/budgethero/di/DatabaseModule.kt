package com.example.budgethero.di

import android.content.Context
import androidx.room.Room
import com.example.budgethero.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "budget_hero_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideBillDao(database: AppDatabase) = database.billDao()

    @Provides
    fun provideWorkDayDao(database: AppDatabase) = database.workDayDao()

    @Provides
    fun provideLineItemDao(database: AppDatabase) = database.lineItemDao()

    @Provides
    fun provideIncomeDao(database: AppDatabase) = database.incomeDao()
}
