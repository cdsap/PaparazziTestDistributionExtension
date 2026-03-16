package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.RenderExtension
import com.android.ide.common.rendering.api.SessionParams.RenderingMode

/**
 * Factory function that creates a [Paparazzi] instance pre-configured with the
 * Test Distribution-compatible snapshot handler.
 *
 * This is a drop-in replacement for [Paparazzi] that automatically selects the
 * appropriate handler (TD report writer or snapshot verifier) based on system properties.
 *
 * Usage:
 * ```
 * @get:Rule
 * val paparazzi = TDPaparazzi()
 * ```
 */
fun TDPaparazzi(
    deviceConfig: DeviceConfig = DeviceConfig(),
    theme: String = "android:Theme.Material.NoActionBar.Fullscreen",
    renderingMode: RenderingMode = RenderingMode.SHRINK,
    appCompatEnabled: Boolean = true,
    maxPercentDifference: Double = 0.0,
    renderExtensions: Set<RenderExtension> = setOf(),
    supportsRtl: Boolean = false,
    showSystemUi: Boolean = false,
    useDeviceResolution: Boolean = false
): Paparazzi = Paparazzi(
    deviceConfig = deviceConfig,
    theme = theme,
    renderingMode = renderingMode,
    appCompatEnabled = appCompatEnabled,
    maxPercentDifference = maxPercentDifference,
    snapshotHandler = TDPaparazziHandlerProvider().determineHandler(maxPercentDifference),
    renderExtensions = renderExtensions,
    supportsRtl = supportsRtl,
    showSystemUi = showSystemUi,
    useDeviceResolution = useDeviceResolution
)
