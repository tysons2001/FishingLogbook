package com.tyson.fishinglogbook.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM trips ORDER BY startMillis DESC")
    fun observeTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE endMillis IS NULL ORDER BY startMillis DESC LIMIT 1")
    fun observeActiveTrip(): Flow<TripEntity?>

    @Insert
    suspend fun insertTrip(item: TripEntity): Long

    @Query("UPDATE trips SET endMillis = :endMillis WHERE id = :id")
    suspend fun endTrip(id: Long, endMillis: Long)

    @Query("SELECT * FROM catches ORDER BY timestampMillis DESC")
    fun observeAllCatches(): Flow<List<CatchEntity>>

    @Query("SELECT * FROM catches WHERE id = :id")
    fun observeCatch(id: Long): Flow<CatchEntity?>
@Query("DELETE FROM catches WHERE id = :id")
suspend fun deleteCatch(id: Long)
    
    @Insert
    suspend fun insertCatch(item: CatchEntity): Long
}

