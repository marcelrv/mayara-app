#!/usr/bin/env bash
# update_mayara.sh — Pull the latest mayara-server changes from upstream and rebuild.
#
# This script:
#   1. Pulls the latest changes from the upstream mayara-server (MarineYachtRadar/mayara-server).
#   2. Rebuilds libradar.so.
#
# Usage:
#   bash scripts/update_mayara.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SUBMODULE_DIR="${REPO_ROOT}/mayara-server"

if [[ ! -d "${SUBMODULE_DIR}/.git" ]] && [[ ! -f "${SUBMODULE_DIR}/.git" ]]; then
    echo "ERROR: mayara-server submodule not initialised." >&2
    echo "       Run: git submodule update --init --recursive" >&2
    exit 1
fi

cd "${SUBMODULE_DIR}"

# Guard: warn if there are uncommitted changes (they would be overwritten)
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "WARNING: Uncommitted changes detected in mayara-server submodule."
    read -rp "Continue anyway? [y/N] " confirm
    if [[ "${confirm}" != "y" && "${confirm}" != "Y" ]]; then
        echo "Aborted."
        exit 0
    fi
fi

echo "==> Fetching latest changes from upstream..."
git fetch origin

echo "==> Merging origin/main..."
git merge origin/main --no-edit

echo "==> Running upstream tests to verify merge..."
cargo test --features emulator 2>&1 | tail -20

echo "==> Rebuilding libradar.so..."
cd "${REPO_ROOT}"
bash scripts/build_jni.sh

echo ""
echo "==> mayara-server updated and libradar.so rebuilt successfully."
echo "    Commit the updated submodule pointer:"
echo "    git add mayara-server && git commit -m 'chore: update mayara-server submodule'"
