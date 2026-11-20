package br.birdteacher.app;

import static org.junit.jupiter.api.Assertions.*;
import br.birdteacher.domain.AudioInput.Device;
import br.birdteacher.domain.Source;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;

class AudioSourcePickerTest {
    private final Preferences prefs = Preferences.userRoot().node("/bird-teacher-tests/" + UUID.randomUUID());
    private final Device usb = new Device("usb", "Headset", Source.MIC);
    private final Device laptop = new Device("built-in", "Headset", Source.MIC);
    private final Device speakers = new Device("render", "Saída", Source.OUTPUT);

    @AfterEach void clear() throws Exception { prefs.removeNode(); }

    @Test void defaultsPersistByEndpointIdIndependentlyOfSessionOverrides() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var settings = new JComboBox<Device>();
            var current = new JComboBox<Device>();
            var mic = new AudioSourcePicker(Source.MIC, settings, current, prefs);
            mic.reload(List.of(laptop, speakers, usb));
            assertEquals("Padrão do Windows", current.getSelectedItem().toString());
            assertEquals(3, settings.getItemCount());
            settings.setSelectedItem(usb);
            assertEquals(usb, current.getSelectedItem());
            current.setSelectedItem(laptop);
            mic.reload(List.of(usb, laptop));
            assertEquals(laptop, current.getSelectedItem());
            mic.applyDefault();
            assertEquals(usb, current.getSelectedItem());
            var reopened = new JComboBox<Device>();
            new AudioSourcePicker(Source.MIC, new JComboBox<>(), reopened, prefs).reload(List.of(usb, laptop));
            assertEquals(usb, reopened.getSelectedItem());
            var out = new JComboBox<Device>();
            new AudioSourcePicker(Source.OUTPUT, new JComboBox<>(), out, prefs).reload(List.of(speakers));
            assertEquals("Padrão do Windows", out.getSelectedItem().toString());
            settings.setSelectedIndex(0);
            mic.reload(List.of());
            assertNull(current.getSelectedItem());
            mic.reload(List.of(laptop));
            assertEquals("Padrão do Windows", current.getSelectedItem().toString());
        });
    }

    @Test void disconnectedLegacyPreferenceSurvivesRefreshAndReturnsOnReconnect() throws Exception {
        prefs.put("MIC", usb.id());
        SwingUtilities.invokeAndWait(() -> {
            var settings = new JComboBox<Device>();
            var current = new JComboBox<Device>();
            var picker = new AudioSourcePicker(Source.MIC, settings, current, prefs);
            picker.reload(List.of(laptop));
            assertTrue(settings.getSelectedItem().toString().contains("desconectado"));
            assertNull(current.getSelectedItem());
            picker.reload(List.of(laptop, usb));
            assertEquals(usb, current.getSelectedItem());
            assertEquals(usb, settings.getSelectedItem());
        });
    }
}
