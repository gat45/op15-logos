---
name: logos-replica
description: Audit and evolve OP15 Logos through a clean-room product cycle: recon, architecture, design, build, visual test, and parity tracking. Use for multi-screen application work; do not use to copy another app's code, assets, text, or private behavior.
---

# Logos replica cycle

Use this cycle to turn an intended assistant capability into a verified Android
flow. It adapts the public Replica method to Logos; it does not clone a third
party application.

## Order

1. **Recon:** create or update `replica/recon.md` and `replica/features.csv`.
   Mark each feature `working`, `partial`, `planned`, or `blocked`, with a
   concrete proof or blocker.
2. **Architecture:** add only the storage, lifecycle, permission, and native
   contracts required by the next end-to-end flow.
3. **Design and build:** implement the flow screen by screen, including empty,
   loading, error, disabled, and interrupted states.
4. **Test:** compile and exercise the changed flow. A device capture is required
   for a visual claim; otherwise report build-only verification.
5. **Parity:** update `replica/parity.md` with must/should/could coverage. A
   feature is complete only when its acceptance evidence is linked.

## Boundaries

- Never copy another application's source, branding, text, private APIs, or
  proprietary assets. Reuse only publicly documented patterns and fresh code.
- A visible button is not a feature. Record its real data flow, external effect,
  permission gate, and failure mode.
- Browser, filesystem, shell, Git, MCP, network, microphone, and root actions
  remain disabled until their approval and audit contract is implemented.
- Do not call an accelerator active merely because native libraries packaged;
  require device evidence for loading and measured execution.

Read `replica/recon.md` before modifying multiple screens. Read
`replica/features.csv` before claiming parity.
