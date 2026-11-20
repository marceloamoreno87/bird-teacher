package br.birdteacher.app;

import br.birdteacher.domain.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Render the real Swing components using synthetic content, without opening the endpoints. */
public final class InterfacePreview {
  public static void main(String[] args) throws Exception {
    Path out = Files.createDirectories(Path.of(args[0]));
    SwingUtilities.invokeAndWait(
        () -> {
          Appearance.palette = "petroleo";
          Appearance.light = false;
          Appearance.install();
          var w = new Window();
          try {
            w.mic.addItem(new AudioInput.Device("mic", "Microfone do headset", Source.MIC));
            w.output.addItem(new AudioInput.Device("out", "Fones de ouvido", Source.OUTPUT));
            w.download.setVisible(false);
            w.showWhisper("Inglês", "medium.en", true);
            w.viewState("Pronto", "Pronto para conversar", false, false, false, false);
            w.copy.setEnabled(false);
            w.finish.setEnabled(false);
            w.toggle.setEnabled(true);
            w.setVisible(true);
            save(w, out.resolve("conversa.png"));
            w.viewState("Gravando", "Ouvindo a conversa", true, false, false, true);
            w.toggle.setText("Pausar gravação");
            w.finish.setEnabled(true);
            w.dialogue.put(
                new Transcript(
                    new Clip(4000, Source.OUTPUT),
                    Transcript.Status.CONFIRMED,
                    "What did you do over the weekend?"));
            w.dialogue.put(
                new Transcript(
                    new Clip(9000, Source.MIC),
                    Transcript.Status.CONFIRMED,
                    "I went hiking with my sister."));
            w.dialogue.put(
                new Transcript(
                    new Clip(14000, Source.OUTPUT),
                    Transcript.Status.PREVIEW,
                    "That sounds great"));
            w.mic.setEnabled(false);
            w.output.setEnabled(false);
            w.history.setEnabled(false);
            w.setSize(w.getMinimumSize());
            save(w, out.resolve("conversa-ativa.png"));
            w.viewState("Processando", "Finalizando a sessão", false, true, false, true);
            w.toggle.setText("Iniciar gravação");
            w.toggle.setEnabled(false);
            w.finish.setEnabled(false);
            save(w, out.resolve("processando.png"));
            w.toggle.setText("Pausar gravação");
            w.toggle.setEnabled(true);
            w.finish.setEnabled(true);
            w.viewState("Gravando", "Ouvindo a conversa", true, false, false, true);
            var mini = new Companion(w);
            try {
              mini.sync("Gravando");
              mini.line(w.dialogue.last());
              w.compact.doClick();
              var dialog =
                  java.util.Arrays.stream(java.awt.Window.getWindows())
                      .filter(
                          it ->
                              it instanceof JDialog d
                                  && d.isVisible()
                                  && d.getTitle().contains("compacto"))
                      .findFirst()
                      .orElseThrow();
              saveContent(((JDialog) dialog).getContentPane(), out.resolve("compacto.png"));
              mini.full();
            } finally {
              mini.close();
            }
            w.viewState("Revisão", "Sessão salva", false, false, true, false);
            w.history.setEnabled(true);
            w.library.load(
                java.util.List.of(new SessionLibrary.Entry("2026-11-19 09-30-10_exemplo", "Aula de conversação", ReviewOutcome.AVAILABLE),
                        new SessionLibrary.Entry("2026-11-18 18-22-40_exemplo", "")));
            w.show(Window.Page.HISTORY);
            save(w, out.resolve("historico.png"));
            var entry = new SessionLibrary.Entry("2026-11-19 09-30-10_exemplo", "Aula de conversação", ReviewOutcome.AVAILABLE);
            w.dialogue.put(new Transcript(new Clip(14000, Source.OUTPUT),
                    Transcript.Status.CONFIRMED, "That sounds great."));
            w.sessionIdentity(entry);
            w.show(Window.Page.TRANSCRIPT);
            save(w, out.resolve("sessao-transcricao.png"));
            w.show(Window.Page.COACH);
            w.report(
                "00:09  ·  Você\n"
                    + "“I have went hiking.”\n\n"
                    + "Experimente dizer\n"
                    + "I went hiking.\n\n"
                    + "Use o passado simples para contar uma atividade que"
                    + " terminou.");
            w.viewState("Revisão", "Sessão salva", false, false, true, false);
            Appearance.light = true;
            Appearance.install();
            com.formdev.flatlaf.FlatLaf.updateUI();
            // Rebuild the light window in the selected palette, as on startup.
            var light = new Window();
            try {
              light.setSize(900, 620);
              light.sessionIdentity(entry);
              light.show(Window.Page.COACH);
              light.report(w.feedback.getText());
              light.viewState("Revisão", "Sessão salva", false, false, true, false);
              light.showWhisper("Inglês", "medium.en", true);
              light.setVisible(true);
              save(light, out.resolve("meu-ingles.png"));
            } finally {
              light.dispose();
            }
          } finally {
            w.dispose();
          }
        });
  }

  private static void save(Window frame, Path path) {
    frame.validate();
    saveContent(frame.getContentPane(), path);
  }

  private static void saveContent(java.awt.Container body, Path path) {
    var img = new BufferedImage(body.getWidth(), body.getHeight(), BufferedImage.TYPE_INT_RGB);
    var g = img.createGraphics();
    body.printAll(g);
    g.dispose();
    try {
      ImageIO.write(img, "PNG", path.toFile());
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }
}
