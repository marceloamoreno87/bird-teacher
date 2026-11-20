package br.birdteacher.app;

import br.birdteacher.domain.AudioInput.Device;
import br.birdteacher.domain.Source;
import br.birdteacher.infra.*;
import java.awt.*;
import java.nio.file.Path;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class SettingsLayoutTest {
    @TempDir Path temp;

    @Test void allSettingsFitAtMinimumAndNormalWidthWithLongDeviceAndModelNames() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Appearance.install();
            var w = new Window();
            try (var whisper = new Whisper(temp, WhisperModel.englishDefault(temp, ""))) {
                w.whisperSettings.bind(whisper, p -> {}, () -> {});
                w.defaultMic.addItem(new Device("1", "Microfone USB ".repeat(15), Source.MIC));
                w.defaultOutput.addItem(new Device("2", "Fone Bluetooth ".repeat(16), Source.OUTPUT));
                w.ollamaSettings.bind(new Ollama(), "modelo-personalizado-".repeat(12), m -> {});
                w.show(Window.Page.SETTINGS);
                w.setVisible(true);
                var tabs = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, w.whisperSettings);
                for (var size : new Dimension[]{w.getMinimumSize(), new Dimension(1090, 750)}) {
                    w.setSize(size);
                    for (int i = 0; i < tabs.getTabCount(); i++) {
                        tabs.setSelectedIndex(i);
                        w.validate();
                        check(tabs.getSelectedComponent(), tabs.getTitleAt(i));
                    }
                }
            } finally { w.ollamaSettings.close(); w.dispose(); }
        });
    }

    private static void check(Component component, String tab) {
        if (!component.isShowing()) return;
        if (component instanceof JScrollPane pane) {
            assertFalse(pane.getHorizontalScrollBar().isShowing(), tab);
            var viewport = pane.getViewport();
            assertTrue(viewport.getView().getWidth() <= viewport.getExtentSize().width, tab);
        }
        if (component instanceof Container group)
            for (Component child : group.getComponents()) check(child, tab);
    }
}
