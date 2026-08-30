# Fix ButtonGroup Errors and Warnings in ReplayJourneyScreen.kt

This plan addresses the compilation errors and lint warnings in `ReplayJourneyScreen.kt`, specifically focusing on the new `ButtonGroup` and `ToggleButton` APIs from Material 3 Expressive.

## Proposed Changes

### [app](file:///Users/karthikeyanmuthu/AndroidStudioProjects/CycleRideTracker/app)

#### [MODIFY] [ReplayJourneyScreen.kt](file:///Users/karthikeyanmuthu/AndroidStudioProjects/CycleRideTracker/app/src/main/java/com/example/cycleridetracker/ReplayJourneyScreen.kt)

- Add `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` to the file and the composable.
- Fix `ButtonGroup` usage:
    - Provide a mandatory `overflowIndicator`.
    - Use `toggleableItem` DSL within the `ButtonGroup` scope instead of calling `ToggleButton` directly. This resolves the `@Composable` invocation error because the `ButtonGroup` content lambda is a DSL scope, not a direct composable container.
    - Remove redundant `remember` and `interactionSource` calls inside the `ButtonGroup` DSL as `toggleableItem` handles these internally.
- Fix various lint warnings:
    - Use `Locale.US` with `String.format`.
    - Use named parameters for boolean literals in `mutableStateOf`.
    - Add trailing commas and clarifying parentheses.
    - Convert `delay` duration to a more modern form.
    - Remove redundant qualifier for `AnimatedVisibility`.

## Verification Plan

### Automated Tests
- Build the project to ensure all compilation errors are resolved.
- Run `analyze_file` again to verify no remaining errors or warnings.

### Manual Verification
- Deploy the app to a device/emulator and navigate to the Replay Journey screen.
- Verify the speed selector (`ButtonGroup`) functions correctly, showing the check icon and responding to clicks.
- Verify the playback speed changes correctly.
