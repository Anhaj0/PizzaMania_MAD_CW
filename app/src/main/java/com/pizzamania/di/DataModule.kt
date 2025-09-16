package com.pizzamania.di

import com.google.firebase.firestore.FirebaseFirestore
import com.pizzamania.data.local.CartDao
import com.pizzamania.data.repo.CartRepository
import com.pizzamania.data.repo.OrderRepository
import com.pizzamania.data.repo.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Lean DI module: repositories only.
 * DB (AppDatabase/CartDao) is provided by DatabaseModule.
 * Firestore/Auth/Storage are provided by FirebaseModule.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun provideCartRepository(dao: CartDao): CartRepository = CartRepository(dao)

    @Provides @Singleton
    fun provideOrderRepository(db: FirebaseFirestore): OrderRepository = OrderRepository(db)

    @Provides @Singleton
    fun provideProfileRepository(db: FirebaseFirestore): ProfileRepository = ProfileRepository(db)
}
