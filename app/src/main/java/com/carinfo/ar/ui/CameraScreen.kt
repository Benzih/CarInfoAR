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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.BiasAlignment
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

    // Shared across navigation so scanned cards persist until the user dismisses them.
    val overlayStates = ScanSession.overlayStates
    val countryRef = remember { mutableStateOf(country ?: SupportedCountry.ISRAEL) }
    countryRef.value = country ?: SupportedCountry.ISRAEL
    // Track exact OCR readings — each exact string counted separately
    val plateExactCounts = remember { mutableMapOf<String, Int>() }
    val activity = context as? Activity

    var viewWidth by remember { mutableIntStateOf(1) }
    var viewHeight by remember { mutableIntStateOf(1) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualPlateText by remember { mutableStateOf("") }
    var showScanOptions by remember { mutableStateOf(false) }

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
    var historyBtnCenter by remember { mutableStateOf(Offset.Zero) }
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

    // Plates scheduled for exit animation before removal from overlayStates
    val exitingPlates = remember { androidx.compose.runtime.mutableStateListOf<String>() }

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

            // All overlays are visible: found cards, loading spinners, and short-lived
            // not-found indicators. The cleanup in onPlatesDetected removes stale
            // not-found entries after 3s, so reset button and scan hint stay in sync.
            val visibleStates = overlayStates.values
                .sortedByDescending { it.lastSeenTime }
                .toList()

            ScanEffect()
            // Viewfinder and scan hint share a vertical center: full-screen center
            // when no cards, shifted up into the band above the cards when any card
            // is visible (cards occupy the bottom 45%). The viewfinder fraction is
            // bumped slightly below the text bias to compensate for BiasAlignment's
            // child-height term, so the text sits at the visual middle of the frame.
            val hintCenterFraction = if (visibleStates.isEmpty()) 0.5f else 0.35f
            val viewfinderCenter = if (visibleStates.isEmpty()) 0.5f else 0.37f
            ViewfinderOverlay(centerYFraction = viewfinderCenter)

            // Invisible scrim to dismiss the scan-options menu on outside taps.
            // Declared BEFORE the toolbar so pills/menu items (declared later) capture
            // their own taps first; taps on empty space fall through to this scrim.
            if (showScanOptions) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { showScanOptions = false }
                        }
                )
            }

            // Top HUD — 1) scan-options pill, 2) history, 3) settings, with the
            // expandable menu anchored below the scan-options pill.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 52.dp, start = 4.dp, end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScanOptionsPillButton(
                        label = stringResource(R.string.toolbar_scan_options),
                        expanded = showScanOptions,
                        onClick = { showScanOptions = !showScanOptions }
                    )
                    ToolbarPillButton(
                        icon = Icons.Default.History,
                        label = stringResource(R.string.toolbar_history),
                        onClick = { onOpenHistory() },
                        scale = historyPulseScale.value,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            val size = coords.size
                            historyBtnCenter = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                        }
                    )
                    ToolbarIconButton(
                        icon = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.toolbar_settings),
                        onClick = { onOpenSettings() }
                    )
                }

                // Expandable menu — aligned to start (same side as scan-options pill).
                // Compose's Alignment.Start is layout-direction-aware, so LTR=left, RTL=right.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    ScanOptionsMenu(
                        visible = showScanOptions,
                        onPickImage = {
                            showScanOptions = false
                            showImagePicker = true
                        },
                        onManualInput = {
                            showScanOptions = false
                            showManualInput = true
                            AnalyticsManager.manualInputOpened()
                        }
                    )
                }
            }

            // Scan hint — big centered when empty, compact top-pill when a card is visible
            val scanHintTransition = rememberInfiniteTransition(label = "blink")
            val scanHintAlpha by scanHintTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = androidx.compose.animation.core.EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "blinkAlpha"
            )
            // Same big hint in both states. When no cards are visible it sits at the
            // true center; once cards fill the bottom 45%, it shifts up into the empty
            // band between the toolbar and the top card so it stays readable without
            // shrinking to a small pill. The vertical bias is derived from the shared
            // hintCenterFraction so the text stays centered inside the viewfinder.
            val hintAlignment = BiasAlignment(
                horizontalBias = 0f,
                verticalBias = 2f * hintCenterFraction - 1f
            )
            Column(
                modifier = Modifier.align(hintAlignment),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val words = stringResource(R.string.camera_scan_plate_title)
                    .split(' ')
                    .filter { it.isNotBlank() }
                words.forEach { word ->
                    Text(
                        text = word,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandPrimary.copy(alpha = scanHintAlpha),
                        letterSpacing = 2.sp,
                        lineHeight = 38.sp
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

                val isRtlList = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .padding(bottom = 56.dp, start = 8.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = visibleStates.size,
                        key = { index -> visibleStates[index].plateNumber }
                    ) { index ->
                        val state = visibleStates[index]
                        val visibleState = remember(state.plateNumber) {
                            MutableTransitionState(initialState = false).apply { targetState = true }
                        }
                        val shouldExit = state.plateNumber in exitingPlates
                        LaunchedEffect(shouldExit) {
                            if (shouldExit) visibleState.targetState = false
                        }
                        LaunchedEffect(visibleState.currentState, visibleState.targetState) {
                            if (!visibleState.targetState && !visibleState.currentState) {
                                overlayStates.remove(state.plateNumber)
                                exitingPlates.remove(state.plateNumber)
                                plateExactCounts.remove(state.plateNumber)
                            }
                        }

                        AnimatedVisibility(
                            visibleState = visibleState,
                            modifier = Modifier.animateItem(
                                placementSpec = tween(durationMillis = 320)
                            ),
                            enter = fadeIn(animationSpec = tween(220)) +
                                scaleIn(initialScale = 0.92f, animationSpec = tween(260)) +
                                slideInVertically(
                                    initialOffsetY = { it / 6 },
                                    animationSpec = tween(280)
                                ),
                            exit = fadeOut(animationSpec = tween(200)) +
                                scaleOut(
                                    targetScale = 0.6f,
                                    transformOrigin = TransformOrigin(0.5f, 0.5f),
                                    animationSpec = tween(260)
                                ) +
                                shrinkVertically(
                                    shrinkTowards = Alignment.Top,
                                    animationSpec = tween(280)
                                ) +
                                slideOutHorizontally(
                                    targetOffsetX = { if (isRtlList) -it / 4 else it / 4 },
                                    animationSpec = tween(280)
                                )
                        ) {
                            when {
                                state.vehicleInfo != null -> FloatingCarInfo(
                                    vehicleInfo = state.vehicleInfo,
                                    plateNumber = state.plateNumber,
                                    onSaveToHistory = { buttonOffset ->
                                        ScanHistory.save(context, state.plateNumber, state.vehicleInfo)
                                        SoundManager.playSaved()
                                        AnalyticsManager.historySavedFromCamera(state.plateNumber, countryRef.value.code)
                                        saveAnimStartPos = buttonOffset
                                        if (BuildConfig.DEBUG) Log.d("SaveAnim", "Save button pos: x=${buttonOffset.x}, y=${buttonOffset.y}")
                                        showSaveAnimation = true
                                    },
                                    onDelete = {
                                        SoundManager.playSaved()
                                        exitingPlates.add(state.plateNumber)
                                    },
                                    onOpenModelInfo = {
                                        AnalyticsManager.modelInfoClicked(state.vehicleInfo.manufacturer, state.vehicleInfo.model)
                                        val url = ScanHistory.buildSearchUrl(state.vehicleInfo)
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                )
                                state.isLoading -> LoadingPlateIndicator(
                                    plateNumber = state.plateNumber,
                                    onDelete = { exitingPlates.add(state.plateNumber) }
                                )
                                else -> PlateNotFoundIndicator(
                                    plateNumber = state.plateNumber,
                                    onDelete = { exitingPlates.add(state.plateNumber) }
                                )
                            }
                        }
                    }
                }
            }

            // Flying save animation: card flies from save button to history button
            // Target position is captured dynamically from the history pill via onGloballyPositioned.
            val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
            val statusBarPx = with(density) { 52.dp.toPx() }
            val historyEndX = if (historyBtnCenter.x > 0f) historyBtnCenter.x
                else if (isRtl) with(density) { 84.dp.toPx() } else viewWidth - with(density) { 84.dp.toPx() }
            val historyEndY = if (historyBtnCenter.y > 0f) historyBtnCenter.y
                else statusBarPx + with(density) { 20.dp.toPx() }

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

@Composable
private fun ToolbarPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(100.dp))
            .background(GlassOverlay)
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(100.dp))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 9.dp)
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/**
 * Circular icon-only toolbar button. Used for the Settings button where the
 * icon alone is self-explanatory (gear = settings) and dropping the label frees
 * up horizontal space for the primary "scan options" pill.
 */
@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(GlassOverlay)
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Featured pill that opens the "more scan options" menu. Differentiated from the
 * plain [ToolbarPillButton] by a brand-tinted gradient background, a sparkle icon,
 * a trailing caret that rotates 180° when expanded, and a pulsing brand border
 * when the menu is open.
 */
@Composable
private fun ScanOptionsPillButton(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val caretRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "caretRotation"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (expanded) 0.65f else 0.18f,
        animationSpec = tween(240),
        label = "borderAlpha"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (expanded) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressScale"
    )

    // Subtle breathing glow when collapsed so users notice the entry point
    val infinite = rememberInfiniteTransition(label = "scanPillBreath")
    val breath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val dynamicBorderAlpha = if (expanded) borderAlpha else (0.16f + breath * 0.22f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(100.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        BrandPrimary.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.06f),
                        BrandPrimary.copy(alpha = 0.12f)
                    )
                )
            )
            .border(
                width = 0.8.dp,
                color = BrandPrimary.copy(alpha = dynamicBorderAlpha),
                shape = RoundedCornerShape(100.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = label,
            tint = BrandPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = BrandPrimary.copy(alpha = 0.85f),
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { rotationZ = caretRotation }
        )
    }
}

/**
 * Elegant dropdown that reveals the two alternative scan modes (image & manual).
 *
 * Animation layers:
 * 1. Container: fade + scale from top-start (spring bouncy) + short vertical slide
 * 2. Each item: staggered fade-in + vertical slide-in using per-item Animatables
 *    so items "cascade" into place after the container opens
 */
@Composable
private fun ScanOptionsMenu(
    visible: Boolean,
    onPickImage: () -> Unit,
    onManualInput: () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible

    AnimatedVisibility(
        visibleState = transitionState,
        enter = fadeIn(tween(180)) +
                scaleIn(
                    initialScale = 0.84f,
                    transformOrigin = TransformOrigin(0f, 0f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) +
                slideInVertically(
                    initialOffsetY = { -it / 3 },
                    animationSpec = tween(260, easing = EaseOutCubic)
                ),
        exit = fadeOut(tween(140)) +
               scaleOut(
                   targetScale = 0.9f,
                   transformOrigin = TransformOrigin(0f, 0f),
                   animationSpec = tween(160)
               )
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF20E1524),
                            Color(0xF21A1A2E)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            BrandPrimary.copy(alpha = 0.55f),
                            BrandPrimary.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(6.dp)
        ) {
            ScanOptionMenuItem(
                icon = Icons.Default.Image,
                label = stringResource(R.string.toolbar_image),
                onClick = onPickImage,
                index = 0
            )
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Spacer(Modifier.height(2.dp))
            ScanOptionMenuItem(
                icon = Icons.Default.Edit,
                label = stringResource(R.string.toolbar_manual),
                onClick = onManualInput,
                index = 1
            )
        }
    }
}

@Composable
private fun ScanOptionMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    index: Int
) {
    val alpha = remember { Animatable(0f) }
    val translateY = remember { Animatable(-14f) }
    LaunchedEffect(Unit) {
        delay(70L * index)
        launch {
            alpha.animateTo(1f, tween(260, easing = EaseOutCubic))
        }
        translateY.animateTo(
            0f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha.value
                this.translationY = translateY.value
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(BrandPrimary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = BrandPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
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
