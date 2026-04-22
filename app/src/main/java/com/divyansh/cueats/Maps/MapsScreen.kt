package com.divyansh.cueats.Maps

import  android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.divyansh.cueats.ShopMenuRoute
import com.divyansh.cueats.ShopsScreen.Shop
import com.divyansh.cueats.ShopsScreen.ShopViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.RectF
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import org.osmdroid.views.overlay.Overlay

// Shop coordinates mapping
val shopCoordinates = mapOf(
    "Punjabi Rasoi" to Pair(30.764158, 76.575615),
    "Catch Up Cafe" to Pair(30.764056, 76.575642),
)

class EnhancedShopOverlay : Overlay() {
    private val shopMarkers = mutableListOf<ShopMarker>()
    private var selectedShopId: String? = null
    private var onShopClick: ((Shop) -> Unit)? = null

    data class ShopMarker(
        val shop: Shop,
        val latitude: Double,
        val longitude: Double,
        val isSelected: Boolean = false
    )

    fun addShopMarker(shop: Shop, latitude: Double, longitude: Double) {
        shopMarkers.add(ShopMarker(shop, latitude, longitude))
    }

    fun clearMarkers() {
        shopMarkers.clear()
    }

    fun setSelectedShop(shopId: String?) {
        selectedShopId = shopId
    }

    fun setOnShopClickListener(listener: (Shop) -> Unit) {
        onShopClick = listener
    }

    // Add touch detection for marker clicks
    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
        if (e == null || mapView == null) return false

        val projection = mapView.projection
        val touchPoint = android.graphics.Point(e.x.toInt(), e.y.toInt())

        // Check if touch point is within any marker's bounds
        for (marker in shopMarkers) {
            val geoPoint = GeoPoint(marker.latitude, marker.longitude)
            val markerPoint = projection.toPixels(geoPoint, null)

            // Calculate marker size (same as in draw method)
            val isSelected = marker.shop.id == selectedShopId
            val markerSize = if (isSelected) 120f else 100f
            val clickRadius = markerSize/2 + 20f // Slightly larger for easier clicking

            // Check if touch is within marker bounds
            val distance = kotlin.math.sqrt(
                (touchPoint.x - markerPoint.x).toDouble() * (touchPoint.x - markerPoint.x).toDouble() +
                        (touchPoint.y - (markerPoint.y - markerSize/2)).toDouble() * (touchPoint.y - (markerPoint.y - markerSize/2)).toDouble()
            )

            if (distance <= clickRadius) {
                // Marker was clicked - trigger callback
                onShopClick?.invoke(marker.shop)
                return true // Consume the touch event
            }
        }

        return false // Touch not handled
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (shadow || canvas == null || mapView == null) return

        val projection = mapView.projection

        shopMarkers.forEach { marker ->
            val isSelected = marker.shop.id == selectedShopId
            drawCustomShopMarker(canvas, projection, marker, isSelected, mapView)
        }
    }

    private fun drawCustomShopMarker(
        canvas: Canvas,
        projection: org.osmdroid.views.Projection,
        marker: ShopMarker,
        isSelected: Boolean,
        mapView: MapView
    ) {
        val geoPoint = GeoPoint(marker.latitude, marker.longitude)
        val point = projection.toPixels(geoPoint, null)

        // Base colors
        val primaryColor = if (isSelected)
            android.graphics.Color.parseColor("#FF6B01")
        else
            android.graphics.Color.parseColor("#4CAF50")

        val shadowColor = android.graphics.Color.parseColor("#40000000")
        val textColor = android.graphics.Color.WHITE
        val backgroundColor = android.graphics.Color.WHITE

        // Marker dimensions
        val markerSize = if (isSelected) 120f else 100f
        val bubbleHeight = 40f
        val bubbleWidth = markerSize * 1.2f

        // Shadow paint
        val shadowPaint = Paint().apply {
            color = shadowColor
            isAntiAlias = true
            setShadowLayer(8f, 0f, 4f, shadowColor)
        }

        // Main marker paint
        val markerPaint = Paint().apply {
            color = primaryColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        // Border paint
        val borderPaint = Paint().apply {
            color = backgroundColor
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = if (isSelected) 6f else 4f
        }

        // Text paints
        val shopNamePaint = Paint().apply {
            color = textColor
            textSize = if (isSelected) 32f else 28f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val iconPaint = Paint().apply {
            color = textColor
            textSize = if (isSelected) 36f else 32f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val bubblePaint = Paint().apply {
            color = primaryColor
            isAntiAlias = true
            alpha = 240
        }

        val bubbleTextPaint = Paint().apply {
            color = textColor
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Draw shadow
        canvas.drawCircle(
            point.x + 3f,
            point.y - markerSize/2 + 3f,
            markerSize/2,
            shadowPaint
        )

        // Draw main marker circle
        canvas.drawCircle(
            point.x.toFloat(),
            point.y - markerSize/2,
            markerSize/2,
            markerPaint
        )

        // Draw border
        canvas.drawCircle(
            point.x.toFloat(),
            point.y - markerSize/2,
            markerSize/2,
            borderPaint
        )

        // Draw food icon (🍴)
        val foodIcon = "🍴"
        canvas.drawText(
            foodIcon,
            point.x.toFloat(),
            point.y - markerSize/2 + iconPaint.textSize/3,
            iconPaint
        )

        // Draw shop name bubble above marker
        val bubbleRect = RectF(
            point.x - bubbleWidth/2,
            point.y - markerSize - bubbleHeight - 10f,
            point.x + bubbleWidth/2,
            point.y - markerSize - 10f
        )

        // Draw bubble with rounded corners
        canvas.drawRoundRect(bubbleRect, 20f, 20f, bubblePaint)

        // Draw bubble tail (triangle pointing down)
        val bubbleTail = Path().apply {
            moveTo(point.x - 15f, point.y - markerSize - 10f)
            lineTo(point.x.toFloat(), point.y - markerSize + 5f)
            lineTo(point.x + 15f, point.y - markerSize - 10f)
            close()
        }
        canvas.drawPath(bubbleTail, bubblePaint)

        // Draw shop name in bubble
        val shopName = if (marker.shop.name.length > 12) {
            "${marker.shop.name.take(10)}..."
        } else {
            marker.shop.name
        }

        canvas.drawText(
            shopName,
            point.x.toFloat(),
            bubbleRect.centerY() + bubbleTextPaint.textSize/3,
            bubbleTextPaint
        )

        // Pulsing effect for selected marker
        if (isSelected) {
            val pulsePaint = Paint().apply {
                color = primaryColor
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 3f
                alpha = 100
            }

            // Draw pulsing rings
            for (i in 1..3) {
                canvas.drawCircle(
                    point.x.toFloat(),
                    point.y - markerSize/2,
                    markerSize/2 + (i * 20f),
                    pulsePaint.apply { alpha = 100 - (i * 30) }
                )
            }
        }
    }
}
class SimpleTextOverlay : Overlay() {
    private val labels = mutableListOf<MapLabel>()

    data class MapLabel(
        val latitude: Double,
        val longitude: Double,
        val text: String
    )

    fun addLabel(latitude: Double, longitude: Double, text: String) {
        labels.add(MapLabel(latitude, longitude, text))
    }

    fun clearLabels() {
        labels.clear()
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (shadow || canvas == null || mapView == null) return

        val projection = mapView.projection

        // Paint for text background with gradient effect
        val backgroundPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#F0F8FF")
            isAntiAlias = true
            alpha = 220
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.parseColor("#40000000"))
        }

        // Paint for border
        val borderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#4A90E2")
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Paint for text
        val textPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#2C3E50")
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        labels.forEach { label ->
            val geoPoint = GeoPoint(label.latitude, label.longitude)
            val point = projection.toPixels(geoPoint, null)

            // Calculate text size
            val textWidth = textPaint.measureText(label.text)
            val textHeight = textPaint.textSize

            // Draw background with rounded corners
            val padding = 12f
            val rect = RectF(
                point.x - textWidth/2 - padding,
                point.y - textHeight/2 - padding,
                point.x + textWidth/2 + padding,
                point.y + textHeight/2 + padding
            )

            canvas.drawRoundRect(rect, 8f, 8f, backgroundPaint)
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

            // Draw text
            canvas.drawText(
                label.text,
                point.x.toFloat(),
                point.y.toFloat() + textHeight/4,
                textPaint
            )
        }
    }
}

fun addEnhancedLabels(mapView: MapView) {
    // Remove any existing text labels
    mapView.overlays.removeAll { it is SimpleTextOverlay }

    // Create new text overlay
    val textOverlay = SimpleTextOverlay()

    // Add building labels with enhanced styling
    textOverlay.addLabel(30.764433, 76.575119, "NC-2")
    textOverlay.addLabel(30.764140, 76.575240, "NC-1")
    textOverlay.addLabel(30.764071, 76.574432, "NC-3")
    textOverlay.addLabel(30.764497, 76.574578, "NC-4")
    textOverlay.addLabel(30.764530, 76.573579, "NC-6")
    textOverlay.addLabel(30.764197, 76.573633, "NC-5")
    textOverlay.addLabel(30.764169, 76.572822, "Zakir-A")
    textOverlay.addLabel(30.764063, 76.571723, "Zakir-B")
    textOverlay.addLabel(30.763615, 76.571492,"Zakir-D")
    textOverlay.addLabel(30.763562, 76.572828,"Zakir-C")

    // Add the text overlay to map
    mapView.overlays.add(textOverlay)
    mapView.invalidate()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedCampusMap(
    context: Context,
    navController: NavController,
    shopViewModel: ShopViewModel = viewModel()
) {
    var selectedShop by remember { mutableStateOf<Shop?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showDirections by remember { mutableStateOf(false) }
    var isMapReady by remember { mutableStateOf(false) }
    var showLabels by remember { mutableStateOf(true) }

    val shops by shopViewModel.shops.observeAsState(emptyList())

    val shopLocations = remember(shops) {
        shops.mapNotNull { shop ->
            val coords = shopCoordinates[shop.id]
                ?: shopCoordinates[shop.name]
                ?: shopCoordinates.entries.find {
                    it.key.equals(shop.name, ignoreCase = true)
                }?.value

            coords?.let { (lat, lng) ->
                shop to GeoPoint(lat, lng)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Campus Map",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "${shopLocations.size} shops located",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showLabels = !showLabels
                        mapView?.let { map ->
                            if (isMapReady) {
                                if (showLabels) {
                                    addEnhancedLabels(map)
                                } else {
                                    map.overlays.removeAll { it is SimpleTextOverlay }
                                    map.invalidate()
                                }
                            }
                        }
                    }) {
                        Icon(
                            if (showLabels) Icons.Default.Label else Icons.Default.LabelOff,
                            contentDescription = "Toggle Labels",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = {
                        mapView?.let { map ->
                            if (isMapReady) {
                                map.controller.animateTo(GeoPoint(30.768766, 76.575355))
                                map.controller.setZoom(16.0)
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Center Map",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF6B01),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Enhanced Map View
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(
                        ctx,
                        ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
                    )

                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(18.0)
                        controller.setCenter(GeoPoint(30.768766, 76.575355))

                        post {
                            try {
                                addEnhancedShopMarkers(this, shopLocations, selectedShop?.id) { shop ->
                                    selectedShop = shop
                                }

                                if (showLabels) {
                                    addEnhancedLabels(this)
                                }

                                isMapReady = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        mapView = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray.copy(alpha = 0.1f)),
                update = { view ->
                    if (isMapReady) {
                        try {
                            view.overlays.removeAll { it !is SimpleTextOverlay }
                            addEnhancedShopMarkers(view, shopLocations, selectedShop?.id) { shop ->
                                selectedShop = shop
                            }

                            if (showLabels) {
                                addEnhancedLabels(view)
                            }

                            view.invalidate()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )

            // Enhanced Shop Cards Overlay
            if (shopLocations.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .zIndex(10f),
                    shadowElevation = 16.dp,
                    color = Color.White,
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                ) {
                    Column {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            items(shopLocations) { (shop, _) ->
                                EnhancedShopMapCard(
                                    shop = shop,
                                    isSelected = shop.id == selectedShop?.id,
                                    onClick = {
                                        selectedShop = shop
                                        mapView?.let { map ->
                                            val coords = shopCoordinates[shop.id]
                                                ?: shopCoordinates[shop.name]
                                                ?: shopCoordinates.entries.find {
                                                    it.key.equals(shop.name, ignoreCase = true)
                                                }?.value

                                            coords?.let { (lat, lng) ->
                                                map.controller.animateTo(GeoPoint(lat, lng))
                                                map.controller.setZoom(19.0)
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Divider(
                            color = Color.Gray.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )
                    }
                }
            }

            // Enhanced selected shop details overlay
            selectedShop?.let { shop ->
                EnhancedShopInfoCard(
                    shop = shop,
                    onDismiss = { selectedShop = null },
                    onDirections = { showDirections = true },
                    onNavigateToMenu = {
                        navController.navigate(ShopMenuRoute(shopId = shop.id))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .zIndex(15f)
                )
            }
        }
    }

    // Enhanced directions bottom sheet
    if (showDirections && selectedShop != null) {
        DirectionsBottomSheet(
            shop = selectedShop!!,
            onDismiss = { showDirections = false },
            context = context
        )
    }
}

// Enhanced ShopMapCard with modern design
@Composable
fun EnhancedShopMapCard(
    shop: Shop,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardColors = if (isSelected) {
        CardDefaults.cardColors(
            containerColor = Color(0xFFFF6B01)
        )
    } else {
        CardDefaults.cardColors(
            containerColor = Color.White
        )
    }

    val elevation = CardDefaults.cardElevation(
        defaultElevation = if (isSelected) 16.dp else 6.dp
    )

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(90.dp)
            .clickable { onClick() },
        elevation = elevation,
        colors = cardColors,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF8A3D))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section with enhanced category badge
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFFFF6B01).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = "Food",
                                tint = if (isSelected) Color.White else Color(0xFFFF6B01),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Food",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else Color(0xFFFF6B01),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Enhanced Shop Name
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) Color.White else Color.Black,
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                )
            }

            // Enhanced bottom section
            Column {
                Text(
                    text = shop.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )

                // Enhanced delivery status
                if (shop.hasDelivery) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Surface(
                            color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DeliveryDining,
                                    contentDescription = "Delivery",
                                    tint = if (isSelected) Color.White else Color(0xFF4CAF50),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Delivery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) Color.White else Color(0xFF4CAF50),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to add enhanced markers to map
private fun addEnhancedShopMarkers(
    mapView: MapView,
    shopLocations: List<Pair<Shop, GeoPoint>>,
    selectedShopId: String?,
    onMarkerClick: (Shop) -> Unit
) {
    // Remove existing shop overlays
    mapView.overlays.removeAll { it is EnhancedShopOverlay }

    // Create new enhanced shop overlay
    val shopOverlay = EnhancedShopOverlay()
    shopOverlay.setSelectedShop(selectedShopId)
    shopOverlay.setOnShopClickListener(onMarkerClick)

    // Add all shop markers
    shopLocations.forEach { (shop, geoPoint) ->
        shopOverlay.addShopMarker(shop, geoPoint.latitude, geoPoint.longitude)
    }

    // Add the overlay to map
    mapView.overlays.add(shopOverlay)
    mapView.invalidate()
}

@Composable
fun EnhancedShopInfoCard(
    shop: Shop,
    onDismiss: () -> Unit,
    onDirections: () -> Unit,
    onNavigateToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Enhanced header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFFF6B01).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6B01),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = shop.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFF6B01),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.1f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            // Enhanced delivery status
            if (shop.hasDelivery) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DeliveryDining,
                            contentDescription = "Delivery",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Free Delivery Available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDirections,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF6B01),
                        containerColor = Color(0xFFFF6B01).copy(alpha = 0.05f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp, Color(0xFFFF6B01)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Get Directions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = onNavigateToMenu,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B01)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "View Menu",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectionsBottomSheet(
    shop: Shop,
    onDismiss: () -> Unit,
    context: Context
) {
    // Get coordinates for the selected shop
    val shopCoords = shopCoordinates[shop.id]
        ?: shopCoordinates[shop.name]
        ?: shopCoordinates.entries.find {
            it.key.equals(shop.name, ignoreCase = true)
        }?.value

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .padding(top = 8.dp),
                color = Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(2.dp)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color(0xFFFF6B01).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color(0xFFFF6B01),
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Get Directions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "to ${shop.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Enhanced Google Maps option
            DirectionOptionCard(
                title = "Open in Google Maps",
                subtitle = "Navigate with turn-by-turn directions",
                icon = Icons.Default.Map,
                iconColor = Color(0xFF4285F4),
                onClick = {
                    val uri = if (shopCoords != null) {
                        "geo:${shopCoords.first},${shopCoords.second}?q=${Uri.encode(shop.name)}"
                    } else {
                        "geo:0,0?q=${Uri.encode("${shop.name}, ${shop.address}")}"
                    }

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(uri)
                        setPackage("com.google.android.apps.maps")
                    }

                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(uri)
                        }
                        context.startActivity(fallbackIntent)
                    }
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Enhanced Walking directions
            DirectionOptionCard(
                title = "Walking Directions",
                subtitle = "Get pedestrian-friendly route",
                icon = Icons.Default.DirectionsWalk,
                iconColor = Color(0xFF34A853),
                onClick = {
                    val uri = if (shopCoords != null) {
                        "google.navigation:q=${shopCoords.first},${shopCoords.second}&mode=w"
                    } else {
                        "google.navigation:q=${Uri.encode(shop.address)}&mode=w"
                    }

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(uri)
                        setPackage("com.google.android.apps.maps")
                    }

                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallbackUri = if (shopCoords != null) {
                            "geo:${shopCoords.first},${shopCoords.second}?q=${Uri.encode(shop.name)}"
                        } else {
                            "geo:0,0?q=${Uri.encode("${shop.name}, ${shop.address}")}"
                        }
                        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(fallbackUri)
                        }
                        context.startActivity(fallbackIntent)
                    }
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DirectionOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = Color(0xFFFF6B01),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFAFA)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = iconColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Surface(
                color = Color.Gray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp)
                )
            }
        }
    }
}