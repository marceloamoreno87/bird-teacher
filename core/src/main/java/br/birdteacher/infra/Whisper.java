package br.birdteacher.infra;

import br.birdteacher.domain.Speech;
import io.github.givimad.whisperjni.*;
import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.LongConsumer;

public final class Whisper implements Speech, AutoCloseable {
    private final Path directory;
    private volatile WhisperModel.Profile profile;
    private WhisperJNI engine;
    private WhisperContext context;

    public Whisper(Path model) {
        this(model.toAbsolutePath().getParent(), new WhisperModel.Profile(WhisperModel.Language.ENGLISH, Arrays.stream(WhisperModel.values()).filter(it -> it.filename().equals(model.getFileName().toString()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Modelo Whisper desconhecido: " + model.getFileName()))));
    }

    public Whisper(Path directory, WhisperModel.Profile profile) {
        this.directory = directory;
        this.profile = profile;
    }

    public WhisperModel.Profile profile() { return profile; }
    public WhisperModel active() { return profile.model(); }
    public boolean available(WhisperModel model) { return model.available(directory); }
    public boolean installed() { return available(active()); }

    public void installAndActivate(WhisperModel model, LongConsumer progress) throws Exception {
        installAndActivate(new WhisperModel.Profile(profile.language(), model), progress);
    }

    public void installAndActivate(WhisperModel.Profile selected, LongConsumer progress) throws Exception {
        installAndActivate(selected, selected.model().uri(), progress);
    }

    void installAndActivate(WhisperModel.Profile selected, URI source, LongConsumer progress) throws Exception {
        var choice = selected.model();
        install(choice, source, progress);
        Path target = directory.resolve(choice.filename());
        synchronized (this) {
            if (active() != choice || context == null) {
                var candidate = load(target);
                if (context != null) context.close();
                context = candidate;
            }
            profile = selected;
        }
    }

    public void install(WhisperModel model, LongConsumer progress) throws Exception {
        install(model, model.uri(), progress);
    }

    private void install(WhisperModel choice, URI source, LongConsumer progress) throws Exception {
        Files.createDirectories(directory);
        Path target = directory.resolve(choice.filename());
        if (!available(choice) || !checksum(target).equals(choice.sha256)) {
            Path tmp = Files.createTempFile(directory, "whisper-", ".part");
            try {
                download(source, choice, tmp, progress);
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(tmp); }
        }
    }

    private static String checksum(Path file) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var in = new DigestInputStream(Files.newInputStream(file), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void download(URI uri, WhisperModel choice, Path tmp, LongConsumer progress) throws Exception {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15)).build()) {
            var response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(60)).build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            var digest = MessageDigest.getInstance("SHA-256");
            try (var in = response.body(); var out = new DigestOutputStream(Files.newOutputStream(tmp), digest)) {
                if (response.statusCode() != 200) throw new IOException("Download Whisper HTTP " + response.statusCode());
                byte[] block = new byte[131072];
                long total = 0;
                for (int n; (n = in.read(block)) != -1;) {
                    if (Thread.interrupted()) throw new InterruptedException();
                    out.write(block, 0, n);
                    total += n;
                    progress.accept(total);
                }
                if (total != choice.bytes || !HexFormat.of().formatHex(digest.digest()).equals(choice.sha256))
                    throw new IOException("Modelo incompleto ou inválido. Tente baixar novamente.");
            }
        }
    }

    public synchronized void prepare() throws Exception {
        if (context != null) return;
        if (!installed()) throw new IOException("Baixe o modelo de voz em Configurações.");
        Path file = directory.resolve(active().filename());
        if (!checksum(file).equals(active().sha256))
            throw new IOException("Arquivo de voz inválido. Use Verificar instalação na aba Whisper para repará-lo.");
        context = load(file);
    }

    private WhisperContext load(Path model) throws Exception {
        if (engine == null) {
            try { loadLibrary(); }
            catch (UnsatisfiedLinkError e) {
                throw new IOException("O Windows não carregou o Whisper. Instale o Visual C++ Redistributable x64 e reabra o aplicativo.", e);
            }
            engine = new WhisperJNI();
        }
        var config = new WhisperContextParams();
        config.useGPU = false;
        var loaded = engine.init(model, config);
        if (loaded == null) throw new IOException("Falha ao carregar o modelo em " + model);
        return loaded;
    }

    private static synchronized void loadLibrary() throws IOException {
        String property = "io.github.givimad.whisperjni.libdir";
        if (System.getProperty("os.name").startsWith("Windows") && System.getProperty(property) == null) {
            Path dir = Files.createTempDirectory("bird-whisper-");
            Path dll = dir.resolve("whisper-jni.dll");
            try (var in = WhisperJNI.class.getResourceAsStream("/win-amd64/whisper-jni_full.dll")) {
                if (in == null) throw new IOException("Biblioteca Whisper x64 ausente.");
                Files.copy(in, dll);
            }
            dir.toFile().deleteOnExit();
            dll.toFile().deleteOnExit();
            System.setProperty(property, dir.toString());
        }
        WhisperJNI.loadLibrary();
        WhisperJNI.setLibraryLogger(null);
    }

    @Override public String recognize(short[] pcm) throws Exception { return decode(pcm, false); }
    @Override public String preview(short[] pcm) throws Exception { return decode(pcm, true); }

    private synchronized String decode(short[] pcm, boolean preview) throws Exception {
        prepare();
        float[] samples = new float[Math.max(17600, pcm.length)];
        for (int i = 0; i < pcm.length; i++) samples[i] = pcm[i] / 32768f;
        var params = new WhisperFullParams(preview ? WhisperSamplingStrategy.GREEDY : WhisperSamplingStrategy.BEAN_SEARCH);
        params.language = profile.language().code;
        params.translate = false;
        params.noContext = true;
        // Abbreviate only the provisional pass. Final text uses the encoder's trained context.
        if (preview) params.audioCtx = Math.min(1500, samples.length / 320 + 64);
        else params.beamSearchBeamSize = 5;
        params.nThreads = Math.clamp(Runtime.getRuntime().availableProcessors() - 2, 1, 8);
        params.printProgress = false;
        params.printTimestamps = false;
        params.suppressNonSpeechTokens = true;
        if (engine.full(context, params, samples, samples.length) != 0) throw new IOException("Falha no Whisper.");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < engine.fullNSegments(context); i++) text.append(engine.fullGetSegmentText(context, i));
        return text.toString().strip();
    }

    @Override public synchronized void close() {
        if (context != null) context.close();
        context = null;
    }
}
