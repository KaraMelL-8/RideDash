package com.example.ridedash.display

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Clock drawn on the virtual display (stand-in for the real dash UI). onTick fires on each redraw. */
class TestPresentation(ctx: Context, d: Display, private val onTick: () -> Unit) : Presentation(ctx, d) {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var tv: TextView
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val tick = object : Runnable {
        override fun run() { tv.text = fmt.format(Date()); onTick(); h.postDelayed(this, 500) }
    }
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        tv = TextView(context).apply { textSize = 72f; setTextColor(Color.WHITE); setBackgroundColor(Color.BLACK); gravity = Gravity.CENTER }
        setContentView(tv); h.post(tick)
    }
    override fun onStop() { h.removeCallbacks(tick); super.onStop() }
}
