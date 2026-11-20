# Implementação de F04

`LocalSession`, `StereoRecording` e controles de revisão do desktop.

## Verificação

SessionTest reabriu trechos e texto após pausa e retomada. A exportação estéreo foi lida de volta e conferida amostra a amostra em sobreposição, com silêncio inicial e atribuição esquerda/direita. Trechos persistidos antes de uma interrupção aparecem recuperáveis.

A execução conjunta de `gradlew clean build :app:installDist` passou, incluindo 13 testes Java. O harness passou em 16 testes. O runner `node scripts/run-gates.mjs` encerrou com código 64 porque requer um backend TypeScript em `apps/backend`; seus seis gates ficaram sem execução neste aplicativo Java.

## Aceite pendente

A audição por uma pessoa da sessão completa em seu player padrão continua para o aceite no desktop de destino.

## Exclusão por linha de sessão

Os controles de abertura, revisão, renomeação, cópia e exclusão atuam na própria linha. O ensaio com duas sessões temporárias no Windows abriu a transcrição da segunda sessão, voltou ao histórico e verificou que somente ela foi para a Lixeira após digitar `delete`. O teste Swing verificou campo vazio, diferenças de maiúsculas, espaços, cancelamento e confirmação. O catálogo de treinos é atualizado a partir dos arquivos restantes. O build Java e os 51 testes passaram. Os gates do backend TypeScript ficaram sem execução neste aplicativo Java.
