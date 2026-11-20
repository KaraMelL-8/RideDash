# RideDash
In-phone "head unit" for motorcycle TFT dashes (Zontes 368K V1 target).
Key goal: connect like Android Auto / CarPlay so the PHONE SCREEN CAN BE OFF (not screen mirroring).

## Two sources, one pipeline (user can switch in the app)
`VideoSource` -> H.264 -> `DashLink` -> dash. Sources: OWN (we render on a virtual display; no Android Auto needed)
and AA (Android Auto as source, like RideScreen AA; needs AA developer mode + head-unit server; NOT ported yet).

## Architecture
`StreamService` (foreground service + CPU/Wi-Fi locks) owns: VirtualDisplay -> H264Encoder -> DashLink.
The picture is rendered on a virtual display, so it does not depend on the real screen.

## Status (nothing here has been compiled or run on a device yet)
- [x] Stage 0: foreground service, wake locks, screen on/off tracking, stats + pass/fail verdict.
- [ ] Stage 0b: can Amap run on the virtual display? (Shizuku/ADB: ShellBridge)
- [ ] Stage 1: real DashLink (QR -> Wi-Fi join -> PXC handshake/heartbeat -> paced H.264). See docs/DASH-LINK-NOTES.md
- [ ] Stage 2: car-style launcher UI, handlebar-button input

## Screen-off test (stage 0)
Install, allow notifications, tap "ยกเว้นการประหยัดแบต", tap "เริ่มสตรีมทดสอบ", press power, wait 60 s,
wake the phone and read the report. PASS = drawing + encoding continued while the screen was off.

## Build
Push to GitHub and run the "Build debug APK" workflow (Actions), or open in Android Studio.

## License
Code ported from RideScreen AA / OpenCfMoto is AGPL-3.0-or-later: keep NOTICE credits and publish source.
