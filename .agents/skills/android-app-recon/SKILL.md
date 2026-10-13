---
name: android-app-recon
description: Map an Android application's screens, navigation, state ownership, and incomplete interactions before reconstructing or extending multiple screens. Use for product-wide UI work, not a one-screen visual adjustment.
---

# Android application reconnaissance

Create a compact inventory before changing multiple screens: entry activity,
navigation destinations, state stores, network/native boundaries, permissions,
and the actual user journeys. Treat source code and a running build as evidence;
do not infer functionality from labels or mock UI.

## Deliverable

For each screen or flow, record:

- purpose and entry point;
- reads, writes, and external effects;
- loading, empty, error, interrupted, and success states;
- incomplete controls or mock values;
- testable acceptance condition.

## Rules

- Separate a visual replica from a working feature. A navigation button without
  a destination is an incomplete interaction.
- Preserve user data and existing routes during refactors.
- Do not enable browser, file, shell, Git, MCP, root, or network actions merely
  because an interface contains a button. Require the app's approval contract.
- Use the resulting inventory to sequence work by reachable user flows, not by
  isolated components.

