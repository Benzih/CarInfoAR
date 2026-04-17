package com.carinfo.ar.ui

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.carinfo.ar.BuildConfig
import com.carinfo.ar.R
import com.carinfo.ar.ads.AdManager
import com.carinfo.ar.camera.PlateAnalyzer
import com.carinfo.ar.camera.StaticImageOcr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.carinfo.ar.data.ScanHistory
import com.carinfo.ar.data.SupportedCountry
import com.carinfo.ar.data.UserPreferences
import com.carinfo.ar.data.VehicleCache
import com.carinfo.ar.data.model.VehicleInfo
import com.carinfo.ar.ui.components.ScanEffect
import com.carinfo.ar.ui.components.ViewfinderOverlay
import com.carinfo.ar.ui.theme.BrandPrimary
import com.carinfo.ar.ui.theme.GlassOverlay
import com.carinfo.ar.analytics.AnalyticsManager
import com.carinfo.ar.util.SoundManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
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
    val selectedCountryCode by UserPreferences.getSelectedCountry(context).collectAsState(initial = "LOADING")
    // While DataStore is loading, show nothing (avoid "not supported" flash)
    if (selectedCountryCode == "LOADING") return
    val country = selectedCountryCode?.takeIf { it.isNotEmpty() }?.let { SupportedCountry.fromCode(it) }
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
    ) { granted ->
        hasCameraPermission = granted
        if (granted) AnalyticsManager.cameraPermissionGranted() else AnalyticsManager.cameraPermissionDenied()
    }

    val overlayStates = remember { mutableStateMapOf<String, PlateOverlayState>() }
    val countryRef = remember { mutableStateOf(country ?: SupportedCountry.ISRAEL) }
    countryRef.value = country ?: SupportedCountry.ISRAEL
    // Track exact OCR readings — each exact string counted separately
    val plateExactCounts = remember { mutableMapOf<String, Int>() }
    val activity = context as? Activity

    var viewWidth by remember { mutableIntStateOf(1) }
    var viewHeight by remember { mutableIntStateOf(1) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualPlateText by remember { mutableStateOf("") }

    // Image-based plate scan state
    var showImagePicker by remember { mutableStateOf(false) }
    var isAnalyzingImage by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Shared handler: given a Uri, load the bitmap and run OCR
    fun processImageUri(uri: Uri) {
        isAnalyzingImage = true
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { loadBitmapFromUri(context, uri) }
                if (bitmap == null) {
                    Toast.makeText(context, context.getString(R.string.image_picker_error), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val plate = withContext(Dispatchers.Default) {
                    StaticImageOcr.extractPlate(bitmap, country ?: SupportedCountry.ISRAEL)
                }
                try { bitmap.recycle() } catch (_: Exception) {}

                if (plate == null) {
                    Toast.makeText(context, context.getString(R.string.image_picker_no_plate), Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Found a plate — run the same lookup flow as manual input
                AnalyticsManager.manualInputSearched(plate, (country ?: SupportedCountry.ISRAEL).code)
                overlayStates[plate] = PlateOverlayState(
                    plateNumber = plate,
                    screenX = viewWidth / 2f,
                    screenY = viewHeight / 2f,
                    isLoading = true
                )
                val info = VehicleCache.fetchIfNeeded(plate, country ?: SupportedCountry.ISRAEL)
                overlayStates[plate]?.let { current ->
                    overlayStates[plate] = current.copy(
                        vehicleInfo = info,
                        isLoading = false,
                        lastSeenTime = System.currentTimeMillis()
                    )
                }
                if (info != null) {
                    SoundManager.playInfoLoaded()
                    ScanHistory.save(context, plate, info)
                    AnalyticsManager.vehicleInfoLoaded(plate, info.manufacturer, info.model, (country ?: SupportedCountry.ISRAEL).code)
                } else {
                    AnalyticsManager.vehicleNotFound(plate, (country ?: SupportedCountry.ISRAEL).code)
                    Toast.makeText(context, context.getString(R.string.image_picker_no_plate), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CameraScreen", "Image OCR failed", e)
                Toast.makeText(context, context.getString(R.string.image_picker_error), Toast.LENGTH_SHORT).show()
            } finally {
                isAnalyzingImage = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) processImageUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) processImageUri(uri)
    }

    // LazyColumn scroll state — auto-scroll to top on new detection
    val listState = rememberLazyListState()

    // Save-to-history flying animation
    var historyButtonPos by remember { mutableStateOf(Offset.Zero) }
    var saveAnimStartPos by remember { mutableStateOf(Offset.Zero) }
    var showSaveAnimation by remember { mutableStateOf(false) }
    // History button pulse when animation arrives
    var historyButtonPulse by remember { mutableStateOf(false) }
    val historyPulseScale = remember { Animatable(1f) }
    val saveAnimProgress = remember { Animatable(0f) }

    LaunchedEffect(showSaveAnimation) {
        if (showSaveAnimation) {
            saveAnimProgress.snapTo(0f)
            saveAnimProgress.animateTo(1f, animationSpec = tween(700))
            showSaveAnimation = false
            // Pulse the history button
            historyButtonPulse = true
        }
    }
    LaunchedEffect(historyButtonPulse) {
        if (historyButtonPulse) {
            historyPulseScale.snapTo(1f)
            historyPulseScale.animateTo(1.4f, animationSpec = tween(150))
            historyPulseScale.animateTo(1f, animationSpec = tween(200))
            historyButtonPulse = false
        }
    }
    var pinchZoomLogged by remember { mutableStateOf(false) }

    // Sync sound setting
    val soundEnabled by UserPreferences.isSoundEnabled(context).collectAsState(initial = true)
    LaunchedEffect(soundEnabled) { SoundManager.soundEnabled = soundEnabled }

    // DVLA API key is hardcoded in VehicleCache

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            AnalyticsManager.cameraOpened()
        }
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                                    if (!pinchZoomLogged) {
                                        pinchZoomLogged = true
                                        AnalyticsManager.pinchZoomUsed()
                                    }
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
                                    AnalyticsManager.doubleTapZoomUsed()
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
                        val cameraProvider = try {
                            cameraProviderFuture.get()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return@addListener
                        }

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

                                                if (BuildConfig.DEBUG) Log.d("VoteSystem", "Plate: ${plate.plateNumber}, exact count: $count")

                                                // Need at least 3 identical readings (or already known from cache)
                                                if (count < 3 && !VehicleCache.isKnown(plate.plateNumber)) continue

                                                // Skip if already showing this plate or a similar one
                                                if (overlayStates.containsKey(plate.plateNumber)) continue
                                                val similarKey = overlayStates.keys.find { existingPlate ->
                                                    isSimilarPlate(existingPlate, plate.plateNumber)
                                                }
                                                if (similarKey != null) continue

                                                if (BuildConfig.DEBUG) Log.d("VoteSystem", "ACCEPTED: ${plate.plateNumber} (count=$count)")
                                                AnalyticsManager.plateAccepted(plate.plateNumber, countryRef.value.code)

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
                                                    if (BuildConfig.DEBUG) Log.d("VoteSystem", ">>> FETCHING: ${plate.plateNumber}")
                                                    scope.launch {
                                                        val info = VehicleCache.fetchIfNeeded(plate.plateNumber, countryRef.value)
                                                        if (BuildConfig.DEBUG) Log.d("VoteSystem", "<<< RESULT: ${plate.plateNumber} -> ${info?.manufacturer} ${info?.model} ${info?.year}")
                                                        // Always show result, even if overlay was removed while loading
                                                        overlayStates[plate.plateNumber] = PlateOverlayState(
                                                            plateNumber = plate.plateNumber,
                                                            vehicleInfo = info,
                                                            isLoading = false,
                                                            lastSeenTime = System.currentTimeMillis()
                                                        )
                                                        if (info != null) {
                                                            SoundManager.playInfoLoaded()
                                                            AnalyticsManager.vehicleInfoLoaded(plate.plateNumber, info.manufacturer, info.model, countryRef.value.code)
                                                            // Save only via manual Save button, not automatically
                                                            if (activity != null) AdManager.onNewPlateDetected(activity)
                                                        } else {
                                                            AnalyticsManager.vehicleNotFound(plate.plateNumber, countryRef.value.code)
                                                        }
                                                    }
                                                }
                                            }

                                            // Remove not-found indicators after 3s so the reset button
                                            // and scan hint don't stay stuck while nothing is visible.
                                            val toRemove = overlayStates.keys.filter { key ->
                                                val state = overlayStates[key] ?: return@filter true
                                                state.vehicleInfo == null && !state.isLoading && (now - state.lastSeenTime) > 3000
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

            // All overlays are visible: found cards, loading spinners, and short-lived
            // not-found indicators. The cleanup in onPlatesDetected removes stale
            // not-found entries after 3s, so reset button and scan hint stay in sync.
            val visibleStates = overlayStates.values
                .sortedByDescending { it.lastSeenTime }
                .toList()

            // Top HUD bar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (visibleStates.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x99FF4444))
                            .clickable { overlayStates.clear(); plateExactCounts.clear(); AnalyticsManager.scanReset() }
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
                // Image picker button (gallery + camera)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassOverlay)
                        .clickable { showImagePicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, stringResource(R.string.image_picker_title), tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassOverlay)
                        .clickable { showManualInput = true; AnalyticsManager.manualInputOpened() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, "Manual", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(historyPulseScale.value)
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
            if (visibleStates.isEmpty()) {
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
            if (visibleStates.isNotEmpty()) {
                // Auto-scroll to top when new items appear
                LaunchedEffect(visibleStates.size) {
                    if (visibleStates.isNotEmpty()) {
                        listState.animateScrollToItem(0)
                    }
                }

                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
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
                                onSaveToHistory = { buttonOffset ->
                                    ScanHistory.save(context, state.plateNumber, state.vehicleInfo)
                                    SoundManager.playSaved()
                                    AnalyticsManager.historySavedFromCamera(state.plateNumber, countryRef.value.code)
                                    // Trigger flying animation from save button to history button
                                    saveAnimStartPos = buttonOffset
                                    if (BuildConfig.DEBUG) Log.d("SaveAnim", "Save button pos: x=${buttonOffset.x}, y=${buttonOffset.y}")
                                    showSaveAnimation = true
                                },
                                onOpenModelInfo = {
                                    AnalyticsManager.modelInfoClicked(state.vehicleInfo.manufacturer, state.vehicleInfo.model)
                                    val url = ScanHistory.buildSearchUrl(state.vehicleInfo)
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            )
                        } else if (state.isLoading) {
                            LoadingPlateIndicator(plateNumber = state.plateNumber)
                        } else {
                            PlateNotFoundIndicator(plateNumber = state.plateNumber)
                        }
                    }
                }
            }

            // Flying save animation: card flies from save button to history button
            // Use computed positions instead of positionInRoot() which is unreliable on RTL + old APIs
            val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
            val statusBarPx = with(density) { 52.dp.toPx() }
            // History button is 2nd from right in LTR, 2nd from left in RTL
            // Layout (LTR): Reset | Spacer | Image(40+8) | Manual(40+8) | History(40+8) | Settings(40) | 16dp padding
            // From right edge to center of History: 16 + 40 + 8 + 20 = 84dp
            val historyEndX = if (isRtl) {
                with(density) { (16 + 40 + 8 + 20).dp.toPx() } // 84dp from left in RTL
            } else {
                viewWidth - with(density) { (16 + 40 + 8 + 20).dp.toPx() } // 84dp from right in LTR
            }
            val historyEndY = statusBarPx + with(density) { 20.dp.toPx() }

            if (showSaveAnimation) {
                val progress = saveAnimProgress.value
                val startX = saveAnimStartPos.x.takeIf { it > 0f } ?: (if (isRtl) viewWidth * 0.3f else viewWidth * 0.7f)
                val startY = saveAnimStartPos.y.takeIf { it > 0f } ?: (viewHeight * 0.65f)
                val currentX = startX + (historyEndX - startX) * progress
                val currentY = startY + (historyEndY - startY) * progress




                val animScale = 1.2f - progress * 0.8f
                val animAlpha = 1f - progress * 0.3f

                val rawPixelX = (currentX - 40f).toInt()
                val pixelY = (currentY - 16f).toInt()


                // Full-screen transparent overlay so we can position the badge with absolute pixels
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    constraints.copy(minWidth = 0, minHeight = 0)
                                )
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    placeable.place(rawPixelX, pixelY)
                                }
                            }
                            .scale(animScale)
                            .alpha(animAlpha)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandPrimary)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Saved!", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // end animation
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

    // Image picker choice dialog (Gallery / Camera)
    if (showImagePicker) {
        ImagePickerDialog(
            onGallery = {
                showImagePicker = false
                galleryLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onCamera = {
                showImagePicker = false
                val uri = createCameraCaptureUri(context)
                if (uri != null) {
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    Toast.makeText(context, context.getString(R.string.image_picker_error), Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showImagePicker = false }
        )
    }

    // Full-screen analyzing overlay while OCR runs
    if (isAnalyzingImage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = BrandPrimary)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.image_picker_analyzing),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
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
                    AnalyticsManager.manualInputSearched(plate, country.code)
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
                AnalyticsManager.manualInputCancelled()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePickerDialog(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
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
                stringResource(R.string.image_picker_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.image_picker_subtitle),
                color = Color(0xFF9A9AB5),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))
            // Gallery option
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandPrimary)
                    .clickable { onGallery() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.image_picker_gallery), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Camera option
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2A4A))
                    .clickable { onCamera() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.image_picker_camera), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.camera_cancel),
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onDismiss() }
            )
        }
    }
}

// ===== Image loading helpers =====

/**
 * Load a bitmap from a content:// or file:// uri, correctly oriented per EXIF.
 * Returns null if decoding fails.
 */
private fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        // On API 28+ ImageDecoder handles EXIF orientation automatically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            // Pre-API 28: decode via BitmapFactory + rotate per EXIF
            val raw = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return null

            val rotation = context.contentResolver.openInputStream(uri)?.use { stream ->
                try {
                    val exif = ExifInterface(stream)
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } catch (e: Exception) {
                    0f
                }
            } ?: 0f

            if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                if (rotated !== raw) raw.recycle()
                rotated
            } else {
                raw
            }
        }
    } catch (e: Exception) {
        Log.e("CameraScreen", "Failed to load bitmap from uri", e)
        null
    }
}

/**
 * Create a temp file in the app cache and return a content:// uri for camera capture.
 */
private fun createCameraCaptureUri(context: android.content.Context): Uri? {
    return try {
        val dir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        Log.e("CameraScreen", "Failed to create camera capture uri", e)
        null
    }
}
