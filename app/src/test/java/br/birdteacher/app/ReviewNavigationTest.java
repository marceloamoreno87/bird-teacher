package br.birdteacher.app;

import static org.junit.jupiter.api.Assertions.*;

import br.birdteacher.domain.*;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

class ReviewNavigationTest {
    @Test void statusOnlyReportHasNoReviewToCopyOrPlay() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            var window = new Window();
            try {
                window.show(Window.Page.COACH);
                window.setVisible(true);
                for (String text : java.util.List.of("Ainda não há fala sua confirmada para analisar nesta sessão.",
                        "O modelo não encontrou uma sugestão com citação verificável nesta sessão.")) {
                    window.report(text);
                    assertFalse(window.copyReport.isShowing());
                    assertFalse(window.hearQuote.isShowing());
                    assertFalse(window.feedback.isShowing());
                }
            } finally { window.dispose(); }
        });
    }

    @Test
    void sessionDateIsReadableAndSearchableWithSeconds() {
        assertEquals("07/01/2026 23:04:05", SessionLibrary.display("2026-01-07 23-04-05_123456"));
        assertEquals("07/01/2026 23:04:05", SessionLibrary.display("2026-01-07 23-04-05"));
        assertEquals("Aula de inglês", SessionLibrary.display("Aula de inglês"));
    }

    @Test
    void theSelectedSuggestionPlaysOnlyItsUniqueMicrophoneClip() throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    var pane = new FeedbackPane();
                    var table = new DialogueTable();
                    String text =
                            "00:04  ·  Você\n"
                                + "“I go.”\n\n"
                                + "Experimente dizer\n"
                                + "I went.\n\n"
                                + "01:22  ·  Você\n"
                                + "“She go.”\n\n"
                                + "She goes.";
                    pane.showReport(text);
                    table.put(
                            new Transcript(
                                    new Clip(4000, Source.OUTPUT),
                                    Transcript.Status.CONFIRMED,
                                    "I go."));
                    assertEquals(-1, table.rowFor(pane.citation()));
                    table.put(
                            new Transcript(
                                    new Clip(4230, Source.MIC),
                                    Transcript.Status.CONFIRMED,
                                    "I go."));
                    assertEquals(1, table.rowFor(pane.citation()));
                    pane.setCaretPosition(text.length());
                    table.put(
                            new Transcript(
                                    new Clip(82020, Source.MIC),
                                    Transcript.Status.CONFIRMED,
                                    "She go."));
                    assertEquals(2, table.rowFor(pane.citation()));
                    table.put(
                            new Transcript(
                                    new Clip(82700, Source.MIC),
                                    Transcript.Status.CONFIRMED,
                                    "She go."));
                    assertEquals(-1, table.rowFor(pane.citation()));
                });
    }
}
