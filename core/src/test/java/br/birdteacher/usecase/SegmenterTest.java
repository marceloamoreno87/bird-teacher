package br.birdteacher.usecase;

import br.birdteacher.domain.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SegmenterTest {
    private static class Output implements Segmenter.Output {
        final List<Clip> voice = new ArrayList<>(), previews = new ArrayList<>();
        int samples;
        public void audio(Clip c, short[] pcm, boolean spoken) { samples += pcm.length; if (spoken) voice.add(c); }
        public void preview(Clip c, short[] pcm) { previews.add(c); }
    }
    @Test void leadingSilenceDoesNotPlaceMyReplyBeforeTheQuestion() throws Exception {
        var out = new Output();
        var mic = new Segmenter(Source.MIC, out);
        mic.append(0, new short[32000]);
        mic.append(2000, new short[]{500, 600, 500});
        mic.flush();
        assertEquals(List.of(new Clip(2000, Source.MIC)), out.voice);
        assertEquals(32003, out.samples);
    }
    @Test void previewAndFinalTextReferToTheSameSourceAndTime() throws Exception {
        var out = new Output();
        var segment = new Segmenter(Source.OUTPUT, out);
        var voice = new short[40000];
        Arrays.fill(voice, (short) 250);
        segment.append(3000, voice);
        segment.idle(6000);
        assertEquals(List.of(new Clip(3000, Source.OUTPUT)), out.previews);
        assertEquals(out.previews, out.voice);
    }
    @Test void continuousSentencePassesSixSecondsAndKeepsLivePreviews() throws Exception {
        var out = new Output();
        var segment = new Segmenter(Source.MIC, out);
        var voice = new short[160];
        Arrays.fill(voice, (short) 450);
        for (int ms = 0; ms < 9500; ms += 10) segment.append(ms, voice);
        segment.flush();
        assertEquals(List.of(new Clip(0, Source.MIC)), out.voice);
        assertTrue(out.previews.size() >= 2);
        assertTrue(out.previews.stream().allMatch(it -> it.millis() == 0));
        assertEquals(152000, out.samples);
    }
}
