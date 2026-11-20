package br.birdteacher.app;

import br.birdteacher.domain.*;
import br.birdteacher.infra.*;
import br.birdteacher.usecase.Session;
import br.birdteacher.usecase.ReviewMarkdown;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.*;

final class DesktopController {
    private enum State {
        READY("Pronto"),
        STARTING("Preparando"),
        LIVE("Gravando"),
        PAUSING("Pausando"),
        PAUSED("Pausado"),
        FINISHING("Concluindo transcrição"),
        REVIEW("Revisão"),
        LOADING("Carregando"),
        ANALYZING("Analisando com Ollama");
        final String label;

        State(String label) {
            this.label = label;
        }
    }

    private final Window ui;
    private final AudioInput audio;
    private final Whisper whisper;
    private final Ollama ollama;
    private final Path library;
    private final AutomaticTraining training;
    private final ExecutorService work = Executors.newSingleThreadExecutor();
    private final Preferences prefs;
    private final AudioSourcePicker micPicker, outputPicker;
    private final Hotkey keys;
    private final Companion companion;
    private State state = State.READY;
    private volatile LocalSession archive;
    private volatile Session session;
    private String openingError;
    private javax.sound.sampled.Clip playing;
    private boolean closing;
    private String feedbackModel;
    private LocalSession awaitingSetup;
    private boolean listingHistory;
    private Future<?> voiceInstall;

    DesktopController(Window ui, AudioInput audio, Whisper whisper, Ollama ollama, Path library) {
        this(ui, audio, whisper, ollama, library, Preferences.userNodeForPackage(DesktopController.class));
    }

    DesktopController(Window ui, AudioInput audio, Whisper whisper, Ollama ollama, Path library, Preferences prefs) {
        this.prefs = prefs;
        this.ui = ui;
        this.audio = audio;
        this.whisper = whisper;
        this.ollama = ollama;
        this.library = library;
        var trainingLibrary = new TrainingLibrary(library, (lines, model) -> {
            WindowsOllama.connect(ollama, false, ignored -> {});
            return ollama.trainingFindings(lines, model);
        });
        training = new AutomaticTraining(trainingLibrary, ui, () -> feedbackModel);
        micPicker = new AudioSourcePicker(Source.MIC, ui.defaultMic, ui.mic, prefs);
        outputPicker = new AudioSourcePicker(Source.OUTPUT, ui.defaultOutput, ui.output, prefs);
        companion = new Companion(ui);
        keys = new Hotkey(() -> SwingUtilities.invokeLater(this::toggle));
    }

    void open() {
        Hotkey.Binding binding;
        try {
            binding = Hotkey.Binding.valueOf(prefs.get("hotkey", "DEFAULT"));
        } catch (IllegalArgumentException e) {
            binding = Hotkey.Binding.DEFAULT;
        }
        ui.bindings.setSelectedItem(binding);
        keys.use(binding);
        ui.shortcut.setText(binding.label);
        ui.bindings.addActionListener(
                e -> {
                    var chosen = (Hotkey.Binding) ui.bindings.getSelectedItem();
                    keys.use(chosen);
                    prefs.put("hotkey", chosen.name());
                    ui.shortcut.setText(chosen.label);
                });
        feedbackModel = prefs.get("ollama", OllamaSettings.DEFAULT_MODEL);
        ui.ollamaSettings.bind(ollama, feedbackModel, selected -> {
            feedbackModel = selected;
            prefs.put("ollama", selected);
            training.update(null);
            if (awaitingSetup != null && awaitingSetup == archive && state == State.REVIEW) analyze();
        });
        ui.toggle.addActionListener(e -> toggle());
        ui.finish.addActionListener(e -> finish());
        ui.analyze.addActionListener(e -> analyze());
        ui.training.listen.addActionListener(e -> {
            var example = ui.training.lesson().audio();
            playFile(library.resolve(example.session()).resolve(example.change().id() + ".wav"));
        });
        ui.training.finish.addActionListener(e -> finishTraining("Treino concluído."));
        ui.training.dismiss.addActionListener(e -> finishTraining("Foco dispensado."));
        ui.history.addActionListener(e -> history());
        ui.conversationNav.addActionListener(e -> {
            if (state != State.REVIEW) return;
            stopPlayback();
            archive = null;
            awaitingSetup = null;
            ui.sessionIdentity(null);
            ui.dialogue.clearRows();
            ui.report("");
            ui.newClock();
            status(State.READY, sourcesHint());
        });
        ui.whisperSettings.bind(whisper, this::activateVoice, () -> {
            if (closing) return;
            if (state == State.READY) status(state, sourcesHint());
            else controls();
        });
        ui.refresh.addActionListener(e -> refresh());
        ui.mic.addActionListener(e -> sourceChanged());
        ui.output.addActionListener(e -> sourceChanged());
        ui.feedback.addCaretListener(e -> quoteControl());
        ui.hearQuote.addActionListener(
                e -> {
                    int index = ui.dialogue.rowFor(ui.feedback.citation());
                    if (state != State.REVIEW || index < 0) return;
                    ui.dialogue.setRowSelectionInterval(index, index);
                    play();
                });
        ui.play.addActionListener(e -> play());
        ui.folder.addActionListener(
                e ->
                        launch(
                                () -> {
                                    Desktop.getDesktop().open(archive.directory().toFile());
                                    return null;
                                },
                                v -> {},
                                this::error));
        ui.conversationAudio.addActionListener(e -> hearConversation());
        ui.dialogue.getSelectionModel().addListSelectionListener(e -> controls());
        ui.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        close();
                    }
                });
        ui.library.onOpen(this::openArchive);
        ui.library.onRename(this::renameArchive);
        ui.library.onDelete(this::deleteArchive);
        ui.renameSession.addActionListener(e -> editCurrentTitle());
        ui.library.onCopyPrompt(this::copyReviewPrompt);
        ui.dialogue.getModel().addTableModelListener(e -> companion.line(ui.dialogue.last()));
        companion.installTray();
        companion.line(null);
        ui.setVisible(true);
        try {
            keys.register();
        } catch (Exception | LinkageError e) {
            ui.shortcut.setText("Atalho indisponível · use o botão");
            ui.bindings.setEnabled(false);
        }
        refresh();
        training.open();
    }

    private void refresh() {
        State previous = state;
        status(State.LOADING, "Procurando dispositivos de áudio…");
        launch(
                audio::devices,
                devices -> {
                    micPicker.reload(devices);
                    outputPicker.reload(devices);
                    String hint = sourcesHint();
                    status(previous, hint);
                },
                msg -> status(previous, msg));
    }

    private String sourcesHint() {
        if (ui.mic.getSelectedItem() == null || ui.output.getSelectedItem() == null)
            return "Fonte indisponível. Reconecte o dispositivo e atualize as fontes ou escolha outro para a conversa.";
        return whisper.installed() ? "Fontes de áudio prontas." : "Baixe o modelo de voz para iniciar.";
    }

    private void sourceChanged() {
        if (state == State.READY || state == State.PAUSED || state == State.REVIEW)
            status(state, sourcesHint());
    }

    private void activateVoice(WhisperModel.Profile selected) {
        if (state != State.READY && state != State.REVIEW) return;
        State previous = state;
        ui.whisperSettings.activating();
        status(State.LOADING, "");
        voiceInstall = launch(
                () -> {
                    whisper.installAndActivate(selected, ignored -> {});
                    return null;
                },
                v -> {
                    prefs.put("whisperModel", selected.preference());
                    if (selected.language() == WhisperModel.Language.ENGLISH)
                        prefs.put("whisperEnglishModel", selected.preference());
                    String message = selected.model().id + " pronto para transcrever em " + selected.language() + ".";
                    ui.whisperSettings.activated(message);
                    status(previous, "");
                },
                msg -> {
                    ui.whisperSettings.activated(msg);
                    status(previous, "");
                });
    }

    private void toggle() {
        if (closing) return;
        if (state == State.LIVE) {
            pause("Ambas as fontes estão pausadas.");
            return;
        }
        if (!(state == State.READY || state == State.REVIEW || state == State.PAUSED)
                || !ui.toggle.isEnabled()) return;
        var mic = (AudioInput.Device) ui.mic.getSelectedItem();
        var output = (AudioInput.Device) ui.output.getSelectedItem();
        boolean fresh = state != State.PAUSED;
        State previous = state;
        stopPlayback();
        openingError = null;
        status(State.STARTING, "Abrindo o modelo e as duas fontes…");
        launch(
                () -> {
                    whisper.prepare();
                    if (fresh) {
                        var target = LocalSession.create(library);
                        archive = target;
                        session =
                                new Session(
                                        audio,
                                        target,
                                        whisper,
                                        row ->
                                                SwingUtilities.invokeLater(
                                                        () -> {
                                                            if (archive == target)
                                                                ui.dialogue.put(row);
                                                        }),
                                        msg ->
                                                SwingUtilities.invokeLater(
                                                        () -> captureError(target, msg)));
                        SwingUtilities.invokeLater(
                                () -> {
                                    ui.newClock();
                                    ui.dialogue.clearRows();
                                    ui.report("");
                                    ui.sessionIdentity(null);
                                    ui.show(Window.Page.CONVERSATION);
                                });
                    }
                    session.resume(mic, output);
                    return null;
                },
                v -> {
                    ui.show(Window.Page.CONVERSATION);
                    status(State.LIVE, "Capturando microfone e saída do fone.");
                    if (openingError != null) pause(openingError);
                },
                msg ->
                        status(
                                session == null ? previous : State.PAUSED,
                                openingError == null ? msg : openingError));
    }

    private void captureError(LocalSession target, String msg) {
        if (target != archive) return;
        if (state == State.STARTING) openingError = msg;
        else if (state == State.LIVE) pause(msg);
        else error(msg);
    }

    private void pause(String hint) {
        status(State.PAUSING, "Fechando as fontes…");
        launch(
                () -> {
                    session.pause();
                    return null;
                },
                v -> status(State.PAUSED, hint),
                msg -> status(State.PAUSED, msg));
    }

    private void finish() {
        status(State.FINISHING, "Salvando os últimos trechos…");
        launch(
                () -> {
                    session.close();
                    return null;
                },
                v -> {
                    session = null;
                    micPicker.applyDefault();
                    outputPicker.applyDefault();
                    ui.sessionIdentity(new SessionLibrary.Entry(archive.directory().getFileName().toString(), ""));
                    status(State.REVIEW, "Sessão salva.");
                    training.update(archive.directory());
                    if (!ui.isVisible()) companion.full();
                    analyze();
                },
                msg -> status(State.PAUSED, msg));
    }

    private void analyze() {
        String model = feedbackModel;
        var target = archive;
        ui.show(Window.Page.COACH);
        status(State.ANALYZING, "Lendo suas falas confirmadas…");
        ui.analyze.setText("Gerando revisão…");
        launch(
                () -> generateFeedback(target, model),
                report -> {
                    awaitingSetup = null;
                    ui.report(report);
                    status(State.REVIEW, ReviewOutcome.fromReport(report) == ReviewOutcome.AVAILABLE
                            ? ReviewOutcome.AVAILABLE.message : "");
                },
                msg -> {
                    awaitingSetup = target;
                    status(State.REVIEW, msg);
                    ui.feedbackError(msg);
                    if (!model.equals(feedbackModel)) analyze();
                });
    }

    private String generateFeedback(LocalSession target, String model) throws Exception {
        var lines = target.rows().stream()
                .filter(row -> row.status() == Transcript.Status.CONFIRMED)
                .map(row -> new Line(row.clip(), row.text())).toList();
        if (lines.stream().anyMatch(line -> line.clip().source() == Source.MIC))
            WindowsOllama.connect(ollama, false, ignored -> {});
        String report = ollama.analyze(lines, model);
        target.write("feedback.txt", report);
        return report;
    }

    private static ReviewOutcome savedReviewOutcome(Path directory) {
        try {
            return ReviewOutcome.fromReport(Files.readString(directory.resolve("feedback.txt")));
        } catch (java.io.IOException ignored) {
            // An unavailable report must not hide the recording.
            return ReviewOutcome.MISSING;
        }
    }

    private void history() {
        if (listingHistory) return;
        listingHistory = true;
        launch(
                () -> {
                    if (!Files.isDirectory(library)) return List.<SessionLibrary.Entry>of();
                    try (var entries = Files.list(library)) {
                        var result = new java.util.ArrayList<SessionLibrary.Entry>();
                        for (var path : entries.filter(Files::isDirectory).sorted(Comparator.reverseOrder()).toList()) {
                            result.add(new SessionLibrary.Entry(path.getFileName().toString(), new LocalSession(path).title(), savedReviewOutcome(path)));
                        }
                        return result;
                    }
                },
                names -> {
                    ui.library.load(names);
                    listingHistory = false;
                },
                msg -> { listingHistory = false; error(msg); });
    }

    private void copyReviewPrompt(SessionLibrary.Entry entry) {
        message("Preparando o prompt de revisão…");
        launch(() -> {
            var target = new LocalSession(library.resolve(entry.id()));
            return ReviewMarkdown.render(target.title(), SessionLibrary.display(entry.id()), target.rows());
        }, document -> {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(document), null);
                message("Prompt e conversa copiados. Use Ctrl+V para colar.");
            } catch (IllegalStateException e) {
                error("Área de transferência ocupada. Tente copiar o prompt novamente.");
            }
        }, this::error);
    }

    private void editCurrentTitle() {
        var target = archive;
        if (target == null || state != State.REVIEW) return;
        ui.renameSession.setEnabled(false);
        launch(target::title, title -> {
            ui.renameSession.setEnabled(true);
            if (target == archive && state == State.REVIEW)
                ui.library.editTitle(ui, new SessionLibrary.Entry(target.directory().getFileName().toString(), title));
        }, msg -> {
            ui.renameSession.setEnabled(true);
            error(msg);
        });
    }

    private void deleteArchive(SessionLibrary.Entry entry) {
        if (state != State.READY && state != State.REVIEW) return;
        var target = library.resolve(entry.id()).normalize();
        if (!library.equals(target.getParent())) return;
        State previous = state;
        stopPlayback();
        status(State.LOADING, "Enviando a sessão para a Lixeira…");
        launch(() -> {
            if (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS))
                throw new java.io.IOException("A pasta da sessão está indisponível.");
            var desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.MOVE_TO_TRASH) || !desktop.moveToTrash(target.toFile()))
                throw new java.io.IOException("O Windows não conseguiu mover a sessão para a Lixeira. Tente novamente.");
            return null;
        }, ignored -> {
            ui.library.removed(entry.id());
            boolean current = archive != null && archive.directory().equals(target);
            if (current) {
                archive = null;
                awaitingSetup = null;
                ui.sessionIdentity(null);
                ui.dialogue.clearRows();
                ui.report("");
                ui.newClock();
            }
            training.refresh();
            status(current ? State.READY : previous, "Sessão enviada para a Lixeira.");
        }, msg -> status(previous, msg));
    }

    private void renameArchive(String id, String title) {
        launch(() -> {
            new LocalSession(library.resolve(id)).rename(title);
            return null;
        }, ignored -> {
            ui.library.renamed(id, title);
            if (archive != null && archive.directory().equals(library.resolve(id)) && state == State.REVIEW)
                ui.sessionIdentity(new SessionLibrary.Entry(id, title));
        }, this::error);
    }

    private void openArchive(String name, Window.Page destination) {
        if (state != State.READY && state != State.REVIEW) return;
        stopPlayback();
        var target = new LocalSession(library.resolve(name));
        State previous = state;
        status(State.LOADING, "Abrindo conversa…");
        launch(
                () -> {
                    var report = target.directory().resolve("feedback.txt");
                    return new Review(
                            target.rows(), Files.exists(report) ? Files.readString(report) : "",
                            new SessionLibrary.Entry(name, target.title()));
                },
                review -> {
                    if (destination == Window.Page.COACH && ReviewOutcome.fromReport(review.report) != ReviewOutcome.AVAILABLE) {
                        status(previous, "A revisão desta sessão não está mais disponível.");
                        history();
                        return;
                    }
                    archive = target;
                    ui.dialogue.clearRows();
                    review.rows.forEach(ui.dialogue::put);
                    ui.sessionIdentity(review.entry);
                    ui.report(review.report);
                    ui.show(destination);
                    status(State.REVIEW, "");
                },
                msg -> status(previous, msg));
    }

    private record Review(List<Transcript> rows, String report, SessionLibrary.Entry entry) {}

    private void finishTraining(String message) {
        stopPlayback();
        training.finish(ui.training.lesson(), message);
    }

    private void play() {
        var row = ui.dialogue.selected();
        if (row == null) return;
        playFile(archive.wav(row.clip()));
    }

    private void playFile(Path file) {
        stopPlayback();
        try (var stream = javax.sound.sampled.AudioSystem.getAudioInputStream(file.toFile())) {
            playing = javax.sound.sampled.AudioSystem.getClip();
            playing.open(stream);
            playing.start();
        } catch (Exception e) {
            stopPlayback();
            error(e.getMessage());
        }
    }

    private void stopPlayback() {
        if (playing != null) {
            playing.close();
            playing = null;
        }
    }

    private void hearConversation() {
        var target = archive;
        status(State.LOADING, "Preparando a gravação estéreo…");
        launch(
                () -> StereoRecording.export(target, target.rows()),
                file -> {
                    try {
                        Desktop.getDesktop().open(file.toFile());
                        status(State.REVIEW, "Gravação aberta no player do Windows.");
                    } catch (Exception e) {
                        status(State.REVIEW, e.getMessage());
                    }
                },
                msg -> status(State.REVIEW, msg));
    }

    private void status(State next, String msg) {
        state = next;
        String heading =
                switch (next) {
                    case LIVE -> "Ouvindo a conversa";
                    case PAUSED -> "Gravação pausada";
                    case REVIEW -> "Sessão salva";
                    case READY -> readyHeading();
                    default -> next.label;
                };
        ui.viewState(
                next.label,
                heading,
                next == State.LIVE,
                next == State.FINISHING || next == State.ANALYZING,
                archive != null && session == null && next == State.REVIEW,
                next == State.LIVE
                        || next == State.PAUSED
                        || next == State.PAUSING
                        || next == State.FINISHING);
        message(msg);
        controls();
    }

    private String readyHeading() {
        if (!whisper.installed()) return "Prepare a transcrição";
        if (ui.mic.getSelectedItem() == null || ui.output.getSelectedItem() == null)
            return "Conecte as fontes de áudio";
        return "Pronto para conversar";
    }

    private void message(String msg) {
        ui.message.setText(msg);
        ui.message.setToolTipText(msg);
    }

    private void error(String msg) {
        message(msg);
    }

    private void controls() {
        boolean idle = state == State.READY || state == State.REVIEW;
        boolean canStart =
                whisper.installed()
                        && ui.mic.getSelectedItem() != null
                        && ui.output.getSelectedItem() != null;
        ui.toggle.setText(
                state == State.PAUSED
                        ? "Retomar gravação"
                        : state == State.LIVE ? "Pausar gravação" : "Iniciar gravação");
        ui.toggle.setEnabled(
                (idle && canStart) || (state == State.PAUSED && canStart) || state == State.LIVE);
        ui.finish.setEnabled(state == State.PAUSED || state == State.LIVE);
        ui.history.setEnabled(idle);
        ui.library.allowActions(idle);
        ui.trainingNav.setEnabled(idle || state == State.ANALYZING);
        ui.history.setToolTipText(idle ? "Abrir sessões gravadas" : "Disponível após concluir a sessão atual.");
        ui.conversationNav.setEnabled(idle || state == State.LIVE || state == State.PAUSED);
        ui.refresh.setEnabled(idle || state == State.PAUSED);
        ui.defaultMic.setEnabled(idle || state == State.PAUSED);
        ui.defaultOutput.setEnabled(idle || state == State.PAUSED);
        ui.mic.setEnabled(idle || state == State.PAUSED);
        ui.output.setEnabled(idle || state == State.PAUSED);
        var profile = whisper.profile();
        ui.showWhisper(profile.language().toString(), profile.model().id, whisper.installed());
        ui.download.setVisible(!whisper.installed());
        ui.download.setEnabled(idle);
        ui.whisperSettings.allowActivation(idle);
        boolean needsAudio = whisper.installed() && !canStart && (idle || state == State.PAUSED);
        ui.configureAudio.setVisible(needsAudio);
        ui.toggle.setVisible(whisper.installed() && !needsAudio);
        ui.analyze.setEnabled(state == State.REVIEW);
        ui.folder.setEnabled(state == State.REVIEW);
        ui.play.setEnabled(state == State.REVIEW && ui.dialogue.selected() != null);
        ui.conversationAudio.setEnabled(state == State.REVIEW);
        var selected = ui.dialogue.selected();
        ui.copy.setEnabled(selected != null && selected.status() == Transcript.Status.CONFIRMED);
        quoteControl();
        companion.sync(state.label);
    }

    private void quoteControl() {
        ui.hearQuote.setEnabled(
                state == State.REVIEW && ui.dialogue.rowFor(ui.feedback.citation()) >= 0);
    }

    private <T> Future<?> launch(Callable<T> task, Consumer<T> success, Consumer<String> failure) {
        return work.submit(
                () -> {
                    try {
                        T result = task.call();
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (!closing) success.accept(result);
                                });
                    } catch (Exception | LinkageError e) {
                        String msg =
                                e.getCause() == null
                                        ? e.toString()
                                        : e.getMessage() + " " + e.getCause().getMessage();
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (!closing) failure.accept(msg);
                                });
                    }
                });
    }

    private void close() {
        if (closing) return;
        if (session != null
                && JOptionPane.showConfirmDialog(
                                ui,
                                "Encerrar a gravação, salvar a sessão e sair?",
                                "Bird Teacher",
                                JOptionPane.YES_NO_OPTION)
                        != JOptionPane.YES_OPTION) return;
        training.close();
        if (voiceInstall != null) voiceInstall.cancel(true);
        ui.whisperSettings.close();
        ui.ollamaSettings.close();
        closing = true;
        companion.close();
        stopPlayback();
        ui.setEnabled(false);
        message("Concluindo operações antes de sair…");
        work.submit(
                () -> {
                    try {
                        if (session != null) {
                            session.close();
                            generateFeedback(archive, feedbackModel);
                        }
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(
                                () -> JOptionPane.showMessageDialog(ui, e.getMessage()));
                    } finally {
                        whisper.close();
                        try { keys.close(); } catch (Exception ignored) { }
                        work.shutdown();
                        SwingUtilities.invokeLater(
                                () -> {
                                    companion.close();
                                    ui.dispose();
                                });
                    }
                });
    }
}
