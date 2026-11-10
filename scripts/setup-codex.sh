#!/usr/bin/env bash
set -euo pipefail

command -v node >/dev/null 2>&1 || { echo '[setup] Node.js não encontrado.' >&2; exit 1; }
ROOT_DIR="$(cd -- "${BASH_SOURCE[0]%/*}/.." && pwd)"

node "$ROOT_DIR/scripts/check-harness.mjs"
echo '[setup] Skills disponíveis em .agents/skills; nenhuma instalação de plugin necessária.'
