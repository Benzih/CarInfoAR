package com.carinfo.ar.ui

import androidx.compose.runtime.mutableStateMapOf

/**
 * Process-wide holder for currently-displayed plate overlays.
 * Survives navigation (e.g. Camera → History → Camera) so scanned cars
 * stay on screen until the user explicitly dismisses them.
 */
object ScanSession {
    val overlayStates = mutableStateMapOf<String, PlateOverlayState>()
}
