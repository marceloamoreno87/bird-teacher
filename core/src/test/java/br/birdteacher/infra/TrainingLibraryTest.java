package br.birdteacher.infra;

import br.birdteacher.domain.*;
import br.birdteacher.domain.Training.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TrainingLibraryTest {
    @TempDir Path root;
    private int requests;
    private final TrainingLibrary.Classifier classifier = (lines, model) -> {
        requests++;
        var findings = new ArrayList<Finding>();
        for (var line : lines) {
            assertEquals(Source.MIC, line.clip().source());
            if (line.text().startsWith("Did you went")) {
                var change = new Suggestion(line.clip().id(), line.text(), line.text().replace("went", "go"), "Use a forma base.");
                findings.add(new Finding(Focus.DID_BASE_VERB, change));
                findings.add(new Finding(Focus.DID_BASE_VERB, change));
            }
            var invented = new Suggestion(line.clip().id(), "unrelated", "other", "sem base");
            findings.add(new Finding(Focus.DID_BASE_VERB, invented));
        }
        return findings;
    };

    private Optional<Lesson> sync(TrainingLibrary library, String model) throws Exception {
        library.sync(model, ignored -> {});
        return library.available().stream().findFirst();
    }

    private void add(String lesson, long time, Source source, Transcript.Status status, String text) throws Exception {
        var dir = Files.createDirectories(root.resolve(lesson));
        var archive = new LocalSession(dir);
        var clip = new Clip(time, source);
        archive.saveAudio(clip, new short[100]);
        archive.saveText(new Transcript(clip, status, text));
    }

    private void mistake(String lesson, long time) throws Exception {
        add(lesson, time, Source.MIC, Transcript.Status.CONFIRMED, "Did you went to school?");
    }

    @Test void countsRealDistinctClipsAcrossSessionsAndRemembersConsumedEvidence() throws Exception {
        mistake("a", 0);
        mistake("a", 100);
        mistake("b", 0);
        add("a", 200, Source.MIC, Transcript.Status.CONFIRMED, "I went to school.");
        add("a", 300, Source.MIC, Transcript.Status.PREVIEW, "Did you went to school?");
        add("a", 0, Source.OUTPUT, Transcript.Status.CONFIRMED, "Did you went to school?");
        var library = new TrainingLibrary(root, classifier);
        assertTrue(sync(library, "local").isEmpty());
        add("b", 100, Source.MIC, Transcript.Status.CONFIRMED, "Did you went home?");
        var lesson = sync(library, "local").orElseThrow();
        assertEquals(Focus.DID_BASE_VERB, lesson.focus());
        assertEquals(4, lesson.examples().size());
        assertNotEquals(lesson.audio().key(), lesson.writing().key());
        assertNotEquals(lesson.audio().change().quote(), lesson.writing().change().quote());
        assertEquals(3, requests);
        library.finish(lesson);
        var reopened = new TrainingLibrary(root, classifier);
        assertTrue(sync(reopened, "local").isEmpty());
        assertEquals(3, requests);
        for (int i = 0; i < 3; i++) mistake("c", i);
        assertTrue(sync(reopened, "local").isEmpty());
        mistake("c", 3);
        assertEquals(4, sync(reopened, "local").orElseThrow().examples().size());
    }

    @Test void doesNotTurnModelFailureIntoASuccessfulEmptyAnalysis() throws Exception {
        for (int i = 0; i < 4; i++) mistake("a", i);
        var offline = new TrainingLibrary(root, (lines, model) -> { throw new IOException("offline"); });
        assertThrows(IOException.class, () -> sync(offline, "local"));
        assertTrue(sync(new TrainingLibrary(root, classifier), "local").isPresent());
    }

    @Test void longLessonResumesAfterLastCompletedBatch() throws Exception {
        for (int i = 0; i < 26; i++) mistake("a", i);
        var firstPass = new ArrayList<Integer>();
        var interrupted = new TrainingLibrary(root, (lines, model) -> {
            firstPass.add(lines.size());
            if (firstPass.size() == 2) throw new IOException("Ollama offline");
            return classifier.classify(lines, model);
        });
        assertThrows(IOException.class, () -> sync(interrupted, "local"));
        assertEquals(List.of(12, 12), firstPass);
        var resumed = new ArrayList<Integer>();
        var reopened = new TrainingLibrary(root, (lines, model) -> {
            resumed.add(lines.size());
            return classifier.classify(lines, model);
        });
        var updates = new ArrayList<TrainingLibrary.Progress>();
        reopened.sync("local", updates::add);
        assertEquals(26, reopened.available().getFirst().examples().size());
        assertEquals(List.of(12, 2), resumed);
        assertEquals(List.of(12, 24, 26), updates.stream().map(TrainingLibrary.Progress::completed).distinct().toList());

        assertEquals(26, updates.getFirst().total());
    }

    @Test void appendingSpeechOrSwitchingModelsOnlyClassifiesNewClips() throws Exception {
        for (int i = 0; i < 4; i++) mistake("a", i);
        var seen = new ArrayList<Integer>();
        var library = new TrainingLibrary(root, (lines, model) -> {
            seen.add(lines.size());
            return classifier.classify(lines, model);
        });
        sync(library, "first");
        mistake("a", 4);
        sync(library, "second");
        assertEquals(List.of(4, 1), seen);
        var offline = new TrainingLibrary(root, (lines, model) -> { throw new IOException("offline"); });
        assertEquals(5, offline.available().getFirst().examples().size());
        offline.sync("another", ignored -> {});
        offline.finish(offline.available().getFirst());
        assertTrue(library.available().isEmpty());
    }

    @Test void restoresOlderLessonsBeforeAttemptingNewSpeechOffline() throws Exception {
        var changes = new ArrayList<Finding>();
        for (int i = 0; i < 4; i++) {
            mistake("a", i);
            changes.add(new Finding(Focus.DID_BASE_VERB, new Suggestion(new Clip(i, Source.MIC).id(),
                    "Did you went to school?", "Did you go to school?", "Use a forma base.")));
        }
        var json = new com.google.gson.Gson();
        Files.writeString(root.resolve("a/treino-v1.json"), json.toJson(Map.of("findings", changes)));
        mistake("z", 0);
        var offline = new TrainingLibrary(root, (lines, model) -> { throw new IOException("offline"); });
        assertThrows(IOException.class, () -> offline.sync("other model", ignored -> {}));
        assertEquals(4, offline.available().getFirst().examples().size());
        offline.finish(offline.available().getFirst());
        assertTrue(offline.available().isEmpty());
    }

    @Test void publishesEarnedLessonBeforeLastBatchFinishes() throws Exception {
        for (int i = 0; i < 13; i++) mistake("a", i);
        var library = new TrainingLibrary(root, (lines, model) -> {
            if (lines.size() == 1) throw new IOException("offline");
            return classifier.classify(lines, model);
        });
        var snapshots = new ArrayList<Integer>();
        assertThrows(IOException.class, () -> library.sync("local", update -> {
            if (update.persisted()) {
                try { snapshots.add(library.available().size()); }
                catch (IOException e) { throw new AssertionError(e); }
            }
        }));
        assertEquals(List.of(1), snapshots);
        assertEquals(12, library.available().getFirst().examples().size());
    }

    @Test void returnsOneLessonPerFocusMostFrequentFirst() {
        var examples = new ArrayList<Example>();
        for (int i = 0; i < 7; i++) {
            var suggestion = new Suggestion("" + i, "some words", "a correction", "feedback");
            examples.add(new Example("lesson", new Finding(Focus.BE_AGREEMENT, suggestion)));
            if (i < 4) examples.add(new Example("lesson", new Finding(Focus.DID_BASE_VERB, suggestion)));
        }
        assertEquals(List.of(Focus.BE_AGREEMENT, Focus.DID_BASE_VERB),
                Training.available(examples, Map.of()).stream().map(Lesson::focus).toList());
    }
}
