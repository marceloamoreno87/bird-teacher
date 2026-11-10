import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, writeFileSync, readdirSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn, spawnSync } from 'node:child_process';

const script = fileURLToPath(new URL('../design-system.mjs', import.meta.url));
function fixture(t) {
  const dir = mkdtempSync(join(tmpdir(), 'scaffold-design-'));
  t.after(() => rmSync(dir, { recursive: true, force: true }));
  return dir;
}
function run(command, dir) {
  return spawnSync(process.execPath, [script, command, dir], { encoding: 'utf8' });
}
function source(dir, data) {
  writeFileSync(join(dir, 'tokens.json'), JSON.stringify(data));
}
const tokens = () => ({
  version: 1,
  defaultTheme: 'light',
  tokens: { palette: { paper: '#ffffff', ink: '#111111' }, space: { unit: '0.25rem' } },
  themes: { light: { background: '{palette.paper}', foreground: '{palette.ink}' }, dark: { background: '{palette.ink}', foreground: '{palette.paper}' } },
});

test('preview serves local tokens and components over loopback without installing a server', async (t) => {
  const dir = fixture(t);
  assert.equal(run('init', dir).status, 0);
  const child = spawn(process.execPath, [script, 'preview', dir], { stdio: ['ignore', 'pipe', 'pipe'] });
  t.after(() => child.kill());
  const url = await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Preview did not start')), 5000);
    child.once('error', reject);
    child.once('exit', (code) => { clearTimeout(timer); reject(new Error(`Preview exited: ${code}`)); });
    child.stdout.on('data', (chunk) => {
      const match = String(chunk).match(/http:\/\/127\.0\.0\.1:\d+/);
      if (match) { clearTimeout(timer); resolve(match[0]); }
    });
  });
  const catalog = await fetch(`${url}/catalog.html`);
  assert.equal(catalog.status, 200);
  assert.match(catalog.headers.get('content-type'), /text\/html/);
  const css = await fetch(`${url}/tokens.css`);
  assert.equal(await css.text(), readFileSync(join(dir, 'tokens.css'), 'utf8'));
  assert.equal((await fetch(`${url}/missing.html`)).status, 404);
});

test('init creates a local design kit and preserves existing work on a second invocation', (t) => {
  const dir = fixture(t);
  const result = run('init', dir);
  assert.equal(result.status, 0, result.stderr);
  for (const name of ['MASTER.md', 'DESIGN.md', 'COMPONENTS.md', 'tokens.json', 'tokens.css', 'catalog.html']) {
    assert.ok(readdirSync(dir).includes(name), name);
  }
  writeFileSync(join(dir, 'MASTER.md'), 'User decisions');
  const repeated = run('init', dir);
  assert.notEqual(repeated.status, 0);
  assert.equal(readFileSync(join(dir, 'MASTER.md'), 'utf8'), 'User decisions');
});

test('build resolves aliases into CSS variables and themes and reflects source edits', (t) => {
  const dir = fixture(t);
  const data = tokens();
  source(dir, data);
  assert.equal(run('build', dir).status, 0);
  const css = readFileSync(join(dir, 'tokens.css'), 'utf8');
  assert.match(css, /--space-unit: 0\.25rem;/);
  assert.match(css, /--background: var\(--palette-paper\);/);
  assert.match(css, /\[data-theme="dark"\]/);
  data.tokens.palette.paper = '#eeeeee';
  source(dir, data);
  assert.equal(run('build', dir).status, 0);
  assert.match(readFileSync(join(dir, 'tokens.css'), 'utf8'), /--palette-paper: #eeeeee;/);
});

for (const [name, mutate] of [
  ['unknown alias', (data) => { data.themes.light.background = '{palette.missing}'; }],
  ['alias cycle', (data) => { data.tokens.palette.ink = '{palette.paper}'; data.tokens.palette.paper = '{palette.ink}'; }],
  ['incomplete theme', (data) => { delete data.themes.dark.foreground; }],
  ['CSS name collision', (data) => { data.tokens['palette-paper'] = '#000000'; }],
  ['CSS injection', (data) => { data.tokens.palette.paper = '#fff; color:red'; }],
  ['external asset', (data) => { data.tokens.palette.paper = 'url(https://example.com/image.png)'; }],
]) {
  test(`build rejects ${name} without replacing the previous stylesheet`, (t) => {
    const dir = fixture(t);
    source(dir, tokens());
    assert.equal(run('build', dir).status, 0);
    const before = readFileSync(join(dir, 'tokens.css'), 'utf8');
    const invalid = tokens();
    mutate(invalid);
    source(dir, invalid);
    assert.notEqual(run('build', dir).status, 0);
    assert.equal(readFileSync(join(dir, 'tokens.css'), 'utf8'), before);
  });
}
