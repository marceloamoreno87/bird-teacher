# Implementação de F01

`settings.gradle`, `build.gradle`, módulos `app` e `core`, janela Swing e `scripts/package-windows.ps1`.

## Verificação

O Gradle compilou o aplicativo e o jpackage gerou o ZIP x64. O Windows extraiu o ZIP e abriu Bird Teacher.exe. A janela respondeu ao fechamento com código 0.

A execução conjunta de `gradlew clean build :app:installDist` passou, incluindo 13 testes Java. O harness passou em 16 testes. O runner `node scripts/run-gates.mjs` encerrou com código 64 porque requer um backend TypeScript em `apps/backend`; seus seis gates ficaram sem execução neste aplicativo Java.

## Aceite pendente

O teste do reconhecedor usou um modelo já baixado com o SHA-256 esperado. O download pelo botão em uma instalação limpa permanece para aceite manual.

## Navegação da versão 1.7.0

A janela nativa do Windows foi exercitada com sessões de teste com e sem revisão. A navegação entre transcrição e revisão manteve o nome, a data e o destaque de Sessões. Voltar à lista e depois a Gravação abriu os controles de captura com a transcrição vazia. Dois arquivos de áudio de teste percorreram a captura, a pausa, o encerramento e a geração automática da revisão com Whisper e Ollama locais.

| Critério visual | Nota | Observação |
|---|---|---|
| Design | 8/10 | Na sessão aberta, título, data e controles de navegação formam um cabeçalho comum. A lista agrupa as ações de leitura e edição. |
| Originalidade | 7/10 | O falcão e a tipografia identificam a tela de gravação. A lista e os controles de busca seguem convenções do desktop. |
| Acabamento | 8/10 | Na janela de 810 × 600, os botões da lista e os controles de gravação ficaram visíveis. O contraste do texto secundário sobre a superfície é de 7,5:1 em Petróleo escuro e 6,3:1 em Petróleo claro. Nomes longos no cabeçalho usam reticências e dica com o nome inteiro. |
| Funcionalidade | 8/10 | O fluxo testado preservou a conversa entre as abas e mostrou o acesso direto à revisão somente quando disponível. A facilidade de descoberta por usuários iniciantes ainda requer observação em uso. |

A média visual ponderada é 7,7/10. As imagens usam dados de demonstração. [Sessões gravadas](evidencias/historico.png) · [Transcrição](evidencias/sessao-transcricao.png) · [Revisão de inglês](evidencias/meu-ingles.png)
