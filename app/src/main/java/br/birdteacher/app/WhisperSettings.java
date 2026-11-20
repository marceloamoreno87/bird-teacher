package br.birdteacher.app;

import static br.birdteacher.app.Widgets.*;

import br.birdteacher.infra.Whisper;
import br.birdteacher.infra.WhisperModel;
import br.birdteacher.infra.WhisperModel.Language;
import br.birdteacher.infra.WhisperModel.Profile;
import java.awt.*;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class WhisperSettings extends JPanel {
    private static final Locale PT = Locale.forLanguageTag("pt-BR");
    final JTextField search = new JTextField();
    final JList<Language> languages = fittedList();
    final JList<WhisperModel> models = fittedList();
    final JButton install = button("Baixar e instalar", null);
    final JButton use = button("Usar na transcrição", "arrow-right");
    private final JButton cancel = button("Cancelar", null);
    private final JLabel current = label("", Appearance.BOLD, 14, "ink");
    private final JLabel status = label("Um modelo multilíngue pode ser usado em vários idiomas.", Appearance.BODY, 13, "muted");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final Language[] sorted = Language.values();
    private Whisper whisper;
    private Runnable installed;
    private SwingWorker<Void, Void> transfer;
    private boolean idle, activating;

    WhisperSettings() {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 16, 14, 16));
        var collator = Collator.getInstance(PT);
        var favorites = java.util.List.of(Language.ENGLISH, Language.PORTUGUESE, Language.SPANISH);
        Arrays.sort(sorted, java.util.Comparator.comparingInt((Language l) ->
                favorites.contains(l) ? favorites.indexOf(l) : favorites.size()).thenComparing(Language::toString, collator));
        var heading = panel(new GridLayout(0,1,0,2));
        heading.add(current);
        heading.add(label("O Whisper inicia em inglês quando você abre o aplicativo.", Appearance.BODY, 13, "muted"));
        add(heading, BorderLayout.NORTH);

        var catalog = panel(new BorderLayout(12, 0));
        var left = panel(new BorderLayout(0, 5));
        left.setPreferredSize(new Dimension(157, 0));
        var searchBox = panel(new BorderLayout(0, 4));
        var languageLabel = label("Idioma", Appearance.BOLD, 14, "ink");
        languageLabel.setLabelFor(search);
        searchBox.add(languageLabel, BorderLayout.NORTH);
        search.putClientProperty("JTextField.placeholderText", "Buscar idioma…");
        searchBox.add(search);
        left.add(searchBox, BorderLayout.NORTH);
        languages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        languages.setFixedCellHeight(28);
        languages.getAccessibleContext().setAccessibleName("Idiomas do Whisper");
        left.add(scroll(languages));
        catalog.add(left, BorderLayout.WEST);

        var right = panel(new BorderLayout(0, 5));
        var modelLabel = label("Modelos compatíveis", Appearance.BOLD, 14, "ink");
        modelLabel.setLabelFor(models);
        right.add(modelLabel, BorderLayout.NORTH);
        models.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        models.setFixedCellHeight(46);
        models.setCellRenderer((list, model, index, selected, focus) -> {
            var cell = new JPanel(new BorderLayout(8, 0));
            cell.setBorder(new EmptyBorder(3, 9, 3, 9));
            cell.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            var title = new JLabel(model.id);
            var details = new JLabel(model.description() + " · " + model.sizeLabel());
            var state = new JLabel(modelState(model));
            title.setFont(Appearance.BOLD.deriveFont(14f));
            details.setFont(Appearance.BODY.deriveFont(12f));
            state.setFont(Appearance.BOLD.deriveFont(12f));
            Color ink = selected ? list.getSelectionForeground() : list.getForeground();
            for (var l : new JLabel[]{title, details, state}) l.setForeground(ink);
            cell.add(title, BorderLayout.CENTER);
            cell.add(details, BorderLayout.SOUTH);
            cell.add(state, BorderLayout.EAST);
            return cell;
        });
        right.add(scroll(models));
        catalog.add(right);
        add(catalog);
        var bottom = panel(new BorderLayout(0, 4));
        bottom.add(row(install, use, cancel), BorderLayout.NORTH);
        bottom.add(progress);
        bottom.add(status, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        progress.setStringPainted(true);
        progress.setVisible(false);
        cancel.setVisible(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        languages.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) listModels(); });
        models.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) update(); });
        install.addActionListener(e -> startInstall());
        cancel.addActionListener(e -> { if (transfer != null) transfer.cancel(true); });
    }

    void bind(Whisper engine, Consumer<Profile> activate, Runnable afterInstall) {
        whisper = engine;
        installed = afterInstall;
        use.addActionListener(e -> activate.accept(new Profile(languages.getSelectedValue(), models.getSelectedValue())));
        filter();
        languages.setSelectedValue(whisper.profile().language(), true);
        update();
    }

    private static String normalized(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).strip();
    }

    private void filter() {
        Language previous = languages.getSelectedValue();
        String query = normalized(search.getText());
        var options = Arrays.stream(sorted)
                .filter(l -> normalized(l.toString()).contains(query) || l.code.equals(query)
                        || l.name().toLowerCase(Locale.ROOT).replace('_', ' ').contains(query))
                .toArray(Language[]::new);
        languages.setListData(options);
        if (options.length == 0) {
            listModels();
            notice("Nenhum idioma encontrado. Busque pelo nome ou código.");
            return;
        }
        notice("Um modelo multilíngue pode ser usado em vários idiomas.");
        Language preferred = previous == null && whisper != null ? whisper.profile().language() : previous;
        for (var option : options) {
            if (option.code.equals(query)) { preferred = option; break; }
        }
        languages.setSelectedValue(preferred, false);
        if (languages.isSelectionEmpty()) languages.setSelectedIndex(0);
        languages.ensureIndexIsVisible(languages.getSelectedIndex());
    }

    private void listModels() {
        var lang = languages.getSelectedValue();
        var prior = models.getSelectedValue();
        var choices = lang == null ? new WhisperModel[0] : lang.models();
        if (whisper != null) {
            // Stable sort preserves the suggested model order within each installation state.
            Arrays.sort(choices, java.util.Comparator.comparingInt((WhisperModel model) ->
                    model == whisper.active() && whisper.available(model) ? 0 : whisper.available(model) ? 1 : 2));
        }
        models.setListData(choices);
        if (choices.length > 0 && whisper != null) {
            var firstInstalled = Arrays.stream(choices).filter(whisper::available).findFirst().orElse(choices[0]);
            var selected = lang == whisper.profile().language() ? whisper.active()
                    : prior != null && lang.supports(prior) && whisper.available(prior) ? prior : firstInstalled;
            models.setSelectedValue(selected, true);
        }
        update();
    }

    private String modelState(WhisperModel model) {
        if (whisper == null || !whisper.available(model)) return "Para baixar";
        if (model == whisper.active() && languages.getSelectedValue() == whisper.profile().language()) return "Em uso";
        return "Instalado";
    }

    void allowActivation(boolean allowed) { idle = allowed; update(); }

    private void update() {
        if (whisper == null) return;
        var model = models.getSelectedValue();
        boolean free = transfer == null && !activating;
        boolean local = model != null && whisper.available(model);
        boolean active = local && model == whisper.active() && languages.getSelectedValue() == whisper.profile().language();
        current.setText((whisper.installed() ? "Em uso: " : "Primeira configuração: ") + whisper.profile().language() + " · " + whisper.active().id);
        search.setEnabled(free);
        languages.setEnabled(free);
        models.setEnabled(free);
        install.setEnabled(free && model != null);
        install.setText(local ? "Verificar instalação" : "Baixar e instalar");
        use.setEnabled(idle && free && local && !active);
        use.setVisible(transfer == null && !active);
        install.setVisible(transfer == null);
        install.putClientProperty("FlatLaf.styleClass", !local && model != null ? "primary" : null);
        use.putClientProperty("FlatLaf.styleClass", local && !active ? "primary" : null);
        models.repaint();
    }

    private void notice(String text) {
        status.setText(text);
        status.setToolTipText(text);
    }

    void activating() {
        activating = true;
        notice("Carregando o modelo de voz…");
        update();
    }

    void activated(String message) {
        activating = false;
        notice(message);
        update();
    }

    private void startInstall() {
        var model = models.getSelectedValue();
        if (model == null || transfer != null || activating) return;
        boolean verify = whisper.available(model);
        progress.setValue(0);
        progress.setIndeterminate(true);
        progress.setString(verify ? "Verificando arquivo…" : "Conectando…");
        progress.setVisible(true);
        cancel.setVisible(true);
        notice((verify ? "Verificando " : "Baixando ") + model.id + " · " + model.sizeLabel());
        transfer = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                whisper.install(model, bytes -> setProgress((int)(100 * bytes / model.bytes)));
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    notice(model.id + " instalado e pronto para uso.");
                } catch (CancellationException e) {
                    notice("Operação cancelada.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    notice("Falha ao instalar " + model.id + " · " + e.getCause().getMessage());
                } finally {
                    transfer = null;
                    progress.setVisible(false);
                    cancel.setVisible(false);
                    update();
                    installed.run();
                }
            }
        };
        transfer.addPropertyChangeListener(e -> {
            if (e.getSource() != transfer || !"progress".equals(e.getPropertyName())) return;
            int percent = (int)e.getNewValue();
            progress.setIndeterminate(false);
            progress.setValue(percent);
            progress.setString(percent == 100 ? "Verificando arquivo…" : percent + "%");
        });
        update();
        transfer.execute();
    }

    void close() { if (transfer != null) transfer.cancel(true); }
}
