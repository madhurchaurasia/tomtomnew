# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
Android navigation app that renders NextBillion API route optimization responses on TomTom Maps with turn-by-turn navigation. The app decodes polyline geometries from NextBillion format and provides real-time GPS navigation.

## Build & Development Commands

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Clean and build
./gradlew clean assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run with logs
adb logcat -s POCApp:*
```

### Testing Commands
```bash
# Run unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "*.PolylineDecoderTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint checks
./gradlew lint
```

## Architecture & Key Components

### Core Navigation System
The app uses a layered architecture for route rendering and navigation:

1. **Data Layer** (`models/NextBillionResponse.kt`)
   - Parses NextBillion API responses containing multiple routes with encoded polylines
   - Each route contains steps (start/job/end) with locations and timestamps

2. **Route Rendering** (`navigation/RouteRenderer.kt`)
   - Decodes polylines using custom decoder with Google Maps fallback
   - Renders multiple routes with color differentiation
   - Manages route selection and highlighting

3. **Turn-by-Turn Navigation** (`navigation/TurnByTurnNavigator.kt`)
   - Tracks GPS position relative to selected route
   - Detects step progression (arrival at waypoints)
   - Calculates remaining distance and generates instructions
   - States: Idle → Active → Completed/Error

4. **Polyline Decoding** (`utils/PolylineDecoder.kt`)
   - Custom implementation for NextBillion's encoded format
   - Falls back to Google Maps decoder if custom fails
   - Handles both 5 and 6 decimal precision levels

### Configuration
- **TomTom API Key**: Set in two places:
  - `AndroidManifest.xml` line 26
  - `utils/Config.kt` line 10 (fallback)
- **SDK Versions**: minSdk 26, targetSdk 33, compileSdk 33
- **TomTom Maps SDK**: v1.9.0

### Data Flow
1. Load NextBillion response from `assets/Next_Billion_response_4.json`
2. Decode polyline geometry to coordinate list
3. Render routes on TomTom map with markers
4. User selects route → Start navigation
5. GPS updates trigger step detection and instruction updates

## Important Implementation Details

### Polyline Decoding Strategy
The app uses a two-tier decoding approach in `PolylineDecoder.kt`:
- Primary: Custom decoder optimized for NextBillion format
- Fallback: Google Maps library decoder
- Both handle coordinate precision differences automatically

### Navigation Logic
`TurnByTurnNavigator` implements distance-based triggers:
- Step completion: Within 30m of waypoint
- Instruction trigger: 50m before turns
- Route following: Finds closest point on route polyline

### Route Rendering Colors
Routes are color-coded (defined in `Config.kt`):
- Route 1: Blue (#2196F3)
- Route 2: Green (#4CAF50)
- Route 3: Orange (#FF9800)
- Route 4: Purple (#9C27B0)

## Testing Considerations

### Sample Data
`assets/Next_Billion_response_4.json` contains 4 real routes in Johannesburg area with:
- Encoded polyline geometries
- Multiple waypoints (start, jobs, end)
- Vehicle identifiers and timing data

### GPS Testing
For navigation testing on emulator:
1. Use Extended Controls → Location to set GPS coordinates
2. Import GPX file or manually set waypoints along route
3. Monitor logcat for navigation state changes

### Permission Handling
The app requires location permissions - handled in `MainActivity`:
- Requests at runtime if not granted
- Falls back to default location if denied