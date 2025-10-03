package com.example.poconnbandtomtom


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.poconnbandtomtom.databinding.ActivityNavigationBinding
import com.example.poconnbandtomtom.models.NextBillionResponse
import com.example.poconnbandtomtom.models.NextBillionRoute
import com.google.gson.Gson
import com.tomtom.sdk.datamanagement.navigationtile.NavigationTileStore
import com.tomtom.sdk.datamanagement.navigationtile.NavigationTileStoreConfiguration
import com.tomtom.sdk.location.DefaultLocationProviderFactory
import com.tomtom.sdk.location.GeoLocation
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.location.LocationProvider
import com.tomtom.sdk.location.OnLocationUpdateListener
import com.tomtom.sdk.location.mapmatched.MapMatchedLocationProviderFactory
import com.tomtom.sdk.location.simulation.SimulationLocationProvider
import com.tomtom.sdk.location.simulation.strategy.InterpolationStrategy
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraTrackingMode
import com.tomtom.sdk.map.display.camera.CameraChangeListener
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.common.screen.Padding
import com.tomtom.sdk.map.display.gesture.MapLongClickListener
import com.tomtom.sdk.map.display.location.LocationMarkerOptions
import com.tomtom.sdk.map.display.route.Instruction
import com.tomtom.sdk.map.display.route.RouteClickListener
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.currentlocation.CurrentLocationButton.VisibilityPolicy
import com.tomtom.sdk.navigation.ActiveRouteChangedListener
import com.tomtom.sdk.navigation.NavigationOptions
import com.tomtom.sdk.navigation.ProgressUpdatedListener
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.navigation.TomTomNavigation
import com.tomtom.sdk.navigation.online.Configuration
import com.tomtom.sdk.navigation.online.OnlineTomTomNavigationFactory
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.navigation.ui.NavigationUiOptions
import com.tomtom.sdk.routing.RoutePlanner
import com.tomtom.sdk.routing.RoutePlanningCallback
import com.tomtom.sdk.routing.RoutePlanningResponse
import com.tomtom.sdk.routing.RoutingFailure
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.options.guidance.ExtendedSections
import com.tomtom.sdk.routing.options.guidance.GuidanceOptions
import com.tomtom.sdk.routing.options.guidance.InstructionPhoneticsType
import com.tomtom.sdk.routing.online.OnlineRoutePlanner
import com.tomtom.sdk.routing.route.Route
import com.tomtom.sdk.vehicle.Vehicle
import com.tomtom.sdk.vehicle.VehicleProviderFactory
import java.io.InputStreamReader
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.example.poconnbandtomtom.WebSocketClient
import android.content.IntentFilter
import android.os.Build
import android.content.Context
import android.content.BroadcastReceiver
import okhttp3.*

class NavigationActivity : AppCompatActivity() {
    private lateinit var mapFragment: MapFragment
    private lateinit var tomTomMap: TomTomMap
    private lateinit var navigationTileStore: NavigationTileStore
    private lateinit var locationProvider: LocationProvider
    private lateinit var onLocationUpdateListener: OnLocationUpdateListener
    private lateinit var routePlanner: RoutePlanner
    private var route: Route? = null
    private lateinit var navigationRoute: Route
    private lateinit var routePlanningOptions: RoutePlanningOptions
    private lateinit var tomTomNavigation: TomTomNavigation
    private lateinit var navigationFragment: NavigationFragment
    private var routesList: List<NextBillionRoute> = emptyList()
    private lateinit var navigationBinding: ActivityNavigationBinding
    private var selectedRouteIndex = -1
//    sGRiHf5v5pmTnfAqyhrKdjP3ifcNf67d
//    3dWb0j1jH79ZLj8xmMbOUoOx17AED32
    private val apiKey = "sGRiHf5v5pmTnfAqyhrKdjP3ifcNf67d"

    private lateinit var webSocket: WebSocket
    private lateinit var client: OkHttpClient
    private var isRoute = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationBinding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(navigationBinding.root)
        client = OkHttpClient()
        val request = Request.Builder()
            .url("ws://110.238.78.42:443/ws")
            .build()

        val webSocketListener = WebSocketClient(applicationContext)
        webSocket = client.newWebSocket(request, webSocketListener)
        navigationBinding.btnDelivered.setOnClickListener {
                val request = Request.Builder()
                    .url("ws://110.238.78.42:443/order/status")
                    .build()

                val webSocketListener = WebSocketClient(applicationContext)
                webSocket = client.newWebSocket(request, webSocketListener)
                webSocket.send("{\"order-id\":\"J4\",\"status\":\"delivered\"}")
                navigationBinding.btnDelivered.visibility = View.GONE
                navigationBinding.btnStartNavigation.visibility = View.GONE
        }

        navigationBinding.btnDelivered.visibility = View.GONE
        navigationBinding.btnStartNavigation.visibility = View.GONE
        navigationBinding.btnClearRoutes.visibility = View.GONE

        navigationBinding.progressBarMapLoading.visibility =View.VISIBLE

        initLocationProvider()

        ensureLocationPermissions {
            // Enables the location provider to start receiving location updates.
            locationProvider.enable()
        }

        initMap {
            /**
             * The LocationProvider itself only reports location changes.
             * It does not interact internally with the map or navigation.
             * Therefore, to show the user’s location on the map you have to set the LocationProvider to the TomTomMap.
             * The TomTomMap will then use the LocationProvider to show a location marker on the map.
             */
            tomTomMap.setLocationProvider(locationProvider)

            showUserLocation()
            setUpMapListeners()
            initNavigationTileStore()
            initRouting()
            initNavigation()
            loadRoutesFromAssets()
            initUI()
        }
    }

    private fun initUI() {
        navigationBinding.btnClearRoutes.setOnClickListener {
            if (isNavigationRunning()) {
                Toast.makeText(applicationContext, "Please stop navigation first", Toast.LENGTH_SHORT).show()
            }
            else{
                navigationBinding.btnDelivered.visibility = View.GONE
                navigationBinding.btnStartNavigation.visibility = View.GONE
                navigationBinding.btnClearRoutes.visibility = View.GONE
                navigationBinding.progressBarMapLoading.visibility =View.VISIBLE
                clearMap()
                navigationBinding.spinnerRoutes.setSelection(0)
                stopNavigation()
                showUserLocation()
            }

        }
        navigationBinding.spinnerRoutes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 1 && position < routesList.size) {
                    selectedRouteIndex = position-1
                 drawRouteOnMap((routesList[position-1]))
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedRouteIndex = -1
            }
        }
        navigationBinding.btnStartNavigation.setOnClickListener {
            if(selectedRouteIndex!=-1){
                if (isNavigationRunning()) {
                    Toast.makeText(applicationContext, "Navigation Already Running", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    mapFragment.currentLocationButton.visibilityPolicy = VisibilityPolicy.Invisible
                    navigationBinding.navigationFragmentContainer.visibility = View.VISIBLE
                    startNavigation(navigationRoute)

                    navigationBinding.btnDelivered.visibility = View.VISIBLE
                    navigationBinding.btnStartNavigation.visibility = View.GONE
                }


            }

        }
    }

    private fun drawRouteOnMap(route: NextBillionRoute) {

        println("mRouteList___"+route)
        if (route.steps.isEmpty()) return

        // First point as origin
        val origin = GeoPoint(route.steps[0].location[0], route.steps[0].location[1])

        println("___Origin"+origin)

        // Last point as destination
        val destination = GeoPoint(
            route.steps[route.steps.size-1].location[0], route.steps[route.steps.size-1].location[1]
        )

        println("___destination"+destination)

        // Intermediate waypoints
        val waypoints = mutableListOf<GeoPoint>()
        for (i in 1 until route.steps.size - 1) {
            waypoints.add(
                GeoPoint(route.steps[i].location[0], route.steps[i].location[1])
            )
            val waypointsMarker = GeoPoint(route.steps[i].location[0], route.steps[i].location[1])
            val markerOptions =
                MarkerOptions(
                    coordinate = waypointsMarker,
                    pinImage = ImageFactory.fromResource(R.drawable.ic_marker_end),
                )
            this.tomTomMap.addMarker(markerOptions)
        }

        println("___waypoints"+waypoints)

        val itinerary = Itinerary(
            origin = origin,
            destination = destination,
            waypoints = waypoints
        )

        routePlanningOptions = RoutePlanningOptions(
            itinerary = itinerary,
            guidanceOptions = GuidanceOptions(
                phoneticsType = InstructionPhoneticsType.Ipa,
                extendedSections = ExtendedSections.All
            ),
            vehicle = Vehicle.Car()
        )

        routePlanner.planRoute(routePlanningOptions, routePlanningCallback)

        navigationBinding.btnDelivered.visibility = View.INVISIBLE
        navigationBinding.btnStartNavigation.visibility = View.VISIBLE
        navigationBinding.btnClearRoutes.visibility = View.VISIBLE

    }

    /**
     * [MapOptions] is required to initialize the map with [MapFragment.newInstance]
     * Use [MapFragment.getMapAsync] to render the map.
     *
     * Optional: You can further configure the map by setting various properties of the MapOptions object.
     * You can learn more in the Map Configuration guide.
     * The next step is adding the MapFragment to the previously created container.
     * The map is ready to use once the [MapFragment.getMapAsync] method is called and the map is fetched,
     * after which the [onMapReady] callback is triggered.
     */
    private fun initMap(onMapReady: () -> Unit) {
        val mapOptions = MapOptions(mapKey = apiKey)
        mapFragment = MapFragment.newInstance(mapOptions)

        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        mapFragment.getMapAsync { map ->
            tomTomMap = map
            onMapReady()
        }
    }

    /**
     * The SDK provides a [NavigationTileStore] class that is used between different modules to get tile data based
     * on the online map.
     */
    private fun initNavigationTileStore() {
        navigationTileStore = NavigationTileStore.create(
            context = applicationContext,
            navigationTileStoreConfig = NavigationTileStoreConfiguration(
                apiKey = apiKey
            )
        )
    }

    /**
     * The SDK provides a [LocationProvider] interface that is used between different modules to get location updates.
     * This examples uses the default [LocationProvider].
     * Under the hood, the engine uses Android’s system location services.
     */
    private fun initLocationProvider() {
        locationProvider = DefaultLocationProviderFactory.create(context = applicationContext)
    }

    /**
     * Plans the route by initializing by using the online route planner and default route replanner.
     */
    private fun initRouting() {
        routePlanner = OnlineRoutePlanner.create(context = applicationContext, apiKey = apiKey)
    }

    /**
     * To use navigation in the application, start by by initialising the navigation configuration.
     */
    private fun initNavigation() {
        val configuration = Configuration(
            context = applicationContext,
            navigationTileStore = navigationTileStore,
            locationProvider = locationProvider,
            routePlanner = routePlanner,
            vehicleProvider = VehicleProviderFactory.create(vehicle = Vehicle.Car())
        )
        tomTomNavigation = OnlineTomTomNavigationFactory.create(configuration)
    }

    /**
     * The application must use the device’s location services, which requires the appropriate permissions.
     */
    private fun ensureLocationPermissions(onLocationPermissionsGranted: () -> Unit) {
        if (areLocationPermissionsGranted()) {
            onLocationPermissionsGranted()
        } else {
            requestLocationPermission(onLocationPermissionsGranted)
        }
    }

    /**
     * Manually enables the location marker.
     * It can be configured using the LocationMarkerOptions class.
     *
     * Read more about user location on the map in the Showing User Location guide.
     */
    private fun showUserLocation() {
        // zoom to current location at city level
        onLocationUpdateListener = OnLocationUpdateListener { location ->
            val locationMarker = LocationMarkerOptions(type = LocationMarkerOptions.Type.Pointer)
            tomTomMap.enableLocationMarker(locationMarker)
            tomTomMap.moveCamera(CameraOptions(location.position, zoom = 10.0))
            locationProvider.removeOnLocationUpdateListener(onLocationUpdateListener)
            navigationBinding.progressBarMapLoading.postDelayed({
                navigationBinding.progressBarMapLoading.visibility = View.GONE
            }, 2000)
        }
        locationProvider.addOnLocationUpdateListener(onLocationUpdateListener)

    }

    /**
     * In this example on planning a route, the origin is the user’s location and the destination is determined by the user selecting a location on the map.
     * Navigation is started once the user taps on the route.
     *
     * To mark the destination on the map, add the MapLongClickListener event handler to the map view.
     * To start navigation, add the addRouteClickListener event handler to the map view.
     */
    private fun setUpMapListeners() {
        tomTomMap.addMapLongClickListener(mapLongClickListener)
        tomTomMap.addRouteClickListener(routeClickListener)
    }

    /**
     * Used to calculate a route based on a selected location.
     * - The method removes all polygons, circles, routes, and markers that were previously added to the map.
     * - It then creates a route between the user’s location and the selected location.
     * - The method needs to return a boolean value when the callback is consumed.
     */
    private val mapLongClickListener = MapLongClickListener { geoPoint ->
        clearMap()
        calculateRouteTo(geoPoint)
        true
    }

    /**
     * Checks whether navigation is currently running.
     */
    private fun isNavigationRunning(): Boolean = tomTomNavigation.navigationSnapshot != null


    /**
     * Used to start navigation based on a tapped route, if navigation is not already running.
     * - Hide the location button
     * - Then start the navigation using the selected route.
     */
    private val routeClickListener = RouteClickListener {
        if (!isNavigationRunning()) {
            route?.let { route ->
                mapFragment.currentLocationButton.visibilityPolicy = VisibilityPolicy.Invisible
                navigationBinding.navigationFragmentContainer.visibility = View.VISIBLE
                startNavigation(route)
            }
        }
    }

    /**
     * Used to calculate a route using the following parameters:
     * - InstructionPhoneticsType - This specifies whether to include phonetic transcriptions in the response.
     * - ExtendedSections - This specifies whether to include extended guidance sections in the response, such as sections of type road shield, lane, and speed limit.
     */
    private fun calculateRouteTo(destination: GeoPoint) {
        val userLocation =
            tomTomMap.currentLocation?.position ?: return
        val itinerary = Itinerary(origin = userLocation, destination = destination)
        routePlanningOptions = RoutePlanningOptions(
            itinerary = itinerary,
            guidanceOptions = GuidanceOptions(
                phoneticsType = InstructionPhoneticsType.Ipa,
                extendedSections = ExtendedSections.All
            ),
            vehicle = Vehicle.Car()
        )
        routePlanner.planRoute(routePlanningOptions, routePlanningCallback)
    }

    /**
     * The RoutePlanningCallback itself has three methods.
     * - The `onFailure()` method is triggered if the request fails.
     * - The `onSuccess()` method returns RoutePlanningResponse containing the routing results.
     * - The `onRoutePlanned()` method is triggered when each route is successfully calculated.
     *
     * This example draws the first retrieved route on the map.
     * You can show the overview of the added routes using the TomTomMap.zoomToRoutes(Int) method.
     * Note that its padding parameter is expressed in pixels.
     */
    private val routePlanningCallback = object : RoutePlanningCallback {
        override fun onSuccess(result: RoutePlanningResponse) {
            route = result.routes.first()
            drawRoute(route!!)
            tomTomMap.zoomToRoutes(ZOOM_TO_ROUTE_PADDING)
        }

        override fun onFailure(failure: RoutingFailure) {
            Toast.makeText(this@NavigationActivity, failure.message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Used to draw route on the map
     */
    private fun drawRoute(route: Route) {
        val instructions = route.mapInstructions()
        val routeOptions = RouteOptions(
            geometry = route.geometry,
            destinationMarkerVisible = true,
            departureMarkerVisible = true,
            instructions = instructions,
            routeOffset = route.routePoints.map { it.routeOffset }
        )
        navigationRoute = route
        tomTomMap.addRoute(routeOptions)
    }

    /**
     * For the navigation use case, the instructions can be drawn on the route in form of arrows that indicate maneuvers.
     * To do this, map the Instruction object provided by routing to the Instruction object used by the map.
     * Note that during navigation, you need to update the progress property of the drawn route to display the next instructions.
     */
    private fun Route.mapInstructions(): List<Instruction> {
        val routeInstructions = legs.flatMap { routeLeg -> routeLeg.instructions }
        return routeInstructions.map {
            Instruction(
                routeOffset = it.routeOffset
            )
        }
    }

    /**
     * Used to start navigation by
     * - initializing the NavigationFragment to display the UI navigation information,
     * - passing the Route object along which the navigation will be done, and RoutePlanningOptions used during the route planning,
     * - handling the updates to the navigation states using the NavigationListener.
     * Note that you have to set the previously-created TomTom Navigation object to the NavigationFragment before using it.
     */

    private fun startNavigation(route: Route) {
        initNavigationFragment()
        // Update navigation with the vehicle used for route planning
        // This is the critical line that fixes the vehicle mismatch error
        updateNavigationWithVehicle(routePlanningOptions.vehicle)
        navigationFragment.setTomTomNavigation(tomTomNavigation)
        val routePlan = RoutePlan(route, routePlanningOptions)
        navigationFragment.startNavigation(routePlan)
        navigationFragment.addNavigationListener(navigationListener)
        tomTomNavigation.addProgressUpdatedListener(progressUpdatedListener)
        tomTomNavigation.addActiveRouteChangedListener(activeRouteChangedListener)
    }

    /**
     * Updates the navigation configuration with the vehicle used for route planning
     * This is critical for making truck navigation work correctly
     */
    private fun updateNavigationWithVehicle(vehicle: Vehicle) {
        try {
            // Re-initialize TomTomNavigation with the correct vehicle
            val configuration = Configuration(
                context = applicationContext,
                navigationTileStore = navigationTileStore,
                locationProvider = locationProvider,
                routePlanner = routePlanner,
                vehicleProvider = VehicleProviderFactory.create(vehicle = vehicle)
            )

            // Close the existing navigation instance first
            if (::tomTomNavigation.isInitialized) {
                tomTomNavigation.close()
            }

            // Create new instance with correct vehicle
            tomTomNavigation = OnlineTomTomNavigationFactory.create(configuration)

            // Update the navigation fragment with the new navigation instance
            if (::navigationFragment.isInitialized) {
                navigationFragment.setTomTomNavigation(tomTomNavigation)
            }

            // Log the vehicle type that's being used
            val vehicleType = when (vehicle) {
                is Vehicle.Car -> "Car"
                is Vehicle.Truck -> "Truck"
                else -> vehicle.javaClass.simpleName
            }
            Log.d("MainActivity", "Updated navigation with vehicle: $vehicleType")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating navigation with vehicle", e)
            Toast.makeText(
                this,
                "Error updating vehicle configuration: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Handle the updates to the navigation states using the NavigationListener
     * - Use CameraChangeListener to observe camera tracking mode and detect if the camera is locked on the chevron. If the user starts to move the camera, it will change and you can adjust the UI to suit.
     * - Use the SimulationLocationProvider for testing purposes.
     * - Once navigation is started, the camera is set to follow the user position, and the location indicator is changed to a chevron. To match raw location updates to the routes, create LocationProvider using MapMatchedLocationProviderFactory and set it to the TomTomMap.
     * - Set the bottom padding on the map. The padding sets a safe area of the MapView in which user interaction is not received. It is used to uncover the chevron in the navigation panel.
     */
    private val navigationListener = object : NavigationFragment.NavigationListener {
        override fun onStarted() {
            tomTomMap.addCameraChangeListener(cameraChangeListener)
            tomTomMap.cameraTrackingMode = CameraTrackingMode.FollowRouteDirection
            tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.Chevron))
            setMapMatchedLocationProvider()
            setSimulationLocationProviderToNavigation(route!!)
            setMapNavigationPadding()
        }

        override fun onStopped() {
            stopNavigation()

        }
    }

    /**
     * Used to initialize the NavigationFragment to display the UI navigation information,
     */
    private fun initNavigationFragment() {
        if (!::navigationFragment.isInitialized) {
            navigationFragment = NavigationFragment.newInstance(
                NavigationUiOptions(
                    keepInBackground = true
                )
            )
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.navigation_fragment_container, navigationFragment)
            .commitNow()
    }

    private val progressUpdatedListener = ProgressUpdatedListener {
        if(tomTomMap.routes.isNotEmpty())
        {
            tomTomMap.routes.first().progress = it.distanceAlongRoute
        }
    }

    private val activeRouteChangedListener = ActiveRouteChangedListener { route ->
        tomTomMap.removeRoutes()
        drawRoute(route)
    }

    /**
     * Use the SimulationLocationProvider for testing purposes.
     */
    private fun setSimulationLocationProviderToNavigation(route: Route) {
        val routeGeoLocations = route.geometry.map { GeoLocation(it) }
        val simulationStrategy = InterpolationStrategy(routeGeoLocations)
        locationProvider = SimulationLocationProvider.create(strategy = simulationStrategy, adjustToCurrentTime = true)
        tomTomNavigation.locationProvider = locationProvider
        locationProvider.enable()
    }

    /**
     * Stop the navigation process using NavigationFragment.
     * This hides the UI elements and calls the TomTomNavigation.stop() method.
     * Don’t forget to reset any map settings that were changed, such as camera tracking, location marker, and map padding.
     */
    private fun stopNavigation() {
        if (::navigationFragment.isInitialized) {
            navigationFragment.stopNavigation()
            mapFragment.currentLocationButton.visibilityPolicy =
                VisibilityPolicy.InvisibleWhenRecentered
            tomTomMap.removeCameraChangeListener(cameraChangeListener)
            tomTomMap.cameraTrackingMode = CameraTrackingMode.None
            tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.Pointer))
            resetMapPadding()
            navigationFragment.removeNavigationListener(navigationListener)
            tomTomNavigation.removeProgressUpdatedListener(progressUpdatedListener)
            tomTomNavigation.removeActiveRouteChangedListener(activeRouteChangedListener)
        }

    }

    /**
     * Set the bottom padding on the map. The padding sets a safe area of the MapView in which user interaction is not received. It is used to uncover the chevron in the navigation panel.
     */
    private fun setMapNavigationPadding() {
        val paddingBottom = resources.getDimensionPixelOffset(R.dimen.map_padding_bottom)
        val padding = Padding(0, 0, 0, paddingBottom)
        tomTomMap.setPadding(padding)
    }

    private fun resetMapPadding() {
        tomTomMap.setPadding(Padding(0, 0, 0, 0))
    }

    /**
     * Once navigation is started, the camera is set to follow the user position, and the location indicator is changed to a chevron.
     * To match raw location updates to the routes, create a LocationProvider instance using MapMatchedLocationProviderFactory and set it to the TomTomMap.
     */
    private fun setMapMatchedLocationProvider() {
        val mapMatchedLocationProvider = MapMatchedLocationProviderFactory.create(tomTomNavigation)
        tomTomMap.setLocationProvider(mapMatchedLocationProvider)
        mapMatchedLocationProvider.enable()
    }

    /**
     *
     * The method removes all polygons, circles, routes, and markers that were previously added to the map.
     */
    private fun clearMap() {
        tomTomMap.clear()
    }

    private val cameraChangeListener = CameraChangeListener {
        val cameraTrackingMode = tomTomMap.cameraTrackingMode
        if (cameraTrackingMode == CameraTrackingMode.FollowRouteDirection) {
            navigationFragment.navigationView.showSpeedView()
        } else {
            navigationFragment.navigationView.hideSpeedView()
        }
    }

    /**
     * Method to verify permissions:
     * - [Manifest.permission.ACCESS_FINE_LOCATION]
     * - [Manifest.permission.ACCESS_COARSE_LOCATION]
     */
    private fun areLocationPermissionsGranted() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED


    private fun requestLocationPermission(onLocationPermissionsGranted: () -> Unit) =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                && permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                onLocationPermissionsGranted()
            } else {
                Toast.makeText(
                    this, "location_permission_denied", Toast.LENGTH_SHORT
                ).show()
            }
        }.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )


    private fun loadRoutesFromAssets() {
        try {
            routesList
            val inputStream = assets.open("Next_Billion_response_4.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val response = gson.fromJson(reader, NextBillionResponse::class.java)

            routesList = response.result.routes

            val routeNames = mutableListOf("Select route")
            // Update spinner
            routeNames.addAll(
                routesList.mapIndexed { index, route ->
                    "Route ${index + 1}: ${route.vehicle}"
                }
            )

            val adapter = ArrayAdapter(this, R.layout.spinner_item, routeNames)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            navigationBinding.spinnerRoutes.adapter = adapter
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading routes: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearRoutes() {
        clearMap()
        stopNavigation()
        navigationBinding.navigationFragmentContainer.visibility = View.INVISIBLE
        showUserLocation()
    }


    override fun onDestroy() {
        tomTomMap.setLocationProvider(null)
        if (::navigationFragment.isInitialized) {
            supportFragmentManager.beginTransaction().remove(navigationFragment)
                .commitNowAllowingStateLoss()
        }
        super.onDestroy()
        tomTomNavigation.close()
        routePlanner.close()
        navigationTileStore.close()
        locationProvider.close()
        webSocket.close(1000, "Activity Destroyed")
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter("com.example.poconnbandtomtom.START_NAVIGATION")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // New overload: registerReceiver(receiver, filter, flags)
            registerReceiver(
                navigationReceiver,
                intentFilter,
                Context.RECEIVER_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(navigationReceiver, intentFilter)
        }
    }

    private val navigationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isRoute = intent?.getBooleanExtra("isRoute", false) ?: false
            val routeIndex = intent?.getIntExtra("route", -1) ?: -1
            println("🔥 Broadcast received: isRoute=$isRoute, routeIndex=$routeIndex")

            if (isRoute && routeIndex >= 0 && routesList.isNotEmpty()) {
                navigationBinding.spinnerRoutes.setSelection(routeIndex)
                navigationBinding.btnDelivered.visibility = View.INVISIBLE
                navigationBinding.btnStartNavigation.visibility = View.VISIBLE
                navigationBinding.btnClearRoutes.visibility = View.VISIBLE
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(navigationReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered — safe to ignore
        }
    }

    companion object {
        private const val ZOOM_TO_ROUTE_PADDING = 100
    }
}