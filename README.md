# NextBillion to TomTom Route Navigation

Android application that renders NextBillion API route optimization responses on TomTom Maps with turn-by-turn navigation.

## Features

✅ **Multi-Route Display** - Visualizes all 4 routes from NextBillion optimization response
✅ **Turn-by-Turn Navigation** - Real-time GPS-based navigation with voice instructions
✅ **Route Selection** - Interactive dropdown to select and highlight specific routes
✅ **Polyline Decoding** - Robust decoder for NextBillion's encoded geometry format
✅ **Live Progress Tracking** - Shows current position and navigation progress

## Quick Start

### 1. Get Your TomTom API Key
Visit [TomTom Developer Portal](https://developer.tomtom.com/) to get your API key.

### 2. Configure API Key
Replace `YOUR_TOMTOM_API_KEY_HERE` in two places:
- `app/src/main/AndroidManifest.xml` (line 25)
- `app/src/main/java/com/example/poconnbandtomtom/utils/Config.kt` (line 10)

### 3. Build and Run
```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Architecture

### Core Components

#### 1. **Data Models** (`models/NextBillionResponse.kt`)
- Complete NextBillion API response structure
- Supports routes, steps, geometry, and metadata

#### 2. **Polyline Decoder** (`utils/PolylineDecoder.kt`)
- Decodes NextBillion's encoded polyline format
- Fallback to Google's decoding algorithm
- Handles precision levels (5 and 6 decimal places)

#### 3. **Route Renderer** (`navigation/RouteRenderer.kt`)
- Displays multiple routes on TomTom map
- Color-coded route differentiation
- Start/end markers for each route

#### 4. **Turn-by-Turn Navigator** (`navigation/TurnByTurnNavigator.kt`)
- Real-time GPS tracking
- Navigation instructions
- Progress updates
- Step-by-step guidance

#### 5. **Main Activity** (`MainActivity.kt`)
- Map initialization
- Route loading and display
- UI controls for navigation
- Location permission handling

## Technical Details

### Dependencies
- TomTom Maps SDK 0.48.0
- Google Play Services Location 21.0.1
- Kotlin Coroutines 1.7.3
- Gson 2.10.1

### Permissions Required
- `ACCESS_FINE_LOCATION` - GPS tracking
- `ACCESS_COARSE_LOCATION` - Approximate location
- `INTERNET` - Map tiles and API calls

### NextBillion Response Format
```json
{
  "result": {
    "routes": [
      {
        "vehicle": "Van-JHB-01",
        "steps": [
          {
            "type": "start|job|end",
            "location": [lat, lng],
            "arrival": timestamp
          }
        ],
        "geometry": "encoded_polyline_string"
      }
    ]
  }
}
```

## Navigation Flow

1. **Load Routes** - Parse NextBillion response from assets
2. **Decode Geometry** - Convert encoded polylines to coordinates
3. **Display on Map** - Render routes with different colors
4. **Select Route** - Choose route from dropdown
5. **Start Navigation** - Begin turn-by-turn guidance
6. **Track Progress** - Monitor GPS position and provide instructions

## Key Features Implementation

### Route Visualization
- Each route displayed in unique color
- Start markers (green) and end markers (red)
- Job locations marked along route

### Navigation Instructions
- Distance-based triggers (50m for turns)
- Voice instruction support ready
- Visual progress indicators
- Remaining distance display

### Error Handling
- Graceful fallback for polyline decoding
- Network error recovery
- GPS signal loss handling
- Invalid data validation

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Test Navigation
1. Launch app on device/emulator
2. Grant location permissions
3. Select a route from dropdown
4. Tap "Start Navigation"
5. Move device to simulate GPS updates

## Production Considerations

### Before Release
1. **API Key Security** - Move to secure configuration
2. **Error Logging** - Implement crash reporting
3. **Offline Support** - Cache map tiles
4. **Voice Instructions** - Add TTS for navigation
5. **Route Recalculation** - Handle off-route scenarios

### Performance Optimization
- Lazy loading for large route datasets
- Background route decoding
- Map clustering for multiple stops
- Battery optimization for GPS tracking

## Troubleshooting

### Common Issues

**Map not loading**
- Verify TomTom API key is correct
- Check internet connectivity
- Ensure permissions are granted

**Routes not displaying**
- Check NextBillion response format
- Verify polyline decoding
- Confirm coordinate system (WGS84)

**Navigation not working**
- Enable GPS/Location services
- Move to area with GPS signal
- Check location permissions

## License
This is a proof-of-concept implementation for demonstration purposes.

## Support
For issues related to:
- TomTom SDK: [TomTom Support](https://developer.tomtom.com/support)
- NextBillion API: [NextBillion Docs](https://docs.nextbillion.ai/)