# Auditar dependências

Entrada: `$project-analizer dependencies [escopo] [--project-folder=PATH] [--output-folder=PATH] [--ignore-folders=LISTA]`.

Escopo padrão: raiz do projeto. Destino padrão: `docs/agents/dependency-auditor`, relativo ao projeto. Respeite exclusões explícitas e exclua dependências geradas e templates do harness das conclusões sobre o produto.

Execute [dependency-auditor](../agents/dependency-auditor.md) na sessão atual. Leia manifests e lockfiles para versões usadas. Consulte fontes oficiais para versões atuais, manutenção e vulnerabilidades, usando ferramentas disponíveis. Registre cada informação que não conseguir verificar.

A análise altera apenas o relatório; instalação e atualização de dependências exigem uma solicitação própria. Salve `dependencies-report-{timestamp}.md` com resumo, riscos confirmados, tabela de dependências, evidências, uso no código e itens não verificados. Analise os arquivos críticos existentes, sem inventar arquivos para cumprir uma quantidade fixa. Informe o caminho e as limitações.
