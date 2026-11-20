package br.birdteacher.usecase;

import br.birdteacher.domain.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReviewMarkdownTest {
    @Test void exportsInstructionsAndChronologicalSpeechWithSpeakersAndGaps() {
        var later = new Transcript(new Clip(61002, Source.MIC), Transcript.Status.CONFIRMED, "I have a question.\nCan we start?");
        var first = new Transcript(new Clip(900, Source.OUTPUT), Transcript.Status.CONFIRMED, "Olá!");
        var gap = new Transcript(new Clip(3661000, Source.MIC), Transcript.Status.FAILED, "exception");
        var draft = new Transcript(later.clip(), Transcript.Status.PREVIEW, "draft");
        var doc = ReviewMarkdown.render("Aula", "07/01/2026 23:04:05", List.of(later, gap, draft, first));
        assertTrue(doc.contains("Aula") && doc.contains("07/01/2026 23:04:05"));
        assertTrue(doc.contains("## Pedido de revisão") && doc.contains("português brasileiro"));
        assertTrue(doc.contains("00:00:00 · Áudio do fone") && doc.contains("00:01:01 · Você"));
        assertTrue(doc.indexOf("> Olá!") < doc.indexOf("> I have a question.\n> Can we start?"));
        assertTrue(doc.contains("01:01:01") && doc.contains("Trecho sem transcrição"));
        assertFalse(doc.contains("draft") || doc.contains("exception"));
    }

    @Test void anEmptySessionHasAnExplicitEmptyTranscript() {
        assertTrue(ReviewMarkdown.render("", "07/01/2026 23:04:05", List.of()).contains("Nenhuma fala transcrita"));
    }
}
