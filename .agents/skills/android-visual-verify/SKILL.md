---
name: android-visual-verify
description: Build, run, capture, and visually verify an Android app on an emulator or connected device. Use after UI or interaction changes when a real rendered proof is needed.
---

# Android visual verification

Compile before attempting visual claims. Install only on the explicitly chosen
emulator or connected device; do not replace another app or uninstall data
without authorization.

## Checks

- Launch the expected activity and capture the target screen.
- Check that the screen fits the viewport, scrolls deliberately, and keeps the
  message composer or primary action reachable when the keyboard is open.
- Exercise every changed action once: success, validation error, loading or
  disabled state, and cancellation where applicable.
- Read logs only to diagnose a visible failure; do not present absence of logs
  as proof of correct behavior.

## Reporting

State the device/emulator, APK version, exact path tested, and which visible
states were captured. If no device is connected, report a build-only result and
leave device validation pending.

