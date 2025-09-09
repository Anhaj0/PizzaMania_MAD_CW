package com.pizzamania.di

import android.content.Context
import androidx.room.Room
import com.pizzamania.data.local.AppDatabase
import com.pizzamania.data.local.CartDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "pizzamania.db").build()

    @Provides
    fun provideCartDao(db: AppDatabase): CartDao = db.cartDao()
}
