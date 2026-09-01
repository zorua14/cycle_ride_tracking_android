package com.example.cycleridetracker.ui

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cycleridetracker.data.Ride
import com.example.cycleridetracker.data.RideRepository
import com.example.cycleridetracker.data.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.exifinterface.media.ExifInterface
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    private val repository: RideRepository,
    private val appPrefs: AppPrefs,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val useMetric: StateFlow<Boolean> = appPrefs.useMetric

    private val _ride = MutableStateFlow<Ride?>(null)
    val ride: StateFlow<Ride?> = _ride

    fun loadRide(rideId: Int) {
        viewModelScope.launch {
            _ride.value = repository.getRideById(rideId)
        }
    }

    fun addPhoto(uri: Uri) {
        val currentRide = _ride.value ?: return

        viewModelScope.launch {
            Log.d("EXIF_FLOW", "--- Start Processing Photo ---")
            Log.d("EXIF_FLOW", "Original URI: $uri")
            
            // 1. Get location from source URI (best chance)
            val location = getExifLocationFromUri(uri)
            Log.d("EXIF_FLOW", "Final Location Decided: $location")

            // 2. Save file for persistence
            val savedUri = saveImageToInternalStorage(uri)
            
            if (savedUri != null) {
                val newPhoto = com.example.cycleridetracker.data.RidePhoto(
                    uri = savedUri.toString(),
                    latitude = location?.first,
                    longitude = location?.second
                )
                val updatedPhotos = currentRide.photos + newPhoto
                val updatedRide = currentRide.copy(photos = updatedPhotos)
                repository.updateRide(updatedRide)
                _ride.value = updatedRide
                Log.d("EXIF_FLOW", "Photo saved and state updated.")
            } else {
                Log.e("EXIF_FLOW", "Failed to save image.")
            }
        }
    }

    private fun getExifLocationFromUri(uri: Uri): Pair<Double, Double>? {
        var photoUri = uri
        try {
            // For API 29+, need to request the original URI to get GPS tags
            // This requires ACCESS_MEDIA_LOCATION permission
            if (uri.scheme == "content") {
                
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, 
                    android.Manifest.permission.ACCESS_MEDIA_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                if (hasPermission) {
                    try {
                        photoUri = MediaStore.setRequireOriginal(uri)
                        Log.d("EXIF_FLOW", "Used setRequireOriginal for Media URI")
                    } catch (e: Exception) {
                        Log.w("EXIF_FLOW", "Could not setRequireOriginal for URI: $uri. Error: ${e.message}")
                    }
                } else {
                    Log.w("EXIF_FLOW", "ACCESS_MEDIA_LOCATION permission not granted. GPS data will be redacted.")
                }
            }

            context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = exif.latLong
                if (latLong != null) {
                    Log.i("EXIF_FLOW", "SUCCESS: Found LatLong: ${latLong[0]}, ${latLong[1]}")
                    return latLong[0] to latLong[1]
                } else {
                    Log.w("EXIF_FLOW", "No LatLong tags found in this image (URI: $photoUri)")
                }
            }
        } catch (e: Exception) {
            Log.e("EXIF_FLOW", "Error reading EXIF: ${e.message}", e)
        }
        return null
    }

    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "ride_photo_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("EXIF_FLOW", "Save error: ${e.message}")
            null
        }
    }

    fun updateNotes(notes: String) {
        val currentRide = _ride.value ?: return
        val updatedRide = currentRide.copy(notes = notes)
        viewModelScope.launch {
            repository.updateRide(updatedRide)
            _ride.value = updatedRide
        }
    }

    fun updateTitle(title: String) {
        val currentRide = _ride.value ?: return
        val updatedRide = currentRide.copy(title = title)
        viewModelScope.launch {
            repository.updateRide(updatedRide)
            _ride.value = updatedRide
        }
    }

    fun deleteRide() {
        val currentRide = _ride.value ?: return
        viewModelScope.launch {
            repository.deleteRide(currentRide.id)
            _ride.value = null
        }
    }

    fun deletePhoto(photoUri: String) {
        val currentRide = _ride.value ?: return
        val updatedPhotos = currentRide.photos.filterNot { it.uri == photoUri }
        val updatedRide = currentRide.copy(photos = updatedPhotos)
        viewModelScope.launch {
            repository.updateRide(updatedRide)
            _ride.value = updatedRide
            
            // Optionally delete the file from internal storage
            try {
                val file = File(Uri.parse(photoUri).path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("EXIF_FLOW", "Failed to delete photo file: ${e.message}")
            }
        }
    }
}
