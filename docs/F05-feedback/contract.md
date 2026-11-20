# Contrato de F05

## Prerequisites

Instalar um modelo local no Ollama. Preparar sessão com falas identificadas.

## Quality gates

Compilar e testar com `gradlew.bat build`.

## Coverage Manifest

O aceite de F05 no PRD é coberto por F05-01 e F05-02.

## E2E

- **F05-01** — Dado o cenário acima, quando a pessoa encerra ou pede nova análise, então cada sugestão cita uma fala do microfone e explica em português uma reformulação em inglês.
- **F05-02** — Dado o mesmo fluxo, verificar a condição adicional: com Ollama indisponível a gravação pode ser revista e a análise pode ser repetida.

- **F05-03** — Em Configurações, instalar ou iniciar o Ollama, escolher o modelo e acompanhar o download. Um erro permite tentar novamente e preserva o modelo configurado anteriormente.
- **F05-04** — Com uma fala em cada fonte e modelo local pronto, pausar mantém a sessão aberta. Encerrar conclui as transcrições, abre Meu inglês e salva o feedback na sessão automaticamente.
