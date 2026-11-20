package com.example.ridedash.encode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import kotlin.concurrent.thread

/** Surface -> H.264 (Annex-B). onFrame(bytes, isKeyOrConfig). */
class H264Encoder(
    private val width: Int, private val height: Int,
    private val fps: Int = 30, private val bitrate: Int = 4_000_000,
    private val onFrame: (ByteArray, Boolean) -> Unit,
) {
    private var codec: MediaCodec? = null
    @Volatile private var running = false
    lateinit var inputSurface: Surface; private set

    fun start() {
        val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1_000_000L / fps)
        }
        val c = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        c.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = c.createInputSurface()
        c.start(); codec = c; running = true
        thread(name = "h264-out") {
            val info = MediaCodec.BufferInfo()
            while (running) {
                val i = try { c.dequeueOutputBuffer(info, 10_000) } catch (e: Exception) { break }
                if (i < 0) continue
                val buf = c.getOutputBuffer(i)
                if (buf != null && info.size > 0) {
                    val out = ByteArray(info.size)
                    buf.position(info.offset); buf.get(out)
                    val key = info.flags and (MediaCodec.BUFFER_FLAG_KEY_FRAME or MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                    onFrame(out, key)
                }
                c.releaseOutputBuffer(i, false)
            }
        }
    }

    fun stop() {
        running = false
        runCatching { codec?.stop(); codec?.release() }
        codec = null
    }
}
