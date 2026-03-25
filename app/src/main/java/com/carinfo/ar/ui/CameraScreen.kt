package com.carinfo.ar.ui

import android.app.Activity
import android.Manifest
import android.util.Log
import com.carinfo.ar.BuildConfig
import android.content.pm.PackageManager
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.carinfo.ar.ads.AdManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.carinfo.ar.camera.FrameMotionTracker
import com.carinfo.ar.data.ScanHistory
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.carinfo.ar.camera.PlateAnalyzer
import com.carinfo.ar.data.SupportedCountry
import com.carinfo.ar.data.UserPreferences
import com.carinfo.ar.data.VehicleCache
import com.carinfo.ar.data.model.VehicleInfo
import com.carinfo.ar.ui.components.CountryIndicator
import com.carinfo.ar.ui.components.ScanEffect
import com.carinfo.ar.ui.components.ViewfinderOverlay
import com.carinfo.ar.ui.theme.BrandPrimary
import com.carinfo.ar.ui.theme.GlassOverlay
import com.carinfo.ar.util.SoundManager
import androidx.compose.ui.res.stringResource
import com.carinfo.ar.R
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Safe plate similarity check for grouping OCR variants.
 * - Same length: allows up to 2-char difference (handles OCR misreads)
 * - Length differs by exactly 1: only if shorter is suffix/prefix of longer (handles OCR adding/dropping a digit)
 * - Length differs by 2+: never similar (prevents cross-plate grouping)
 */
private fun isSimilarPlate(a: String, b: String): Boolean {
    if (a.length == b.length) {
        return a.zip(b).count { (x, y) -> x == y } >= (a.length - 2)
    }
    if (abs(a.length - b.length) == 1) {
        val longer = if (a.length > b.length) a else b
        val shorter = if (a.length > b.length) b else a
        return longer.endsWith(shorter) || longer.startsWith(shorter)
    }
    return false
}

data class PlateOverlayState(
    val plateNumber: String,
    val screenX: Float = 0f,
    val screenY: Float = 0f,
    val vehicleInfo: VehicleInfo? = null,
    val isLoading: Boolean = true,
    val lastSeenTime: Long = System.currentTimeMillis()
)

@Composable
fun CameraScreen(onOpenSettings: () -> Unit = {}, onOpenHistory: () -> Unit = {}) {
    val context = LocalContext.current
    val selectedCountryCode by UserPreferences.getSelectedCountry(context).collectAsState(initial = null)
    val country = selectedCountryCode?.let { SupportedCountry.fromCode(it) }
        ?: SupportedCountry.fromLocale()

    if (country == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.error_country_not_supported), style = MaterialTheme.typography.bodyLarge, color = Color(0xFFFF6B6B))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.error_supported_countries), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val adsRemoved by com.carinfo.ar.ads.BillingManager.adsRemoved.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val overlayStates = remember { mutableStateMapOf<String, PlateOverlayState>() }
    val countryRef = remember { mutableStateOf(country ?: SupportedCountry.ISRAEL) }
    countryRef.value = country ?: SupportedCountry.ISRAEL
    val knownPlates = remember { mutableSetOf<String>() }
    // Track exact OCR readings — each exact string counted separately
    val plateExactCounts = remember { mutableMapOf<String, Int>() }
    val activity = context as? Activity

    var viewWidth by remember { mutableIntStateOf(1) }
    var viewHeight by remember { mutableIntStateOf(1) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualPlateText by remember { mutableStateOf("") }

    // Sync sound setting
    val soundEnabled by UserPreferences.isSoundEnabled(context).collectAsState(initial = true)
    LaunchedEffect(soundEnabled) { SoundManager.soundEnabled = soundEnabled }

    // DVLA API key is hardcoded in VehicleCache

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current

            AndroidView(
                factory = { ctx ->
                    var camera: Camera? = null

                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                            viewWidth = right - left
                            viewHeight = bottom - top
                        }

                        // Pinch to zoom
                        val scaleDetector = ScaleGestureDetector(ctx,
                            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                override fun onScale(detector: ScaleGestureDetector): Boolean {
                                    val cam = camera ?: return true
                                    val currentZoom = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                                    cam.cameraControl.setZoomRatio(currentZoom * detector.scaleFactor)
                                    return true
                                }
                            })

                        // Double tap to toggle zoom
                        val gestureDetector = GestureDetector(ctx,
                            object : GestureDetector.SimpleOnGestureListener() {
                                override fun onDoubleTap(e: MotionEvent): Boolean {
                                    val cam = camera ?: return true
                                    val state = cam.cameraInfo.zoomState.value ?: return true
                                    val current = state.zoomRatio
                                    val min = state.minZoomRatio
                                    val max = state.maxZoomRatio
                                    val target = if (current > min + 0.1f) min else (max * 0.5f).coerceAtMost(max)
                                    cam.cameraControl.setZoomRatio(target)
                                    return true
                                }
                            })

                        setOnTouchListener { _, event ->
                            scaleDetector.onTouchEvent(event)
                            gestureDetector.onTouchEvent(event)
                            true
                        }
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val resolutionSelector = ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build().also {
                                it.setAnalyzer(
                                    ContextCompat.getMainExecutor(ctx),
                                    PlateAnalyzer(
                                        countryProvider = { countryRef.value },
                                        onPlatesDetected = { plates, imgW, imgH ->
                                            val now = System.currentTimeMillis()

                                            for (plate in plates) {
                                                // Count exact readings (no fuzzy grouping)
                                                val count = (plateExactCounts[plate.plateNumber] ?: 0) + 1
                                                plateExactCounts[plate.plateNumber] = count

                                                Log.d("VoteSystem", "Plate: ${plate.plateNumber}, exact count: $count")

                                                // Need at least 3 identical readings (or already known from cache)
                                                if (count < 3 && !VehicleCache.isKnown(plate.plateNumber)) continue

                                                // Skip if already showing this plate or a similar one
                                                if (overlayStates.containsKey(plate.plateNumber)) continue
                                                val similarKey = overlayStates.keys.find { existingPlate ->
                                                    isSimilarPlate(existingPlate, plate.plateNumber)
                                                }
                                                if (similarKey != null) continue

                                                Log.d("VoteSystem", "ACCEPTED: ${plate.plateNumber} (count=$count)")

                                                val existing = overlayStates[plate.plateNumber]
                                                val cachedInfo = VehicleCache.getCached(plate.plateNumber)

                                                overlayStates[plate.plateNumber] = PlateOverlayState(
                                                    plateNumber = plate.plateNumber,
                                                    vehicleInfo = cachedInfo ?: existing?.vehicleInfo,
                                                    isLoading = cachedInfo == null && existing?.vehicleInfo == null && !VehicleCache.isKnown(plate.plateNumber),
                                                    lastSeenTime = now
                                                )

                                                if (!VehicleCache.isKnown(plate.plateNumber) && !VehicleCache.isLoading(plate.plateNumber)) {
                                                    SoundManager.playScanDetected()
                                                    SoundManager.vibrate(context)
                                                    Log.d("VoteSystem", ">>> FETCHING: ${plate.plateNumber}")
                                                    scope.launch {
                                                        val info = VehicleCache.fetchIfNeeded(plate.plateNumber, countryRef.value)
                                                        Log.d("VoteSystem", "<<< RESULT: ${plate.plateNumber} -> ${info?.manufacturer} ${info?.model} ${info?.year}")
                                                        // Always show result, even if overlay was removed while loading
                                                        overlayStates[plate.plateNumber] = PlateOverlayState(
                                                            plateNumber = plate.plateNumber,
                                                            vehicleInfo = info,
                                                            isLoading = false,
                                                            lastSeenTime = System.currentTimeMillis()
                                                        )
                                                        if (info != null) {
                                                            SoundManager.playInfoLoaded()
                                                            ScanHistory.save(context, plate.plateNumber, info)
                                                            if (activity != null) AdManager.onNewPlateDetected(activity)
                                                        }
                                                    }
                                                }
                                            }

                                            // Only remove loading overlays that timed out (no data after 10s)
                                            val toRemove = overlayStates.keys.filter { key ->
                                                val state = overlayStates[key] ?: return@filter true
                                                state.vehicleInfo == null && !state.isLoading && (now - state.lastSeenTime) > 5000
                                            }
                                            toRemove.forEach { overlayStates.remove(it) }
                                        }
                                    )
                                )
                            }

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            ScanEffect()
            ViewfinderOverlay()

            // Top HUD bar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (overlayStates.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x99FF4444))
                            .clickable { overlayStates.clear(); plateExactCounts.clear() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, "Reset", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.camera_reset), color = Color.White, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassOverlay)
                        .clickable { showManualInput = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, "Manual", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassOverlay)
                        .clickable { onOpenHistory() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.History, "History", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassOverlay)
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Bottom hint/reset
            if (overlayStates.isEmpty()) {
                val infiniteTransition = rememberInfiniteTransition(label = "blink")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = androidx.compose.animation.core.EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "blinkAlpha"
                )
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.camera_scan_plate_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandPrimary.copy(alpha = alpha),
                        letterSpacing = 2.sp
                    )
                }
            }

            // Banner Ad (hidden if premium purchased)
            if (!adsRemoved) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            AdView(ctx).apply {
                                setAdSize(AdSize.BANNER)
                                adUnitId = AdManager.BANNER_AD_UNIT_ID
                                loadAd(AdRequest.Builder().build())
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Vehicle info cards — scrollable list in top third of screen
            val visibleStates = overlayStates.values
                .filter { it.vehicleInfo != null || it.isLoading }
                .sortedByDescending { it.lastSeenTime }
                .toList()

            if (visibleStates.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.33f)
                        .padding(bottom = 56.dp, start = 8.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleStates.size) { index ->
                        val state = visibleStates[index]
                        if (state.vehicleInfo != null) {
                            FloatingCarInfo(
                                vehicleInfo = state.vehicleInfo,
                                plateNumber = state.plateNumber,
                                onClick = {
                                    ScanHistory.save(context, state.plateNumber, state.vehicleInfo)
                                    SoundManager.playSaved()
                                    Toast.makeText(context, context.getString(R.string.camera_saved_to_history), Toast.LENGTH_SHORT).show()
                                },
                                onSaveToHistory = {
                                    ScanHistory.save(context, state.plateNumber, state.vehicleInfo)
                                    SoundManager.playSaved()
                                    Toast.makeText(context, context.getString(R.string.camera_saved_to_history), Toast.LENGTH_SHORT).show()
                                },
                                onOpenModelInfo = {
                                    val url = ScanHistory.buildSearchUrl(state.vehicleInfo)
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            )
                        } else if (state.isLoading) {
                            LoadingPlateIndicator(plateNumber = state.plateNumber)
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Settings, null, tint = BrandPrimary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.error_camera_required), style = MaterialTheme.typography.headlineMedium, color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.error_camera_grant), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    }

    // Manual plate input dialog
    if (showManualInput) {
        ManualPlateDialog(
            plateText = manualPlateText,
            onPlateTextChange = { manualPlateText = it },
            onSearch = {
                val plate = manualPlateText.trim().uppercase()
                if (plate.isNotEmpty() && country != null) {
                    showManualInput = false
                    // Add as overlay in center of screen
                    overlayStates[plate] = PlateOverlayState(
                        plateNumber = plate,
                        screenX = viewWidth / 2f,
                        screenY = viewHeight / 2f,
                        isLoading = true
                    )
                    scope.launch {
                        val info = VehicleCache.fetchIfNeeded(plate, country)
                        overlayStates[plate]?.let { current ->
                            overlayStates[plate] = current.copy(
                                vehicleInfo = info,
                                isLoading = false
                            )
                        }
                        if (info != null) {
                            ScanHistory.save(context, plate, info)
                        }
                    }
                    manualPlateText = ""
                }
            },
            onDismiss = {
                showManualInput = false
                manualPlateText = ""
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualPlateDialog(
    plateText: String,
    onPlateTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, Color(0xFF2A2A4A), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.camera_enter_plate),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            TextField(
                value = plateText,
                onValueChange = onPlateTextChange,
                placeholder = { Text(stringResource(R.string.camera_plate_placeholder), color = Color(0xFF666666)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF0F0F23),
                    unfocusedContainerColor = Color(0xFF0F0F23),
                    cursorColor = BrandPrimary,
                    focusedIndicatorColor = BrandPrimary,
                    unfocusedIndicatorColor = Color(0xFF333333)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A4A))
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.camera_cancel), color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandPrimary)
                        .clickable { onSearch() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, "Search", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.camera_search), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
