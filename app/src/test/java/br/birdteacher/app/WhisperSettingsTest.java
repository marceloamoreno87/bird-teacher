package br.birdteacher.app;

import static org.junit.jupiter.api.Assertions.*;
import br.birdteacher.infra.*;
import br.birdteacher.infra.WhisperModel.Language;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WhisperSettingsTest {
    @TempDir Path models;

    @Test void searchingByNameOrCodeOffersOnlyCompatibleModels() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel = new WhisperSettings();
            try (var engine = new Whisper(models, WhisperModel.englishDefault(models, ""))) {
                panel.bind(engine, p -> fail("Searching must not activate a model"), () -> {});
                assertEquals(Language.ENGLISH, panel.languages.getSelectedValue());
                panel.search.setText("cantones");
                assertEquals(Language.CANTONESE, panel.languages.getSelectedValue());
                assertEquals(1, panel.models.getModel().getSize());
                assertEquals(WhisperModel.TURBO, panel.models.getSelectedValue());
                panel.search.setText("ja");
                assertTrue(java.util.stream.IntStream.range(0, panel.languages.getModel().getSize())
                        .mapToObj(panel.languages.getModel()::getElementAt).anyMatch(l -> l == Language.JAPANESE));
                panel.search.setText("idioma inexistente");
                assertNull(panel.languages.getSelectedValue());
                assertFalse(panel.install.isEnabled());
                assertFalse(panel.use.isEnabled());
                assertEquals(Language.ENGLISH, engine.profile().language());
            }
        });
    }
}
