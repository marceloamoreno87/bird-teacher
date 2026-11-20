package br.birdteacher.app;

import br.birdteacher.domain.*;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;

class DialogueTableTest {
    @Test void ordersSourcesAndReplacesThePreviewInPlace() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var table = new DialogueTable();
            var question = new Clip(1000, Source.OUTPUT);
            var reply = new Clip(3000, Source.MIC);
            table.put(new Transcript(reply, Transcript.Status.PREVIEW, "I"));
            table.put(new Transcript(question, Transcript.Status.CONFIRMED, "Where?"));
            table.put(new Transcript(reply, Transcript.Status.QUEUED, ""));
            table.put(new Transcript(reply, Transcript.Status.CONFIRMED, "I was there."));
            assertEquals(2, table.getRowCount());
            assertEquals("Where?", table.getValueAt(0, 2));
            assertEquals("Você", table.getValueAt(1, 1));
            assertEquals("I was there.", table.getValueAt(1, 2));
        });
    }
}
