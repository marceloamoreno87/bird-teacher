---
name: adrs-management
description: Mapear decisões arquiteturais do projeto, identificar ADRs, gerar registros e ligar decisões relacionadas usando procedimentos locais do scaffold.
---

# Gerenciamento de ADRs

Skill autocontida; comandos e procedimentos estão incluídos neste diretório. Use `$adrs-management map|identify|generate|link` com os módulos ou caminhos desejados.

| Operação | Referência do comando | Procedimento detalhado |
|---|---|---|
| map | [adr-map](references/upstream/commands/adr-map.md) | [adr-analyzer, Phase 1](references/upstream/agents/adr-analyzer.md) |
| identify | [adr-identify](references/upstream/commands/adr-identify.md) | [adr-analyzer, Phase 2](references/upstream/agents/adr-analyzer.md) |
| generate | [adr-generate](references/upstream/commands/adr-generate.md) | [adr-generator](references/upstream/agents/adr-generator.md) |
| link | [adr-link](references/upstream/commands/adr-link.md) | [adr-linker](references/upstream/agents/adr-linker.md) |

Leia apenas as referências da operação selecionada, resolva parâmetros e execute na sessão atual com as ferramentas disponíveis no Codex. Execute sequencialmente por padrão. Delegue somente quando a solicitação atual autorizar agentes. Histórico Git é contexto opcional: se indisponível, use os arquivos e declare a limitação.

Baseie ADRs no código e nas decisões reais, diferenciando proposta de decisão tomada. O destino padrão é `docs/adrs/`; preserve registros existentes e crie ligações relativas válidas. Para projeto ainda sem aplicação, declare a limitação em vez de inventar arquitetura implementada. Entregue os arquivos produzidos e as evidências consultadas.
