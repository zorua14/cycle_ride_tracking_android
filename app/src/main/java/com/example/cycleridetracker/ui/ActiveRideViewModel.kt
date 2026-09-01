package com.example.cycleridetracker.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cycleridetracker.data.ActiveRideState
import com.example.cycleridetracker.data.Ride
import com.example.cycleridetracker.data.RideRepository
import com.example.cycleridetracker.service.RideForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveRideViewModel @Inject constructor(
    private val repository: RideRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val activeRideState: StateFlow<ActiveRideState> = repository.activeRideState

    fun startRide() {
        val intent = Intent(context, RideForegroundService::class.java).apply {
            action = RideForegroundService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun pauseRide() {
        val intent = Intent(context, RideForegroundService::class.java).apply {
            action = RideForegroundService.ACTION_PAUSE
        }
        context.startForegroundService(intent)
    }

    fun resumeRide() {
        val intent = Intent(context, RideForegroundService::class.java).apply {
            action = RideForegroundService.ACTION_RESUME
        }
        context.startForegroundService(intent)
    }

    fun finishRide() {
        val intent = Intent(context, RideForegroundService::class.java).apply {
            action = RideForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
