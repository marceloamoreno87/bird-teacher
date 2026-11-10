---
name: design-system
description: Criar ou evoluir um design system local com briefing, direção visual, tokens, temas, componentes e catálogo, sem pressupor linguagem ou framework da aplicação.
---

# Design system local

Use para definir a linguagem visual e seus contratos. O fluxo completo está no [guia local](../../../docs/design-workflow.md). Todos os recursos necessários estão incluídos; as ferramentas comerciais ou skills citadas no guia são referências opcionais.

1. Leia requisitos, jornadas e o design existente. Resolva público, contexto, ações prioritárias, conteúdo, restrições e referências visuais. Identifique hipóteses que afetam a direção; preserve escolhas já dadas pelo usuário.
2. Para um design novo, crie a base com `node scripts/design-system.mjs init docs/design`. O comando preserva destinos não vazios. Para um design existente, edite os arquivos seletivamente, mantendo histórico e decisões. Os valores iniciais são exemplos; adapte-os ao briefing.
3. Preencha [MASTER.md](templates/MASTER.md) com briefing, direção criativa e decisões. Diferencie alternativas propostas de escolhas definidas. Evite impor uma estética ou stack universal.
4. Use [tokens.json](templates/tokens.json) como única fonte de valores. Defina tokens base, aliases semânticos e temas; documente seu consumo em [DESIGN.md](templates/DESIGN.md). Execute `node scripts/design-system.mjs build docs/design`. CSS e adaptações para a stack são derivados, sem edição de valores em paralelo.
5. Defina os contratos em [COMPONENTS.md](templates/COMPONENTS.md) e atualize o [catálogo](templates/catalog.html) e seus [estilos locais](templates/components.css) com variantes, estados e interações relevantes ao produto. Use fontes e assets locais. Bibliotecas de componentes só entram após escolha explícita de stack.
6. Inicie a prévia com `node scripts/design-system.mjs preview docs`; use a URL local informada para conferir o catálogo e os protótipos com `playwright-cli` quando disponível. Encerre a prévia ao terminar. Examine temas, teclado, foco, contraste medido, texto longo e layouts estreito/amplo. Compile tokens antes de revisar e use `frontend-ui-design` para crítica visual. Registre evidências e verificações pendentes; compilação não valida acessibilidade ou qualidade visual.

Entregue os caminhos, decisões, hipóteses, componentes, comandos executados e limitações. O kit HTML/CSS funciona sem npm, CDN, MCP, credenciais ou serviço remoto; gerar/validar seus arquivos usa o Node.js já empregado pelo harness. Navegação e evidências exigem um navegador local disponível. Consulte `screen-prototype` para exercitar jornadas em telas.
