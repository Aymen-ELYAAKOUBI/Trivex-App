package com.example.suiverliverson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.suiverliverson.ui.theme.Blue
import com.example.suiverliverson.ui.theme.Green
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun MapScreen(
    deliveryManager: DeliveryManager,
    onDeliveryComplete: () -> Unit
) {
    val activeOrder = deliveryManager.currentActiveOrder
    val darkMode = LocalDarkMode.current

    val driverStartPoint = deliveryManager.activeRouteStartPoint
        ?: casablancaDriverStartPoints.first()
    val pickupPoint = activeOrder?.pickupLocation ?: casablancaPickupPlaces.first().point
    val deliveryPoint = activeOrder?.deliveryLocation ?: casablancaDeliveryPlaces.first().point

    var roadRoute by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var routeLoading by remember { mutableStateOf(false) }
    var routeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(driverStartPoint, pickupPoint, deliveryPoint) {
        routeLoading = true
        routeError = null
        try {
            roadRoute = fetchRoadRoute(driverStartPoint, pickupPoint, deliveryPoint)
        } catch (exception: kotlinx.coroutines.CancellationException) {
            throw exception
        } catch (exception: Exception) {
            roadRoute = emptyList()
            routeError = exception.message ?: "Unable to load the road route."
        } finally {
            routeLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        InteractiveOpenMapView(
            driverPoint = driverStartPoint,
            pickupPoint = pickupPoint,
            deliveryPoint = deliveryPoint,
            roadRoute = roadRoute,
            currentBikePoint = driverStartPoint,
            activeOrder = activeOrder,
            darkMode = darkMode,
            routeLoading = routeLoading,
            routeError = routeError
        )

        if (routeLoading) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = "Finding an OpenStreetMap road route…",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 12.sp
                )
            }
        }

        activeOrder?.let { order ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = order.customerName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Green)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$${"%.2f".format(order.price)}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveOpenMapView(
    driverPoint: GeoPoint,
    pickupPoint: GeoPoint,
    deliveryPoint: GeoPoint,
    roadRoute: List<GeoPoint>,
    currentBikePoint: GeoPoint,
    activeOrder: Order?,
    darkMode: Boolean,
    routeLoading: Boolean,
    routeError: String?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(context) {
        MapLibre.getInstance(context)
        MapView(context).also { it.onCreate(null) }
    }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleLoaded by remember { mutableStateOf(false) }
    val styleUrl = if (darkMode) {
        "https://tiles.openfreemap.org/styles/dark"
    } else {
        "https://tiles.openfreemap.org/styles/liberty"
    }
    val routePoints = roadRoute.map { LatLng(it.latitude, it.longitude) }
    val visiblePoints = routePoints.ifEmpty {
        listOf(driverPoint, pickupPoint, deliveryPoint).map { LatLng(it.latitude, it.longitude) }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapLibreMap, styleUrl) {
        val map = mapLibreMap ?: return@LaunchedEffect
        styleLoaded = false
        map.setStyle(Style.Builder().fromUri(styleUrl)) {
            styleLoaded = true
        }
    }

    LaunchedEffect(mapLibreMap, styleLoaded, driverPoint, pickupPoint, deliveryPoint, currentBikePoint, activeOrder, roadRoute) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!styleLoaded) return@LaunchedEffect

        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(LatLng(currentBikePoint.latitude, currentBikePoint.longitude))
                .title("Courier")
        )
        map.addMarker(
            MarkerOptions()
                .position(LatLng(pickupPoint.latitude, pickupPoint.longitude))
                .title(activeOrder?.pickup ?: "Pickup")
        )
        map.addMarker(
            MarkerOptions()
                .position(LatLng(deliveryPoint.latitude, deliveryPoint.longitude))
                .title(activeOrder?.client ?: "Delivery")
        )
        if (routePoints.size > 1) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(android.graphics.Color.rgb(66, 133, 244))
                    .width(6f)
            )
        }

        val bounds = LatLngBounds.Builder().apply {
            visiblePoints.forEach(::include)
        }.build()
        mapView.post {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.also { view ->
                    view.getMapAsync { map ->
                        map.uiSettings.isCompassEnabled = true
                        mapLibreMap = map
                    }
                }
            },
            update = {}
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        routeLoading -> "Loading OSM road route…"
                        routeError != null -> "Road route unavailable"
                        else -> "OpenStreetMap • Casablanca"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = routeError
                        ?: if (activeOrder != null) "Route to ${activeOrder.client}" else "Delivery area • Ready",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { mapLibreMap?.animateCamera(CameraUpdateFactory.zoomIn()) },
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { mapLibreMap?.animateCamera(CameraUpdateFactory.zoomOut()) },
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = {
                    mapLibreMap?.let { map ->
                        val bounds = LatLngBounds.Builder().apply {
                            visiblePoints.forEach(::include)
                        }.build()
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                    }
                },
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter map", modifier = Modifier.size(20.dp))
            }
        }
    }
}
