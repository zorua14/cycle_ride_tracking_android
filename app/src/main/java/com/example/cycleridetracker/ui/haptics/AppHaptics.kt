package com.example.cycleridetracker.ui.haptics

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object AppHaptics {
    fun performAction(haptic: HapticFeedback, enabled: Boolean = true) {
        if (enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun performSelection(haptic: HapticFeedback, enabled: Boolean = true) {
        if (enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}
