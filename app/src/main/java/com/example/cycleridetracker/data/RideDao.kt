package com.example.cycleridetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM rides ORDER BY startTimeMillis DESC")
    fun getAllRides(): Flow<List<Ride>>

    @Query("SELECT * FROM rides WHERE id = :id")
    suspend fun getRideById(id: Int): Ride?

    @Query("SELECT * FROM rides WHERE isFinished = 0 LIMIT 1")
    suspend fun getActiveRide(): Ride?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: Ride): Long

    @Update
    suspend fun updateRide(ride: Ride)

    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun deleteRide(id: Int)

    @Query("DELETE FROM rides WHERE isFinished = 0")
    suspend fun deleteUnfinishedRides()
}
