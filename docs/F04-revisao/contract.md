# Contrato de F04

## Prerequisites

Gravar duas frases, uma por fonte, e encerrar durante a última.

## Quality gates

Compilar e testar com `gradlew.bat build`.

## Coverage Manifest

O aceite de F04 no PRD é coberto pelos itens F04-01 a F04-06.

## E2E

- **F04-01** — Dado o cenário acima, quando a pessoa reabre a sessão salva, então a última fala aparece após o processamento e pode ser reproduzida.
- **F04-02** — Dado o mesmo fluxo, verificar a condição adicional: uma falha de reconhecimento aparece junto ao áudio preservado.
- **F04-03** — Renomear uma sessão e reabrir o aplicativo. A busca por nome e por data encontra a mesma gravação. Limpar o nome restaura a exibição da data.
- **F04-04** — Clicar em **Copiar prompt de revisão** e conferir a confirmação. Ao colar, o Markdown reúne o pedido de revisão e a transcrição em ordem, com os tempos e as origens de cada fala.
- **F04-05** — Em **Sessões**, selecionar uma gravação com feedback salvo e abrir **Ver revisão de inglês**. A análise exibida pertence à gravação selecionada. Para gravações sem revisão, sem fala confirmada ou sem sugestões, o botão fica oculto. Transcrição e Revisão de inglês mantêm Sessões selecionado no menu lateral.

- **F04-06** — Com duas aulas no histórico, acionar Excluir na segunda linha. Cancelar preserva ambas. O botão de confirmação permanece bloqueado com o campo vazio ou com `DELETE`. Digitar `delete` e confirmar envia a segunda pasta à Lixeira, remove a aula da lista e atualiza Treinar. A primeira aula segue disponível.
