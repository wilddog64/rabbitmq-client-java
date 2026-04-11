# Retrospective — CI Fix: RabbitMQ Management API Wait + Docker Hostname

**Date:** 2026-04-11
**Milestone:** CI fix — Docker networking + management API readiness
**PR:** #6 — merged to main (`22c92d96`)
**Participants:** Claude, Copilot

## What Went Well

- Root cause identified correctly from first-principles reasoning: Docker service containers
  communicate via service label hostnames, not `localhost`; `localhost` inside Vault container
  resolves to itself, not RabbitMQ
- The temp branch condition (`fix/rabbitmq-mgmt-api-wait` added to Integration Tests trigger)
  allowed the fix to be verified pre-merge — first time Integration Tests passed on a non-main
  branch; confirmed the fix before landing on main
- Both Copilot findings were valid and quick to address: credentials in curl flag, temp branch
  condition leaking into final commit

## What Went Wrong

- Took three commits to fully fix: (1) management API wait, (2) Docker hostname, (3) Copilot
  fixes — each iteration required a separate CI cycle; root cause could have been diagnosed
  fully before the first push
- The CI flake chain was never properly analyzed before starting: `apt-key` → management API
  not ready → Docker hostname — three distinct bugs piled up; the first PR (PR #5) only fixed
  the outermost one

## Decisions Made

- `connection_uri` in Vault's rabbitmq engine must use the Docker service label hostname
  (`rabbitmq:15672`) not `localhost` — Vault runs inside its own container, not on the runner host
- Temporary branch conditions for CI verification are acceptable but must be reverted in the
  same PR before merge (Copilot enforcement worked correctly here)
- Management API readiness check uses HTTP (`curl -sf /api/overview`) not just TCP (`nc -z`) —
  port open ≠ API ready

## Theme

Three bugs were hiding behind each other. The apt-key flake (PR #5) was the outermost layer;
fixing it revealed the management API readiness gap; fixing that revealed the Docker hostname
mismatch. Each layer required a CI cycle to surface because `Integration Tests` had never
successfully run. The breakthrough was temporarily enabling the job on the fix branch — once
we had a live CI gate, the remaining failures were diagnosable from logs rather than theory.
