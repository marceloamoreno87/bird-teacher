package br.birdteacher.infra;

import br.birdteacher.domain.*;
import br.birdteacher.domain.Training.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;

public final class TrainingLibrary {
    @FunctionalInterface
    public interface Classifier {
        List<Finding> classify(List<Line> lines, String model) throws Exception;
    }

    public record Progress(int completed, int total, boolean persisted) {}
    private record Index(Set<String> processed, List<Finding> findings) {
        Index() { this(new LinkedHashSet<>(), new ArrayList<>()); }

        void add(List<Line> batch, List<Finding> candidates) {
            for (var finding : candidates) {
                if (finding != null && finding.supportedBy(batch)) findings.add(finding);
            }
            for (var line : batch) processed.add(line.clip().id());
        }
    }
    private record Legacy(String fingerprint, Integer completed, List<Finding> findings) {}
    private record Receipt(Focus focus, Set<String> keys) {}
    private static final String INDEX = "licoes-v2.json";
    private static final Gson JSON = new Gson();
    private final Path root;
    private final Classifier classifier;

    public TrainingLibrary(Path root, Classifier classifier) {
        this.root = root;
        this.classifier = classifier;
    }

    private List<Path> sessions() throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        try (var paths = Files.list(root)) {
            return paths.filter(Files::isDirectory).sorted(Comparator.reverseOrder()).toList();
        }
    }

    /** Reads saved lessons without opening transcripts, audio or a connection to Ollama. */
    public List<Lesson> available() throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        List<Example> examples = new ArrayList<>();
        for (var dir : sessions()) {
            var file = dir.resolve(INDEX);
            if (!Files.isRegularFile(file)) continue;
            Index index;
            try { index = readIndex(file); }
            catch (NoSuchFileException removed) { continue; }
            String session = dir.getFileName().toString();
            for (var finding : index.findings) examples.add(new Example(session, finding));
        }
        return Training.available(examples, receipts());
    }

    public void sync(String model, Consumer<Progress> saved) throws Exception {
        var dirs = sessions();
        // Restore all previously earned lessons even when the model is currently offline.
        for (var dir : dirs) {
            if (!Files.exists(dir.resolve(INDEX)) && (Files.exists(dir.resolve("treino-v1.json"))
                    || Files.exists(dir.resolve("treino-parcial-v1.json"))))
                updateSession(dir, model, saved, true);
        }
        for (var dir : dirs) syncSession(dir, model, saved);
    }

    public void syncSession(Path dir, String model, Consumer<Progress> saved) throws Exception {
        updateSession(dir, model, saved, false);
    }

    private void updateSession(Path dir, String model, Consumer<Progress> saved, boolean migrateOnly) throws Exception {
        if (!Files.isDirectory(dir)) return;
        var archive = new LocalSession(dir);
        // Two open windows may discover the same new recording at startup.
        try (var channel = FileChannel.open(dir.resolve("licoes.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            var file = dir.resolve(INDEX);
            Index index = Files.isRegularFile(file) ? readIndex(file) : new Index();
            var unseen = archive.unprocessedSpeech(index.processed);
            if (unseen.isEmpty()) return;
            if (!Files.exists(file) && importLegacy(archive, unseen, index, model)) {
                archive.write(INDEX, JSON.toJson(index));
                saved.accept(new Progress(index.processed.size(), unseen.size(), true));
                unseen = unseen.stream().filter(l -> !index.processed.contains(l.clip().id())).toList();
            }
            if (migrateOnly) return;
            int total = index.processed.size() + unseen.size();
            for (var batch : Ollama.trainingBatches(unseen)) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                saved.accept(new Progress(index.processed.size(), total, false));
                index.add(batch, classifier.classify(batch, model));
                archive.write(INDEX, JSON.toJson(index));
                saved.accept(new Progress(index.processed.size(), total, true));
            }
        } catch (NoSuchFileException e) {
            if (Files.exists(dir)) throw e;
            // A recording can be moved to the Recycle Bin while the model is working.
        }
    }

    private static Index readIndex(Path file) throws IOException {
        try {
            var index = JSON.fromJson(Files.readString(file), Index.class);
            if (index == null || index.processed == null || index.findings == null)
                throw new JsonParseException("índice incompleto");
            return index;
        } catch (JsonParseException e) {
            throw new IOException("Não foi possível ler as lições salvas desta aula.", e);
        }
    }

    private static boolean importLegacy(LocalSession archive, List<Line> lines, Index index, String model) throws IOException, NoSuchAlgorithmException {
        var dir = archive.directory();
        var old = dir.resolve("treino-v1.json");
        if (!Files.isRegularFile(old)) old = dir.resolve("treino-parcial-v1.json");
        if (!Files.isRegularFile(old)) return false;
        var legacy = JSON.fromJson(Files.readString(old), Legacy.class);
        if (legacy == null || legacy.findings == null) return false;
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(JSON.toJson(List.of(model, lines)).getBytes(StandardCharsets.UTF_8)));
        int limit = legacy.completed == null ? lines.size() : Math.clamp(legacy.completed, 0, lines.size());
        if (fingerprint.equals(legacy.fingerprint)) {
            index.add(lines.subList(0, limit), legacy.findings);
            return limit > 0;
        }
        // v1 recorded an ordered prefix, without clip IDs for negative results.
        // Exclude speech added or replaced after that snapshot.
        var written = Files.getLastModifiedTime(old);
        var original = new ArrayList<Line>();
        for (var line : lines.subList(0, limit)) {
            var metadata = dir.resolve(line.clip().id() + ".json");
            if (Files.getLastModifiedTime(metadata).compareTo(written) <= 0) original.add(line);
        }
        index.add(original, legacy.findings);
        return !original.isEmpty();
    }

    private Map<Focus, Set<String>> receipts() throws IOException {
        Map<Focus, Set<String>> used = new EnumMap<>(Focus.class);
        try (var paths = Files.newDirectoryStream(root, "treino-concluido-*.json")) {
            for (var path : paths) {
                var receipt = JSON.fromJson(Files.readString(path), Receipt.class);
                used.computeIfAbsent(receipt.focus, ignored -> new HashSet<>()).addAll(receipt.keys);
            }
        }
        return used;
    }

    public void finish(Lesson lesson) throws IOException {
        var keys = new TreeSet<String>();
        lesson.examples().forEach(example -> keys.add(example.key()));
        // Separate atomic receipts preserve concurrent completions for different sets of examples.
        var receipt = JSON.toJson(new Receipt(lesson.focus(), keys));
        var id = UUID.nameUUIDFromBytes(receipt.getBytes(StandardCharsets.UTF_8));
        new LocalSession(root).write("treino-concluido-" + id + ".json", receipt);
    }
}
