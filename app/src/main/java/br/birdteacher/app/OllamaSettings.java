package br.birdteacher.app;

import static br.birdteacher.app.Widgets.*;
import br.birdteacher.infra.Ollama;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;

/** Setup runs outside the audio queue so downloading a model cannot delay a pause. */
final class OllamaSettings extends JPanel {
    static final String DEFAULT_MODEL = "qwen3:4b-instruct";
    private final JComboBox<String> models = new JComboBox<>(new String[]{DEFAULT_MODEL, "gemma3:1b", "llama3.2:3b"});
    private final JButton install = button("Instalar Ollama", null), use = button("Baixar e usar modelo", "arrow-right");
    private final JLabel active = label("", Appearance.BOLD, 13, "ink");
    private final JLabel status = label("Verificando o Ollama…", Appearance.BODY, 13, "muted");
    private boolean connected;
    private SwingWorker<List<String>, String> worker;
    private Ollama ollama;
    private Consumer<String> onReady;

    OllamaSettings() {
        super(new GridLayout(0, 1, 0, 4));
        setOpaque(false);
        var title = label("Modelo de revisão · Ollama", Appearance.BOLD, 14, "ink");
        models.setEditable(true);
        models.setPrototypeDisplayValue(DEFAULT_MODEL);
        title.setLabelFor(models);
        add(title);
        add(models);
        add(row(install, use));
        add(active);
        add(status);
        install.addActionListener(e -> run(Action.INSTALL));
        use.addActionListener(e -> run(Action.USE));
        enabled();
    }

    void bind(Ollama service, String model, Consumer<String> selected) {
        ollama = service;
        onReady = selected;
        models.setSelectedItem(model);
        active.setText("Modelo configurado · " + model);
        active.setToolTipText(model);
    }

    void check() { if (ollama != null && worker == null) run(Action.CHECK); }

    private enum Action { CHECK, INSTALL, USE }

    private void run(Action action) {
        if (worker != null || ollama == null) return;
        String chosen = String.valueOf(models.getEditor().getItem()).strip();
        String initial = switch (action) {
            case CHECK -> "Verificando o Ollama…";
            case INSTALL -> "Preparando o Ollama…";
            case USE -> "Verificando " + chosen + "…";
        };
        showStatus(initial);
        worker = new SwingWorker<>() {
            @Override protected List<String> doInBackground() throws Exception {
                if (action == Action.INSTALL) return WindowsOllama.connect(ollama, true, this::publish);
                if (action == Action.USE) {
                    WindowsOllama.connect(ollama, false, this::publish);
                    ollama.prepareModel(chosen, this::publish);
                }
                return ollama.models();
            }
            @Override protected void process(List<String> messages) {
                if (!isCancelled()) showStatus(messages.getLast());
            }
            @Override protected void done() {
                if (isCancelled()) return;
                try {
                    var installed = get();
                    Object draft = models.getEditor().getItem();
                    for (var name : installed) {
                        if (((DefaultComboBoxModel<String>)models.getModel()).getIndexOf(name) < 0) models.addItem(name);
                    }
                    models.getEditor().setItem(draft);
                    connected = true;
                    if (action == Action.USE) {
                        active.setText("Modelo configurado · " + chosen);
                        active.setToolTipText(chosen);
                        onReady.accept(chosen);
                    }
                    showStatus(action == Action.USE ? "Modelo pronto para gerar a revisão." : "Ollama em execução neste computador.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    if (action != Action.USE || e.getCause() instanceof java.net.ConnectException) connected = false;
                    if (action == Action.CHECK && e.getCause() instanceof java.net.ConnectException)
                        showStatus(WindowsOllama.installed() ? "O Ollama está instalado e precisa ser iniciado." : "Instale o Ollama para baixar um modelo.");
                    else showStatus(e.getCause().getMessage() == null ? "Falha ao preparar o Ollama. Tente novamente." : e.getCause().getMessage());
                } finally { worker = null; enabled(); }
            }
        };
        enabled();
        worker.execute();
    }

    private void enabled() {
        boolean idle = worker == null;
        install.setVisible(!connected);
        install.setText(WindowsOllama.installed() ? "Iniciar Ollama" : "Instalar Ollama");
        install.setEnabled(idle && !connected);
        models.setEnabled(idle);
        use.setEnabled(idle && connected);
    }

    private void showStatus(String text) {
        status.setText(text);
        status.setToolTipText(text);
    }

    void close() { if (worker != null) worker.cancel(true); }
}
