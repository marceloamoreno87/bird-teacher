# Identificar decisões arquiteturais

Entrada: `$adrs-management identify [MODULE...] [--output-dir=PATH] [--adrs-dir=PATH]`.

- `--output-dir`: padrão `docs/adrs`.
- `--adrs-dir`: ADRs existentes; padrão `{output-dir}/generated`.
- Sem módulos, apresente os IDs de `mapping.md` e solicite o escopo.

Leia `{output-dir}/mapping.md` e [adr-analyzer](../agents/adr-analyzer.md), Phase 2. Se o mapa estiver ausente, execute primeiro o mapeamento. Analise os módulos selecionados sequencialmente, aplicando os filtros e a pontuação do procedimento. Confira decisões existentes para evitar duplicação; use histórico Git como contexto quando disponível.

Produza arquivos sem numeração em `{output-dir}/potential-adrs/must-document/{MODULE}/` (pontuação ≥100) ou `consider/{MODULE}/` (75–99). Descarte candidatos abaixo do limiar e atualize o índice sem apagar resultados de outros módulos. Identificação produz candidatos; diferencie evidências observadas e decisões ainda propostas.

Informe módulos analisados, candidatos, caminhos e limitações. Paralelismo depende de autorização no pedido atual e exige consolidação serial do índice compartilhado.
