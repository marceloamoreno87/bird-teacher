# Implementação de F03

`Whisper`, `Segmenter`, `Session` e `DialogueTable`.

## Verificação

No Windows x64, Whisper base reconheceu a amostra JFK com “ask not what your country can do for you”. SegmenterTest cobre silêncio inicial, prévia e confirmação. DialogueTableTest cobre ordenação e substituição sem duplicação.

A execução conjunta de `gradlew clean build :app:installDist` passou, incluindo 13 testes Java. O harness passou em 16 testes. O runner `node scripts/run-gates.mjs` encerrou com código 64 porque requer um backend TypeScript em `apps/backend`; seus seis gates ficaram sem execução neste aplicativo Java.

## Aceite pendente

A latência em conversa com dois interlocutores depende do hardware. O ensaio com áudio conhecido e fontes controladas cobre caminhos distintos.

## Comparação no Windows com o Bird Teacher 1.4.0

As versões 1.3.0 e 1.4.0 receberam os mesmos 18 WAVs mono a 16 kHz em pacotes de dez milissegundos. O ensaio usou o segmentador e o reconhecedor de cada versão e comparou o texto final com a referência humana. A máquina foi um Ryzen 9 9950X com a inferência em CPU. As gravações são leituras de audiolivro. Conversas espontâneas, microfones reais e sobreposição de vozes podem produzir resultados diferentes. [LibriSpeech](https://huggingface.co/datasets/openslr/librispeech_asr)

| Amostra | Palavras de referência | Base anterior | Medium.en com a nova segmentação e decodificação |
|---|---:|---:|---:|
| Primeiros seis trechos | 69 | 17 erros | 8 erros |
| Outros doze trechos | 215 | 40 erros | 13 erros |
| Total | 284 | 57 erros (20,1%) | 21 erros (7,4%) |

Selecionamos os índices 0, 100 e 250 de cada conjunto de teste clean e other para a primeira comparação e os índices 320, 520, 720, 920, 1120 e 1320 de cada conjunto para a segunda. A taxa de erro usa a soma de substituições, omissões e inserções dividida pelo total de palavras de referência. A normalização converte letras para minúsculas, separa hífens e preserva apóstrofos internos. O resultado inclui erros de grafia e de nomes próprios.

Nos seis primeiros trechos, Small.en obteve 9 erros e levou 11,3 segundos para confirmar 25,7 segundos de áudio. Medium.en levou 32,3 segundos. A medição inclui a segmentação e a decodificação final, com o modelo já carregado. As prévias ao vivo usam decodificação mais rápida.

WindowsSetupSmoke baixou e ativou os modelos Small.en e Medium.en pela tela de Configurações. A sessão de demonstração transcreveu fala do microfone e do fone e gerou feedback ao encerrar. O arquivo Medium.en instalado passou pela validação SHA-256 antes da ativação. [Configurações](evidencias/configuracoes.png)

## Idiomas na versão 1.5.0

Em uma janela Windows com preferências isoladas, a seleção do Small multilíngue em Configurações baixou os pesos para o computador. O mesmo arquivo foi ativado para português brasileiro e espanhol. A sonda transcreveu as amostras FLEURS pt_br/10008174856218516817 e es_419/1001146817223348054 no idioma de cada gravação. Depois ativou Medium.en, transcreveu o trecho público de JFK em inglês e verificou a restauração das preferências ao reabrir. Esse ensaio confirma a troca de idiomas. A taxa de erro publicada na seção anterior foi medida apenas para inglês. [Gravações de referência](https://huggingface.co/datasets/google/fleurs)


## Catálogo de modelos na versão 1.6.0

A aba Whisper permite buscar 100 idiomas por nome ou código e apresenta os modelos compatíveis, com tamanho e estado de instalação. A instalação isolada do Turbo foi exercitada no Windows usando o catálogo e uma pasta de modelos temporária. O perfil ativo permaneceu em inglês até a aplicação explícita de cantonês. O reconhecedor processou no idioma escolhido o trecho público FLEURS yue_hant_hk. O perfil inicial com as preferências do usuário foi confirmado como en:medium.en. A cobertura automatizada inclui busca sem acentos, atribuição do token de cantonês ao modelo v3, entradas antigas, falhas de download e idioma de abertura. [Tela de Configurações](evidencias/catalogo-whisper.png)
