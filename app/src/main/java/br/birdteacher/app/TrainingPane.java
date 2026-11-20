package br.birdteacher.app;

import br.birdteacher.domain.Training.Lesson;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import static br.birdteacher.app.Widgets.*;

final class TrainingPane extends JPanel {
    private List<Lesson> available = List.of();
    private final JTextArea preparation = paragraph("");
    private final JButton back = button("Voltar às lições", null);
    final JButton listen = button("Ouvir minha fala", null);
    final JButton finish = button("Concluir treino", null);
    final JButton dismiss = button("Dispensar este foco", null);
    final JButton reveal = button("Conferir reformulação", null);
    final JButton write = button("Ir para escrita", null);
    final JButton check = button("Comparar resposta", null);
    final JTextArea answer = new JTextArea(3, 30);
    private final JPanel body = card(new GridBagLayout(), "surface", 18);
    private final JTextArea status = paragraph("");
    private Lesson lesson;
    private final JScrollPane answerField = new JScrollPane(answer);
    private boolean oralRevealed;
    private boolean compared;

    TrainingPane() {
        super(new BorderLayout());
        setOpaque(false);
        add(settingsScroll(body));
        answer.setMargin(new Insets(8, 10, 8, 10));
        Appearance.tint(answer, "ink", "background");
        answer.setLineWrap(true);
        answer.setWrapStyleWord(true);
        answer.getAccessibleContext().setAccessibleName("Sua resposta em inglês");
        answer.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { enableCheck(); }
            public void removeUpdate(DocumentEvent e) { enableCheck(); }
            public void changedUpdate(DocumentEvent e) { enableCheck(); }
        });
        reveal.addActionListener(e -> { oralRevealed = true; audioExercise(); });
        write.addActionListener(e -> writtenExercise());
        check.addActionListener(e -> { compared = true; writtenExercise(); });
        back.addActionListener(e -> catalog(""));
        preparation("");
        catalog("");
    }

    Lesson lesson() { return lesson; }

    void available(List<Lesson> lessons) {
        if (available.equals(lessons)) return;
        available = List.copyOf(lessons);
        if (lesson == null || lessons.stream().noneMatch(item -> item.focus() == lesson.focus()
                && new java.util.HashSet<>(item.examples()).containsAll(lesson.examples()))) catalog("");
    }

    void preparation(String text) {
        preparation.setText(text);
        preparation.setVisible(!text.isBlank());
    }

    void catalog(String message) {
        lesson = null;
        status.setText(message);
        var rows = new ArrayList<Component>();
        if (!message.isBlank()) rows.add(status);
        if (available.isEmpty()) {
            rows.add(label("Ainda não há lições disponíveis", Appearance.BOLD, 18, "ink"));
            rows.add(paragraph("Uma lição aparece aqui quando o mesmo ponto de inglês se repete em quatro falas suas."));
        }
        for (var entry : available) {
            var start = button(entry.focus().title, "book-open");
            start.setHorizontalAlignment(SwingConstants.LEFT);
            start.setToolTipText("Praticar com base em " + entry.examples().size() + " falas");
            start.addActionListener(e -> openLesson(entry));
            rows.add(start);
        }
        rows.add(preparation);
        render(rows.toArray(Component[]::new));
    }

    void saving() { render(paragraph("Salvando treino…")); }

    void openLesson(Lesson selected) {
        lesson = selected;
        oralRevealed = false;
        compared = false;
        answer.setText("");
        answer.setEditable(true);
        status.setText("");
        audioExercise();
    }

    private JLabel heading() {
        return label(lesson.focus().title, Appearance.BOLD, 18, "ink");
    }

    private void audioExercise() {
        var example = lesson.audio();
        var count = paragraph("Identificado em " + lesson.examples().size() + " falas suas.");
        var instruction = paragraph("Ouça sua fala. Depois, diga em voz alta como você expressaria a mesma ideia.");
        var step = label("Áudio", Appearance.BOLD, 16, "ink");
        if (oralRevealed) {
            render(heading(), count, step, instruction, row(listen),
                    paragraph("Sua fala · " + example.change().quote()),
                    paragraph("Uma reformulação · " + example.change().english()),
                    paragraph(example.change().explanation()), row(write, dismiss, back), status);
        } else {
            render(heading(), count, step, instruction, row(listen, reveal), row(dismiss, back), status);
        }
    }

    private void writtenExercise() {
        var change = lesson.writing().change();
        var instruction = paragraph("Reescreva esta fala em inglês, aplicando o mesmo ponto.");
        var step = label("Escrita", Appearance.BOLD, 16, "ink");
        var quote = paragraph("“" + change.quote() + "”");
        var caption = label("Sua resposta em inglês", Appearance.BOLD, 13, "muted");
        caption.setLabelFor(answer);
        if (compared) {
            answer.setEditable(false);
            render(heading(), step, instruction, quote, caption, answerField,
                    paragraph("Uma reformulação · " + change.english()), paragraph(change.explanation()),
                    row(finish, dismiss, back), status);
        } else {
            enableCheck();
            render(heading(), step, instruction, quote, caption, answerField, row(check, dismiss, back), status);
            answer.requestFocusInWindow();
        }
    }

    private void enableCheck() { check.setEnabled(!answer.getText().isBlank()); }

    void saveFailed(String message) {
        status.setText(message);
        if (compared) writtenExercise(); else audioExercise();
    }

    private static JTextArea paragraph(String text) {
        var area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Appearance.BODY.deriveFont(15f));
        Appearance.tint(area, "ink", null);
        return area;
    }

    private void render(Component... rows) {
        body.removeAll();
        var c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 12, 0);
        for (var component : rows) {
            body.add(component, c);
            c.gridy++;
        }
        body.revalidate();
        body.repaint();
    }
}
