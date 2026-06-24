package com.example.unimarketguajira.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarketguajira.data.entities.ChatRoomEntity

@Dao
interface ChatRoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(chatRoom: ChatRoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chatRooms: List<ChatRoomEntity>)

    @Query("SELECT * FROM chat_rooms WHERE id = :id")
    suspend fun getChatRoomById(id: String): ChatRoomEntity?

    @Query("SELECT * FROM chat_rooms WHERE id_comprador = :email OR id_vendedor = :email")
    suspend fun getChatRoomsForUser(email: String): List<ChatRoomEntity>

    @Query("SELECT * FROM chat_rooms WHERE (id_comprador = :email OR id_vendedor = :email) AND status = :status ORDER BY timestamp DESC")
    suspend fun getChatRoomsForUserByStatus(email: String, status: String): List<ChatRoomEntity>

    @Query("DELETE FROM chat_rooms WHERE id = :id")
    suspend fun deleteChatRoom(id: String)
}
