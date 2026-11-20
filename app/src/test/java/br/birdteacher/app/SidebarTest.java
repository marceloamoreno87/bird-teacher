package br.birdteacher.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import br.birdteacher.domain.*;
import br.birdteacher.domain.Training.*;
import java.awt.GraphicsEnvironment;
import javax.swing.*;
import org.junit.jupiter.api.Test;

class SidebarTest {
    private static int visibleOccurrences(java.awt.Component item, String text) {
        if (!item.isShowing()) return 0;
        if (item instanceof JLabel label && text.equals(label.getText())) return 1;
        if (item instanceof AbstractButton button && (text.equals(button.getText())
                || text.equals(button.getAccessibleContext().getAccessibleName()))) return 1;
        if (!(item instanceof java.awt.Container container)) return 0;
        return java.util.Arrays.stream(container.getComponents())
                .mapToInt(child -> visibleOccurrences(child, text)).sum();
    }

    @Test void trainingHasItsOwnSidebarDestination() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Appearance.install();
            var window = new Window();
            try {
                window.setVisible(true);
                assertEquals(1, visibleOccurrences(window, "Treinar"));
                window.trainingNav.doClick();
                assertTrue(window.trainingNav.isSelected());
                assertFalse(window.lessonCount.isVisible());
                assertEquals(0, visibleOccurrences(window, "Buscar treino nas aulas"));
                assertFalse(window.history.isSelected());
                var examples = new java.util.ArrayList<Example>();
                var focus = Focus.DID_BASE_VERB;
                for (int i = 0; i < 4; i++) {
                    var change = new Suggestion("" + i, "Did he went?", "Did he go?", "Use a forma base.");
                    examples.add(new Example("aula", new Finding(focus, change)));
                }
                var lesson = new Lesson(focus, examples);
                window.training.available(java.util.List.of(lesson));
                window.showLessonCount(1);
                assertTrue(window.lessonCount.isShowing());
                assertEquals("1", window.lessonCount.getText());
                assertEquals(1, visibleOccurrences(window.training, focus.title));
                window.training.openLesson(lesson);
                assertTrue(window.training.listen.isShowing());
                window.training.reveal.doClick();
                window.training.write.doClick();
                assertFalse(window.training.check.isEnabled());
                window.training.answer.setText("Did he go?");
                window.training.check.doClick();
                assertTrue(window.training.finish.isShowing());
                assertFalse(window.training.answer.isEditable());
                var remaining = examples.stream()
                        .map(e -> new Example("outra aula", e.finding())).toList();
                window.training.available(java.util.List.of(new Lesson(focus, remaining)));
                assertNull(window.training.lesson());
                assertEquals(1, visibleOccurrences(window.training, focus.title));
            } finally { window.dispose(); }
        });
    }

    @Test void showsEachNavigationLabelAndBusyStatusOnlyOnce() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Appearance.install();
            var window = new Window();
            try {
                window.setVisible(true);
                for (var tab : java.util.List.of(window.history, window.settingsNav, window.conversationNav)) {
                    tab.doClick();
                    assertEquals(1, visibleOccurrences(window, tab.getText()), tab.getText());
                }
                window.conversationNav.doClick();
                window.viewState("Preparando", "Preparando", false, false, false, false);
                assertEquals(1, visibleOccurrences(window, "Preparando"));
            } finally { window.dispose(); }
        });
    }

    @Test void renameIsAvailableInHistoryAndWhileReviewingTheSavedSession() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Appearance.install();
            var window = new Window();
            try {
                window.setVisible(true);
                window.viewState("Pronto", "Pronto", false, false, false, false);
                assertEquals(0, visibleOccurrences(window, "Renomear sessão"));
                window.viewState("Revisão", "Sessão salva", false, false, true, false);
                window.library.load(java.util.List.of(new SessionLibrary.Entry("a", "Aula")));
                for (var page : java.util.List.of(Window.Page.TRANSCRIPT, Window.Page.COACH, Window.Page.HISTORY)) {
                    window.show(page);
                    assertEquals(1, visibleOccurrences(window, "Renomear sessão"), page.name());
                }
            } finally { window.dispose(); }
        });
    }

    @Test void archivedTranscriptAndReviewStayInsideSessionsAndHideRecordingControls() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Appearance.install();
            var window = new Window();
            try {
                window.setVisible(true);
                assertTrue(window.toggle.isShowing());
                assertFalse(window.coachNav.isShowing());
                window.sessionIdentity(new SessionLibrary.Entry("2026-04-03 14-21-09_123", "Aula de inglês"));
                window.viewState("Salva", "", false, false, true, false);
                window.show(Window.Page.TRANSCRIPT);
                assertFalse(window.toggle.isShowing());
                assertFalse(window.mic.isShowing());
                assertTrue(window.conversationAudio.isShowing());
                window.coachNav.doClick();
                assertEquals(1, visibleOccurrences(window, "Aula de inglês"));
                assertEquals(1, visibleOccurrences(window, "03/04/2026 14:21:09"));
                assertTrue(window.history.isSelected());
                assertTrue(window.analyze.isShowing());
                assertFalse(window.conversationAudio.isShowing());
                window.history.doClick();
                assertTrue(window.library.isShowing());
                assertFalse(window.coachNav.isShowing());
            } finally { window.dispose(); }
        });
    }

    @Test void sessionsShowsReviewOnlyForASelectionWithSavedFeedback() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Appearance.install();
            var window = new Window();
            try {
                window.setVisible(true);
                window.history.doClick();
                assertEquals("Sessões", window.history.getText());
                var entry = new SessionLibrary.Entry("a", "Aula", ReviewOutcome.AVAILABLE);
                window.library.load(java.util.List.of(entry));
                window.library.renamed("a", "Aula de inglês");
                assertEquals(1, visibleOccurrences(window, "Excluir sessão"));
                assertEquals(1, visibleOccurrences(window, "Ver revisão de inglês"));
                window.library.load(java.util.List.of(new SessionLibrary.Entry("b", "Entrevista")));
                assertEquals(0, visibleOccurrences(window, "Ver revisão de inglês"));
                window.library.load(java.util.List.of());
                assertEquals(0, visibleOccurrences(window, "Ver revisão de inglês"));
                assertEquals(0, visibleOccurrences(window, "Renomear sessão"));
                assertEquals(0, visibleOccurrences(window, "Excluir sessão"));
                assertTrue(window.library.goRecord.isShowing());
                window.library.goRecord.doClick();
                assertTrue(window.mic.isShowing());
            } finally { window.dispose(); }
        });
    }

    @Test void selectingReviewKeepsOnlySessionsSelectedEvenAfterSettingsHadFocus() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Appearance.install();
            var window = new Window();
            try {
                window.setVisible(true);
                window.settingsNav.doClick();
                window.history.doClick();
                window.show(Window.Page.TRANSCRIPT);
                window.coachNav.doClick();
                assertTrue(window.history.isSelected());
                assertFalse(window.settingsNav.isSelected());
                assertFalse(window.conversationNav.isSelected());
                assertTrue(window.coachNav.isSelected());
                assertTrue(window.feedback.isShowing() || window.analyze.isShowing());
            } finally { window.dispose(); }
        });
    }

    @Test void clickingHistoryHighlightsHistoryImmediatelyAfterSettings() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Appearance.install();
            var window = new Window();
            try {
                window.settingsNav.doClick();
                window.history.doClick();
                assertTrue(window.history.isSelected());
                assertFalse(window.settingsNav.isSelected());
                assertTrue(window.library.isVisible());
            } finally { window.dispose(); }
        });
    }
}
