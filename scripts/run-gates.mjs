#!/usr/bin/env node
import { existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const explicit = process.argv.find((arg) => arg.startsWith('--cwd='));
const backend = explicit ? resolve(explicit.slice('--cwd='.length)) : resolve(root, 'apps/backend');

if (!existsSync(resolve(backend, 'package.json')) || !existsSync(resolve(backend, 'tsconfig.json'))) {
  console.error('[gates] Backend ausente. Crie o scaffold antes de executar os gates da aplicação.');
  process.exit(64);
}
if (!existsSync(resolve(root, 'node_modules')) && !existsSync(resolve(backend, 'node_modules'))) {
  console.error('[gates] Dependências ausentes. Execute ./scripts/init.sh após criar o scaffold.');
  process.exit(64);
}

// Use the canonical runner as the sole definition of the six architecture gates.
process.argv = process.argv.filter((arg) => !arg.startsWith('--cwd='));
process.argv.push(`--cwd=${backend}`);
await import('../.agents/skills/clean-arch/scripts/run-gates.mjs');
