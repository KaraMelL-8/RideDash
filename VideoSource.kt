package com.example.ridedash.source

import android.content.Context

/**
 * Where the picture comes from. Every source ends as H.264 frames pushed into a sink (the DashLink),
 * so the phone screen never has to be on:
 *  - OWN: we render our own UI on a virtual display and encode it.
 *  - AA : Android Auto renders and already produces H.264; we receive it as a fake head unit.
 */
interface VideoSource {
    val label: String
    /** Returns null on success, otherwise a human-readable error. Called on the main thread. */
    fun start(ctx: Context, sink: (ByteArray, Boolean) -> Unit): String?
    fun stop()
}

enum class SourceMode { OWN, AA }

object SourcePrefs {
    private const val FILE = "ridedash"
    private const val KEY = "source_mode"
    fun get(ctx: Context): SourceMode = runCatching {
        SourceMode.valueOf(ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "OWN")!!)
    }.getOrDefault(SourceMode.OWN)

    fun set(ctx: Context, m: SourceMode) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, m.name).apply()
    }
}
