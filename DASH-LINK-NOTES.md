# Dash link notes (from public docs of RideScreen AA / OpenCfMoto / OpenCFLink)

Status: collected from READMEs and release notes only. Wire formats (PXC messages, QR payload) are NOT
documented there; they must be read from the source code (AGPL-3.0). Items marked [?] are unverified for
our Zontes 368K V1.

## Connection sequence
1. Dash shows a pairing QR (an Android-style and an iOS/CarPlay-style QR exist; the iOS one often works
   when the Android one is flaky). The QR gives bike model + Wi-Fi credentials. Remember it: no rescan later.
2. Join the dash Wi-Fi. Usually a normal AP; some dashes are Wi-Fi Direct group owners
   (`DIRECT-go-CFMOTO-...`, no default route, dash found at `.1` of the phone's own /24).
   - Use WifiNetworkSpecifier / requestNetwork WITH a timeout and a wall-clock watchdog: a request for an
     absent AP otherwise stays pending forever.
   - Pin sockets to that Network. An always-on VPN kill-switch makes this fail with EPERM.
3. Control socket ("PXC link"): the dash may never send its own heartbeat, and drops the session after
   ~7 s of silence. The phone must emit a handshake/heartbeat itself.
4. Media plane is TCP. The dash pulls video at ~24 fps: pace the encoder to it. Dropped frames break the
   H.264 chain (green flashing) -> request a keyframe on overrun. Keyframes every ~5 s are enough
   (IDR ~20 KB vs P-frame ~500 B).
5. The dash reports a model id / SDK version; pick a screen profile from it (e.g. 800x400 landscape,
   720x712 touch). Unknown dash: learn the panel size and remember it. Zontes 368 G/K/E have a known
   profile in RideScreen AA [?: exact numbers not published].
6. Some dashes show a stuck clock (00:00) after connect; RideScreen AA detects it and reconnects once (~3 s).

## Bluetooth
- Auto-connect trigger: connect when the dash Bluetooth device connects (Zontes names start with "ZT").
- Handlebar buttons do NOT travel on the PXC link; they arrive as Bluetooth AVRCP (media keys / absolute
  volume jumps). Only one app can own media buttons at a time.
- 2.4 GHz Wi-Fi video and Bluetooth audio compete; a lower picture bitrate leaves room for audio.

## Power / lifecycle
- Foreground service + wake lock keeps the link alive with the screen off.
- Give up searching for an absent bike after ~2-20 min to save battery.
- Relaunching another app (e.g. Android Auto) from the background needs "Display over other apps".

## Android Auto source
- AA renders fixed sizes and can't draw some dash sizes: tell it to keep its UI inside margins, then crop.
- Receiver is derived from headunit-revived (AGPL); app dials AA's head-unit server on the phone.
- Pipeline is decode -> recompose (fit/fill/margins) -> re-encode, which heats the phone.

## To read in the source next
QR payload format, handshake byte layout, heartbeat message and interval, video/keyframe request messages,
touch/button message layout, the Yunmo fallback.
