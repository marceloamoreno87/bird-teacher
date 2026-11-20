package br.birdteacher.app;

import br.birdteacher.domain.*;
import br.birdteacher.infra.*;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Manual Windows integration probe. Opens endpoints, then exits before capturing personal audio. */
public final class WindowsSmoke {
    public static void main(String[] args) throws Exception {
        var audio = new Wasapi();
        var devices = audio.devices();
        for (Source source : Source.values()) {
            var device = devices.stream().filter(d -> d.source() == source).findFirst().orElseThrow();
            var opened = new AtomicBoolean();
            audio.capture(device, new AtomicBoolean(true), new AudioInput.Listener() {
                public void ready() { opened.set(true); }
                public void samples(long t, short[] samples) { throw new AssertionError("Unexpected capture"); }
                public void idle(long t) {}
            });
            if (!opened.get()) throw new AssertionError("Endpoint indisponível");
            System.out.println(source + " WASAPI OK");
        }
        var key = new Hotkey(() -> {});
        key.register();
        key.close();
        System.out.println("JNativeHook OK");
        FlatLightLaf.setup();
        SwingUtilities.invokeAndWait(() -> {
            Window window = new Window();
            window.pack();
            window.dispose();
        });
        System.out.println("Swing OK");
        var sample = new LocalSession(Path.of(args[1]));
        var clip = new Clip(0, Source.MIC);
        try (var whisper = new Whisper(Path.of(args[0]))) {
            String text = whisper.recognize(sample.readAudio(clip));
            System.out.println(text);
            if (!text.toLowerCase(Locale.ROOT).contains("country")) throw new AssertionError("Whisper não reconheceu a amostra JFK");
        }
        System.exit(0);
    }
}
