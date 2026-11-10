import test from 'node:test';
import assert from 'node:assert/strict';
import { cpSync, existsSync, mkdtempSync, mkdirSync, readdirSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';

function fixture(t) {
  const root = mkdtempSync(join(tmpdir(), 'scaffold-harness-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  cpSync(new URL('../', import.meta.url), join(root, 'scripts'), {
    recursive: true,
    filter: (source) => !source.endsWith('/test'),
  });
  const git = spawnSync('git', ['init', '--quiet', '--initial-branch=main', root]);
  assert.equal(git.status, 0);
  return root;
}

test('init before scaffolding succeeds without generating app state', (t) => {
  const root = fixture(t);
  const before = readdirSync(root).sort();
  const result = spawnSync('bash', ['scripts/init.sh'], { cwd: root, encoding: 'utf8' });
  assert.equal(result.status, 0, result.stdout + result.stderr);
  assert.match(result.stdout, /scaffold/i);
  assert.deepEqual(readdirSync(root).sort(), before);
});

test('environment resolves an unborn branch with the scaffold database namespace', (t) => {
  const root = fixture(t);
  const result = spawnSync('bash', ['-c', 'source scripts/resolve-env.sh; printf "%s\\n" "$APP_DB_NAME"'], {
    cwd: root, encoding: 'utf8',
  });
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /scaffold_main/);
});

test('backend gates fail before scaffolding without invoking or downloading tools', (t) => {
  const root = fixture(t);
  const bin = join(root, 'bin');
  mkdirSync(bin);
  const marker = join(root, 'unexpected-tool-execution');
  writeFileSync(join(bin, 'npx'), '#!/bin/sh\ntouch "$HARNESS_TEST_MARKER"\nexit 99\n', { mode: 0o755 });
  const result = spawnSync(process.execPath, ['scripts/run-gates.mjs'], {
    cwd: root, encoding: 'utf8',
    env: { ...process.env, PATH: `${bin}:${process.env.PATH}`, HARNESS_TEST_MARKER: marker },
  });
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /scaffold|backend/i);
  assert.equal(existsSync(marker), false);
});

function portableHarness(t) {
  const root = mkdtempSync(join(tmpdir(), 'scaffold-portable-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  for (const path of ['scripts', '.agents', '.codex', '.playwright', 'docs', 'AGENTS.md', 'README.md', 'plugins.md']) {
    cpSync(new URL(`../../${path}`, import.meta.url), join(root, path), { recursive: true });
  }
  const bin = join(root, 'bin');
  const profile = join(root, 'empty-profile');
  mkdirSync(bin);
  mkdirSync(profile);
  symlinkSync(process.execPath, join(bin, 'node'));
  symlinkSync('/bin/bash', join(bin, 'bash'));
  const marker = join(root, 'unexpected-tool-execution');
  for (const tool of ['codex', 'claude', 'git', 'curl', 'wget', 'npm', 'npx']) {
    writeFileSync(join(bin, tool), '#!/bin/bash\nprintf "%s\\n" "$0" >> "$HARNESS_TEST_MARKER"\nexit 99\n', { mode: 0o755 });
  }
  return {
    root, profile, marker,
    env: { PATH: bin, HOME: profile, CODEX_HOME: join(profile, '.codex'), HARNESS_TEST_MARKER: marker },
  };
}

function runPortableSetup(copy) {
  return spawnSync('/bin/bash', [join(copy.root, 'scripts/setup-codex.sh')], {
    cwd: copy.profile, encoding: 'utf8', env: copy.env,
  });
}

test('a copied scaffold validates with an empty profile and no plugin or download tools', (t) => {
  const copy = portableHarness(t);
  const result = runPortableSetup(copy);
  assert.equal(result.status, 0, result.stdout + result.stderr);
  assert.match(result.stdout, /16 skills/);
  assert.equal(existsSync(copy.marker), false);
  assert.deepEqual(readdirSync(copy.profile), []);
});

test('a copied scaffold creates a design kit without plugin or download tools', (t) => {
  const copy = portableHarness(t);
  const design = join(copy.root, 'design-example');
  const result = spawnSync(process.execPath, [join(copy.root, 'scripts/design-system.mjs'), 'init', design], {
    cwd: copy.profile, encoding: 'utf8', env: copy.env,
  });
  assert.equal(result.status, 0, result.stdout + result.stderr);
  assert.ok(existsSync(join(design, 'tokens.css')));
  assert.ok(existsSync(join(design, 'catalog.html')));
  assert.equal(existsSync(copy.marker), false);
  assert.deepEqual(readdirSync(copy.profile), []);
});

test('setup fails if a bundled procedure is missing instead of downloading a replacement', (t) => {
  const copy = portableHarness(t);
  rmSync(join(copy.root, '.agents/skills/adrs-management/references/upstream/agents/adr-analyzer.md'));
  const result = runPortableSetup(copy);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /adr-analyzer\.md/);
  assert.equal(existsSync(copy.marker), false);
});

test('setup rejects a procedure symlink that would depend on a personal profile', (t) => {
  const copy = portableHarness(t);
  const procedure = join(copy.root, '.agents/skills/adrs-management/references/upstream/agents/adr-analyzer.md');
  const personalFile = join(copy.profile, 'adr-analyzer.md');
  cpSync(procedure, personalFile);
  rmSync(procedure);
  symlinkSync(personalFile, procedure);
  const result = runPortableSetup(copy);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /adr-analyzer\.md/);
  assert.equal(existsSync(copy.marker), false);
});
