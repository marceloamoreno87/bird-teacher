# Contrato de F01

## Prerequisites

Windows x64 com o modelo de voz removido ou instalado na pasta indicada.

## Quality gates

Compilar e testar com `gradlew.bat build`.

## Coverage Manifest

O aceite de F01 no PRD é coberto por F01-01 e F01-02.

## E2E

- **F01-01** — Dado o cenário acima, quando a pessoa abre o aplicativo, então a janela identifica o modelo ausente e permite preparar o reconhecimento.
- **F01-02** — Dado o mesmo fluxo, verificar a condição adicional: com o modelo instalado, exibe os dispositivos disponíveis.
