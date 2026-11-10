---
name: development-guidelines
description: Elaborar diretrizes de desenvolvimento para a linguagem e stack escolhidas do projeto usando procedimentos locais e documentação oficial atual.
---

# Diretrizes de desenvolvimento

Skill autocontida; a referência de geração está incluída neste diretório. Entrada: `$development-guidelines <linguagem> [--orm=...] [--web=...] [--testing=...]`.

Leia [guidelines-generate.md](references/upstream/commands/guidelines-generate.md) para parâmetros, categorias e estrutura. Use a linguagem/stack escolhida e documentação oficial atual dos componentes relevantes; resolva a biblioteca no Context7 quando disponível e confirme suas APIs.

Execute na sessão atual com as ferramentas do Codex. Preserve escolhas do projeto. Defaults sugeridos, regras, formatos e exemplos da referência são candidatos a adaptar, não decisões aprovadas do projeto. Dimensione a extensão pelo uso real, sem impor uma contagem de linhas ou exemplos como critério de qualidade.

Produza o documento no destino solicitado, ou `docs/development-guidelines.md`. Cite fontes, diferencie regras existentes de propostas novas e verifique os exemplos. A geração de diretrizes não altera automaticamente `AGENTS.md`, ferramentas ou dependências do projeto.
