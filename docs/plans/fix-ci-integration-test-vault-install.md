# Plan: Fix Integration Tests — Remove Flaky Vault CLI apt Install

## Problem

The `Integration Tests` job fails on every CI run. The `Configure Vault` step installs vault
CLI using the deprecated `apt-key add` path:

```bash
curl -fsSL https://apt.releases.hashicorp.com/gpg | sudo apt-key add -
sudo apt-add-repository "deb [arch=amd64] https://apt.releases.hashicorp.com $(lsb_release -cs) main"
sudo apt-get update && sudo apt-get install vault
```

This is unreliable: `apt-key add` is deprecated in Ubuntu 22.04+, the HashiCorp apt repo
intermittently returns 503, and `$(lsb_release -cs)` can fail if the package is missing.
None of the vault CLI commands do anything that `curl` cannot — they all talk to the Vault
HTTP API over localhost.

Additionally, the vault service container uses `hashicorp/vault:latest`, which is a
floating tag that can silently change behaviour.

## Fix

**File:** `.github/workflows/java-ci.yml`

Two changes:
1. Pin vault service image from `latest` → `1.15.6`
2. Replace the entire `Configure Vault` step body with `curl` calls to the Vault HTTP API

### Change 1 — Pin vault service container image

**Old:**
```yaml
      vault:
        image: hashicorp/vault:latest
```

**New:**
```yaml
      vault:
        image: hashicorp/vault:1.15.6
```

### Change 2 — Replace `Configure Vault` step body

**Old (lines 130–151):**
```yaml
      - name: Configure Vault
        env:
          VAULT_ADDR: http://localhost:8200
          VAULT_TOKEN: root
        run: |
          # Install vault CLI
          curl -fsSL https://apt.releases.hashicorp.com/gpg | sudo apt-key add -
          sudo apt-add-repository "deb [arch=amd64] https://apt.releases.hashicorp.com $(lsb_release -cs) main"
          sudo apt-get update && sudo apt-get install vault

          # Enable RabbitMQ secrets engine
          vault secrets enable rabbitmq || true

          # Configure RabbitMQ connection
          vault write rabbitmq/config/connection \
            connection_uri="http://localhost:15672" \
            username="guest" \
            password="guest"

          # Create a role
          vault write rabbitmq/roles/rabbitmq-role \
            vhosts='{"/":{\"write\": \".*\", \"read\": \".*\", \"configure\": \".*\"}}'
```

**New:**
```yaml
      - name: Configure Vault
        env:
          VAULT_ADDR: http://localhost:8200
          VAULT_TOKEN: root
        run: |
          # Enable RabbitMQ secrets engine (ignore 400 if already mounted)
          curl -sf -X POST \
            -H "X-Vault-Token: ${VAULT_TOKEN}" \
            -d '{"type":"rabbitmq"}' \
            "${VAULT_ADDR}/v1/sys/mounts/rabbitmq" || true

          # Configure RabbitMQ connection
          curl -sf -X POST \
            -H "X-Vault-Token: ${VAULT_TOKEN}" \
            -d '{"connection_uri":"http://localhost:15672","username":"guest","password":"guest"}' \
            "${VAULT_ADDR}/v1/rabbitmq/config/connection"

          # Create a role
          curl -sf -X POST \
            -H "X-Vault-Token: ${VAULT_TOKEN}" \
            -d '{"vhosts":"{\"\/\":{\"write\":\".*\",\"read\":\".*\",\"configure\":\".*\"}}"}' \
            "${VAULT_ADDR}/v1/rabbitmq/roles/rabbitmq-role"
```

## Before You Start

- Branch (all work): `fix/ci-stabilization` in `rabbitmq-client-java` — create from `origin/main`
- Read `.github/workflows/java-ci.yml` in full before touching it

## Definition of Done

- [ ] `.github/workflows/java-ci.yml` updated with both changes (vault image pinned, Configure Vault step replaced)
- [ ] No other files modified
- [ ] Committed to `fix/ci-stabilization` with message:
  `fix(ci): replace flaky vault apt install with curl API calls; pin vault image to 1.15.6`
- [ ] Pushed to `origin/fix/ci-stabilization` — do NOT report done until push succeeds
- [ ] After push: report commit SHA

## What NOT to Do

- Do NOT create a PR
- Do NOT skip pre-commit hooks (`--no-verify`)
- Do NOT modify any files other than `.github/workflows/java-ci.yml`
- Do NOT commit to `main`
- Do NOT install vault CLI — the entire point is to remove that dependency
