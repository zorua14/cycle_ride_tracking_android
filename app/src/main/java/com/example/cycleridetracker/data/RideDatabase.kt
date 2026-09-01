package com.example.cycleridetracker.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [Ride::class],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = RideDatabase.V2ToV3Migration::class),
        AutoMigration(from = 3, to = 4, spec = RideDatabase.V3ToV4Migration::class),
        AutoMigration(from = 4, to = 5, spec = RideDatabase.V4ToV5Migration::class),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7)
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RideDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao

    @DeleteColumn(tableName = "rides", columnName = "photoUris")
    class V2ToV3Migration : AutoMigrationSpec

    @DeleteColumn(tableName = "rides", columnName = "elevationGainMeters")
    class V3ToV4Migration : AutoMigrationSpec

    @DeleteColumn(tableName = "rides", columnName = "isFavorite")
    class V4ToV5Migration : AutoMigrationSpec
}
