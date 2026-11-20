# Aparência e composição

O falcão-peregrino é o mascote do Bird Teacher. A plumagem azul-petróleo, a face creme e o olho âmbar se repetem nas três poses e no emblema usado pelo Windows. A aparência inicial combina a paleta Petróleo com o modo escuro. Configurações permite escolher entre cinco paletas. O controle no rodapé da navegação alterna entre claro e escuro. O aplicativo lembra as duas escolhas.

O título identifica a página. A barra lateral mantém os destinos no mesmo lugar, junto à condição do modelo de voz. Na conversa, o cartão de coleta precede os seletores de dispositivos. A lista ocupa o espaço restante e preserva a posição de quem estiver relendo uma fala.

A fonte editável das cores é `tokens.json`. A tarefa Gradle `exportPalette` produz o recurso de cores do Swing. O catálogo usa `tokens.css`, compilado com `node scripts/design-system.mjs build docs/design`. O pacote carrega as fontes e as ilustrações localmente.

A janela completa admite 810 × 600 unidades lógicas. Para acompanhar a chamada em menos espaço, existe o modo compacto, que mantém a última fala e os controles da mesma sessão. No Windows com escala maior, as dimensões físicas acompanham a escala do Swing.
