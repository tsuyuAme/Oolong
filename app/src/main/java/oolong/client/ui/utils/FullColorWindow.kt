package oolong.client.ui.utils

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import oolong.client.findActivity
import java.util.WeakHashMap

/** One of those in composition asking a window for full colour, and whether it shows HDR now. */
private class Ask(var hdr: Boolean)

/** Who in composition asks each window for full colour. */
private val asking = WeakHashMap<Window, MutableList<Ask>>()

@RequiresApi(Build.VERSION_CODES.O)
private fun Window.applyColorMode(asks: List<Ask>, wideGamut: Boolean) {
    colorMode = when {
        asks.isEmpty() -> ActivityInfo.COLOR_MODE_DEFAULT
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && asks.any { it.hdr } ->
            ActivityInfo.COLOR_MODE_HDR
        wideGamut -> ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        else -> ActivityInfo.COLOR_MODE_DEFAULT
    }
}

/**
 * While in composition, the window shows pictures in their full colour: a wide-gamut one beyond
 * sRGB on a screen that can, and from Android 14, while [hdr] says what is shown is HDR, brighter
 * than white. Only where pictures are looked at, as the rest of the app is sRGB and a wide-gamut
 * window can cost more memory and power.
 *
 * HDR only while an HDR picture is shown: in that mode many phones raise the whole screen's
 * brightness, whatever is on it. Several may ask at once; the window goes back when the last is
 * gone.
 */
@Composable
fun FullColorWindow(hdr: Boolean = false) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val view = LocalView.current
    val ask = remember { Ask(hdr) }
    val window = remember(view) { view.context.findActivity()?.window } ?: return
    val wideGamut = view.display?.isWideColorGamut == true
    DisposableEffect(window) {
        val asks = asking.getOrPut(window) { mutableListOf() }
        asks += ask
        window.applyColorMode(asks, wideGamut)
        onDispose {
            asks -= ask
            if (asks.isEmpty()) asking.remove(window)
            window.applyColorMode(asks, wideGamut)
        }
    }
    DisposableEffect(window, hdr) {
        if (ask.hdr != hdr) {
            ask.hdr = hdr
            asking[window]?.let { window.applyColorMode(it, wideGamut) }
        }
        onDispose {}
    }
}

/** Whether the picture carries an HDR gain map, which only an HDR window shows brighter than white. */
fun Bitmap.isHdr(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasGainmap()
