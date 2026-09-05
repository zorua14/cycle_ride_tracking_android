# Implementation Plan - Enhanced Monthly Insights

Refactor the Insights screen to provide a detailed monthly view with daily ride data, monthly navigation, and efficient database queries.

## User Review Required

> [!IMPORTANT]
> The "Month" view will switch from a `LineChart` (weekly aggregation) to a scrollable `BarChart` (daily aggregation) to clearly show each day's distance, as requested ("1st sept 10 km 2nd 0 km").
> Navigation arrows will be added to the chart card to allow switching between months.

## Proposed Changes

### Data Layer

#### [MODIFY] [RideDao.kt](file:///Users/karthikeyanmuthu/AndroidStudioProjects/CycleRideTracker/app/src/main/java/com/example/cycleridetracker/data/RideDao.kt)
- Add `@Query` to fetch rides between two timestamps: `getRidesByTimeRange(start: Long, end: Long): Flow<List<Ride>>`.

#### [MODIFY] [RideRepository.kt](file:///Users/karthikeyanmuthu/AndroidStudioProjects/CycleRideTracker/app/src/main/java/com/example/cycleridetracker/data/RideRepository.kt)
- Expose the new DAO query.

---

### ViewModel Layer

#### [MODIFY] [InsightsViewModel.kt](file:///Users/karthikeyanmuthu/AndroidStudioProjects/CycleRideTracker/app/src/main/java/com/example/cycleridetracker/ui/InsightsViewModel.kt)
- Add `currentMonthCalendar` StateFlow (initialized to the current month).
- Update `uiState` to use a `flatMapLatest` pattern (or similar) to fetch only the relevant rides from the repository based on the selected range and current month.
- Refactor `prepareChartData` for the "Month" range:
    - Generate labels and data for every day of the selected month (e.g., "1", "2", ..., "31").
    - Aggregate ride distances by day.
- Add `nextMonth()` and `previousMonth()` functions.
- Update `InsightsStats` to include a `displayMonth` string (e.g., "September 2026") for the UI.

---

### UI Layer

#### [MODIFY] [InsightsScreen.kt](file:///Users/karthikeyanmuthu/AndroidStudioProjects/CycleRideTracker/app/src/main/java/com/example/cycleridetracker/InsightsScreen.kt)
- Update `DistanceVisualizationCard`:
    - Add a header row with "Previous Month", "Current Month Name", and "Next Month" buttons when the "Month" range is selected.
    - Switch "Month" view to use `BarChart` with daily data.
- Ensure `BarChart` and `LineChart` use `rememberVicoScrollState()` (already present) to handle many data points gracefully.

## Verification Plan

### Automated Tests
- I'll verify the `prepareChartData` logic for different months (leap years, 30 vs 31 days).

### Manual Verification
- Deploy to device/emulator.
- Select "Month" range.
- Verify today's ride shows up on the correct day (e.g., 4th Sept).
- Use navigation arrows to go back to August and verify data (if any).
- Verify "Week" and "All Time" views still work as expected.
