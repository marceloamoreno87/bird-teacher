#!/usr/bin/env node
import assert from 'node:assert/strict';
import { existsSync, mkdirSync, readdirSync, readFileSync, realpathSync, writeFileSync } from 'node:fs';
import { extname, join, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createServer } from 'node:http';

const templates = fileURLToPath(new URL('../.agents/skills/design-system/templates/', import.meta.url));
const keyPattern = /^[a-z][a-z0-9-]*$/;
const aliasPattern = /^\{([a-z][a-z0-9.-]*)\}$/;
const object = (value) => value !== null && typeof value === 'object' && !Array.isArray(value);

function flatten(tree, prefix = '', result = new Map(), names = new Set()) {
  assert.ok(object(tree), `Grupo de tokens inválido: ${prefix || 'raiz'}; esperado objeto.`);
  for (const [key, value] of Object.entries(tree)) {
    assert.match(key, keyPattern, `Nome inválido: ${key}; esperado kebab-case.`);
    const path = prefix ? `${prefix}.${key}` : key;
    if (object(value)) {
      flatten(value, path, result, names);
      continue;
    }
    assert.ok(typeof value === 'string' && value.trim(), `Token ${path}: esperado texto CSS ou alias {grupo.nome}.`);
    const name = path.replaceAll('.', '-');
    assert.ok(!names.has(name), `Colisão de nome CSS: ${path} -> --${name}.`);
    names.add(name);
    result.set(path, value);
  }
  return result;
}

function validateReferences(tokens) {
  const checked = new Set();
  function visit(path, visiting = new Set()) {
    assert.ok(tokens.has(path), `Alias desconhecido: ${path}.`);
    assert.ok(!visiting.has(path), `Ciclo de aliases: ${[...visiting, path].join(' -> ')}.`);
    if (checked.has(path)) return;
    const value = tokens.get(path);
    const alias = value.match(aliasPattern);
    if (alias) visit(alias[1], new Set([...visiting, path]));
    else assert.ok(!/[;{}<>\\]|\/\*|\*\/|url\s*\(|var\s*\(|@import/i.test(value), `Valor inválido em ${path}: use valor CSS local ou alias {grupo.nome}.`);
    checked.add(path);
  }
  for (const path of tokens.keys()) visit(path);
}

function declarations(tokens) {
  return [...tokens].map(([path, value]) => {
    const alias = value.match(aliasPattern);
    const css = alias ? `var(--${alias[1].replaceAll('.', '-')})` : value;
    return `  --${path.replaceAll('.', '-')}: ${css};`;
  }).join('\n');
}

function compile(data) {
  assert.equal(data.version, 1, 'Versão de tokens esperada: 1.');
  assert.ok(object(data.themes) && Object.keys(data.themes).length, 'Esperado ao menos um tema.');
  assert.ok(Object.hasOwn(data.themes, data.defaultTheme), 'defaultTheme deve identificar um tema existente.');
  const base = flatten(data.tokens);
  validateReferences(base);
  const defaults = flatten(data.themes[data.defaultTheme]);
  const blocks = ['/* Gerado de tokens.json. Edite a fonte e execute design-system.mjs build. */', `:root {\n${declarations(base)}\n${declarations(defaults)}\n}`];
  let expected;
  for (const [name, tree] of Object.entries(data.themes)) {
    assert.match(name, keyPattern, `Tema inválido: ${name}.`);
    const theme = flatten(tree);
    const keys = [...theme.keys()].sort();
    expected ??= keys;
    assert.deepEqual(keys, expected, `Tema ${name}: todos os temas devem definir os mesmos tokens semânticos.`);
    const cssNames = new Set([...base.keys()].map((key) => key.replaceAll('.', '-')));
    for (const path of theme.keys()) {
      assert.ok(!cssNames.has(path.replaceAll('.', '-')), `Token de tema conflita com token base: ${path}.`);
    }
    validateReferences(new Map([...base, ...theme]));
    blocks.push(`[data-theme="${name}"] {\n${declarations(theme)}\n}`);
  }
  return `${blocks.join('\n\n')}\n`;
}

function init(dir) {
  assert.ok(!existsSync(dir) || readdirSync(dir).length === 0, `Destino não vazio: ${dir}. Preserve o design existente; use build para atualizar CSS.`);
  const files = readdirSync(templates).map((name) => [name, readFileSync(join(templates, name), 'utf8')]);
  const data = JSON.parse(files.find(([name]) => name === 'tokens.json')[1]);
  const css = compile(data);
  mkdirSync(dir, { recursive: true });
  for (const [name, content] of files) writeFileSync(join(dir, name), content, { flag: 'wx' });
  writeFileSync(join(dir, 'tokens.css'), css, { flag: 'wx' });
  console.log(`[design] Kit de exemplo criado em ${dir}. Preencha o briefing antes de adotá-lo como design do produto.`);
}

function preview(dir) {
  const root = realpathSync(dir);
  const types = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png' };
  const home = existsSync(join(root, 'catalog.html')) ? '/catalog.html' : '/design/catalog.html';
  const server = createServer((request, response) => {
    if (!['GET', 'HEAD'].includes(request.method)) { response.writeHead(405).end(); return; }
    try {
      const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
      const path = realpathSync(resolve(root, `.${pathname === '/' ? home : pathname}`));
      if (!path.startsWith(`${root}${sep}`)) { response.writeHead(403).end(); return; }
      const data = readFileSync(path);
      response.writeHead(200, { 'Content-Type': `${types[extname(path)] || 'application/octet-stream'}; charset=utf-8`, 'Cache-Control': 'no-store' });
      response.end(request.method === 'HEAD' ? undefined : data);
    } catch {
      response.writeHead(404).end('Arquivo local não encontrado.');
    }
  });
  server.on('error', (error) => { console.error(`[design] Prévia indisponível: ${error.message}`); process.exitCode = 1; });
  server.listen(0, '127.0.0.1', () => {
    console.log(`[design] Prévia local: http://127.0.0.1:${server.address().port}${home} (Ctrl+C para encerrar).`);
  });
}

try {
  const [command, directory = 'docs/design', ...extra] = process.argv.slice(2);
  assert.ok(['init', 'build', 'preview'].includes(command) && !extra.length, 'Uso: node scripts/design-system.mjs init|build|preview [diretório].');
  const dir = resolve(directory);
  if (command === 'init') init(dir);
  else if (command === 'preview') preview(dir);
  else {
    const css = compile(JSON.parse(readFileSync(join(dir, 'tokens.json'), 'utf8')));
    writeFileSync(join(dir, 'tokens.css'), css);
    console.log(`[design] Tokens e temas validados; CSS gerado em ${join(dir, 'tokens.css')}.`);
  }
} catch (error) {
  console.error(`[design] FALHA: ${error.message}`);
  process.exitCode = 1;
}
