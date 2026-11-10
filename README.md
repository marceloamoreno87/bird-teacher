Harness de desenvolvimento do projeto preparado para o Codex.
O produto está aguardando a definição de requisitos e etapas.

- [Operação do harness](docs/harness.md)
- [Guia de desenvolvimento: projetos novos e existentes](docs/guia-desenvolvimento.md)
- [Design systems e protótipos locais](docs/design-workflow.md)
- [Instruções do projeto](AGENTS.md)
- [Skills locais de arquitetura e análise](plugins.md)
- [Progresso das etapas](docs/prd_progress.json)

Para verificar a preparação:

```bash
node scripts/check-harness.mjs
node --test scripts/test/*.test.mjs
./scripts/init.sh
```

Para começar, descreva o produto e a primeira etapa no Codex ou use `$prd-writer`.
