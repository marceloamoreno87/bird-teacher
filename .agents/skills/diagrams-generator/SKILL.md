---
name: diagrams-generator
description: Gerar diagramas C4 ou Mermaid do projeto a partir de uma especificação de feature ou documento de arquitetura usando procedimentos locais do scaffold.
---

# Geração de diagramas

Skill autocontida; comandos e procedimentos estão incluídos neste diretório. Entrada: `$diagrams-generator c4|mermaid <documento> [pasta] [--no-images]`.

- C4: leia [c4-generate](references/upstream/commands/c4-generate.md) e [c4-diagram-generator](references/upstream/agents/c4-diagram-generator.md).
- Mermaid: leia [mermaid-generate](references/upstream/commands/mermaid-generate.md) e [mermaid-diagram-generator](references/upstream/agents/mermaid-diagram-generator.md).

Trate `spec.md` ou documento arquitetural fornecido como fonte, mesmo que a referência o chame FDD. Execute o procedimento na sessão atual com ferramentas do Codex; execute sequencialmente por padrão. Use agentes somente quando autorizado no pedido atual.

Gere somente níveis/superfícies sustentados pela fonte. Preserve os fontes `.puml` ou `.mmd` e o texto de análise; não embuta todo o código na análise quando a referência pedir arquivos separados. Confira sintaxe e links. Renderize imagens quando solicitado ou previsto pela operação, se o renderer estiver disponível; reporte separadamente fonte criada e imagem efetivamente renderizada. Entregue caminhos e níveis omitidos com motivo.
