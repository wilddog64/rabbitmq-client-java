# Retrospective — CI Stabilization (fix/ci-stabilization)

**Date:** 2026-04-11
**Milestone:** CI fix — remove flaky vault apt install
**PR:** #5 — merged to main (`6268b08a`)
**Participants:** Claude, Codex, Copilot

## What Went Well

- Root cause identified quickly: `apt-key add` deprecated on Ubuntu 22.04+, HashiCorp apt repo
  intermittently 503s — replacing with `curl` against Vault HTTP API removes the dependency entirely
- Codex implemented the two spec changes (image pin + curl replacement) cleanly on first attempt
- Copilot caught a real bug in the `|| true` suppression — valid finding, fixed same session
- Copilot thread replied to and resolved before merge
- CI loop (push → Build and Test SUCCESS → Publish 409 noise) was predictable and did not block

## What Went Wrong

- `fix/ci-stabilization` branch had divergent history from `origin/main` — Codex's initial
  `git checkout -b fix/ci-stabilization origin/main` required a force-push to align; caught by
  Claude pre-approval and corrected before Codex ran
- `Integration Tests` job condition (`github.ref == 'refs/heads/main'`) means the fix can only
  be verified post-merge — no way to confirm the curl approach worked until CI runs on main

## Decisions Made

- Vault CLI dropped entirely in CI — all three operations (`mount`, `config/connection`, `roles`)
  are plain HTTP and need no additional binary
- Vault service image pinned to `1.15.6` for supply-chain safety; update deliberately when
  upgrading Vault
- `Publish to GitHub Packages` 409 failure on `fix/ci-stabilization` is expected noise (v1.0.1
  already published) — not a merge gate

## Theme

A two-commit fix: Codex replaced the brittle apt install path, Copilot tightened the error
handling on the mount call. The workflow's branch condition meant we couldn't run Integration
Tests to confirm the fix pre-merge, but the curl API approach is a direct mechanical substitution
with no logic change — confidence was high enough to merge.
