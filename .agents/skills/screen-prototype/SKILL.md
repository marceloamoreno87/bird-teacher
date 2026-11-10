---
name: screen-prototype
description: Criar ou revisar protótipos navegáveis de telas e jornadas com dados simulados, consumindo os tokens locais do design system e mantendo a stack da aplicação em aberto.
---

# Prototipar telas e jornadas

Leia o [guia local](../../../docs/design-workflow.md), requisitos ou spec da tela e o design em `docs/design/`. Se o design ainda não existir, use `design-system`; se faltar uma decisão que muda a jornada, esclareça-a enquanto prepara as partes independentes.

1. Identifique ator, objetivo, ação principal, informação necessária, entrada/saída e transições. Preencha o [roteiro](references/journey-template.md), distinguindo regra confirmada de hipótese e comportamento real de simulação.
2. Antes da escolha de stack, use HTML/CSS/JavaScript local a partir do [template de tela](templates/screen.html), sem framework, CDN, fonte remota ou instalação. Depois da escolha, use a stack definida quando a integração fizer parte do escopo autorizado.
3. Salve em `docs/FXX-slug/prototype/` junto da feature. Sem feature definida, use `docs/design/prototypes/<slug>/`. Ajuste links relativos para `tokens.css` e `components.css` canônicos; preserve referências compartilhadas, sem copiar valores de tokens para a tela.
4. Adapte hierarquia, navegação, conteúdo e composição ao briefing. Torne as ações centrais navegáveis com estados de carregamento, vazio, erro, sucesso, validação e indisponibilidade pertinentes. Use dados locais claramente simulados; não declare integração inexistente. Explique o que cada ação demonstra.
5. Use `node scripts/design-system.mjs preview docs` para servir os arquivos locais e encerre ao terminar. Verifique com `playwright-cli` quando disponível: jornada principal, teclado, foco, diálogo quando houver, temas relevantes, texto longo e viewport estreito/amplo. Examine screenshots. Use `frontend-ui-design` para crítica; confira aderência a tokens e contratos de componentes. Recursos de navegador ausentes deixam verificação pendente e não impedem produzir os fontes locais.
6. Entregue fontes editáveis, roteiro, limites da simulação e evidências junto da feature. Uma tela aprovada visualmente não conclui o contrato funcional do produto. Na passagem para implementação, ligue telas e estados aos componentes, tokens e critérios de aceite existentes.

O template é um ponto de partida editável, não um gerador automático de regras de negócio. Evite generalizar sua composição para todo produto. O consumo de uma ferramenta externa exige solicitação própria e sincronização com os tokens canônicos; nenhuma instalação externa é necessária para o fluxo local.
