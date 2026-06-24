package com.example.unimarketguajira.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarketguajira.data.entities.UserFavoriteEntity

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: UserFavoriteEntity)

    @Query("DELETE FROM user_favorites WHERE userEmail = :userEmail AND productId = :productId")
    suspend fun deleteFavorite(userEmail: String, productId: Int)

    @Query("SELECT productId FROM user_favorites WHERE userEmail = :userEmail")
    suspend fun getFavoriteProductIds(userEmail: String): List<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM user_favorites WHERE userEmail = :userEmail AND productId = :productId)")
    suspend fun isFavorite(userEmail: String, productId: Int): Boolean
}
