package br.birdteacher.usecase;

import br.birdteacher.domain.*;
import java.util.Arrays;

/** One segmenter per source and recording interval. Audio is kept even during silence. */
final class Segmenter {
    interface Output {
        void audio(Clip clip, short[] pcm, boolean speech) throws Exception;
        void preview(Clip clip, short[] pcm);
    }
    private final Source source;
    private final Output output;
    private final short[] buffer = new short[20 * 16000];
    private long start;
    private int used, quiet;
    private boolean spoken;
    private int nextPreview = 32000;

    Segmenter(Source source, Output output) { this.source = source; this.output = output; }

    void append(long millis, short[] samples) throws Exception {
        idle(millis);
        // Long silence belongs to the recording, but must not timestamp the next reply.
        if (!spoken && used >= 3200 && containsVoice(samples)) flush();
        int cursor = 0;
        while (cursor < samples.length) {
            if (used == 0) start = millis + cursor / 16;
            int n = Math.min(buffer.length - used, samples.length - cursor);
            System.arraycopy(samples, cursor, buffer, used, n);
            for (int i = cursor; i < cursor + n; i++) {
                if (Math.abs((int) samples[i]) > 80) { spoken = true; quiet = 0; }
                else quiet++;
            }
            cursor += n;
            used += n;
            if (used == buffer.length || (spoken && quiet >= 7200)
                    || (used >= 8 * 16000 && quiet >= 3200)
                    || (!spoken && used >= 6 * 16000)) flush();
            else if (spoken && used >= nextPreview) {
                nextPreview = used + 4 * 16000;
                output.preview(new Clip(start, source), Arrays.copyOf(buffer, used));
            }
        }
    }

    private static boolean containsVoice(short[] samples) {
        for (short value : samples) if (Math.abs((int) value) > 80) return true;
        return false;
    }

    void idle(long millis) throws Exception {
        // A loopback endpoint can stop delivering packets as soon as the player goes silent.
        if (used > 0 && millis - (start + used / 16) > 180) flush();
    }

    void flush() throws Exception {
        if (used == 0) return;
        output.audio(new Clip(start, source), Arrays.copyOf(buffer, used), spoken);
        used = quiet = 0;
        spoken = false;
        nextPreview = 32000;
    }
}
