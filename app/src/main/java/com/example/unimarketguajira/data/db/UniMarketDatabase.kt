package com.example.unimarketguajira.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.unimarketguajira.data.daos.CartDao
import com.example.unimarketguajira.data.daos.ProductDao
import com.example.unimarketguajira.data.daos.UserDao
import com.example.unimarketguajira.data.entities.CartItemEntity
import com.example.unimarketguajira.data.entities.ProductEntity
import com.example.unimarketguajira.data.entities.UserEntity

import com.example.unimarketguajira.data.entities.PurchaseEntity
import com.example.unimarketguajira.data.entities.NotificationEntity
import com.example.unimarketguajira.data.entities.ProductViewHistoryEntity
import com.example.unimarketguajira.data.daos.PurchaseDao
import com.example.unimarketguajira.data.daos.NotificationDao
import com.example.unimarketguajira.data.daos.ProductViewHistoryDao

@Database(
    entities = [
        UserEntity::class,
        ProductEntity::class,
        CartItemEntity::class,
        PurchaseEntity::class,
        NotificationEntity::class,
        ProductViewHistoryEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UniMarketDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun notificationDao(): NotificationDao
    abstract fun productViewHistoryDao(): ProductViewHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: UniMarketDatabase? = null

        fun getDatabase(context: Context): UniMarketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UniMarketDatabase::class.java,
                    "unimarket_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
