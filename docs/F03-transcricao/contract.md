# Contrato de F03

## Prerequisites

Preparar o modelo Whisper e duas frases conhecidas em inglês.

## Quality gates

Compilar e testar com `gradlew.bat build`.

## Coverage Manifest

O aceite de F03 no PRD é coberto por F03-01 e F03-02.

## E2E

- **F03-01** — Dado o cenário acima, quando duas frases com origens diferentes chegam durante a chamada, então a tela mostra o texto antes do encerramento com a origem correta.
- **F03-02** — Dado o mesmo fluxo, verificar a condição adicional: a confirmação substitui a prévia e preserva a ordem temporal.

- **F03-03** — Escolher um Whisper em Configurações, baixar e ativar. Reabrir o aplicativo restaura o perfil de inglês. Um download incompleto mantém o modelo anterior disponível.
- **F03-04** — Durante uma fala contínua de nove segundos, há prévias com a mesma origem e horário e uma única confirmação.

- **F03-05** — O catálogo permite buscar os 100 idiomas do Whisper, mostra modelos compatíveis e distingue os instalados. Baixar um modelo preserva a transcrição em uso. Usar o modelo aplica o idioma escolhido às prévias e à confirmação. Cantonês requer os pesos v3.
