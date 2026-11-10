# Relacionar ADRs

Entrada: `$adrs-management link [MODULE...] [--validate] [--report-only] [--adrs-path=PATH] [--output-dir=PATH]`.

- `--adrs-path`: padrão `docs/adrs/generated`.
- `--output-dir`: relatórios; padrão `docs/adrs/reports`.
- Sem módulos, examine todos; inclua registros em `needs-input/`, excluindo índices e arquivos arquivados.
- `--validate`: confira apenas os links existentes e grave relatório.
- `--report-only`: detecte relações e grave relatório, preservando os ADRs.

Execute [adr-linker](../agents/adr-linker.md) na sessão atual. Identifique Supersedes, Depends on, Related to e Amends com evidências temporais, semânticas e técnicas; preserve relações manuais. Histórico Git é apoio opcional.

No modo padrão, atualize relações com seus pares inversos e links Markdown relativos. Nos modos de leitura, altere apenas o relatório. Verifique destinos, reciprocidade e relações conflitantes; reporte separadamente relações confirmadas e sugestões incertas em `{output-dir}/adr-link-report-{timestamp}.md`.
