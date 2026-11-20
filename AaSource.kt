package com.example.ridedash.source

import android.content.Context

/**
 * Android Auto as the picture source (like RideScreen AA): we act as a fake head unit that connects to
 * Android Auto's head-unit server and receive its H.264 video. NOT IMPLEMENTED YET: the AA client
 * (transport, SSL, protobuf channels) must be ported from RideScreen AA (AGPL-3.0-or-later).
 */
class AaSource : VideoSource {
    override val label = "Android Auto"
    override fun start(ctx: Context, sink: (ByteArray, Boolean) -> Unit): String? =
        "โหมด Android Auto ยังไม่พร้อม: ต้องพอร์ตไคลเอนต์ AA head unit จาก RideScreen AA ก่อน"
    override fun stop() {}

    companion object {
        const val AA_PACKAGE = "com.google.android.projection.gearhead" // verify on your phone
        fun installed(ctx: Context): Boolean = try {
            ctx.packageManager.getPackageInfo(AA_PACKAGE, 0); true
        } catch (e: Exception) { false }

        const val SETUP =
            "เตรียม Android Auto (ทำครั้งเดียว)\n" +
            "1. เปิดแอป Android Auto > การตั้งค่า แตะเวอร์ชันหลายครั้งเพื่อเปิดโหมดนักพัฒนา\n" +
            "2. เริ่มเซิร์ฟเวอร์ head unit ตามวิธีที่ใช้กับ RideScreen AA (เมนูนักพัฒนาหรือสั่งด้วยเสียง)\n" +
            "3. กลับมากดเริ่มสตรีมในแอปนี้"
    }
}
