package br.birdteacher.infra;

import br.birdteacher.domain.*;
import br.birdteacher.domain.Clip;
import br.birdteacher.domain.Line;
import com.google.gson.Gson;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static java.nio.file.StandardCopyOption.*;

public final class LocalSession implements Recording {
    private static final Gson JSON = new Gson();
    private static final AudioFormat PCM = new AudioFormat(16000, 16, 1, true, false);
    private final Path dir;

    public LocalSession(Path dir) { this.dir = dir; }
    public Path directory() { return dir; }
    public static LocalSession create(Path root) throws IOException {
        Files.createDirectories(root);
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
        return new LocalSession(Files.createTempDirectory(root, date + "_"));
    }

    public String title() throws IOException {
        try { return Files.readString(dir.resolve("titulo.txt")).strip(); }
        catch (NoSuchFileException e) { return ""; }
    }

    public void rename(String title) throws IOException {
        String value = title.strip();
        if (value.isEmpty()) Files.deleteIfExists(dir.resolve("titulo.txt"));
        else write("titulo.txt", value);
    }

    public Path wav(Clip clip) { return dir.resolve(clip.id() + ".wav"); }

    @Override public void saveAudio(Clip clip, short[] pcm) throws IOException {
        byte[] bytes = new byte[pcm.length * 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm);
        Path target = wav(clip), staging = dir.resolve(clip.id() + ".part");
        try (var input = new AudioInputStream(new ByteArrayInputStream(bytes), PCM, pcm.length)) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, staging.toFile());
        }
        Files.move(staging, target, ATOMIC_MOVE);
    }

    @Override public short[] readAudio(Clip clip) throws IOException {
        try (var input = AudioSystem.getAudioInputStream(wav(clip).toFile())) {
            var buffer = ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            short[] pcm = new short[buffer.remaining()];
            buffer.get(pcm);
            return pcm;
        } catch (UnsupportedAudioFileException e) { throw new IOException("WAV inválido: " + clip.id(), e); }
    }

    @Override public void saveText(Transcript row) throws IOException {
        write(row.clip().id() + ".json", JSON.toJson(row));
    }

    public void write(String file, String text) throws IOException {
        Path tmp = Files.createTempFile(dir, "salvando-", ".part");
        try {
            Files.writeString(tmp, text);
            Files.move(tmp, dir.resolve(file), ATOMIC_MOVE, REPLACE_EXISTING);
        } finally { Files.deleteIfExists(tmp); }
    }

    public List<Line> unprocessedSpeech(Set<String> processed) throws IOException {
        List<Line> result = new ArrayList<>();
        try (var paths = Files.newDirectoryStream(dir, "*-MIC.json")) {
            for (var path : paths) {
                var name = path.getFileName().toString();
                var id = name.substring(0, name.length() - 5);
                if (processed.contains(id) || !id.matches("[0-9]{12}-MIC")
                        || !Files.isRegularFile(dir.resolve(id + ".wav"))) continue;
                var row = JSON.fromJson(Files.readString(path), Transcript.class);
                if (row != null && row.status() == Transcript.Status.CONFIRMED && row.clip().id().equals(id))
                    result.add(new Line(row.clip(), row.text()));
            }
        }
        result.sort(Comparator.comparing(Line::clip));
        return result;
    }

    public List<Transcript> rows() throws IOException {
        List<Transcript> rows = new ArrayList<>();
        try (var paths = Files.list(dir)) {
            for (var file : paths.sorted().toList()) {
                String name = file.getFileName().toString();
                if (!name.matches("[0-9]{12}-(MIC|OUTPUT)\\.wav")) continue;
                String id = name.substring(0, name.length() - 4);
                var clip = new Clip(Long.parseLong(id.substring(0, 12)), Source.valueOf(id.substring(13)));
                Path metadata = dir.resolve(id + ".json");
                var row = new Transcript(clip, Transcript.Status.FAILED, "Transcrição interrompida. Áudio disponível.");
                if (Files.exists(metadata)) row = JSON.fromJson(Files.readString(metadata), Transcript.class);
                rows.add(row);
            }
        }
        return rows;
    }
}
