# Implementação de F02

`Wasapi`, `ComScope`, `MixFormat`, `Session`, `Hotkey` e controles Swing.

## Verificação

WindowsSmoke abriu os dois endpoints reais e registrou JNativeHook. SessionTest verificou com fontes controladas que ambas pararam na pausa e retomaram na mesma sessão. HotkeyTest verificou a combinação e a supressão da repetição ao segurar a tecla.

A execução conjunta de `gradlew clean build :app:installDist` passou, incluindo 13 testes Java. O harness passou em 16 testes. O runner `node scripts/run-gates.mjs` encerrou com código 64 porque requer um backend TypeScript em `apps/backend`; seus seis gates ficaram sem execução neste aplicativo Java.

## Aceite pendente

Pendente uma chamada com o headset do usuário, incluindo o atalho com o aplicativo da chamada em foco e a desconexão de um dispositivo.

## Dispositivos padrão na versão 1.5.0

O ensaio nativo abriu os endpoints multimídia padrão de captura e reprodução com WASAPI e encerrou a sonda antes de ler áudio. Em uma janela com preferências de teste, selecionou os dispositivos em Configurações, verificou a seleção na conversa e reabriu o aplicativo. Os IDs das duas fontes foram restaurados. Os testes de seleção exercitam desconexão, reconexão e uma troca temporária na conversa.
