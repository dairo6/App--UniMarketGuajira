package com.example.unimarketguajira.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarketguajira.data.entities.PurchaseEntity

@Dao
interface PurchaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Query("SELECT * FROM purchases WHERE id = :purchaseId")
    suspend fun getPurchaseById(purchaseId: String): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE buyerId = :buyerId ORDER BY purchaseDate DESC")
    suspend fun getPurchasesForBuyer(buyerId: String): List<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE sellerId = :sellerId ORDER BY purchaseDate DESC")
    suspend fun getPurchasesForSeller(sellerId: String): List<PurchaseEntity>
}
