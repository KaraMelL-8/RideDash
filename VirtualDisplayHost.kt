package com.example.ridedash.display

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.Surface

/** Creates the off-screen display that is encoded and sent to the dash. */
class VirtualDisplayHost(private val context: Context) {
    private var vd: VirtualDisplay? = null
    val displayId: Int get() = vd?.display?.displayId ?: -1
    val display get() = vd?.display

    fun create(width: Int, height: Int, dpi: Int, target: Surface) {
        val dm = context.getSystemService(DisplayManager::class.java)
        // STAGE 0: verify on a real device which flags let other apps (Amap) run here.
        val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
        vd = dm.createVirtualDisplay("RideDash", width, height, dpi, target, flags)
    }

    fun release() { vd?.release(); vd = null }
}
