package com.example.ridedash.link

/** Needs shell-level rights (Shizuku or ADB). Not implemented yet - see STAGE 0 tests. */
interface ShellBridge {
    fun launchOnDisplay(displayId: Int, component: String)   // am start --display <id> -n <component>
    fun tap(displayId: Int, x: Int, y: Int)                  // input -d <id> tap x y
}
// Map app is user-selected via MapAppRepository (no hard-coded Amap).
