---
name: research-url-audit
description: Audit a collected research URL catalogue in batches, recording ownership, licence, code evidence, Android suitability and a concrete integration decision. Use for OP15 Logos source research; do not use it to add unreviewed dependencies.
---

# Audit de registre d'URLs — OP15 Logos

Use this skill when a request asks to read, compare, classify or integrate the
URLs collected for Logos. Its output is an evidence-backed decision register,
not a claim that every URL has been read.

## Start with the local register

1. Refresh `docs/URL_REGISTRY.csv` from the Markdown sources:

   ```powershell
   pwsh -File .agents/skills/research-url-audit/scripts/build_url_registry.ps1
   ```

   Then test availability without changing audit status:

   ```powershell
   pwsh -File .agents/skills/research-url-audit/scripts/test_url_registry.ps1
   ```

2. Read `docs/SOURCE_AUDIT.md` and `docs/CODE_COMPARISON.md` first. Preserve
   existing row statuses when refreshing the registry.
3. Work one thematic batch at a time. For a repository, inspect licence,
   dependency manifest, public interfaces and tests. For a paper, inspect the
   abstract, method, evaluation setting and stated limitation.

## Status vocabulary

Only these values are allowed in `docs/URL_REGISTRY.csv`:

- `known` — collected only; not examined.
- `opened` — page consulted, no code or method audit yet.
- `audited` — owner, purpose, licence/terms, interfaces and limitations checked.
- `experimented` — a bounded, reproducible local experiment has run.
- `integrated` — a reviewed adapter or code change is present and tested.
- `rejected` — unsuitable, unavailable, incompatible or licence-blocked; state why.

Never infer `audited`, `experimented` or `integrated` from a README claim.
An HTTP response only establishes the separate `availability` field; redirects,
login pages and a `200` response are not an audit.

## Integration rules

- Do not embed Node, Python, CUDA, Lean or a remote service into the APK merely
  because it is useful on a PC.
- Prefer a small Android interface and an optional PC/service adapter.
- Preserve provenance: any fact or result needs source URL, retrieval date,
  verification method, status and linked claim identifier.
- A model response is an `unverified` claim until an independent verifier has
  produced a result.
- Check licence compatibility before copying code or datasets. Keep datasets
  such as FOLIO out of the production APK unless redistribution terms have been
  reviewed.
- For source clones, keep them under `vendor-audit/` and commit only audit
  findings; do not vendor their whole Git histories.

## Required audit note

For every completed batch, append a compact table to `docs/SOURCE_AUDIT.md`:
URL, factual role, code or method inspected, licence/constraint, Android cost,
decision, and the specific next verification. If code comparisons influence
architecture, also update `docs/CODE_COMPARISON.md`.

## Stop conditions

Stop a source audit when access requires credentials, a paid service, an
unknown licence, or a potentially destructive install. Record the blocker as
`rejected` or `known` with its reason; do not fabricate an audit result.

