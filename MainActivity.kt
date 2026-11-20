package com.example.ridedash

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.ridedash.source.AaSource
import com.example.ridedash.source.SourceMode
import com.example.ridedash.source.SourcePrefs
import com.example.ridedash.stream.StreamService
import com.example.ridedash.stream.StreamStats

/** Stage 0 test screen: start the foreground service, switch the screen off, read the result. */
class MainActivity : Activity() {
    private lateinit var report: TextView
    private val ui = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() { report.text = StreamStats.report(System.currentTimeMillis()); ui.postDelayed(this, 1000) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad) }
        fun btn(t: String, f: () -> Unit) { col.addView(Button(this).apply { text = t; setOnClickListener { f() } }) }

        col.addView(TextView(this).apply { text = "RideDash · ทดสอบเชื่อมต่อแบบปิดจอได้"; textSize = 20f })
        col.addView(TextView(this).apply {
            text = "วิธีทดสอบ\n1. กด \"ยกเว้นการประหยัดแบต\" และอนุญาตการแจ้งเตือน\n2. กด \"เริ่มสตรีมทดสอบ\"\n" +
                "3. กดปุ่มพาวเวอร์เพื่อดับจอ รอ 60 วินาที\n4. ปลุกจอ แล้วดูผลด้านล่าง\n" +
                "(ผลนี้ยืนยันว่าเครื่องเรนเดอร์และเข้ารหัสต่อได้ตอนจอดับ ยังไม่ใช่การส่งไปจอรถจริง)"
            setPadding(0, pad / 2, 0, pad / 2)
        })
        col.addView(TextView(this).apply { text = "แหล่งภาพ (มีผลตอนกดเริ่มครั้งถัดไป)"; setPadding(0, pad / 2, 0, 0) })
        val group = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val rOwn = RadioButton(this).apply { id = 1001; text = "เรนเดอร์เอง (Amap/แอปที่เลือก ไม่ใช้ Android Auto)" }
        val rAa = RadioButton(this).apply { id = 1002; text = "Android Auto เป็นต้นทาง (แบบ RideScreen)" }
        group.addView(rOwn); group.addView(rAa)
        val aaInfo = TextView(this)
        fun refreshMode() {
            val m = SourcePrefs.get(this)
            group.check(if (m == SourceMode.AA) 1002 else 1001)
            aaInfo.text = if (m == SourceMode.AA)
                (if (AaSource.installed(this)) "พบแอป Android Auto ในเครื่อง\n" else "ไม่พบแอป Android Auto\n") + AaSource.SETUP
            else ""
        }
        group.setOnCheckedChangeListener { _, id ->
            SourcePrefs.set(this, if (id == 1002) SourceMode.AA else SourceMode.OWN); refreshMode()
        }
        col.addView(group); col.addView(aaInfo)
        refreshMode()
        btn("เริ่มสตรีมทดสอบ") { ContextCompat.startForegroundService(this, Intent(this, StreamService::class.java)) }
        btn("หยุด") { startService(Intent(this, StreamService::class.java).setAction(StreamService.ACTION_STOP)) }
        btn("ยกเว้นการประหยัดแบต") { askIgnoreBattery() }
        report = TextView(this).apply { typeface = Typeface.MONOSPACE; textSize = 13f; setPadding(0, pad, 0, 0) }
        col.addView(report)
        setContentView(ScrollView(this).apply { addView(col) })

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    private fun askIgnoreBattery() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, "ยกเว้นการประหยัดแบตแล้ว", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }
    }

    override fun onResume() { super.onResume(); ui.post(poll) }
    override fun onPause() { ui.removeCallbacks(poll); super.onPause() }
}
