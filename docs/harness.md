# Harness do projeto

O harness do projeto organiza o desenvolvimento no Codex: requisitos → especificação/plano/contrato → implementação → avaliação → correção. Skills, scripts, templates e validadores estão contidos no projeto. O projeto começa sem features e sem aplicação gerada.

Para o passo a passo de planejamento e desenvolvimento em projetos novos ou existentes, consulte o [guia de desenvolvimento](guia-desenvolvimento.md).

## Configuração

| Local | Responsabilidade |
|---|---|
| `AGENTS.md` | Regras e fontes canônicas do projeto |
| `.codex/config.toml` | Configuração do projeto, sem habilitação de plugins externos |
| `.agents/skills/` | Workflows adaptados e recursos reutilizáveis |
| `.codex/agents/` | Papéis de implementação, avaliação e correção, disponíveis quando houver delegação autorizada |
| `scripts/` | Ambiente, gates, instalação e verificação do harness |
| `.playwright/cli.config.json` | Chromium headless e sessão de navegador isolada |
| `docs/prd_progress.json` | Estado das features; inicialmente vazio |
| `.codex/harness-sources.json` | Proveniência das referências incorporadas ao scaffold |

Modelo, esforço de raciocínio, permissões, MCPs e hooks ai-memory são herdados da configuração pessoal existente. O projeto já está marcado como confiável no Codex local. Em outra máquina, a confiança é uma escolha local do usuário; o instalador não muda permissões.

O Codex descobre skills de projeto em `.agents/skills/`. Os quatro recursos antes fornecidos pelo marketplace Full Cycle estão incorporados como skills locais, com comandos e procedimentos completos. A execução usa os arquivos do scaffold e as ferramentas disponíveis na sessão, sem instalação de plugins ou acesso ao cache pessoal.

## Começar a definir etapas

1. Descreva produto, público, problema e limites. Use `$prd-writer` para elaborar `docs/PRD.md` com IDs, dependências, critérios de aceite e ondas.
2. Use `$spec-writer F01` para produzir `docs/F01-slug/spec.md`, `plan.md` e `contract.md`.
3. Na etapa de fundação, escolha frontend/banco e integre o scaffold. Os templates incluem TypeScript/Fastify, Next.js e PostgreSQL/Prisma como opções; sua presença não executa instalação de aplicação.
4. Use `$implement-feature F01` para apenas implementar ou `$implement-and-evaluate F01` para o ciclo completo. Use `$evaluator F01` e `$fix-runner` separadamente quando necessário.
5. Peça `$implement-and-evaluate-tmux wave N` somente quando quiser executar uma onda em paralelo. Fundações com arquivos comuns permanecem sequenciais.

A próxima etapa é definir os requisitos e as etapas do produto com o usuário.

## Design systems e protótipos

Use `$design-system` para briefing, direção visual, tokens, temas e catálogo local. Use `$screen-prototype` para telas navegáveis e estados das jornadas, consumindo os mesmos tokens. `$frontend-ui-design` registra crítica e evidências. O [guia de design](design-workflow.md) descreve os recursos autocontidos e as referências opcionais de ferramentas. A prototipação HTML/CSS não seleciona a stack da aplicação.

## Ambiente e gates

```bash
./scripts/init.sh
./scripts/stop.sh
node scripts/run-gates.mjs
./scripts/migrate-dev.sh -- --name <mudanca>
```

Antes do scaffold, `init.sh` informa que apenas o harness está pronto, sem gerar estado de aplicação. O runner de gates retorna erro de pré-requisito (código 64) enquanto o backend/dependências não existirem. Isso não representa uma avaliação da aplicação.

Após a fundação, os scripts esperam workspaces npm em `apps/backend` e, opcionalmente, `apps/web`. A presença de `apps/backend/prisma/schema.prisma` habilita o ciclo de banco; a presença do package da web habilita o frontend. Se a stack escolhida mudar, adapte o lifecycle nessa etapa. O backend precisa de `dev` e `/health`; com Prisma, também `prisma:generate` e migrations. O root package precisa declarar os workspaces.

O ambiente usa banco `scaffold_<branch-normalizada>` e procura portas livres a partir de 4000/3000 com offset por branch. Portas, logs e PIDs ficam sob controle dos scripts; variáveis adicionais existentes são preservadas. PostgreSQL já ativo ou Docker Compose podem ser usados pelos scripts de ambiente. Os scripts de banco usam credenciais locais de desenvolvimento e pressupõem ambiente descartável.

`stop.sh` encerra processos identificados deste checkout. `stop.sh --clean` também apaga o banco de desenvolvimento da branch quando habilitado; não o use em bancos compartilhados ou fora do escopo autorizado.

O runner canônico, preservado em `clean-arch/scripts/run-gates.mjs`, executa TypeScript, ESLint, Dependency Cruiser, Architecture, Madge e Knip. O wrapper da raiz valida pré-requisitos e direciona para o backend. Os linters, verificadores e testes do gerador foram preservados junto da skill.

## Scaffold sem substituir o harness

O gerador de scaffold está em `.agents/skills/clean-arch/scripts/scaffold.mjs`. Ele protege arquivos existentes; `--force` não é um mecanismo de integração do harness. Gere em pasta temporária, confira a prévia e incorpore os arquivos de aplicação seletivamente:

```bash
node .agents/skills/clean-arch/scripts/scaffold.mjs ProjetoExemplo --yes --shape backend --no-db --no-install --no-gates --no-git --dry-run
```

Escolha `backend-n-nextjs`/`--db` somente conforme a fundação definida. Preserve `AGENTS.md`, README, `.gitignore`, `.codex/`, `.agents/` e os scripts já adaptados ao integrar. Os templates também mantêm uma opção de compatibilidade `CLAUDE.md` para outros consumidores; o arquivo canônico do projeto é `AGENTS.md`.

## Progresso

`docs/prd_progress.json` usa `schema_version: 1`. O schema detalhado está em `.agents/skills/prd-writer/references/prd-format.md`. O PRD é a fonte de nomes, prioridades, dependências e ondas; o JSON registra execução.

| Status | Significado |
|---|---|
| `pending` | Etapa definida, ainda não iniciada |
| `implementing` | Implementação em andamento |
| `implemented` | Pronta para avaliação |
| `done` | Contrato completo aprovado pelo avaliador |
| `fail` | Falha terminal com motivo registrado |
| `pr-blocked` | Avaliação limpa; integração de PR autorizada bloqueada |
| `removed` | Etapa retirada do PRD, mantendo histórico |

Escreva apenas a entrada da feature em execução, preservando as demais e os campos de origem do PRD. Faça escrita atômica com temporário e rename; em uma mesma worktree, serialize o read-modify-write inteiro. Em paralelo, cada worktree mantém seu JSON e o coordenador consolida as entradas individuais, evitando perda de atualizações.

Datas usam UTC/RFC 3339. `cycles` conta correções; `failure_reason` descreve falha/bloqueio em até 200 caracteres. Ao concluir, grave `completed_at`, `report_path` e limpe `failure_reason`; ao reabrir uma etapa, limpe `completed_at`. Nunca use uma aprovação antiga como evidência da execução atual.

O avaliador gera `eval-report-<timestamp>.md`; o ciclo completo gera `orchestration-<timestamp>.md`. PASS exige evidência; BLOCKED/MANUAL/SKIPPED não equivalem a aprovação. Contratos e relatórios anteriores permanecem preservados.

## Skills locais e ferramentas

As quatro skills de arquitetura e análise e seus exemplos estão em [plugins.md](../plugins.md). Ao copiar o scaffold, preserve os diretórios ocultos e valide os recursos no novo ambiente:

```bash
./scripts/setup-codex.sh
```

O script valida a distribuição local usando Node.js e Bash. Funciona com perfil vazio e sem repositório Git; não executa o CLI do Codex, não instala plugins e não faz downloads. Configuração e instalação de ferramentas pessoais continuam sob controle do usuário.

Navegação usa `playwright-cli` e Chromium headless isolado, configurado em `.playwright/cli.config.json`. Em nova máquina, instale o CLI e execute `playwright-cli install-browser chromium`. Consulte a skill local para snapshots e screenshots. Node, npm e Git atendem o harness; `lsof`/`curl` atendem o lifecycle. tmux é opcional. PostgreSQL/Docker e renderizadores de diagramas serão necessários apenas nas etapas que os utilizarem.

Skills locais ficam disponíveis na próxima atualização de descoberta do Codex; se a sessão atual mantiver o catálogo antigo, abra uma nova sessão na raiz deste scaffold.

## Validação e proveniência

```bash
node scripts/check-harness.mjs
node --test scripts/test/*.test.mjs
node --test .agents/skills/clean-arch/scripts/scaffold/test/*.test.mjs
./scripts/setup-codex.sh
```

Essas verificações cobrem recursos e scripts locais. Os testes incluem uma cópia do scaffold com perfil vazio e ferramentas de plugin/download bloqueadas, além da falha quando falta um procedimento incorporado. Para validar a configuração pelo CLI sem iniciar uma sessão, use `codex --strict-config app-server --print-startup-config` e mantenha a saída privada, pois pode conter detalhes locais. Testes/gates/jornadas da aplicação serão executados após sua criação. O fluxo paralelo completo precisa de features especificadas, um commit base e ambientes reais; não há execução de produto nesta preparação.

Proveniência histórica: [Full Cycle Claude Marketplace](https://github.com/devfullcycle/claude-mkt-place), revisão registrada em `.codex/harness-sources.json`, sem consulta ou instalação durante o setup; documentação oficial de [configuração do Codex](https://learn.chatgpt.com/docs/config-file/config-basic), [skills](https://learn.chatgpt.com/docs/extend/skills) e [plugins](https://developers.openai.com/plugins/build).
