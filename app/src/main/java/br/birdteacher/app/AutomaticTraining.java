package br.birdteacher.app;

import br.birdteacher.domain.Training.Lesson;
import br.birdteacher.infra.TrainingLibrary;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.function.Consumer;
import javax.swing.*;

/** Inference runs independently of recording, and saved lessons stay usable while it runs. */
final class AutomaticTraining implements AutoCloseable {
    private final TrainingLibrary library;
    private final Window ui;
    private final Supplier<String> selectedModel;
    private final ExecutorService inference = Executors.newSingleThreadExecutor();
    private final ExecutorService catalog = Executors.newSingleThreadExecutor();
    private final Timer retry;
    private volatile boolean closed;

    AutomaticTraining(TrainingLibrary library, Window ui, Supplier<String> selectedModel) {
        this.library = library;
        this.ui = ui;
        this.selectedModel = selectedModel;
        retry = new Timer(90000, e -> update(null));
        retry.setRepeats(false);
    }

    void open() {
        refresh();
        update(null);
    }

    void update(Path session) {
        if (closed) return;
        String model = selectedModel.get();
        inference.submit(() -> {
            try {
                Consumer<TrainingLibrary.Progress> progress = p -> {
                    later(() -> ui.training.preparation("Preparando lições em segundo plano · "
                            + p.completed() + " de " + p.total() + " falas desta aula"));
                    if (p.persisted()) refresh();
                };
                if (session == null) library.sync(model, progress);
                else library.syncSession(session, model, progress);
                later(() -> {
                    if (session == null) retry.stop();
                    ui.training.preparation("");
                });
            } catch (Exception e) {
                later(() -> {
                    ui.training.preparation("Preparação pausada. " + e.getMessage() + " O aplicativo tentará novamente.");
                    if (!retry.isRunning()) retry.start();
                });
            }
        });
    }

    void finish(Lesson lesson, String message) {
        ui.training.saving();
        catalog.submit(() -> {
            try {
                library.finish(lesson);
                displayCatalog(message);
            } catch (Exception e) {
                later(() -> ui.training.saveFailed("Falha ao salvar o treino. " + e.getMessage()));
            }
        });
    }

    void refresh() {
        if (closed) return;
        catalog.submit(() -> {
            try {
                displayCatalog(null);
            } catch (Exception e) {
                later(() -> ui.training.preparation("Falha ao ler as lições salvas. " + e.getMessage()));
            }
        });
    }

    private void displayCatalog(String message) throws Exception {
        var lessons = library.available();
        later(() -> {
            ui.showLessonCount(lessons.size());
            ui.training.available(lessons);
            if (message != null) ui.training.catalog(message);
        });
    }

    private void later(Runnable action) {
        SwingUtilities.invokeLater(() -> { if (!closed) action.run(); });
    }

    @Override public void close() {
        closed = true;
        retry.stop();
        inference.shutdownNow();
        // A completion already requested by the student must reach disk.
        catalog.shutdown();
    }
}
