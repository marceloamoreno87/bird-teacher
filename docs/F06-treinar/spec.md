# F06 · Treinar erros recorrentes

O aplicativo usa os áudios e as transcrições de F04 e o Ollama local configurado em F05. Ao iniciar o aplicativo e ao finalizar a aula, um processo em segundo plano identifica falas confirmadas do microfone que ainda precisam de classificação. Cada resultado fica associado à sessão e ao trecho de áudio, inclusive quando o modelo não identificou erros. Resultados completos e parciais da versão anterior são aproveitados.

Quatro falas distintas do aluno com o mesmo erro específico liberam uma lição. O catálogo atualiza a cada lote salvo e apresenta uma entrada por ponto elegível, por ordem de frequência. O balão de Treinar conta lições pendentes e fica oculto quando a contagem é zero. Abrir a seção ou uma lição mantém a contagem. Uma nova lição pode aparecer enquanto o aluno escreve uma resposta.

Cada lição contém prática oral com o áudio da aula e prática escrita sobre outro trecho. Concluir ou dispensar remove as evidências apresentadas do conjunto pendente. O mesmo ponto pode voltar após mais quatro ocorrências. O aplicativo restaura a biblioteca do disco e retoma automaticamente a preparação após indisponibilidade do modelo.
