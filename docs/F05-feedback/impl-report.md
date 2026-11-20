# Implementação de F05

`Ollama`, `Suggestion` e aba Meu inglês.

## Verificação

OllamaSmoke executou qwen3:4b-instruct localmente com diálogo de teste. O relatório final citou “Yesterday I goed to school by bus.”, sugeriu “Yesterday I went to school by bus.” e explicou o passado de go em português. Os testes cobrem atribuição, modelo remoto e falta de fala do estudante.

A execução conjunta de `gradlew clean build :app:installDist` passou, incluindo 13 testes Java. O harness passou em 16 testes. O runner `node scripts/run-gates.mjs` encerrou com código 64 porque requer um backend TypeScript em `apps/backend`; seus seis gates ficaram sem execução neste aplicativo Java.

## Aceite pendente

A qualidade é uma propriedade do modelo e do áudio transcrito. O teste do Ollama usou um serviço local isolado e diálogo sintético.

## Verificação da configuração e do feedback automático no Windows

O ensaio WindowsSetupSmoke abriu Configurações, ativou o modelo local já instalado e navegou para Histórico. Reproduziu dois WAVs de voz sintética pelas entradas de teste do controlador, pausou a sessão e acionou Encerrar. O Whisper transcreveu as duas fontes. O Ollama local gerou uma sugestão para “Yesterday I go to the library with my friend.” e o aplicativo exibiu e gravou o relatório na pasta temporária da sessão. As preferências do ensaio ficaram em uma chave exclusiva.

Os testes da API de download cobrem progresso, interrupção, reutilização de modelo instalado e rejeição de modelo remoto. O PC do ensaio já tinha Ollama instalado. [Tela de configuração](evidencias/configuracoes.png)
