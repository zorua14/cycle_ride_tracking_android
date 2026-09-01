package com.example.cycleridetracker.di

import android.content.Context
import androidx.room.Room
import com.example.cycleridetracker.data.RideDao
import com.example.cycleridetracker.data.RideDatabase
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
    fun provideRideDatabase(@ApplicationContext context: Context): RideDatabase {
        return Room.databaseBuilder(
            context,
            RideDatabase::class.java,
            "cycle_ride_tracker_db"
        ).build()
    }

    @Provides
    fun provideRideDao(database: RideDatabase): RideDao {
        return database.rideDao()
    }
}
