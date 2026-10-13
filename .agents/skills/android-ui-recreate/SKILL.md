---
name: android-ui-recreate
description: Recreate or refine one Android Jetpack Compose screen from a screenshot, mockup, or explicit visual reference. Use for layout fidelity and interaction states; do not use to invent product features.
---

# Android UI recreation

Work one screen at a time. First identify the target route, viewport, visual
reference, and required states (empty, loading, error, populated). Preserve the
existing application architecture unless the requested screen cannot be built
within it.

## Implementation

- Derive spacing, typography, color roles, focus, touch targets, scrolling, and
  system insets from the supplied reference; do not infer hidden screens.
- Prefer Compose Material components and project tokens. Add a reusable
  component only when it appears in more than one required state.
- Keep actions visibly distinguishable: destructive, disabled, loading, and
  selected states must not look identical.
- Make keyboard focus and accessibility labels usable. A control that is not
  reachable or has no visible focus is not complete.

## Validation

- Build the APK after editing.
- If an emulator or device is available, capture the relevant screen and compare
  it against the reference at the same approximate viewport.
- Report visual differences that remain; never claim pixel accuracy without a
  captured comparison.

