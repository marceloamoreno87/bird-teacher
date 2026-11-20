package br.birdteacher.usecase;

import br.birdteacher.domain.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/** Commands are serialized by the desktop controller. Each source owns its WASAPI apartment. */
public final class Session implements AutoCloseable {
    private final AudioInput audio;
    private final Recording archive;
    private final Speech speech;
    private final Consumer<Transcript> changed;
    private final Consumer<String> error;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicInteger pending = new AtomicInteger();
    private final List<Thread> threads = new ArrayList<>();
    private AtomicBoolean stop = new AtomicBoolean(true);
    private long accumulated, since;

    public Session(AudioInput audio, Recording archive, Speech speech, Consumer<Transcript> changed, Consumer<String> error) {
        this.audio = audio;
        this.archive = archive;
        this.speech = speech;
        this.changed = changed;
        this.error = error;
    }

    public void resume(AudioInput.Device mic, AudioInput.Device output) throws Exception {
        if (!threads.isEmpty()) throw new IllegalStateException("Sessão já iniciada.");
        stop = new AtomicBoolean(false);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var failure = new AtomicReference<Throwable>();
        since = System.nanoTime();
        for (var device : List.of(mic, output)) {
            var thread = Thread.ofPlatform().name("wasapi-" + device.source()).start(() -> {
                var segmenter = new Segmenter(device.source(), sink());
                try {
                    audio.capture(device, stop, new AudioInput.Listener() {
                        public void ready() throws Exception { ready.countDown(); start.await(); }
                        public void samples(long nanos, short[] pcm) throws Exception { segmenter.append(time(nanos), pcm); }
                        public void idle(long nanos) throws Exception { segmenter.idle(time(nanos)); }
                    });
                } catch (Throwable e) {
                    if (failure.compareAndSet(null, e)) {
                        stop.set(true);
                        error.accept(device.name() + " — " + e.getMessage());
                    }
                } finally {
                    ready.countDown();
                    try { segmenter.flush(); }
                    catch (Exception e) { error.accept("Falha ao salvar áudio: " + e.getMessage()); }
                }
            });
            threads.add(thread);
        }
        try {
            if (!ready.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tempo esgotado ao abrir as fontes.");
            if (failure.get() != null) throw new IllegalStateException("Falha ao abrir a fonte.", failure.get());
            since = System.nanoTime();
        } catch (Exception e) {
            stop.set(true);
            throw e;
        } finally {
            start.countDown();
            if (stop.get()) pause();
        }
    }

    private long time(long nanos) { return accumulated + Math.max(0, (nanos - since) / 1_000_000L); }

    public void pause() throws InterruptedException {
        if (threads.isEmpty()) return;
        stop.set(true);
        long end = System.nanoTime();
        for (var thread : threads) thread.join();
        threads.clear();
        accumulated = time(end) + 1;
    }

    private Segmenter.Output sink() {
        return new Segmenter.Output() {
            public void preview(Clip clip, short[] pcm) {
                if (!pending.compareAndSet(0, 1)) return;
                worker.execute(() -> {
                    try { changed.accept(new Transcript(clip, Transcript.Status.PREVIEW, speech.preview(pcm))); }
                    catch (Exception | LinkageError ignored) { /* The complete clip will be tried and persisted. */ }
                    finally { pending.decrementAndGet(); }
                });
            }

            public void audio(Clip clip, short[] pcm, boolean spoken) throws Exception {
                archive.saveAudio(clip, pcm);
                if (!spoken) {
                    archive.saveText(new Transcript(clip, Transcript.Status.SILENT, ""));
                    return;
                }
                changed.accept(new Transcript(clip, Transcript.Status.QUEUED, ""));
                pending.incrementAndGet();
                worker.execute(() -> recognize(clip));
            }
        };
    }

    private void recognize(Clip clip) {
        try {
            Transcript row;
            try {
                String text = speech.recognize(archive.readAudio(clip)).strip();
                row = new Transcript(clip, text.isEmpty() ? Transcript.Status.SILENT : Transcript.Status.CONFIRMED, text);
            } catch (Exception | LinkageError e) {
                row = new Transcript(clip, Transcript.Status.FAILED, "Falha na transcrição: " + e.getMessage());
            }
            archive.saveText(row);
            changed.accept(row);
        } catch (Exception e) { error.accept("Falha ao salvar transcrição: " + e.getMessage()); }
        finally { pending.decrementAndGet(); }
    }

    @Override public void close() throws InterruptedException {
        pause();
        worker.shutdown();
        while (!worker.awaitTermination(1, TimeUnit.SECONDS)) { /* Wait for confirmed text before feedback. */ }
    }
}
