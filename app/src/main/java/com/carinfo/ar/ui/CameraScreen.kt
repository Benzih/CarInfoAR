package com.carinfo.ar.ui

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.carinfo.ar.camera.FrameMotionTracker
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
import kotlinx.coroutines.launch

data class PlateOverlayState(
    val plateNumber: String,
    val screenX: Float = 0f,
    val screenY: Float = 0f,
    val vehicleInfo: VehicleInfo? = null,
    val isLoading: Boolean = true,
    val lastSeenTime: Long = System.currentTimeMillis()
)

@Composable
fun CameraScreen(onOpenSettings: () -> Unit = {}) {
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
                Text("CarInfo AR", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text("Your country is not supported yet", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFFF6B6B))
                Spacer(Modifier.height(8.dp))
                Text("Supported: Israel, Netherlands, UK", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

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
    val motionTracker = remember { FrameMotionTracker() }
    val knownPlates = remember { mutableSetOf<String>() }
    val activity = context as? Activity

    var viewWidth by remember { mutableIntStateOf(1) }
    var viewHeight by remember { mutableIntStateOf(1) }

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
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                            viewWidth = right - left
                            viewHeight = bottom - top
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
                                        country = country,
                                        motionTracker = motionTracker,
                                        screenWidth = { viewWidth },
                                        screenHeight = { viewHeight },
                                        onMotionEstimated = { dx, dy ->
                                            if (overlayStates.isNotEmpty()) {
                                                for ((plate, state) in overlayStates.entries.toList()) {
                                                    overlayStates[plate] = state.copy(
                                                        screenX = state.screenX + dx,
                                                        screenY = state.screenY + dy
                                                    )
                                                }
                                            }
                                        },
                                        onPlatesDetected = { plates, imgW, imgH ->
                                            val now = System.currentTimeMillis()

                                            for (plate in plates) {
                                                val scaleX = viewWidth.toFloat() / imgW.toFloat()
                                                val scaleY = viewHeight.toFloat() / imgH.toFloat()

                                                val box = plate.boundingBox
                                                val screenCenterX = box.centerX() * scaleX
                                                val screenTopY = box.top * scaleY

                                                val existing = overlayStates[plate.plateNumber]
                                                val cachedInfo = VehicleCache.getCached(plate.plateNumber)

                                                overlayStates[plate.plateNumber] = PlateOverlayState(
                                                    plateNumber = plate.plateNumber,
                                                    screenX = screenCenterX,
                                                    screenY = screenTopY,
                                                    vehicleInfo = cachedInfo ?: existing?.vehicleInfo,
                                                    isLoading = cachedInfo == null && existing?.vehicleInfo == null && !VehicleCache.isKnown(plate.plateNumber),
                                                    lastSeenTime = now
                                                )

                                                if (knownPlates.add(plate.plateNumber) && activity != null) {
                                                    AdManager.onNewPlateDetected(activity)
                                                }

                                                if (!VehicleCache.isKnown(plate.plateNumber) && !VehicleCache.isLoading(plate.plateNumber)) {
                                                    scope.launch {
                                                        val info = VehicleCache.fetchIfNeeded(plate.plateNumber, country)
                                                        overlayStates[plate.plateNumber]?.let { current ->
                                                            overlayStates[plate.plateNumber] = current.copy(
                                                                vehicleInfo = info,
                                                                isLoading = false
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Remove plates not seen recently
                                            val toRemove = overlayStates.keys.filter { key ->
                                                val state = overlayStates[key] ?: return@filter true
                                                if (state.vehicleInfo != null) false
                                                else (now - state.lastSeenTime) > 1500
                                            }
                                            toRemove.forEach { overlayStates.remove(it) }
                                        }
                                    )
                                )
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
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
                CountryIndicator(country = country)
                Spacer(Modifier.weight(1f))
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
                Text(
                    "Point at a license plate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0x88FFFFFF),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .clip(CircleShape)
                        .background(GlassOverlay)
                        .clickable { overlayStates.clear() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, "Reset", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reset", color = Color.White, fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }

            // Banner Ad
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 0.dp)
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

            // AR Overlays
            overlayStates.values.forEach { state ->
                if (state.vehicleInfo == null && !state.isLoading) return@forEach

                val overlayHeightPx = with(density) { 80.dp.toPx() }
                val offsetXPx = state.screenX.toInt()
                val offsetYPx = (state.screenY - overlayHeightPx).coerceAtLeast(0f).toInt()

                Box(modifier = Modifier.offset { IntOffset(offsetXPx, offsetYPx) }) {
                    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(-overlaySize.width / 2, 0) }
                            .onSizeChanged { overlaySize = it }
                    ) {
                        if (state.vehicleInfo != null) {
                            FloatingCarInfo(vehicleInfo = state.vehicleInfo)
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
                Text("Camera Access\nRequired", style = MaterialTheme.typography.headlineMedium, color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Please grant camera access in settings", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    }
}
