# Bird Teacher

Aplicativo para Windows x64 que grava e transcreve conversas nos idiomas compatíveis com o Whisper e oferece feedback para o estudo de inglês. Ele grava o microfone e a saída escolhida para o fone em canais separados, transcreve os dois com Whisper e usa o Ollama no próprio computador para comentar as falas do estudante.

## Primeira conversa

1. Extraia o ZIP **Bird-Teacher-Windows-x64.zip** e abra **Bird Teacher.exe**. O pacote inclui o Java. O reconhecimento usa a biblioteca OpenMP do Microsoft Visual C++ Redistributable x64. [Runtime da Microsoft](https://learn.microsoft.com/pt-br/cpp/windows/latest-supported-vc-redist)
2. Em **Gravação**, confira o microfone e a saída do fone e clique em **Iniciar gravação**. Se faltar o modelo de voz, **Configurar transcrição** abre o catálogo do Whisper. Escolha o idioma, baixe o modelo e clique em **Usar na transcrição**. Ao abrir o aplicativo, o idioma inicial é inglês.
3. Acompanhe as falas na tela. **Pausar gravação** e **Retomar gravação** mantêm a mesma sessão. O atalho padrão **Ctrl + Shift + F9** também inicia, pausa e retoma. Em **Configurações → Áudio e atalhos**, você pode trocar o atalho e escolher os dispositivos padrão.
4. Clique em **Encerrar e revisar** para salvar a sessão e abrir a **Revisão de inglês**. Se o Ollama precisar de configuração, use **Configurar Ollama**, instale ou inicie o serviço e escolha o modelo. Você também pode fazer isso em **Configurações → Revisão de inglês**.
5. Em **Sessões**, busque uma conversa pelo nome ou pela data. Clique em **Abrir transcrição** para ler ou ouvir as falas, ou em **Ver revisão de inglês** para ir diretamente à análise salva. Dentro da sessão, alterne entre **Transcrição** e **Revisão de inglês**. **Voltar às sessões** reabre a lista.
6. Os botões ficam ao lado do título de cada sessão. **Excluir** abre uma confirmação. Digite `delete` e confirme para enviar a pasta da aula para a Lixeira do Windows. O ícone de lápis permite renomear a sessão. **Copiar prompt de revisão** copia o pedido e a conversa em Markdown para colar com **Ctrl+V** em outra IA.

O áudio do fone abrange tudo que o Windows toca naquela saída. As sugestões de inglês citam falas do microfone, com explicação em português e exemplos em inglês.

## Treinar

O número ao lado de **Treinar** indica quantas lições estão disponíveis. Depois de uma aula, o aplicativo prepara as lições automaticamente com o Ollama local. Quatro falas distintas com o mesmo erro liberam uma lição sobre aquele ponto. Abra o menu, escolha a lição, ouça sua fala e pratique a reformulação oral e escrita.

O aplicativo salva na pasta de cada sessão o arquivo `licoes-v2.json`, com os identificadores das falas já verificadas e o material dos exercícios. Ao reabrir, as lições salvas ficam disponíveis mesmo com o Ollama desligado. A preparação pendente continua em segundo plano. Concluir ou dispensar uma lição atualiza o contador. O mesmo ponto pode voltar após quatro novas ocorrências.

## Dados locais

As sessões ficam em `%LOCALAPPDATA%\Bird Teacher\sessões`. Cada pasta contém os trechos WAV, sua transcrição e, depois da análise, `feedback.txt`. A exportação `conversa.wav` traz sua voz à esquerda e o som do fone à direita. O modelo de voz fica em `%LOCALAPPDATA%\Bird Teacher\models`. Com os modelos instalados, captura, transcrição e análise funcionam localmente. A conexão com o Ollama usa `127.0.0.1:11434`.

## Compilar no Windows

Com JDK 21 x64 em `JAVA_HOME`, rode no PowerShell a partir da raiz do projeto.

```powershell
.\gradlew.bat build
.\gradlew.bat :app:run
```

Para gerar a distribuição com runtime Java incluído, execute o script abaixo. O ZIP fica em `app\build\windows`.

```powershell
.\scripts\package-windows.ps1
```

O módulo `core` reúne WASAPI via JNA, reconhecimento whisper.cpp via whisper-jni, arquivos de sessão e cliente Ollama. O módulo `app` contém a interface Swing/FlatLaf e o atalho global via JNativeHook. A referência de tecnologia foi o Arara Voice. [Projeto de referência](https://github.com/davimatosms/Arara-voice) · [PRD](docs/PRD.md)

## Interface de chamadas

**Modo compacto** mantém a origem e a última fala sobre a janela da chamada. Use **Janela completa** para retornar. O ícone do Bird Teacher na bandeja do Windows permite reabrir a janela, pausar, retomar ou encerrar.

O falcão-peregrino aparece pousado quando a coleta está pronta ou pausada, em planeio durante a gravação e em mergulho durante o processamento. Em **Configurações**, você pode trocar a paleta de cores, o modelo do Ollama e a combinação de teclas da coleta. O modo claro ou escuro fica no rodapé da barra lateral. Em **Sessões**, busque a sessão pela data ou horário. Na **Revisão de inglês** de uma sessão, clique numa sugestão e use **Ouvir minha fala** para reproduzir a citação correspondente. [Referências da interface](docs/design/MASTER.md)
