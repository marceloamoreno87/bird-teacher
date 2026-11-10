# Mapear o código para ADRs

Entrada: `$adrs-management map [--project-dir=PATH] [--context-dir=PATH] [--output-dir=PATH]`.

- `--project-dir`: projeto a mapear; padrão `.`.
- `--context-dir`: documentação contextual opcional.
- `--output-dir`: destino; padrão `docs/adrs`.

Leia [adr-analyzer](../agents/adr-analyzer.md) e execute a Phase 1 na sessão atual. Identifique stack, módulos com IDs, escopo e relações transversais. Use os documentos contextuais como apoio e confira suas afirmações nos arquivos atuais.

Produza `{output-dir}/mapping.md` com visão geral, módulos, evidências e orientações para identificação incremental. Declare ausência de aplicação quando houver apenas o harness. Ao terminar, informe o arquivo e os módulos mapeados; a identificação é uma operação separada.
