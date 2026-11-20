package br.birdteacher;

import br.birdteacher.domain.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FeedbackTest {
    final Clip mine = new Clip(250, Source.MIC);
    final Clip theirs = new Clip(250, Source.OUTPUT);
    final List<Line> dialogue = List.of(new Line(mine, "Yesterday I go to class."),
            new Line(theirs, "How was it?"));

    @Test void distinguishesStoredEmptyAnalysisMessagesFromReviewContent() {
        assertEquals(ReviewOutcome.NO_SPEECH, ReviewOutcome.fromReport(
                "Ainda não há fala sua confirmada para analisar nesta sessão.\r\n"));
        assertEquals(ReviewOutcome.NO_SUGGESTIONS, ReviewOutcome.fromReport(
                "\uFEFFO modelo não encontrou uma sugestão com citação verificável nesta sessão."));
        assertEquals(ReviewOutcome.MISSING, ReviewOutcome.fromReport(" \n"));
        assertEquals(ReviewOutcome.AVAILABLE, ReviewOutcome.fromReport(
                "00:01  ·  Você\n“I go.”\n\nExperimente dizer\nI went.\n\nUse o passado."));
    }

    @Test void acceptsAnExactStudentQuote() {
        assertTrue(new Suggestion(mine.id(), "I go to class", "I went to class", "Use o passado.").matchesStudent(dialogue));
    }
    @Test void rejectsTheOtherSideOfTheConversationAsARewrite() {
        assertFalse(new Suggestion(mine.id(), "Yesterday I go to class.", "How was it?", "Use o passado.").matchesStudent(dialogue));
    }
    @Test void rejectsRemoteOrInventedEvidence() {
        assertFalse(new Suggestion(theirs.id(), "How was it?", "How did it go?", "Alternativa").matchesStudent(dialogue));
        assertFalse(new Suggestion(mine.id(), "How was it?", "How did it go?", "Alternativa").matchesStudent(dialogue));
    }
}
