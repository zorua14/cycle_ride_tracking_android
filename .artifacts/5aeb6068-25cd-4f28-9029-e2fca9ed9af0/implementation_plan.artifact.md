# Implementation Plan - Settings & Preferences Screen (M3 Expressive)

Build a high-fidelity mock of the "Settings & Preferences" screen using Material 3 Expressive guidance.

## User Review Required

> [!IMPORTANT]
> The implementation will use **Material 3 Expressive** patterns, specifically focusing on "Connected Button Groups" (replacing Segmented Buttons) and enhanced shape/motion as per the provided skill.

## Proposed Changes

### UI Components

#### [NEW] [SettingsScreen.kt](file:///Users/karthikeyanmuthu/AndroidStudioProjects/CycleRideTracker/app/src/main/java/com/example/cycleridetracker/SettingsScreen.kt)
- **TopAppBar**: Large or Center-aligned "Settings & Preferences".
- **Theme & Display Section**:
    - `Card` with tonal elevation.
    - Theme selection using a connected button group (System, Light, Dark).
    - Toggle for Metric Units.
    - List items for Weekly/Monthly goals with edit affordances.
- **Recording Engine Section**:
    - `Card` with tonal elevation.
    - GPS Sampling Rate selection.
    - Toggles for Auto-Pause and Haptic Feedback with supporting text.
- **NavigationBar**: Four destinations (Dashboard, History, Insights, Settings).
- **FloatingActionButton**: Floating cycle action button.

#### [MODIFY] [MainActivity.kt](file:///Users/karthikeyanmuthu/AndroidStudioProjects/CycleRideTracker/app/src/main/java/com/example/cycleridetracker/MainActivity.kt)
- Update to display the `SettingsScreen`.

## Verification Plan

### Automated Tests
- N/A (UI-only mock).

### Manual Verification
- Deploy to device/emulator.
- Verify Material You color adaptation by changing system accent colors.
- Check animations and haptics (simulated where possible).
- Verify layout on different screen sizes (Compact vs Medium).
