# Create Active Ride Screen

Implement a new "Active Ride" screen that appears when the user presses the bicycle FAB on the Dashboard/Main screens. This screen will follow the provided design but with specific elements removed as requested.

## User Review Required

> [!IMPORTANT]
> The "Active Ride" screen will be a new top-level-like state. Pressing the back button will return to the Dashboard (canceling the ride for now, unless we implement a "running in background" state later).

## Proposed Changes

### UI Components

#### [NEW] [ActiveRideScreen.kt](file:///Users/karthikeyanmuthu/AndroidStudioProjects/CycleRideTracker/app/src/main/java/com/example/cycleridetracker/ActiveRideScreen.kt)
- Create the `ActiveRideScreen` composable.
- **Top Bar**: Display "Cycling Ride" with the bicycle icon. Remove the gauge and lock icons from the top right.
- **Map View**: A placeholder grid with a simulated ride path (light blue line).
- **Elapsed Time Section**: A prominent card showing the timer (e.g., "01:05").
- **Metrics Grid**: Two rows of cards:
    - Row 1: Distance and Avg Speed.
    - Row 2: Elevation, Max Speed, and Photos (0 pins).
- **Control Bar**:
    - A "PAUSE" button with a pause icon.
    - A "HOLD TO FINISH RIDE" bar at the bottom.
    - **Note**: The camera FAB in the bottom right will be removed.

### Navigation

#### [MODIFY] [MainActivity.kt](file:///Users/karthikeyanmuthu/AndroidStudioProjects/CycleRideTracker/app/src/main/java/com/example/cycleridetracker/MainActivity.kt)
- Add "ActiveRide" to the `currentScreen` state options.
- Update the bicycle FAB `onClick` to set `currentScreen = "ActiveRide"`.
- Handle the `ActiveRide` screen in the `AnimatedContent` block.
- Hide the bottom navigation toolbar when on the `ActiveRide` screen.

## Verification Plan

### Manual Verification
- Deploy the app.
- Tap the bicycle FAB at the bottom center.
- Verify that the "Active Ride" screen appears with the correct design:
    - No camera FAB.
    - No top-right icons.
    - Design matches the project's theme (colors, card shapes).
- Verify the "PAUSE" and "HOLD TO FINISH RIDE" buttons are present.
- Press the back button to return to the Dashboard.
