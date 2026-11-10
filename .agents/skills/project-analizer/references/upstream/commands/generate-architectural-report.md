# Análise arquitetural completa

Entrada: `$project-analizer architecture [escopo] [--project-folder=PATH] [--output-folder=PATH] [--ignore-folders=LISTA]`.

Escopo padrão: raiz do projeto. Destino padrão: `docs/agents`, relativo ao projeto.

1. Crie `MANIFEST.md` com escopo, exclusões, data e estados pendentes para dependências, arquitetura e componentes.
2. Execute [dependency-auditor](../agents/dependency-auditor.md) e [architectural-analyzer](../agents/architectural-analyzer.md) sequencialmente. Grave relatórios em `dependency-auditor/` e `architectural-analyzer/`. Preserve o código analisado; consultas externas não verificadas permanecem explícitas.
3. Extraia do relatório arquitetural os componentes observados. Para cada um, execute [component-deep-analyzer](../agents/component-deep-analyzer.md), salvando em `component-deep-analyzer/`. Registre regras de negócio, relações e testes com caminhos reais.
4. Atualize o manifesto após cada fase. Use COMPLETED quando houver arquivo e evidência; FAILED para erro e PARTIAL quando alguma análise permanecer incompleta. Continue fases independentes após falhas e registre a causa.
5. Confira todos os arquivos e crie `PROJECT-OVERVIEW-{timestamp}.md` com visão da arquitetura, saúde das dependências, componentes, riscos e recomendações baseadas nos relatórios. Forneça links relativos para as evidências e liste pendências.

Execute as fases na sessão atual. Quando o usuário autorizar agentes paralelos, distribua apenas análises independentes e serialize a consolidação do manifesto. Antes do scaffold, declare que só existe o harness e limite as conclusões a ele; templates não demonstram arquitetura implementada do produto.
