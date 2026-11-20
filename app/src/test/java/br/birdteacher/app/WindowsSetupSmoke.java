package br.birdteacher.app;

import br.birdteacher.domain.*;
import br.birdteacher.infra.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Windows end-to-end probe with two prerecorded English WAVs and an isolated session directory. */
public final class WindowsSetupSmoke {
    private static <T> T edt(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(4);
        while (!edt(condition)) {
            if (System.nanoTime() > deadline) throw new AssertionError("Tempo esgotado na verificação.");
            Thread.sleep(150);
        }
    }

    private static JButton button(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton b && b.getText().equals(text)) return b;
            if (child instanceof Container c) {
                var found = button(c, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private record SampleAudio(LocalSession samples) implements AudioInput {
        public List<Device> devices() {
            return List.of(new Device("mic", "Microfone de demonstração", Source.MIC),
                           new Device("out", "Fone de demonstração", Source.OUTPUT));
        }
        public void capture(Device device, AtomicBoolean stopped, Listener listener) throws Exception {
            listener.ready();
            listener.samples(System.nanoTime(), samples.readAudio(new Clip(0, device.source())));
            while (!stopped.get()) Thread.sleep(10);
        }
    }

    public static void main(String[] args) throws Exception {
        var workspace = Path.of(args[0]);
        var sessions = workspace.resolve("sessions");
        var prefs = Preferences.userRoot().node("/bird-teacher-tests/" + UUID.randomUUID());
        try {
            var w = edt(() -> {
                Appearance.install();
                var ui = new Window();
                new DesktopController(ui, new SampleAudio(new LocalSession(workspace.resolve("samples"))),
                    new Whisper(Path.of(args[1])), new Ollama(), sessions, prefs).open();
                return ui;
            });
            await(() -> w.toggle.isEnabled() && w.mic.getItemCount() == 2);
            edt(() -> { w.settingsNav.doClick(); return null; });
            if (args.length > 2) {
                var choice = WhisperModel.valueOf(args[2]);
                edt(() -> {
                    w.whisperSettings.models.setSelectedValue(choice, true);
                    if (w.whisperSettings.install.getText().equals("Baixar e instalar")) w.whisperSettings.install.doClick();
                    return null;
                });
                await(() -> w.whisperSettings.install.isEnabled());
                if (edt(() -> w.whisperSettings.use.isEnabled())) {
                    edt(() -> { w.whisperSettings.use.doClick(); return null; });
                    await(() -> ("en:" + choice.id).equals(prefs.get("whisperModel", "")));
                }
                var dir = Path.of(args[1]).getParent();
                if (WhisperModel.startup(dir, prefs.get("whisperModel", choice.id)) != choice)
                    throw new AssertionError("Modelo escolhido não foi restaurado");
                System.out.println("Whisper pronto pela interface: " + choice.id);
            }
            await(() -> button(w.ollamaSettings, "Baixar e usar modelo").isEnabled());
            edt(() -> { button(w.ollamaSettings, "Baixar e usar modelo").doClick(); return null; });
            await(() -> OllamaSettings.DEFAULT_MODEL.equals(prefs.get("ollama", "")));
            edt(() -> {
                var body = w.getContentPane();
                var image = new BufferedImage(body.getWidth(), body.getHeight(), BufferedImage.TYPE_INT_RGB);
                var g = image.createGraphics();
                body.printAll(g);
                g.dispose();
                ImageIO.write(image, "png", workspace.resolve("configuracoes.png").toFile());
                w.history.doClick();
                if (!w.library.isShowing() || !w.history.isEnabled()
                    || !w.history.isSelected()
                    || w.settingsNav.isSelected())
                    throw new AssertionError("Seleção de Histórico incorreta");
                return null;
            });
            System.out.println("Ollama configurado pela interface e Histórico selecionado");
            edt(() -> { w.conversationNav.doClick(); w.toggle.doClick(); return null; });
            await(() -> w.state.getText().equals("Gravando"));
            edt(() -> { w.toggle.doClick(); return null; });
            await(() -> w.state.getText().equals("Pausado"));
            Path dir;
            try (var list = Files.list(sessions)) { dir = list.findFirst().orElseThrow(); }
            if (Files.exists(dir.resolve("feedback.txt"))) throw new AssertionError("Feedback durante pausa");
            edt(() -> { w.finish.doClick(); return null; });
            await(() -> Files.isRegularFile(dir.resolve("feedback.txt")) && w.analyze.isEnabled() && w.analyze.isShowing());
            var sources = new LocalSession(dir).rows().stream()
                .filter(t -> t.status() == Transcript.Status.CONFIRMED).map(t -> t.clip().source()).distinct().count();
            String report = Files.readString(dir.resolve("feedback.txt"));
            boolean available = ReviewOutcome.fromReport(report) == ReviewOutcome.AVAILABLE;
            if (sources != 2 || !edt(() -> w.feedback.isShowing() == available
                    && w.feedback.getText().equals(available ? report : "")))
                throw new AssertionError("Relatório automático ou transcrições ausentes");
            System.out.println("Pausa sem análise; ao encerrar, duas fontes transcritas e feedback local salvo e exibido");
            edt(() -> { w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING)); return null; });
            await(() -> !w.isDisplayable());
        } finally { prefs.removeNode(); }
    }
}
