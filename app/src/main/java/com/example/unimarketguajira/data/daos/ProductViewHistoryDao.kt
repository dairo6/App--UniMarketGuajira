package com.example.unimarketguajira.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarketguajira.data.entities.ProductViewHistoryEntity

@Dao
interface ProductViewHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertView(view: ProductViewHistoryEntity): Long

    @Query("SELECT * FROM product_view_history WHERE userId = :userId ORDER BY viewedAt DESC LIMIT 10")
    suspend fun getViewHistory(userId: String): List<ProductViewHistoryEntity>
    
    @Query("DELETE FROM product_view_history WHERE userId = :userId")
    suspend fun clearHistory(userId: String)

    @Query("DELETE FROM product_view_history WHERE productId = :productId AND userId = :userId")
    suspend fun deleteHistoryItem(productId: Int, userId: String)
}
