package com.example.ridedash.link

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/** Transport to the motorcycle dash. Real impl = BLE wake + Wi-Fi + EasyConn/PXC handshake. */
interface DashLink {
    fun connect()
    fun sendFrame(h264: ByteArray, keyOrConfig: Boolean)
    fun close()
    // TODO: var onInput: ((DashInput) -> Unit)?  (handlebar buttons / touch)
}

/** Stage-0 stand-in: writes the raw stream to a file. Play with: ffplay -f h264 out.h264 */
class FileDashLink(private val ctx: Context) : DashLink {
    private var out: FileOutputStream? = null
    val file: File get() = File(ctx.getExternalFilesDir(null), "out.h264")
    override fun connect() { out = FileOutputStream(file) }
    override fun sendFrame(h264: ByteArray, keyOrConfig: Boolean) { try { out?.write(h264) } catch (e: java.io.IOException) { /* closed while stopping */ } }
    override fun close() { runCatching { out?.close() }; out = null }
}

// TODO(real link): port BLE wake-up, QR/Wi-Fi join and PXC handshake from
// RideScreen AA / OpenCfMoto (AGPL-3.0). Read docs/OPENCFMOTO-README.md first.
