package br.birdteacher.app;

import br.birdteacher.domain.AudioInput.Device;
import br.birdteacher.domain.Source;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;

/** The settings selector owns the saved preference. The conversation selector is session-local. */
final class AudioSourcePicker {
    private final Source source;
    private final JComboBox<Device> settings, current;
    private final Preferences prefs;
    private boolean loading;

    AudioSourcePicker(Source source, JComboBox<Device> settings, JComboBox<Device> current, Preferences prefs) {
        this.source = source;
        this.settings = settings;
        this.current = current;
        this.prefs = prefs;
        settings.addActionListener(e -> save());
    }

    void reload(List<Device> devices) {
        String saved = prefs.get(source.name(), "");
        var selected = (Device) current.getSelectedItem();
        var endpoints = devices.stream().filter(d -> d.source() == source).toList();
        var system = Device.systemDefault(source);
        loading = true;
        try {
            settings.removeAllItems();
            current.removeAllItems();
            settings.addItem(system);
            if (!endpoints.isEmpty()) current.addItem(system);
            for (var device : endpoints) {
                settings.addItem(device);
                current.addItem(device);
            }
            Device preferred = find(settings, saved);
            if (preferred == null) {
                String name = prefs.get(source.name() + ".name", "Dispositivo salvo");
                preferred = new Device(saved, name + " (desconectado)", source);
                settings.addItem(preferred);
            }
            settings.setSelectedItem(preferred);
            var previous = selected == null ? null : find(current, selected.id());
            current.setSelectedItem(previous == null ? find(current, saved) : previous);
        } finally { loading = false; }
    }

    void applyDefault() {
        current.setSelectedItem(find(current, prefs.get(source.name(), "")));
    }

    private void save() {
        if (loading || !(settings.getSelectedItem() instanceof Device choice)) return;
        prefs.put(source.name(), choice.id());
        if (choice.followsWindows()) prefs.remove(source.name() + ".name");
        else if (find(current, choice.id()) != null) prefs.put(source.name() + ".name", choice.name());
        applyDefault();
    }

    private static Device find(JComboBox<Device> box, String id) {
        for (int i = 0; i < box.getItemCount(); i++) {
            Device item = box.getItemAt(i);
            if (item.id().equals(id)) return item;
        }
        return null;
    }
}
