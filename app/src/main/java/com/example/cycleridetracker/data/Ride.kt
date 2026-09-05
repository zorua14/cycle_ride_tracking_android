package com.example.cycleridetracker.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize

@Parcelize
data class LatLngPoint(val latitude: Double, val longitude: Double) : Parcelable

@Parcelize
@Entity(
    tableName = "ride_path_points",
    foreignKeys = [
        ForeignKey(
            entity = Ride::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rideId")]
)
data class RidePathPoint(
    @PrimaryKey(autoGenerate = true) val pointId: Int = 0,
    val rideId: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
) : Parcelable

@Parcelize
data class RidePhoto(
    val uri: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = 0L
) : Parcelable

@Parcelize
@Entity(tableName = "rides")
data class Ride(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val distanceMeters: Float,
    val averageSpeedKmh: Float,
    val maxSpeedKmh: Float,
    @Ignore val pathPoints: List<LatLngPoint> = emptyList(),
    @ColumnInfo(defaultValue = "[]") val photos: List<RidePhoto> = emptyList(),
    @ColumnInfo(defaultValue = "''") val notes: String = "",
    @ColumnInfo(defaultValue = "0") val isFinished: Boolean = false
) : Parcelable {

    constructor(
        id: Int,
        title: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        distanceMeters: Float,
        averageSpeedKmh: Float,
        maxSpeedKmh: Float,
        photos: List<RidePhoto>,
        notes: String,
        isFinished: Boolean
    ) : this(
        id = id,
        title = title,
        startTimeMillis = startTimeMillis,
        endTimeMillis = endTimeMillis,
        distanceMeters = distanceMeters,
        averageSpeedKmh = averageSpeedKmh,
        maxSpeedKmh = maxSpeedKmh,
        pathPoints = emptyList(),
        photos = photos,
        notes = notes,
        isFinished = isFinished
    )
    fun toRideData(useMetric: Boolean = true): com.example.cycleridetracker.ui.components.RideData {
        val date = java.text.SimpleDateFormat("EEE, MMM d • h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(startTimeMillis))
        
        val duration = (endTimeMillis - startTimeMillis)
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = (duration / (1000 * 60 * 60))
        val durationStr = if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)

        val distance = if (useMetric) distanceMeters / 1000f else (distanceMeters / 1000f) * 0.621371f
        val unit = if (useMetric) "km" else "mi"
        val speed = if (useMetric) averageSpeedKmh else averageSpeedKmh * 0.621371f
        val speedUnit = if (useMetric) "km/h" else "mph"

        return com.example.cycleridetracker.ui.components.RideData(
            title = title,
            time = date,
            distance = "%.1f %s".format(distance, unit),
            duration = durationStr,
            avgSpeed = "%.1f %s".format(speed, speedUnit)
        )
    }
}

class Converters {
    @TypeConverter
    fun fromLatLngList(value: List<LatLngPoint>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toLatLngList(value: String): List<LatLngPoint> {
        val listType = object : TypeToken<List<LatLngPoint>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromPhotoList(value: List<RidePhoto>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toPhotoList(value: String): List<RidePhoto> {
        val listType = object : TypeToken<List<RidePhoto>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
