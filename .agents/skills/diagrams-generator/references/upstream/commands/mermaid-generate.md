# Gerar diagramas Mermaid

Entrada: `$diagrams-generator mermaid <documento.md> [pasta] [--no-images]`.

Destino padrão: `docs/mermaid`. Leia a fonte inteira e execute [mermaid-diagram-generator](../agents/mermaid-diagram-generator.md) na sessão atual. Selecione os diagramas que esclarecem fluxos centrais, partes difíceis, decisões arquiteturais, contratos ou relações.

Produza `{feature}-diagrams.md` com análise e diagramas; preserve fontes `.mmd` separados quando necessários para validação ou renderização. Use o idioma da fonte nos títulos e rótulos e preserve termos técnicos. A quantidade segue o conteúdo disponível, sem preencher uma cota de diagramas.

Valide sintaxe e consistência com a fonte; corrija redundâncias e elementos inventados. Renderize imagens somente quando solicitadas e com ferramenta disponível. `--no-images` mantém apenas fontes e análise. Informe arquivos, quantidade, justificativa e verificações efetivamente executadas.
