# Implementação de F06

A seção Treinar usa a biblioteca de sessões do aplicativo. TrainingLibrary examina transcrições confirmadas do microfone, guarda as análises por conteúdo e modelo e persiste as conclusões ou dispensas. Ollama classifica erros em 16 focos específicos. Training verifica citações e deduplica evidências antes de escolher a lição. Window, DesktopController e TrainingPane integram navegação, busca, reprodução, escrita e comparação.

| Item | Evidência |
|---|---|
| T01 | SidebarTest abre o menu e verifica sua seleção. O fluxo também foi exercitado no Windows com o DesktopController. |
| T02 | TrainingLibraryTest cobre três e quatro ocorrências entre sessões com IDs locais coincidentes, duplicatas, prévias, citações sem suporte e áudio do fone. OllamaTest inclui mudanças apenas de pontuação e modelo remoto. |
| T03 | O teste de seleção prioriza um único foco. Em dois conjuntos sintéticos com qwen3:4b-instruct no Windows, a classificação agrupou quatro perguntas com o verbo flexionado depois de did e descartou duas falas corretas por conjunto. Foram conferidas as quatro reformulações em cada conjunto. O teste Swing percorreu as duas atividades. [Exercício de escrita no tamanho mínimo da janela](evidencias/escrita.png). |
| T04 | O ensaio desktop com biblioteca temporária acionou o player com um WAV sintético, revelou a referência, preencheu a resposta, concluiu e buscou novamente. O treino não se repetiu. O teste de persistência reabre a biblioteca e verifica a exigência de quatro novas ocorrências do mesmo foco. |
| T05 | O teste de indisponibilidade simula falha do classificador e nova tentativa bem-sucedida. O painel permite repetir a busca. |

## Verificação

O build Gradle com JDK 21, display virtual e reexecução de todas as tarefas passou em 44 testes Java, sem pulos. O script de empacotamento Windows gerou o ZIP e os dois JARs internos correspondem ao build testado. O ensaio do player usou um WAV de teste, sem avaliação humana da reprodução.

O runner de gates do harness terminou com código 64 porque requer backend TypeScript. O verificador do harness e quatro de seus 16 testes falharam pela ausência de plugins.md no checkout. Esses resultados permanecem separados da verificação da aplicação Java. A precisão pedagógica nas aulas reais depende das transcrições e do modelo local.

## Progresso e recuperação em aulas longas

O teste de regressão de T06 falhou na implementação anterior, que só salvava uma aula completa. A busca agora grava um checkpoint atômico por lote, limita a geração de tokens por resposta e subdivide lotes quando o Ollama informa truncamento. O painel informa contagem de falas concluídas, aula, lote e tempo de espera. Os testes cobrem a retomada após falha intermediária e a recuperação de uma resposta truncada. Os 46 testes Java passaram. Os gates específicos do backend TypeScript continuam sem execução neste aplicativo Java.

Na instalação aberta pelo atalho do Windows, a interrupção e a retomada pela interface preservaram a contagem anterior. A busca chegou ao resultado final e gravou os índices completos.

## Biblioteca automática de lições

A versão atual substitui a busca manual pela preparação automática ao abrir o aplicativo e após salvar uma aula. Cada sessão persiste as citações verificadas e os IDs de todas as falas já classificadas em `licoes-v2.json`. A migração aproveita índices completos ou parciais e preserva os registros anteriores de conclusão e dispensa. Cada lote salvo pode liberar lições imediatamente. O catálogo permanece disponível durante a preparação.

O teste de regressão da versão anterior mostrou que acrescentar uma fala e trocar o modelo provocava nova classificação de toda a aula. Na versão atual, o classificador recebe apenas a fala acrescentada. Os testes também cobrem liberação antes do último lote, múltiplos focos, preservação de uma resposta em edição, contagem após conclusão e acesso offline.

O build e os 50 testes Java passaram. No Windows, com uma biblioteca isolada e Ollama real, o balão apareceu automaticamente com uma lição. A conclusão ocultou o balão e a reabertura da biblioteca com outro modelo preservou o resultado sem inferência adicional. [Catálogo com uma lição liberada no ensaio](evidencias/catalogo-automatico.png). O runner de gates TypeScript manteve o código 64 por falta de backend TypeScript.
