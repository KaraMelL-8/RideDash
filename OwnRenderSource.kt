package com.example.ridedash.source

import android.content.Context
import com.example.ridedash.display.TestPresentation
import com.example.ridedash.display.VirtualDisplayHost
import com.example.ridedash.encode.H264Encoder

/** Our own UI on a virtual display -> H.264. Works with the phone screen off. */
class OwnRenderSource(
    private val w: Int, private val h: Int, private val dpi: Int,
    private val onTick: () -> Unit,
) : VideoSource {
    override val label = "เรนเดอร์เอง"
    private var host: VirtualDisplayHost? = null
    private var enc: H264Encoder? = null
    private var pres: TestPresentation? = null

    override fun start(ctx: Context, sink: (ByteArray, Boolean) -> Unit): String? {
        return try {
            val e = H264Encoder(w, h) { bytes, key -> sink(bytes, key) }
            e.start(); enc = e
            val hst = VirtualDisplayHost(ctx); host = hst
            hst.create(w, h, dpi, e.inputSurface)
            val d = hst.display
            if (d == null) {
                stop(); "สร้างจอเสมือนไม่สำเร็จ"
            } else {
                pres = TestPresentation(ctx, d, onTick).also { it.show() }
                null
            }
        } catch (t: Throwable) {
            stop(); t.message ?: t.javaClass.simpleName
        }
    }

    override fun stop() {
        runCatching { pres?.dismiss() }; pres = null
        runCatching { enc?.stop() }; enc = null
        runCatching { host?.release() }; host = null
    }
}
