# Gerar registros de decisão

Entrada: `$adrs-management generate [MODULE...|arquivo...] [--all] [--context-dir=PATH] [--language=LOCALE] [--include-consider] [--output-dir=PATH]`.

- Destino base: `docs/adrs`, alterável com `--output-dir`.
- Selecione candidatos em `potential-adrs/must-document/{MODULE}/`; inclua `consider` somente com `--include-consider`.
- `--all` seleciona todos os módulos. Sem alvo identificável, solicite a seleção.
- `--context-dir` fornece contexto adicional; `--language` define o idioma.

Leia [adr-generator](../agents/adr-generator.md) e execute para cada candidato selecionado na sessão atual. Preserve contexto, alternativas, consequências, evidências e relações com ADRs existentes. Use o placeholder `XXX` conforme o procedimento; informe que a numeração definitiva permanece pendente.

Salve em `{output-dir}/generated/{MODULE}/`, ou `needs-input/` quando faltar informação essencial. Preserve registros existentes e sinalize conflitos antes de sobrescrever conteúdo. Confira links, idioma, arquivos gerados e lacunas. Informe a lista de entregas e as pendências; a geração de um registro não transforma proposta em decisão aprovada.
