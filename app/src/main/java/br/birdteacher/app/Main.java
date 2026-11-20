package br.birdteacher.app;

import br.birdteacher.infra.*;

import java.nio.file.*;

import javax.swing.*;

public final class Main {
    public static void main(String[] args) {
        if (!System.getProperty("os.name").startsWith("Windows")
                || !System.getProperty("os.arch").equals("amd64")) {
            System.err.println("O Bird Teacher requer Windows x64 e Java 21.");
            return;
        }
        var root = Path.of(System.getenv("LOCALAPPDATA"), "Bird Teacher");
        // JNativeHook otherwise attempts to extract beside its jar (possibly read-only).
        System.setProperty("jnativehook.lib.path", root.resolve("native").toString());
        try {
            Files.createDirectories(root.resolve("native"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            return;
        }
        var modelDir = root.resolve("models");
        var prefs = java.util.prefs.Preferences.userNodeForPackage(DesktopController.class);
        var profile = WhisperModel.englishDefault(modelDir, prefs.get("whisperEnglishModel", prefs.get("whisperModel", "")));
        Appearance.install();
        SwingUtilities.invokeLater(
                () -> {
                    var view = new Window();
                    var app =
                            new DesktopController(
                                    view,
                                    new Wasapi(),
                                    new Whisper(modelDir, profile),
                                    new Ollama(),
                                    root.resolve("sessões"));
                    app.open();
                });
    }
}
