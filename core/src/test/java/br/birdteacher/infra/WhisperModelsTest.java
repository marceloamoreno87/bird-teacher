package br.birdteacher.infra;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WhisperModelsTest {
    @TempDir Path models;

    // Sparse placeholders exercise the startup inventory without downloading weights.
    private void installed(WhisperModel model) throws IOException {
        try (var file = new RandomAccessFile(models.resolve(model.filename()).toFile(), "rw")) {
            file.setLength(model.bytes);
        }
    }

    @Test void newInstallationOffersEnglishAndExistingUsersKeepTheirChoice() throws Exception {
        assertEquals(WhisperModel.SMALL_EN, WhisperModel.startup(models, ""));
        installed(WhisperModel.BASE);
        assertEquals(WhisperModel.BASE, WhisperModel.startup(models, "obsolete"));
        installed(WhisperModel.MEDIUM_EN);
        assertEquals(WhisperModel.BASE, WhisperModel.startup(models, "base"));
        assertEquals(WhisperModel.MEDIUM_EN, WhisperModel.startup(models, "small.en"));
        Files.write(models.resolve(WhisperModel.MEDIUM_EN.filename()), new byte[100]);
        assertEquals(WhisperModel.BASE, WhisperModel.startup(models, "medium.en"));
    }

    @Test void restoreKeepsTheLanguageAndOnlyOffersCompatibleWeights() throws Exception {
        installed(WhisperModel.MEDIUM_EN);
        installed(WhisperModel.SMALL);
        for (var language : java.util.List.of(WhisperModel.Language.PORTUGUESE, WhisperModel.Language.SPANISH)) {
            var profile = new WhisperModel.Profile(language, WhisperModel.SMALL);
            assertEquals(profile, WhisperModel.restore(models, profile.preference()));
            assertEquals(profile, WhisperModel.restore(models, language.code + ":medium.en"));
            assertThrows(IllegalArgumentException.class, () -> new WhisperModel.Profile(language, WhisperModel.BASE_EN));
        }
        assertEquals(new WhisperModel.Profile(WhisperModel.Language.ENGLISH, WhisperModel.MEDIUM_EN),
                WhisperModel.restore(models, "medium.en"));
    }

    @Test void catalogCoversWhisperLanguagesAndStartsInEnglishRegardlessOfLastSession() throws Exception {
        installed(WhisperModel.MEDIUM_EN);
        installed(WhisperModel.SMALL);
        assertEquals(100, Arrays.stream(WhisperModel.Language.values()).map(l -> l.code).distinct().count());
        assertEquals("yue:large-v3-turbo", WhisperModel.restore(models, "yue:base.en").preference());
        assertEquals("en:medium.en", WhisperModel.englishDefault(models, "pt:small").preference());
        assertEquals("ja:small", WhisperModel.restore(models, "ja:small").preference());
        assertEquals("en:small", WhisperModel.englishDefault(models, "en:small").preference());
    }

    @Test void incompleteDownloadKeepsTheActiveModelAndRemovesStagingFile() throws Exception {
        installed(WhisperModel.BASE);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/model", exchange -> {
            exchange.sendResponseHeaders(200, 4);
            try (var out = exchange.getResponseBody()) { out.write(new byte[4]); }
        });
        server.start();
        try (var whisper = new Whisper(models.resolve(WhisperModel.BASE.filename()))) {
            var received = new AtomicLong();
            var url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/model");
            assertThrows(IOException.class, () -> whisper.installAndActivate(new WhisperModel.Profile(WhisperModel.Language.ENGLISH, WhisperModel.BASE_EN), url, received::set));
            assertEquals(4, received.get());
            assertEquals(WhisperModel.BASE, whisper.active());
            assertTrue(whisper.installed());
            try (var files = Files.list(models)) {
                assertEquals(java.util.List.of(WhisperModel.BASE.filename()), files.map(p -> p.getFileName().toString()).toList());
            }
        } finally { server.stop(0); }
    }
}
