# Contrato de F02

## Prerequisites

Selecionar um microfone e uma saída ativos no Windows e abrir outra janela.

## Quality gates

Compilar e testar com `gradlew.bat build`.

## Coverage Manifest

O aceite de F02 no PRD é coberto por F02-01 e F02-02.

## E2E

- **F02-01** — Dado o cenário acima, quando a pessoa aciona o atalho em outra janela, então as duas fontes alternam entre gravando e pausadas na mesma sessão.
- **F02-02** — Dado o mesmo fluxo, verificar a condição adicional: uma desconexão identifica a origem e permite encerrar a sessão.

- **F02-03** — Configurações salva o microfone e a saída de forma independente. A seleção sobrevive à reabertura e à troca de ordem dos endpoints. “Padrão do Windows” resolve a fonte ao abrir a captura. Uma fonte fixa desconectada permanece identificada até a reconexão ou escolha de outra fonte.
