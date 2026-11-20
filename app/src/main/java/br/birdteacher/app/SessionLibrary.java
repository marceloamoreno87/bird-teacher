package br.birdteacher.app;

import static br.birdteacher.app.Widgets.*;

import br.birdteacher.domain.ReviewOutcome;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.text.Normalizer;

import javax.swing.*;
import javax.swing.event.*;

final class SessionLibrary extends JPanel {
    private static final DateTimeFormatter DIRECTORY_DATE = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH-mm-ss", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss", Locale.ROOT);

    record Entry(String id, String title, ReviewOutcome outcome) {
        Entry(String id, String title) { this(id, title, ReviewOutcome.MISSING); }

        boolean hasReview() { return outcome == ReviewOutcome.AVAILABLE; }

        String caption() {
            String date = display(id);
            return title.isEmpty() ? date : title + " · " + date;
        }
    }

    private final JTextField query = new JTextField();
    private final JPanel rows = panel(new GridLayout(0, 1, 0, 4));
    private final JLabel summary = label("", Appearance.BODY, 14, "muted");
    final JButton goRecord = button("Ir para gravação", "mic");
    private final JButton clearSearch = button("Limpar busca", null);
    private Consumer<Entry> copyPromptAction = entry -> {};
    private Consumer<Entry> removeSession = entry -> {};
    private List<Entry> entries = List.of();
    private boolean actionsEnabled = true;
    private BiConsumer<String, String> saveTitle = (id, title) -> {};
    private BiConsumer<String, Window.Page> listener = (id, page) -> {};

    SessionLibrary() {
        super(new BorderLayout(0, 14));
        setOpaque(false);
        query.putClientProperty("JTextField.placeholderText", "Buscar por nome, data ou horário…");
        query.putClientProperty("JTextField.leadingIcon", Appearance.icon("search", 16, query));
        query.getAccessibleContext().setAccessibleName("Buscar sessões por nome, data ou horário");
        query.getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            public void insertUpdate(DocumentEvent e) {
                                filter();
                            }

                            public void removeUpdate(DocumentEvent e) {
                                filter();
                            }

                            public void changedUpdate(DocumentEvent e) {
                                filter();
                            }
                        });
        add(query, BorderLayout.NORTH);
        var body = card(new BorderLayout(0, 10), "surface", 12);
        clearSearch.addActionListener(e -> query.setText(""));
        body.add(summary, BorderLayout.NORTH);
        body.add(settingsScroll(rows));
        var actions = row(goRecord, clearSearch);
        body.add(actions, BorderLayout.SOUTH);
        add(body);
        filter();
    }

    static String display(String name) {
        int suffix = name.indexOf('_');
        String timestamp = suffix < 0 ? name : name.substring(0, suffix);
        try {
            return LocalDateTime.parse(timestamp, DIRECTORY_DATE).format(DISPLAY_DATE);
        } catch (DateTimeParseException e) {
            return name;
        }
    }

    private JPanel sessionRow(Entry entry) {
        var container = card(new BorderLayout(10, 0), "background", 8);
        String date = display(entry.id());
        var name = label("", Appearance.BOLD, 16, "ink");
        name.putClientProperty("html.disable", true);
        name.setText(entry.title().isEmpty() ? date : entry.title());
        name.setToolTipText(entry.caption());
        String availability = switch (entry.outcome()) {
            case MISSING -> "Sem revisão";
            case NO_SPEECH -> "Sem fala confirmada";
            case NO_SUGGESTIONS -> "Sem sugestões";
            case AVAILABLE -> "Revisão disponível";
        };
        var identity = panel(new BorderLayout(0, 2));
        identity.add(name);
        String subtitle = entry.title().isEmpty() ? availability : date;
        if (!entry.title().isEmpty() && !entry.hasReview()) subtitle += " · " + availability;
        var detail = label(subtitle, Appearance.BODY, 12, "muted");
        detail.setToolTipText(subtitle);
        identity.add(detail, BorderLayout.SOUTH);
        identity.setMinimumSize(new Dimension(0, 40));
        container.add(identity);
        var open = action(entry, "Abrir", "arrow-right", "Abrir transcrição",
                () -> listener.accept(entry.id(), Window.Page.TRANSCRIPT));
        name.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) open.doClick();
            }
        });
        var buttons = panel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.add(open);
        if (entry.hasReview())
            buttons.add(action(entry, "Revisão", "book-open", "Ver revisão de inglês",
                    () -> listener.accept(entry.id(), Window.Page.COACH)));
        buttons.add(action(entry, "", "pencil", "Renomear sessão", () -> editTitle(this, entry)));
        buttons.add(action(entry, "", "copy", "Copiar prompt de revisão", () -> copyPromptAction.accept(entry)));
        buttons.add(action(entry, "Excluir", "trash-2", "Excluir sessão", () -> {
            if (confirmDelete(this, entry)) removeSession.accept(entry);
        }));
        var aligned = panel(new GridBagLayout());
        aligned.add(buttons);
        container.add(aligned, BorderLayout.EAST);
        return container;
    }

    private JButton action(Entry entry, String text, String icon, String description, Runnable run) {
        var control = button(text, icon);
        control.setMargin(new Insets(5, 6, 5, 6));
        control.setToolTipText(description);
        control.getAccessibleContext().setAccessibleName(description);
        control.getAccessibleContext().setAccessibleDescription(entry.caption());
        control.addActionListener(e -> run.run());
        control.setEnabled(actionsEnabled);
        control.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "pressed");
        control.getInputMap().put(KeyStroke.getKeyStroke("released ENTER"), "released");
        return control;
    }

    private static boolean confirmDelete(Component parent, Entry entry) {
        var name = new JLabel();
        name.putClientProperty("html.disable", true);
        name.setText(entry.caption());
        var token = new JTextField(22);
        var instruction = new JLabel("Digite delete para confirmar.");
        instruction.setLabelFor(token);
        var form = panel(new GridLayout(0, 1, 0, 5));
        form.add(name);
        form.add(new JLabel("O áudio, a transcrição e a revisão irão para a Lixeira."));
        form.add(instruction);
        form.add(token);
        var cancel = new JButton("Cancelar");
        var confirm = new JButton("Excluir");
        confirm.setEnabled(false);
        token.getDocument().addDocumentListener(new DocumentListener() {
            private void validateToken() { confirm.setEnabled("delete".equals(token.getText())); }
            public void insertUpdate(DocumentEvent e) { validateToken(); }
            public void removeUpdate(DocumentEvent e) { validateToken(); }
            public void changedUpdate(DocumentEvent e) { validateToken(); }
        });
        var pane = new JOptionPane(form, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, new Object[]{cancel, confirm}, cancel);
        cancel.addActionListener(e -> pane.setValue(cancel));
        confirm.addActionListener(e -> pane.setValue(confirm));
        var dialog = pane.createDialog(parent, "Excluir sessão");
        try {
            dialog.getRootPane().setDefaultButton(confirm);
            SwingUtilities.invokeLater(token::requestFocusInWindow);
            dialog.setVisible(true);
            return pane.getValue() == confirm && "delete".equals(token.getText());
        } finally { dialog.dispose(); }
    }

    void onDelete(Consumer<Entry> action) { removeSession = action; }

    void allowActions(boolean allowed) {
        if (allowed == actionsEnabled) return;
        actionsEnabled = allowed;
        filter();
    }

    void removed(String id) {
        load(entries.stream().filter(e -> !e.id().equals(id)).toList());
    }

    void onOpen(BiConsumer<String, Window.Page> action) {
        listener = action;
    }

    void onCopyPrompt(Consumer<Entry> action) {
        copyPromptAction = action;
    }

    void onRename(BiConsumer<String, String> action) {
        saveTitle = action;
    }

    void editTitle(Component parent, Entry entry) {
        if (entry == null) return;
        var field = new JTextField(entry.title(), 28);
        field.selectAll();
        var label = new JLabel("Nome da sessão de " + display(entry.id()));
        label.setLabelFor(field);
        var panel = new JPanel(new BorderLayout(0, 6));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field);
        panel.add(new JLabel("Deixe em branco para exibir só a data."), BorderLayout.SOUTH);
        String[] options = {"Salvar", "Cancelar"};
        int answer = JOptionPane.showOptionDialog(parent, panel, "Renomear sessão",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (answer == 0 && !field.getText().strip().equals(entry.title()))
            saveTitle.accept(entry.id(), field.getText().strip());
    }

    void load(List<Entry> items) {
        entries = List.copyOf(items);
        filter();
    }

    void renamed(String id, String title) {
        load(entries.stream().map(e -> e.id().equals(id) ? new Entry(id, title, e.outcome()) : e).toList());
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).strip();
    }

    private void filter() {
        String term = normalize(query.getText());
        rows.removeAll();
        for (var entry : entries) {
            if (normalize(entry.caption()).contains(term) || entry.id().contains(term))
                rows.add(sessionRow(entry));
        }
        int count = rows.getComponentCount();
        summary.setText(entries.isEmpty() ? "Suas sessões aparecerão aqui depois da primeira gravação."
                : count == 0 ? "Nenhuma sessão encontrada."
                : count == 1 ? "1 sessão" : count + " sessões");
        goRecord.setVisible(entries.isEmpty());
        clearSearch.setVisible(!entries.isEmpty() && count == 0);
        rows.revalidate();
        rows.repaint();
    }
}
