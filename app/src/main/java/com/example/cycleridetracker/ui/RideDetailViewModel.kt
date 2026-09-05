package com.example.cycleridetracker.ui

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import androidx.exifinterface.media.ExifInterface
import android.content.ContentUris
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

sealed interface RideDetailUiState {
    object Loading : RideDetailUiState
    data class Success(
        val ride: Ride,
        val useMetric: Boolean,
    ) : RideDetailUiState
    object Error : RideDetailUiState
}

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    private val repository: RideRepository,
    private val appPrefs: AppPrefs,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RideDetailUiState>(RideDetailUiState.Loading)
    val uiState: StateFlow<RideDetailUiState> = _uiState

    fun loadRide(rideId: Int) {
        viewModelScope.launch {
            _uiState.value = RideDetailUiState.Loading
            val ride = withContext(Dispatchers.IO) {
                repository.getRideById(rideId)
            }
            val useMetric = withContext(Dispatchers.IO) {
                appPrefs.useMetric.value // Assuming we can get current value or collect
            }
            if (ride != null) {
                _uiState.value = RideDetailUiState.Success(ride, useMetric)
            } else {
                _uiState.value = RideDetailUiState.Error
            }
        }
    }

    val useMetric: StateFlow<Boolean> = appPrefs.useMetric

    fun addPhotos(uris: List<Uri>) {
        val currentState = (_uiState.value as? RideDetailUiState.Success) ?: return
        val rideId = currentState.ride.id

        viewModelScope.launch {
            Log.d("EXIF_FLOW", "--- Start Processing ${uris.size} Photos ---")
            
            val newPhotos = uris.map { uri ->
                async(Dispatchers.IO) {
                    val location = getExifLocationFromUri(uri)
                    val timestamp = getExifTimestampFromUri(uri) ?: System.currentTimeMillis()
                    val savedUri = saveImageToInternalStorage(uri)
                    
                    if (savedUri != null) {
                        com.example.cycleridetracker.data.RidePhoto(
                            uri = savedUri.toString(),
                            latitude = location?.first,
                            longitude = location?.second,
                            timestamp = timestamp
                        )
                    } else null
                }
            }.awaitAll().filterNotNull()

            if (newPhotos.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    // Get latest ride from DB to avoid overwriting photos added by concurrent calls
                    val currentRide = repository.getRideById(rideId) ?: return@withContext
                    val updatedRide = currentRide.copy(photos = currentRide.photos + newPhotos)
                    repository.updateRide(updatedRide)
                    
                    _uiState.update { state ->
                        if (state is RideDetailUiState.Success && state.ride.id == rideId) {
                            state.copy(ride = updatedRide)
                        } else state
                    }
                }
                Log.d("EXIF_FLOW", "${newPhotos.size} photos saved and state updated.")
            }
        }
    }

    fun addPhoto(uri: Uri) {
        addPhotos(listOf(uri))
    }

    private fun getExifLocationFromUri(uri: Uri): Pair<Double, Double>? {
        try {
            // 1. Try reading directly from the provided URI
            val location = readExifLocation(uri)
            if (location != null) return location

            // 2. If no location, check if it's a Picker URI and try to map it to MediaStore
            // Photo Picker URIs redact EXIF by design, so we try to find the original record.
            if (uri.toString().contains("com.android.providers.media.photopicker")) {
                val mediaId = uri.lastPathSegment?.toLongOrNull()
                if (mediaId != null) {
                    val mediaStoreUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )
                    
                    val hasLocationPerm = ContextCompat.checkSelfPermission(
                        context, 
                        android.Manifest.permission.ACCESS_MEDIA_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    val hasMediaPerm = ContextCompat.checkSelfPermission(
                        context, 
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasLocationPerm && hasMediaPerm) {
                        try {
                            val originalUri = MediaStore.setRequireOriginal(mediaStoreUri)
                            return readExifLocation(originalUri)
                        } catch (e: Exception) {
                            Log.w("EXIF_FLOW", "Could not get original EXIF from MediaStore for ID $mediaId: ${e.message}")
                        }
                    }
                }
            }

            // 3. Fallback for non-picker content URIs
            if (uri.scheme == "content" && !uri.toString().contains("photopicker")) {
                try {
                    val originalUri = MediaStore.setRequireOriginal(uri)
                    return readExifLocation(originalUri)
                } catch (e: Exception) {
                    Log.w("EXIF_FLOW", "Could not setRequireOriginal for generic URI: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("EXIF_FLOW", "Error in getExifLocationFromUri: ${e.message}")
        }
        return null
    }

    private fun getExifTimestampFromUri(uri: Uri): Long? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                
                if (dateTime != null) {
                    val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                    format.parse(dateTime)?.time
                } else {
                    // Fallback to MediaStore if it's a content URI
                    if (uri.scheme == "content") {
                        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN)
                        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                                cursor.getLong(dateIndex)
                            } else null
                        }
                    } else null
                }
            }
        } catch (e: Exception) {
            Log.e("EXIF_FLOW", "Error getting timestamp from URI $uri: ${e.message}")
            null
        }
    }

    private fun readExifLocation(uri: Uri): Pair<Double, Double>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = exif.latLong
                if (latLong != null) {
                    Log.i("EXIF_FLOW", "SUCCESS: Found LatLong: ${latLong[0]}, ${latLong[1]}")
                    latLong[0] to latLong[1]
                } else {
                    Log.d("EXIF_FLOW", "No LatLong tags found for URI: $uri")
                    null
                }
            }
        } catch (e: Exception) {
            // Rethrow to be caught by getExifLocationFromUri
            throw e
        }
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
        val currentState = _uiState.value as? RideDetailUiState.Success ?: return
        val currentRide = currentState.ride
        val updatedRide = currentRide.copy(notes = notes)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.updateRide(updatedRide) }
            _uiState.value = currentState.copy(ride = updatedRide)
        }
    }

    fun updateTitle(title: String) {
        val currentState = _uiState.value as? RideDetailUiState.Success ?: return
        val currentRide = currentState.ride
        val updatedRide = currentRide.copy(title = title)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.updateRide(updatedRide) }
            _uiState.value = currentState.copy(ride = updatedRide)
        }
    }

    fun deleteRide() {
        val currentState = _uiState.value as? RideDetailUiState.Success ?: return
        val currentRide = currentState.ride
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteRide(currentRide.id) }
            _uiState.value = RideDetailUiState.Error
        }
    }

    fun deletePhoto(photoUri: String) {
        val currentState = _uiState.value as? RideDetailUiState.Success ?: return
        val currentRide = currentState.ride
        val updatedPhotos = currentRide.photos.filterNot { it.uri == photoUri }
        val updatedRide = currentRide.copy(photos = updatedPhotos)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.updateRide(updatedRide) }
            _uiState.value = currentState.copy(ride = updatedRide)
            
            // Optionally delete the file from internal storage
            try {
                withContext(Dispatchers.IO) {
                    val file = File(photoUri.toUri().path ?: "")
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("EXIF_FLOW", "Failed to delete photo file: ${e.message}")
            }
        }
    }
}

