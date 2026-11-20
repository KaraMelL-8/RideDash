package com.example.ridedash.stream

import java.util.concurrent.atomic.AtomicLong

/** Live numbers used to prove the stream keeps running while the phone screen is off. */
object StreamStats {
    @Volatile var running = false
    @Volatile var error: String? = null
    @Volatile var filePath = ""
    @Volatile var screenOff = false
    @Volatile private var startedAt = 0L
    @Volatile private var stoppedAt = 0L
    @Volatile private var offSince = 0L
    @Volatile private var offMs = 0L
    @Volatile private var lastTickAt = 0L
    @Volatile private var maxGapMs = 0L
    private val frames = AtomicLong(); private val framesOff = AtomicLong()
    private val ticks = AtomicLong(); private val ticksOff = AtomicLong()
    private val bytes = AtomicLong()

    @Synchronized fun reset(now: Long) {
        startedAt = now; stoppedAt = 0L; offMs = 0L; offSince = 0L; lastTickAt = 0L; maxGapMs = 0L
        frames.set(0); framesOff.set(0); ticks.set(0); ticksOff.set(0); bytes.set(0); error = null
    }
    @Synchronized fun stop(now: Long) {
        if (screenOff) { offMs += now - offSince; offSince = now }
        stoppedAt = now; running = false
    }
    @Synchronized fun setScreenOff(off: Boolean, now: Long) {
        if (off && !screenOff) { screenOff = true; offSince = now }
        else if (!off && screenOff) { offMs += now - offSince; screenOff = false }
    }
    fun onFrame(size: Int) { frames.incrementAndGet(); bytes.addAndGet(size.toLong()); if (screenOff) framesOff.incrementAndGet() }
    fun onTick(now: Long) {
        ticks.incrementAndGet(); if (screenOff) ticksOff.incrementAndGet()
        if (lastTickAt > 0) maxGapMs = maxOf(maxGapMs, now - lastTickAt)
        lastTickAt = now
    }

    fun report(now: Long): String {
        if (startedAt == 0L) return "ยังไม่เริ่มทดสอบ"
        val end = if (running) now else (if (stoppedAt > 0) stoppedAt else now)
        val sec = (end - startedAt) / 1000
        val off = (offMs + if (screenOff && running) now - offSince else 0L) / 1000
        val expectedTicksOff = off * 2 // วาดทุก 500 ms
        val verdict = when {
            error != null -> "เกิดข้อผิดพลาด: $error"
            off < 20 -> "ยังไม่ได้ทดสอบจอดับ: กดปุ่มพาวเวอร์ รอ 30-60 วินาที แล้วปลุกจอ"
            ticksOff.get() >= expectedTicksOff * 8 / 10 && framesOff.get() > 0 ->
                "ผ่าน: ตอนจอดับยังวาดและเข้ารหัสภาพต่อเนื่อง (บนเครื่องนี้)"
            else -> "ไม่ผ่าน: ตอนจอดับงานช้าลงหรือหยุด ลองยกเว้นการประหยัดแบตและทดสอบใหม่"
        }
        return buildString {
            appendLine("สถานะ: ${if (running) "กำลังสตรีม" else "หยุด"}")
            appendLine("เวลารวม: $sec วิ · จอดับรวม: $off วิ")
            appendLine("เฟรมที่เข้ารหัส: ${frames.get()} (ตอนจอดับ ${framesOff.get()})")
            appendLine("วาดภาพ: ${ticks.get()} ครั้ง (ตอนจอดับ ${ticksOff.get()} จากที่ควรได้ ~$expectedTicksOff)")
            appendLine("ช่วงห่างสูงสุดระหว่างการวาด: $maxGapMs ms")
            appendLine("ข้อมูลที่เขียน: ${bytes.get() / 1024} KB")
            if (filePath.isNotEmpty()) appendLine("ไฟล์: $filePath")
            append(verdict)
        }
    }
}
