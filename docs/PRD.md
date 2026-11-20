# Bird Teacher

## 1. Resumo

Bird Teacher é um aplicativo desktop para quem pratica inglês em conversas pelo computador. Durante uma chamada, a pessoa acompanha o que disse ao microfone e o que ouviu pelo fone. Ao encerrar, pode rever a gravação e estudar sugestões com base em suas próprias falas.

A referência técnica é o Arara Voice. Ele captura o microfone com Java Sound, divide o ditado em trechos e produz prévias com Whisper local. O Bird Teacher precisa também capturar a saída de áudio do sistema e manter as duas origens identificadas desde a gravação. [Código do Arara Voice analisado](https://github.com/davimatosms/Arara-voice/tree/3b8c80aaa43bbe44d1c76fc5885336060e6921a0/core/src/main/java/br/arara/core)

## 2. Problema e oportunidade

Quem participa de uma conversa precisa prestar atenção na resposta do interlocutor. Anotar uma expressão ou corrigir uma frase naquele momento interrompe a troca. Uma gravação preserva o que aconteceu, mas localizar uma frase depois exige ouvir a chamada de novo. Uma transcrição com origem e tempo permite ligar uma sugestão ao momento em que a pessoa poderia usá-la.

## 3. Público

Pessoas que praticam inglês em chamadas, usam microfone e fone no computador e querem revisar seu próprio inglês com processamento local.

## 4. Objetivos

- Mostrar as duas partes da conversa enquanto a chamada acontece.
- Permitir iniciar, pausar e retomar a coleta com outra janela em foco.
- Guardar áudio, texto e identificação da origem para revisão posterior.
- Gerar feedback com um modelo executado pelo Ollama na máquina do usuário.

## 5. Histórias de uso

| Etapa | Como estudante, quero… |
|---|---|
| F01 | Abrir o Bird Teacher e verificar se posso começar uma sessão. |
| F02 | Gravar o microfone e o som reproduzido no fone, e pausar ambos sem sair da chamada. |
| F03 | Ler as falas em inglês e distinguir o que eu disse do que ouvi. |
| F04 | Reabrir a conversa e ouvir o trecho correspondente a uma fala. |
| F05 | Entender como eu poderia expressar uma ideia com mais clareza e praticá-la para a próxima conversa. |
| F06 | Treinar um ponto específico de inglês que se repetiu nas minhas aulas com exercícios de áudio e escrita. |

## 6. Funcionalidades

### F01. Aplicativo desktop

Java 21, Gradle, núcleo separado da interface Swing com FlatLaf, seguindo a organização do Arara. O menu principal contém Gravação, Sessões, Treinar e Configurações. A tela inicial mostra o estado dos recursos necessários à gravação e à transcrição. Sessões reúne as conversas salvas. Cada uma identifica seu nome e data ao alternar entre Transcrição e Revisão de inglês. A disponibilidade do Ollama só condiciona a análise final. O produto é exclusivo para Windows. A distribuição inicial usa x64, arquitetura da biblioteca nativa de voz selecionada. [Módulos e dependências da referência](https://github.com/davimatosms/Arara-voice/tree/3b8c80aaa43bbe44d1c76fc5885336060e6921a0)

### F02. Gravação e controle da sessão

A pessoa identifica o microfone e a saída ligada ao fone. Em Configurações pode fixar cada dispositivo para as próximas conversas ou seguir o padrão multimídia do Windows. Um atalho global inicia a sessão, pausa os dois canais e retoma a mesma sessão. Uma ação distinta encerra a conversa. O app indica se está gravando ou pausado. Se uma fonte ficar indisponível, informa qual foi afetada e preserva o que já gravou.

Entrega áudio por origem, estado e posição na sessão para F03 e F04. O som do fone corresponde ao áudio reproduzido naquela saída, inclusive quando há mais de uma voz. A implementação do atalho parte do JNativeHook usado pelo Arara e precisa ser validada no Windows.

### F03. Transcrição ao vivo

Recebe as duas fontes de F02. Executa whisper.cpp localmente via whisper-jni, com reconhecimento no idioma selecionado entre os compatíveis com o Whisper. O aplicativo inicia em inglês. O catálogo permite instalar um modelo local e escolher quando ativá-lo. Em Configurações, a pessoa escolhe, baixa e ativa o modelo de voz. Modelos multilíngues podem ser reutilizados entre idiomas. Para inglês, há também modelos dedicados. Mostra as falas em ordem de captura, com tempo e rótulos “Você” e “Áudio do fone”. Uma prévia dá lugar ao trecho confirmado na mesma posição. Falas simultâneas continuam associadas às respectivas fontes. Se a transcrição atrasar, o app informa que há áudio em processamento e continua gravando.

Entrega trechos confirmados com texto, origem e intervalo para F04.

### F04. Revisão da conversa

Reúne o áudio de F02 e os trechos de F03 numa sessão local. Ao encerrar a captura, conclui os trechos pendentes. A pessoa pode reabrir a sessão, ler o diálogo e ouvir o áudio na posição de uma fala. Uma falha na transcrição deve ser visível ao rever a gravação. Entrega a conversa confirmada com referências aos trechos para F05.

### F05. Feedback de inglês com Ollama

Ao finalizar a sessão, o app fornece a transcrição ao modelo local. A saída do fone contextualiza as respostas do estudante. Cada sugestão cita o trecho do microfone, apresenta uma reformulação em inglês e explica o motivo. O app confere se a citação corresponde ao trecho indicado. Se ninguém falou ao microfone, informa que falta uma amostra do estudante. Se o modelo estiver indisponível, a pessoa pode tentar de novo sobre a sessão salva. [API local de conversa e resposta estruturada](https://docs.ollama.com/api/chat)

Configurações permite instalar ou iniciar o Ollama e baixar e selecionar o modelo local. Encerrar a coleta conclui a transcrição das duas fontes antes de gerar e salvar o feedback automaticamente. Uma pausa permite retomar a mesma sessão.

A explicação vem em português e os exemplos ficam em inglês. Exemplo ilustrativo para “I am working here since May” em uma conversa sobre emprego atual: “I've been working here since May”. A explicação conecta a forma verbal ao fato de o trabalho ter começado no passado e continuar hoje.

### F06. Treinar erros recorrentes

Ao encerrar uma aula, o aplicativo prepara automaticamente as lições de áudio e escrita com o Ollama local. Cada ponto precisa de quatro falas distintas do aluno. O menu Treinar exibe as lições disponíveis, com um balão que conta os pontos liberados. A biblioteca persiste os trechos já verificados e seus resultados, inclusive os que não geraram correções. Ao reabrir, o app recupera as lições salvas e retoma as aulas pendentes. Concluir ou dispensar uma lição consome suas evidências e atualiza o contador.

## 7. Fora do escopo inicial

Colagem no cursor, identificação individual de várias pessoas na mesma saída e avaliação de pronúncia a partir do texto.

## 8. Dependências e ondas

| ID | Etapa | Prioridade | Depende de | Onda |
|---|---|---|---|---|
| F01 | Aplicativo desktop | 1 | — | 1 |
| F02 | Gravação e controle da sessão | 1 | F01 | 2 |
| F03 | Transcrição ao vivo | 1 | F02 | 3 |
| F04 | Revisão da conversa | 1 | F02, F03 | 4 |
| F05 | Feedback de inglês com Ollama | 1 | F04 | 5 |
| F06 | Treinar erros recorrentes | 1 | F04, F05 | 6 |

A fundação compartilhada é F01. Prioridade 1 indica função essencial à experiência solicitada.

## 9. Critérios de aceite

| ID | Verificação observável |
|---|---|
| F01 | Abrir o app no Windows, identificar se os recursos estão disponíveis e receber uma indicação acionável caso falte o modelo de voz. |
| F02 | Com a janela da chamada em foco, usar o atalho para gravar, pausar e retomar. Reproduzir e falar frases conhecidas antes, durante e depois da pausa. As frases externas à pausa devem aparecer nas trilhas corretas. Durante a pausa nenhum dos dois canais é gravado. |
| F03 | Em uma chamada com falas alternadas e sobrepostas, ler o diálogo antes do encerramento. Conferir a origem e a ordem das falas mesmo quando uma delas levar mais tempo para ser reconhecida. Confirmar que a prévia é substituída sem duplicação. |
| F04 | Encerrar com uma fala ainda em processamento, fechar e reabrir a sessão depois de salva. Conferir a última fala, os dois lados e a correspondência entre texto e reprodução. Abrir a revisão salva em **Sessões** quando disponível. Renomear uma sessão, buscá-la pelo nome ou pela data no formato dd/MM/yyyy HH:mm:ss e copiar o pedido de revisão com a conversa em Markdown para a área de transferência. Excluir pela linha da aula, digitando `delete` para enviá-la à Lixeira. |
| F05 | Gerar uma análise usando modelos já instalados e sem internet. Abrir o trecho citado e conferir autoria, texto e sugestão. Repetir com Ollama desligado, reativá-lo e obter o relatório da mesma conversa. |
| F06 | Ver o contador de Treinar aumentar automaticamente após quatro falas distintas do aluno com o mesmo erro, abrir uma lição e fazer os exercícios de áudio e escrita. Reabrir e verificar que as mesmas evidências já concluídas ou dispensadas não geram outra lição. |

**Aceite integrado.** Usar uma frase distinta em cada fonte e outra durante a pausa. Conferir que a tela ao vivo, o áudio salvo e o diálogo reaberto concordam sobre a origem e o intervalo de captura. No relatório, uma frase dita exclusivamente no fone não pode ser atribuída ao estudante.
