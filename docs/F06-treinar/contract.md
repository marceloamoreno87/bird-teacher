# Contrato F06

## Prerequisites

Usar uma biblioteca temporária com WAVs e transcrições confirmadas de várias sessões. Nos testes de integração, controlar as classificações do modelo por um substituto local. Para ensaio com IA real, selecionar um modelo instalado no Ollama.

## Coverage Manifest

O aceite F06 do PRD corresponde a T01–T06.

| ID | Dado | Quando | Então |
|---|---|---|---|
| T01 | Aplicativo aberto | As lições salvas são carregadas | Treinar mostra a quantidade disponível. Ao abrir o menu, o aluno encontra o catálogo ou o estado vazio. |
| T02 | Falas confirmadas e resultados da IA | Agregar os trechos | Quatro ocorrências distintas do mesmo ponto habilitam a lição. Duplicatas, citações inexistentes e falas do fone ficam fora da contagem. |
| T03 | Mais de um foco elegível | Uma análise em segundo plano salva um lote | O catálogo oferece uma lição por foco, o balão atualiza sua contagem e uma resposta em edição é preservada. |
| T04 | Uma lição disponível | Concluir ou dispensar e reabrir o aplicativo | O contador diminui, fica oculto ao zerar e as mesmas evidências continuam consumidas. |
| T05 | Ollama indisponível e falas pendentes | Abrir o aplicativo | As lições salvas permanecem disponíveis. A preparação volta a ser tentada automaticamente. |
| T06 | Aulas já analisadas, inclusive da versão anterior | Finalizar nova aula ou reabrir após falha ou troca de modelo | Só as falas pendentes seguem para o Ollama, e cada lote concluído é persistido. |

## Quality gates

Build Gradle e testes do núcleo e da interface Swing. Registrar o resultado do runner de gates do harness.
