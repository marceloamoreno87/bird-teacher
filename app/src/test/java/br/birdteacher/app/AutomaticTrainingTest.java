package br.birdteacher.app;

import br.birdteacher.domain.*;
import br.birdteacher.domain.Training.*;
import br.birdteacher.infra.*;
import java.awt.GraphicsEnvironment;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AutomaticTrainingTest {
    @TempDir Path root;

    @Test void showsEarnedLessonsWhileIndexingAndRestoresTheRemainingCountOffline() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        var archive = LocalSession.create(root);
        for (int i = 0; i < 16; i++) {
            var clip = new Clip(i, Source.MIC);
            archive.saveAudio(clip, new short[10]);
            archive.saveText(new Transcript(clip, Transcript.Status.CONFIRMED,
                    i < 12 ? "Did he went?" : "She have a bike."));
        }

        var release = new CountDownLatch(1);
        var requests = new AtomicInteger();
        var library = new TrainingLibrary(root, (lines, model) -> {
            requests.incrementAndGet();
            boolean first = lines.size() == 12;
            if (!first && !release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Second batch timed out");
            return lines.stream().map(line -> new Finding(first ? Focus.DID_BASE_VERB : Focus.THIRD_PERSON_S,
                    new Suggestion(line.clip().id(), line.text(), first ? "Did he go?" : "She has a bike.",
                            "Confira a forma do verbo."))).toList();
        });
        var w = edt(() -> { Appearance.install(); return new Window(); });
        var training = edt(() -> {
            var service = new AutomaticTraining(library, w, () -> "local");
            w.training.finish.addActionListener(event -> service.finish(w.training.lesson(), "Treino concluído."));
            w.setVisible(true);
            service.open();
            return service;
        });
        try {
            await(() -> w.lessonCount.isShowing() && w.lessonCount.getText().equals("1"));
            edt(() -> {
                w.trainingNav.doClick();
                w.training.openLesson(library.available().getFirst());
                w.training.reveal.doClick();
                w.training.write.doClick();
                w.training.answer.setText("Did he go?");
                return null;
            });
            release.countDown();
            await(() -> w.lessonCount.getText().equals("2"));
            edt(() -> {
                assertEquals("Did he go?", w.training.answer.getText());
                assertTrue(w.training.check.isShowing());
                w.training.check.doClick();
                w.training.finish.doClick();
                return null;
            });
            await(() -> w.lessonCount.getText().equals("1"));
            assertEquals(2, requests.get());
        } finally {
            release.countDown();
            edt(() -> { training.close(); w.dispose(); return null; });
        }

        var offline = new TrainingLibrary(root, (lines, model) -> { throw new AssertionError("Unexpected inference"); });
        assertEquals(List.of(Focus.THIRD_PERSON_S), offline.available().stream().map(Lesson::focus).toList());
        offline.sync("different", ignored -> {});
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void await(Callable<Boolean> done) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!edt(done)) {
            if (System.nanoTime() > deadline) fail("Expected state did not appear");
            Thread.sleep(20);
        }
    }
}
