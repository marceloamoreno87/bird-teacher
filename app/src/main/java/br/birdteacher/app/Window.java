package br.birdteacher.app;

import static br.birdteacher.app.Widgets.*;

import br.birdteacher.domain.*;
import br.birdteacher.domain.AudioInput.Device;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

final class Window extends JFrame {
  enum Page {
    CONVERSATION,
    TRANSCRIPT,
    COACH,
    HISTORY,
    TRAINING,
    SETTINGS
  }

  final JComboBox<Device> mic = new JComboBox<>(), output = new JComboBox<>();
  final JComboBox<Device> defaultMic = new JComboBox<>(), defaultOutput = new JComboBox<>();
  final JButton refresh = button("Atualizar fontes", null);
  final JToggleButton history = navigationButton("Sessões", "history");
  final JButton download = button("Configurar transcrição", "arrow-right"),
      configureAudio = button("Configurar áudio", "settings-2"),
      toggle = button("Iniciar gravação", "mic");
  final WhisperSettings whisperSettings = new WhisperSettings();
  private final JTabbedPane settingsTabs = new JTabbedPane();
  private final JLabel engineModel = label("", Appearance.BOLD, 14, "ink");
  private final JLabel engineLanguage = label("", Appearance.BOLD, 14, "ink");
  private final JLabel modelPending = label("Download pendente", Appearance.BODY, 13, "muted");
  final JButton finish = button("Encerrar e revisar", null), play = button("Ouvir trecho", null);
  final JButton conversationAudio = button("Ouvir conversa", null),
      folder = button("Arquivos", "folder"),
      analyze = button("Gerar revisão", "book-open");
  final JButton compact = button("Modo compacto", null), copy = button("Copiar fala", "copy");
  final JLabel state = label("Preparando", Appearance.BOLD, 14, "speaker"),
      message = label(" ", Appearance.BODY, 13, "muted");
  final JLabel shortcut = label("Ctrl  +  Shift  +  F9", Appearance.BOLD, 13, "muted");
  final JButton renameSession = button("Renomear sessão", null);
  final JToggleButton transcriptNav = navigationButton("Transcrição", null);
  private final JButton backToSessions = button("Voltar às sessões", null);
  private final JButton setupReview = button("Configurar Ollama", "settings-2");
  private final JPanel recordingSetup = panel(new BorderLayout(0, 12));
  private final JPanel sessionNav = panel(new BorderLayout());
  private final JLabel sessionDate = label("", Appearance.BODY, 14, "muted");
  private final JPanel badge = card(new FlowLayout(FlowLayout.CENTER, 8, 3), "selected", 2);
  private SessionLibrary.Entry opened;
  private Page page;
  private boolean reviewingSession;
  private boolean activeRecording;

  final JButton hearQuote = button("Ouvir minha fala", null);
  final JButton copyReport = button("Copiar revisão", "copy");
  final FeedbackPane feedback = new FeedbackPane();
  final OllamaSettings ollamaSettings = new OllamaSettings();
  final DialogueTable dialogue = new DialogueTable();
  final TrainingPane training = new TrainingPane();
  final JLabel lessonCount = new JLabel("", SwingConstants.CENTER) {
    @Override protected void paintComponent(Graphics g) {
      var ink = (Graphics2D) g.create();
      ink.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int height = Math.min(21, getHeight());
      ink.setColor(Appearance.color("accent"));
      ink.fillRoundRect(0, (getHeight() - height) / 2, getWidth(), height, height, height);
      ink.dispose();
      super.paintComponent(g);
    }
  };
  final JToggleButton trainingNav = navigationButton("Treinar", "book-open");
  final SessionLibrary library = new SessionLibrary();
  final JToggleButton coachNav = navigationButton("Revisão de inglês", "book-open"),
      conversationNav = navigationButton("Gravação", "mic"),
      settingsNav = navigationButton("Configurações", "settings-2");
  private final JButton mode = button("", "moon");
  private final JLabel title = label("Conversa ao vivo", Appearance.DISPLAY, 36, "ink");
  private final JLabel heroTitle = label("Pronto para conversar", Appearance.BOLD, 22, "ink");
  private final JLabel brandBird = new JLabel();
  private final JPanel pages = panel(new CardLayout()),
      transcript = panel(new CardLayout()),
      reports = panel(new CardLayout());
  private final JPanel reviewActions = row(play, conversationAudio, folder);
  private final BirdScene scene = new BirdScene();
  private final JLabel emptyText =
      label("As falas do microfone e do fone aparecem aqui.", Appearance.BODY, 15, "muted");
  private final JPanel rail = new JPanel(new BorderLayout());
  private final JLabel reportHint =
      label(
          "Encerre uma sessão para receber sugestões sobre suas falas.",
          Appearance.BODY,
          15,
          "muted");
  final JComboBox<Hotkey.Binding> bindings = new JComboBox<>(Hotkey.Binding.values());
  private final JLabel clock = label("", Appearance.BOLD, 13, "muted");
  private long accumulated, started;
  private final Timer tick = new Timer(500, e -> updateClock());

  Window() {
    super("Bird Teacher");
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    setMinimumSize(new Dimension(810, 600));
    setSize(1090, 750);
    setLocationByPlatform(true);
    var root = new JPanel(new BorderLayout());
    setContentPane(root);
    root.add(sidebar(), BorderLayout.WEST);
    var main = panel(new BorderLayout(0, 16));
    main.setBorder(new EmptyBorder(20, 24, 15, 24));
    root.add(main);
    var header = panel(new BorderLayout(8, 0));
    title.putClientProperty("html.disable", true);
    var identity = panel(new BorderLayout(0, 2));
    identity.add(title);
    identity.add(sessionDate, BorderLayout.SOUTH);
    header.add(identity);
    badge.add(state);
    badge.add(clock);
    var right = panel(new GridBagLayout());
    right.add(backToSessions);
    right.add(badge);
    header.add(right, BorderLayout.EAST);
    var top = panel(new BorderLayout(0, 10));
    top.add(header, BorderLayout.NORTH);
    var sessionTabs = new ButtonGroup();
    sessionTabs.add(transcriptNav);
    sessionTabs.add(coachNav);
    sessionNav.add(row(transcriptNav, coachNav));
    sessionNav.add(renameSession, BorderLayout.EAST);
    top.add(sessionNav, BorderLayout.SOUTH);
    main.add(top, BorderLayout.NORTH);
    backToSessions.addActionListener(e -> history.doClick());
    transcriptNav.addActionListener(e -> show(Page.TRANSCRIPT));
    pages.add(conversation(), Page.CONVERSATION.name());
    pages.add(coach(), Page.COACH.name());
    pages.add(training, Page.TRAINING.name());
    pages.add(library, Page.HISTORY.name());
    pages.add(settings(), Page.SETTINGS.name());
    main.add(pages);
    message.putClientProperty("html.disable", true);
    main.add(message, BorderLayout.SOUTH);
    dialogue.getModel().addTableModelListener(e -> showRows());
    conversationNav.addActionListener(e -> show(Page.CONVERSATION));
    coachNav.addActionListener(e -> show(Page.COACH));
    trainingNav.addActionListener(e -> show(Page.TRAINING));
    history.addActionListener(e -> show(Page.HISTORY));
    library.goRecord.addActionListener(e -> conversationNav.doClick());
    settingsNav.addActionListener(e -> show(Page.SETTINGS));
    download.addActionListener(e -> {
      settingsTabs.setSelectedComponent(whisperSettings);
      show(Page.SETTINGS);
    });
    configureAudio.setVisible(false);
    configureAudio.addActionListener(e -> {
      settingsTabs.setSelectedIndex(settingsTabs.indexOfTab("Áudio e atalhos"));
      show(Page.SETTINGS);
    });
    mode.addActionListener(e -> changeTheme(Appearance.palette, !Appearance.light));
    copy.addActionListener(
        e -> {
          var r = dialogue.selected();
          if (r != null)
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(r.text()), null);
        });
    setupReview.addActionListener(e -> {
      settingsTabs.setSelectedIndex(settingsTabs.indexOfTab("Revisão de inglês"));
      show(Page.SETTINGS);
    });
    show(Page.CONVERSATION);
    brandBird.setIcon(
        new ImageIcon(
            new java.awt.image.BaseMultiResolutionImage(
                Appearance.art("falcao/icone-32"), Appearance.art("falcao/icone-64"))));
    setIconImages(Appearance.emblemSizes());
    updateArt();
    showRows();
    report("");
  }

  private JPanel sidebar() {
    rail.setPreferredSize(new Dimension(180, 1));
    rail.setBorder(new EmptyBorder(22, 13, 16, 13));
    Appearance.tint(rail, null, "surface");
    var top = panel(new BorderLayout(0, 28));
    var brand = panel(new FlowLayout(FlowLayout.LEFT, 5, 2));
    brand.add(brandBird);
    brand.add(label("Bird Teacher", Appearance.BOLD, 19, "ink"));
    top.add(brand, BorderLayout.NORTH);
    var links = panel(new GridLayout(0, 1, 0, 6));
    lessonCount.setPreferredSize(new Dimension(24, 21));
    lessonCount.setFont(Appearance.BOLD.deriveFont(12f));
    Appearance.tint(lessonCount, "on-accent", null);
    trainingNav.setLayout(new BorderLayout());
    trainingNav.add(lessonCount, BorderLayout.EAST);
    showLessonCount(0);
    var destinations = new ButtonGroup();
    for (var b : List.of(conversationNav, history, trainingNav, settingsNav)) {
      destinations.add(b);
      b.setHorizontalAlignment(SwingConstants.LEADING);
      b.setIconTextGap(11);
      b.setMargin(new Insets(10, 9, 10, 8));
      links.add(b);
    }
    top.add(links);
    rail.add(top, BorderLayout.NORTH);
    var bottom = panel(new BorderLayout(0, 12));
    var engine = card(null, "background", 10);
    engine.setLayout(new BoxLayout(engine, BoxLayout.Y_AXIS));
    engine.add(label("Whisper", Appearance.BOLD, 16, "ink"));
    engine.add(Box.createVerticalStrut(6));
    engine.add(label("Idioma", Appearance.BODY, 12, "muted"));
    engine.add(engineLanguage);
    engine.add(Box.createVerticalStrut(5));
    engine.add(label("Modelo", Appearance.BODY, 12, "muted"));
    engine.add(engineModel);
    modelPending.setVisible(false);
    engine.add(modelPending);
    bottom.add(engine, BorderLayout.NORTH);
    mode.setHorizontalAlignment(SwingConstants.LEADING);
    bottom.add(mode);
    rail.add(bottom, BorderLayout.SOUTH);
    return rail;
  }

  void showLessonCount(int count) {
    lessonCount.setText(Integer.toString(count));
    lessonCount.setVisible(count > 0);
    String description = count == 1 ? "1 lição disponível" : count + " lições disponíveis";
    trainingNav.getAccessibleContext().setAccessibleName("Treinar · " + description);
    trainingNav.setToolTipText(description);
  }

  void showWhisper(String language, String model, boolean installed) {
    engineLanguage.setText(language);
    engineLanguage.setToolTipText(language);
    engineModel.setText(model);
    modelPending.setVisible(!installed);
  }

  private JPanel conversation() {
    var body = panel(new BorderLayout(0, 12));
    var hero = card(new BorderLayout(18, 0), "surface", 12);
    hero.add(scene, BorderLayout.EAST);
    var content = panel(new GridBagLayout());
    var c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 1;
    c.anchor = GridBagConstraints.WEST;
    content.add(heroTitle, c);
    c.gridy++;
    c.insets = new Insets(3, 0, 7, 0);
    content.add(keycaps(), c);
    toggle.putClientProperty("FlatLaf.styleClass", "primary");
    download.putClientProperty("FlatLaf.styleClass", "primary");
    var actions = row(toggle, finish, download, configureAudio);
    c.gridy++;
    c.insets = new Insets(0, 0, 0, 0);
    content.add(actions, c);
    hero.add(content);
    recordingSetup.add(hero, BorderLayout.NORTH);
    var sources = panel(new GridLayout(1, 2, 12, 0));
    sources.add(field("Microfone · você", mic));
    sources.add(field("Saída · áudio do fone", output));
    recordingSetup.add(sources);
    body.add(recordingSetup, BorderLayout.NORTH);
    var paper = card(new BorderLayout(0, 9), "surface", 14);
    var bar = panel(new BorderLayout());
    bar.add(label("Falas da conversa", Appearance.BOLD, 19, "ink"));
    bar.add(row(copy, compact), BorderLayout.EAST);
    paper.add(bar, BorderLayout.NORTH);
    var empty = panel(new GridBagLayout());
    empty.add(emptyText);
    transcript.add(empty, "empty");
    transcript.add(scroll(dialogue), "rows");
    paper.add(transcript);
    paper.add(reviewActions, BorderLayout.SOUTH);
    body.add(paper);
    return body;
  }

  private JPanel keycaps() {
    var group = row();
    Runnable render =
        () -> {
          group.removeAll();
          group.add(label("Atalho", Appearance.BODY, 12, "muted"));
          for (String key : shortcut.getText().split("\\s*\\+\\s*")) {
            var cap = card(new BorderLayout(), "background", 3);
            var text = label(" " + key.strip() + " ", Appearance.BOLD, 12, "muted");
            cap.add(text);
            group.add(cap);
          }
          group.revalidate();
          group.repaint();
        };
    render.run();
    shortcut.addPropertyChangeListener("text", event -> render.run());
    return group;
  }

  private JPanel coach() {
    var body = card(new BorderLayout(0, 16), "surface", 18);
    var bar = panel(new BorderLayout());
    bar.add(row(setupReview, analyze), BorderLayout.EAST);
    setupReview.setVisible(false);
    body.add(bar, BorderLayout.NORTH);
    var none = panel(new GridBagLayout());
    none.add(reportHint);
    var tools = row(hearQuote, copyReport);
    copyReport.addActionListener(
        e ->
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(feedback.getText()), null));
    body.add(tools, BorderLayout.SOUTH);
    reports.add(none, "empty");
    reports.add(scroll(feedback), "report");
    body.add(reports);
    return body;
  }

  private JComponent settings() {
    var form = card(new GridBagLayout(), "surface", 20);
    var c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.insets = new Insets(0, 0, 14, 0);
    for (var box : List.of(defaultMic, defaultOutput)) {
      box.setPrototypeDisplayValue(Device.systemDefault(Source.MIC));
      box.addActionListener(e -> box.setToolTipText(java.util.Objects.toString(box.getSelectedItem(), "")));
    }
    form.add(field("Microfone padrão", defaultMic), c);
    c.gridy++;
    form.add(field("Saída de áudio padrão", defaultOutput), c);
    c.gridy++;
    form.add(row(refresh, label("Os padrões são salvos automaticamente.", Appearance.BODY, 13, "muted")), c);
    c.gridy++;
    var palettes = new JComboBox<>(Appearance.PALETTE_NAMES);
    palettes.setSelectedIndex(
        java.util.Arrays.asList(Appearance.PALETTES).indexOf(Appearance.palette));
    palettes.addActionListener(
        e -> changeTheme(Appearance.PALETTES[palettes.getSelectedIndex()], Appearance.light));
    form.add(field("Paleta de cores", palettes), c);
    c.gridy++;
    form.add(field("Atalho de gravação e pausa", bindings), c);
    c.gridy++;
    c.weighty = 1;
    form.add(Box.createGlue(), c);
    var viewport = settingsScroll(form);
    settingsTabs.addTab("Transcrição", whisperSettings);
    settingsTabs.addTab("Áudio e atalhos", viewport);
    var ollama = panel(new BorderLayout());
    ollama.setBorder(new EmptyBorder(16, 16, 16, 16));
    ollama.add(ollamaSettings, BorderLayout.NORTH);
    settingsTabs.addTab("Revisão de inglês", settingsScroll(ollama));
    return settingsTabs;
  }

  private static JPanel field(String name, JComponent control) {
    var p = panel(new BorderLayout(0, 5));
    var text = label(name, Appearance.BOLD, 13, "muted");
    text.setLabelFor(control);
    p.add(text, BorderLayout.NORTH);
    p.add(control);
    return p;
  }

  private void updateArt() {

    mode.setText(Appearance.light ? "Modo escuro" : "Modo claro");
    mode.setIcon(Appearance.icon(Appearance.light ? "moon" : "sun", 16, mode));
    scene.repaint();
  }

  private void changeTheme(String palette, boolean light) {
    Appearance.change(palette, light);
    updateArt();
    feedback.showReport(feedback.getText());
  }

  void sessionIdentity(SessionLibrary.Entry entry) {
    opened = entry;
    updatePage();
  }

  private boolean sessionPage() {
    return page == Page.TRANSCRIPT || page == Page.COACH;
  }

  private void updatePage() {
    if (page == null) return;
    boolean detail = sessionPage();
    String heading = switch (page) {
      case CONVERSATION -> "Gravar conversa";
      case HISTORY -> "Sessões gravadas";
      case TRAINING -> "Treinar";
      case SETTINGS -> "Preferências";
      case TRANSCRIPT, COACH -> opened == null || opened.title().isBlank() ? "Sessão gravada" : opened.title();
    };
    title.setText(heading);
    title.setToolTipText(heading);
    sessionDate.setText(detail && opened != null ? SessionLibrary.display(opened.id()) : "");
    sessionDate.setVisible(detail);
    sessionNav.setVisible(detail);
    backToSessions.setVisible(detail);
    backToSessions.setEnabled(reviewingSession);
    renameSession.setEnabled(reviewingSession);
    badge.setVisible(page == Page.CONVERSATION || activeRecording);
    recordingSetup.setVisible(page == Page.CONVERSATION);
    compact.setVisible(page == Page.CONVERSATION);
    reviewActions.setVisible(page == Page.TRANSCRIPT);
    if (detail) (page == Page.TRANSCRIPT ? transcriptNav : coachNav).setSelected(true);
  }

  void show(Page next) {
    if (page != next) {
      message.setText(" ");
      message.setToolTipText(null);
    }
    page = next;
    updatePage();
    var card = next == Page.TRANSCRIPT ? Page.CONVERSATION : next;
    ((CardLayout) pages.getLayout()).show(pages, card.name());
    var selected = switch (next) {
      case CONVERSATION -> conversationNav;
      case TRANSCRIPT, COACH, HISTORY -> history;
      case SETTINGS -> settingsNav;
      case TRAINING -> trainingNav;
    };
    selected.setSelected(true);
    var focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (sessionPage()) {
      // The Sessions button can still be disabled while the archive is opening.
      (next == Page.TRANSCRIPT ? transcriptNav : coachNav).requestFocusInWindow();
    } else if (focus != null && rail.isAncestorOf(focus) && focus != selected) {
      selected.requestFocusInWindow();
    }
    if (next == Page.SETTINGS) ollamaSettings.check();
  }

  void feedbackError(String text) {
    setupReview.setVisible(true);
    reportHint.setText("A gravação está salva. Confira o Ollama e tente gerar a revisão.");
    reportHint.setToolTipText(text);
    analyze.setText("Tentar novamente");
  }

  void report(String text) {
    setupReview.setVisible(false);
    var outcome = ReviewOutcome.fromReport(text);
    boolean available = outcome == ReviewOutcome.AVAILABLE;
    reportHint.setText(outcome.message);
    reportHint.setToolTipText(null);
    analyze.setText(outcome == ReviewOutcome.MISSING ? "Gerar revisão" : "Gerar nova revisão");
    hearQuote.setVisible(available);
    copyReport.setVisible(available);
    copyReport.setEnabled(available);
    feedback.showReport(available ? text : "");
    ((CardLayout) reports.getLayout()).show(reports, available ? "report" : "empty");
  }

  void viewState(
      String label,
      String heading,
      boolean live,
      boolean busy,
      boolean reviewing,
      boolean activeSession) {
    state.setText(label);
    heroTitle.setText(heading);
    heroTitle.setVisible(!heading.equals(label));
    scene.state(live, busy);
    long now = System.nanoTime();
    if (live && started == 0) {
      started = now;
      tick.start();
    }
    if (!live && started != 0) {
      accumulated += now - started;
      started = 0;
      tick.stop();
    }
    clock.setVisible(live || activeSession && accumulated > 0);
    updateClock();
    reviewingSession = reviewing;
    activeRecording = activeSession;
    updatePage();
    finish.setVisible(activeSession);
    if (busy && page == Page.COACH) {
      setupReview.setVisible(false);
      reportHint.setText("O Ollama está preparando a revisão desta sessão…");
    }
    emptyText.setText(
        live
            ? "Ouvindo. Sua primeira fala aparecerá aqui."
            : reviewing
                ? "Esta sessão não contém falas transcritas."
                : "As falas do microfone e do fone aparecem aqui.");
  }

  void newClock() {
    accumulated = 0;
    started = 0;
    tick.stop();
    updateClock();
  }

  private void updateClock() {
    long elapsed =
        (accumulated + (started == 0 ? 0 : System.nanoTime() - started)) / 1_000_000_000L;
    clock.setText("%02d:%02d".formatted(elapsed / 60, elapsed % 60));
  }

  @Override
  public void dispose() {
    tick.stop();
    super.dispose();
  }

  private void showRows() {
    ((CardLayout) transcript.getLayout())
        .show(transcript, dialogue.getRowCount() > 0 ? "rows" : "empty");
  }
}
