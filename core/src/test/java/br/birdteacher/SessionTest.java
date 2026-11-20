package br.birdteacher;

import br.birdteacher.domain.*;
import br.birdteacher.infra.*;
import br.birdteacher.usecase.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class SessionTest {
    @TempDir Path temp;
    private static final class TestEndpoints implements AudioInput {
        final Device mic = new Device("mic", "Headset", Source.MIC);
        final Device output = new Device("out", "Loopback", Source.OUTPUT);
        final BlockingQueue<Source> delivered = new LinkedBlockingQueue<>();
        final AtomicInteger active = new AtomicInteger();
        public List<Device> devices() { return List.of(mic, output); }
        public void capture(Device device, AtomicBoolean stop, Listener listener) throws Exception {
            listener.ready();
            active.incrementAndGet();
            try {
                short[] packet = new short[160];
                Arrays.fill(packet, (short) (device.source() == Source.MIC ? 301 : 401));
                listener.samples(System.nanoTime(), packet);
                delivered.put(device.source());
                while (!stop.get()) Thread.sleep(2);
            } finally { active.decrementAndGet(); }
        }
        void awaitBoth() throws Exception {
            assertNotNull(delivered.poll(2, TimeUnit.SECONDS));
            assertNotNull(delivered.poll(2, TimeUnit.SECONDS));
        }
    }

    @Test void pauseStopsBothEndpointsAndResumeKeepsAudioAndFinalText() throws Exception {
        var src = new TestEndpoints();
        var storage = LocalSession.create(temp);
        var errors = new CopyOnWriteArrayList<String>();
        try (var session = new Session(src, storage, pcm -> pcm[0] == 301 ? "I went there" : "Where?",
                row -> {}, errors::add)) {
            session.resume(src.mic, src.output);
            src.awaitBoth();
            session.pause();
            assertEquals(0, src.active.get());
            Thread.sleep(15);
            session.resume(src.mic, src.output);
            src.awaitBoth();
        }
        assertEquals(List.of(), errors);
        var saved = new LocalSession(storage.directory()).rows();
        assertEquals(4, saved.size());
        for (var row : saved) {
            assertEquals(Transcript.Status.CONFIRMED, row.status());
            assertEquals(row.clip().source() == Source.MIC ? "I went there" : "Where?", row.text());
            assertEquals(160, storage.readAudio(row.clip()).length);
        }
    }

    @Test void titleIsOptionalAndPersistsBesideTheOriginalRecording() throws Exception {
        var storage = LocalSession.create(temp);
        var clip = new Clip(200, Source.MIC);
        storage.saveAudio(clip, new short[]{10, 20});
        assertEquals("", storage.title());
        storage.rename("  Reunião / inglês: estágio  ");
        var reopened = new LocalSession(storage.directory());
        assertEquals("Reunião / inglês: estágio", reopened.title());
        assertArrayEquals(new short[]{10, 20}, reopened.readAudio(clip));
        reopened.rename("Entrevista");
        assertEquals("Entrevista", storage.title());
        reopened.rename("   ");
        assertEquals("", storage.title());
    }

    @Test void stereoExportKeepsOverlappingSourcesAndSilenceAtTheRightTime() throws Exception {
        var storage = LocalSession.create(temp);
        var mic = new Clip(2, Source.MIC);
        var out = new Clip(2, Source.OUTPUT);
        storage.saveAudio(mic, new short[]{100, 200});
        storage.saveAudio(out, new short[]{-100, -200});
        var rows = storage.rows(); // Also covers recovery of an interrupted transcription.
        assertTrue(rows.stream().allMatch(r -> r.status() == Transcript.Status.FAILED));
        var file = StereoRecording.export(storage, rows);
        try (var wav = javax.sound.sampled.AudioSystem.getAudioInputStream(file.toFile())) {
            assertEquals(2, wav.getFormat().getChannels());
            assertEquals(34, wav.getFrameLength());
            var data = java.nio.ByteBuffer.wrap(wav.readAllBytes()).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < 64; i++) assertEquals(0, data.getShort());
            for (short value : new short[]{100, -100, 200, -200}) assertEquals(value, data.getShort());
        }
    }
}
