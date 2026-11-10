# Regras de design

## Tokens e temas

Edite valores apenas em `tokens.json`. Use grupos de tokens base e aliases semânticos nos temas. O formato local usa `version: 1`, `defaultTheme`, `tokens` e `themes`. Chaves são kebab-case; referências usam `{grupo.nome}`. Todos os temas definem as mesmas chaves semânticas. O formato não declara compatibilidade automática com formatos de outras ferramentas.

Gere `tokens.css` com `node scripts/design-system.mjs build docs/design` a partir da raiz do projeto. Ajuste o diretório se necessário. O CSS gerado é um artefato derivado; a edição manual será substituída. O comando verifica aliases, ciclos, colisões e consistência estrutural de temas. Validade visual, contraste e adequação ao produto exigem revisão no navegador.

Catálogo, protótipos e aplicação consomem a mesma fonte. Na stack escolhida, use CSS variables ou gere um adaptador local a partir deste JSON. Registre o comando e valide equivalência. Bibliotecas devem receber os tokens existentes, sem introduzir outra paleta canônica.

## Composição

Defina hierarquia, espaçamento, densidade, largura de leitura, alinhamento e navegação a partir das jornadas. Use tokens nos valores visuais; exceções precisam de motivo. Dimensões estruturais como `100%` e `1fr` podem permanecer no layout. Fontes do sistema são a base disponível; fontes ou imagens específicas precisam de arquivos locais e licença apropriada.

## Responsividade

Escolha mudanças de layout conforme o conteúdo deixa de caber. Verifique viewport estreito e amplo, textos longos, zoom e orientação quando relevante. Registre os tamanhos efetivamente observados na revisão. A ordem de leitura e as ações devem continuar acessíveis.

## Interação e acessibilidade

Use elementos semânticos, labels associados, foco visível e teclado. Declare estados de erro, carregamento, vazio e sucesso; comunique estados por texto, além da cor. Respeite redução de movimento e mantenha controles utilizáveis por toque. Meça contraste para os pares reais de texto/fundo de cada tema e registre método e resultado; aparência ou compilação não demonstram conformidade.

## Evolução

Uma alteração de tokens exige regeneração e revisão das telas afetadas nos temas relevantes. Diferencie proposta, protótipo demonstrável e componente implementado. Integrações externas, quando solicitadas, importam/exportam estes tokens com verificação de divergências.
