# Scaffold

## Escopo e fontes

Este repositório está na preparação do harness. O produto e suas etapas serão definidos pelo usuário. Registre requisitos em `docs/PRD.md` quando forem fornecidos; mantenha `docs/prd_progress.json` sem features até essa definição.

O harness do projeto está em `.agents/skills/`, `.codex/` e `scripts/`. Consulte [docs/harness.md](docs/harness.md) para operação e configuração.

Requisitos e instruções atuais do usuário prevalecem sobre templates. Use ai-memory conforme as instruções globais para histórico e handoffs; confirme fatos nos arquivos atuais. Aprovação e fonte canônica de requisitos ficam no repositório e na conversa.

## Fluxo de desenvolvimento

- PRD e etapas: use `prd-writer`; preserve IDs `F01`, dependências, prioridades e ondas.
- Especificar uma feature: use `spec-writer`; produza `docs/FXX-slug/{spec,plan,contract}.md`.
- Implementar uma etapa definida: use `implement-feature`. Para o ciclo completo solicitado, use `implement-and-evaluate`, `evaluator` e `fix-runner`.
- Contratos descrevem comportamento observável. Avaliação registra evidências por item; marque `done` somente após avaliação completa e gates aprovados.
- Use `implement-and-evaluate-tmux` somente quando o usuário pedir execução paralela de uma onda. Cada feature usa worktree e ambiente próprios; fundações que alteram arquivos comuns são sequenciais.
- Commit, push, PR, merge e deploy dependem do escopo autorizado na conversa; a instalação deste harness não os autoriza automaticamente.

## Ambiente

Execute `./scripts/init.sh` antes de trabalho que precise da aplicação; use `./scripts/stop.sh` para encerrar seus processos. Antes do scaffold, `init.sh` apenas informa o estado de preparação. `stop.sh --clean` também remove o banco de desenvolvimento da branch quando Prisma estiver habilitado; use apenas em ambiente descartável autorizado.

A referência de organização é `apps/backend/` e `apps/web/`. O scaffold e as escolhas de banco/frontend pertencem à etapa de fundação. Inicie servidores pelos scripts de ambiente; preserve o isolamento de portas e o prefixo de banco `scaffold_`.

## Backend

Leia a skill `clean-arch` antes de alterações no backend. Preserve `domain/` ← `usecase/` ← `infra/`, DI por construtor e composição em `src/main.ts`. `process.env` fica em `src/config/env.ts`. Handlers validam entrada, chamam casos de uso e delegam erros ao tratamento central.

Mantenha módulos pequenos, tipos explícitos e retornos antecipados. Evite `any`, lógica duplicada e mais de dois níveis de indentação. Comente motivos. Mensagens de erro devem informar valor problemático e formato esperado, sem expor segredos.

Cubra regras de negócio e defeitos com testes significativos; use fakes nomeados para I/O externo. Logs de aplicação são JSON estruturado; saída de CLI é texto legível.

Execute `node scripts/run-gates.mjs` antes de concluir backend: TypeScript, ESLint, Dependency Cruiser, Architecture, Madge e Knip. Reuse os validadores fornecidos; falha de gate é falha, e gate não executado permanece pendente.

## Frontend

Para criar design systems e protótipos, use as skills locais `design-system` e `screen-prototype` conforme [docs/design-workflow.md](docs/design-workflow.md). Mantenha `docs/design/tokens.json` como fonte única de valores; protótipos e implementação consomem os mesmos tokens. Defina a stack antes de adicionar bibliotecas específicas.

Use `playwright-cli` para navegação e verificação no navegador; leia a skill correspondente. Priorize jornadas críticas e regras de negócio. Quando o design do projeto for definido, salve-o em `docs/design/` e use `frontend-ui-design` para avaliar telas com evidências visuais.

## Arquivos e verificação

Mantenha artefatos de feature junto de sua especificação. Temporários ficam em diretório temporário do sistema ou pasta de execução ignorada, fora da raiz. Segredos ficam no ambiente; nunca em relatórios ou versionamento.

Para verificar o harness sem aplicação: `node --test scripts/test/*.test.mjs` e `node scripts/check-harness.mjs`. Isso não substitui os gates nem os testes de uma aplicação futura.
