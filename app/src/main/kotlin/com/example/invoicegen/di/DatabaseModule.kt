package com.example.invoicegen.di

import android.content.Context
import androidx.room.Room
import com.example.invoicegen.data.db.AppDatabase
import com.example.invoicegen.data.db.BusinessInfoDao
import com.example.invoicegen.data.db.InvoiceDao
import com.example.invoicegen.data.db.LineItemDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "invoice_maker.db"
        ).build()

    @Provides
    fun provideInvoiceDao(db: AppDatabase): InvoiceDao = db.invoiceDao()

    @Provides
    fun provideLineItemDao(db: AppDatabase): LineItemDao = db.lineItemDao()

    @Provides
    fun provideBusinessInfoDao(db: AppDatabase): BusinessInfoDao = db.businessInfoDao()
}
