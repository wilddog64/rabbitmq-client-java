# Retrospective — ConnectionManager NPE Fix

**Date:** 2026-04-10
**Milestone:** `fix/connection-manager-get-stats-npe`
**PR:** #3 — merged to main (`723eb7fc`)
**Participants:** Claude, Codex, Copilot

## What Went Well

- Fix was minimal and correct on first pass — Codex guarded `getCacheProperties()` exactly as specced
- Copilot review caught two real issues (overly broad catch + missing test) before merge
- All 3 Copilot threads addressed and resolved in a single follow-up commit
- CHANGELOG.md bootstrapped as part of this PR — repo now has a change history baseline
- Test added via `ReflectionTestUtils` without requiring Mockito setup changes

## What Went Wrong

- Original spec used `catch (Exception ignored)` — too broad; should have specified `catch (NullPointerException)` from the start
- CHANGELOG format was wrong (`[1.0.1] - Unreleased` violates Keep a Changelog spec) — Copilot caught it
- No unit test was included in the original fix spec — Copilot caught the gap

## Process Rules Added

| Rule | Where |
|------|--------|
| Tag `copilot-pull-request-reviewer[bot]` immediately after `gh pr create` in all managed repos | `memory/feedback_copilot_autotag.md` |

## Decisions Made

- CHANGELOG.md follows Keep a Changelog strictly: unreleased fixes go under `[Unreleased]` only; a dated version header is added only at release time
- No Mockito added to pom.xml — `ReflectionTestUtils` + Mockito (bundled via `spring-boot-starter-test`) is sufficient for unit-level field injection

## Theme

A focused startup-probe fix: `CachingConnectionFactory.getCacheProperties()` throws NPE before any AMQP channel is opened (Spring AMQP 3.1.0), causing every `/actuator/health` probe to fail during pod boot. The fix was straightforward but the original spec was too permissive — `catch (Exception)` masks future regressions. Copilot correctly tightened the catch type and demanded a test. Both gaps were addressed cleanly. The PR also established CHANGELOG.md and a test baseline for the `ConnectionManager` class.
