# Recursos locais do scaffold

Os quatro recursos antes fornecidos pelos plugins Full Cycle estão incorporados em `.agents/skills/`, com comandos e procedimentos completos. O scaffold não precisa instalar, registrar ou consultar o marketplace `devfullcycle/claude-mkt-place`, nem acessar um cache pessoal de plugins.

| Skill local | Operações | Entrada |
|---|---|---|
| `adrs-management` | `map`, `identify`, `generate`, `link` | [SKILL.md](.agents/skills/adrs-management/SKILL.md) |
| `diagrams-generator` | `c4`, `mermaid` | [SKILL.md](.agents/skills/diagrams-generator/SKILL.md) |
| `project-analizer` | `architecture`, `component`, `dependencies` | [SKILL.md](.agents/skills/project-analizer/SKILL.md) |
| `development-guidelines` | linguagem e parâmetros de stack | [SKILL.md](.agents/skills/development-guidelines/SKILL.md) |

Exemplos:

```text
$adrs-management map
$adrs-management identify AUTH
$diagrams-generator mermaid docs/F01-exemplo/spec.md
$project-analizer architecture
$development-guidelines TypeScript --testing=vitest
```

O Codex descobre as skills no próprio projeto. Os arquivos em `references/upstream/commands/` e `references/upstream/agents/` são referências locais adaptadas; os nomes dos diretórios registram sua origem. Os procedimentos executam na sessão atual e usam as ferramentas disponíveis. `project-analizer` preserva a grafia existente para manter as invocações.

Ao copiar o scaffold, inclua os diretórios ocultos `.agents/`, `.codex/` e `.playwright/`. Confira os recursos com:

```bash
./scripts/setup-codex.sh
```

O comando precisa apenas de Node.js e Bash: valida os arquivos, links e scripts locais. Não instala plugins, acessa rede ou modifica o perfil pessoal. Também funciona fora de um repositório Git. Uma referência local ausente causa falha de validação, sem download automático.

Consultas atuais a documentação, versões e vulnerabilidades usam acesso externo quando a tarefa exigir. Renderizar diagramas exige um renderizador disponível; os fontes podem ser gerados sem ele. Essas ferramentas são independentes do marketplace Full Cycle.

A origem histórica, versões e revisão de referência estão em [.codex/harness-sources.json](.codex/harness-sources.json), apenas para proveniência. Atualizações das referências são manuais e revisadas no scaffold. Para os demais workflows, consulte [docs/harness.md](docs/harness.md).

## Design e prototipação

As skills locais `$design-system` e `$screen-prototype` e a revisão `$frontend-ui-design` oferecem o [fluxo de design autocontido](docs/design-workflow.md). Templates, tokens, catálogo e compilador CSS estão incluídos. As ferramentas externas citadas como inspiração não são dependências instaladas.
