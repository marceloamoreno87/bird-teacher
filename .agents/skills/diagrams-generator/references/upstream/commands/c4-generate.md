# Gerar diagramas C4

Entrada: `$diagrams-generator c4 <documento.md> [pasta] [--no-images]`.

Destino padrão: `docs/c4`. Leia o documento e execute as fases de [c4-diagram-generator](../agents/c4-diagram-generator.md) na sessão atual. Uma especificação ou documento de arquitetura pode fornecer o contexto descrito como FDD no procedimento.

Crie arquivos `.puml` separados para os níveis sustentados pela fonte e um `.md` com a análise, sem duplicar o código PlantUML. Preserve o idioma da fonte e justifique níveis omitidos.

Com `--no-images`, entregue apenas fontes e análise. Caso contrário, renderize PNG com renderizador local disponível; tente corrigir erros de sintaxe até três vezes por arquivo. Se a ferramenta estiver ausente, entregue os fontes e declare a renderização pendente, sem instalar ferramentas automaticamente.

Confira consistência, sintaxe e lista de entregas. Informe idioma, níveis gerados/omitidos, arquivos e resultado real da renderização.
