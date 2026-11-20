package br.birdteacher.app;

import br.birdteacher.infra.Ollama;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/** Windows per-user installation and startup. Model data belongs to the local Ollama server. */
final class WindowsOllama {
    private static final URI INSTALLER = URI.create("https://ollama.com/download/OllamaSetup.exe");

    private static Optional<Path> executable() {
        var folders = new ArrayList<Path>();
        var local = System.getenv("LOCALAPPDATA");
        if (local != null) folders.add(Path.of(local, "Programs", "Ollama"));
        for (String entry : System.getenv().getOrDefault("PATH", "").split(File.pathSeparator)) {
            try {
                var folder = Path.of(entry.replace("\"", ""));
                if (folder.isAbsolute()) folders.add(folder);
            } catch (InvalidPathException ignored) { }
        }
        return folders.stream().map(p -> p.resolve("ollama.exe")).filter(Files::isRegularFile).findFirst();
    }

    static boolean installed() { return executable().isPresent(); }

    static synchronized List<String> connect(Ollama ollama, boolean allowInstall, Consumer<String> progress) throws Exception {
        try { return ollama.models(); }
        catch (ConnectException offline) {
            if (!installed()) {
                if (!allowInstall) throw new IOException("Instale o Ollama em Configurações para gerar a revisão.", offline);
                install(progress);
                try { return ollama.models(); } catch (ConnectException starting) { /* Start below. */ }
            }
        }
        progress.accept("Iniciando o Ollama local…");
        var exe = executable();
        // The installer may have launched a custom installation outside this process's PATH.
        if (exe.isPresent()) {
            var desktop = exe.get().resolveSibling("ollama app.exe");
            var command = Files.isRegularFile(desktop) ? new ProcessBuilder(desktop.toString())
                    : new ProcessBuilder(exe.get().toString(), "serve");
            command.redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        }
        for (int i = 0; i < 40; i++) {
            try { return ollama.models(); }
            catch (ConnectException waiting) { Thread.sleep(750); }
        }
        throw new IOException("O Ollama não respondeu na porta 11434. Tente iniciar novamente.");
    }

    private static void install(Consumer<String> progress) throws Exception {
        var temp = Files.createTempDirectory("bird-ollama-");
        var file = temp.resolve("OllamaSetup.exe");
        try {
            progress.accept("Baixando o instalador oficial do Ollama…");
            try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20)).build()) {
                var response = client.send(HttpRequest.newBuilder(INSTALLER).timeout(Duration.ofMinutes(90)).build(),
                        HttpResponse.BodyHandlers.ofInputStream());
                try (var in = response.body(); var out = Files.newOutputStream(file)) {
                    if (response.statusCode() != 200) throw new IOException("Download do instalador falhou (HTTP " + response.statusCode() + ").");
                    byte[] block = new byte[131072];
                    long total = 0, last = -1;
                    for (int read; (read = in.read(block)) >= 0;) {
                        if (Thread.interrupted()) throw new InterruptedException();
                        out.write(block, 0, read);
                        total += read;
                        long mib = total / 1048576;
                        if (mib != last) progress.accept("Baixando o instalador · " + mib + " MiB");
                        last = mib;
                    }
                }
            }
            progress.accept("Verificando o instalador…");
            var verifier = new ProcessBuilder(Path.of(System.getenv("SystemRoot"), "System32", "WindowsPowerShell", "v1.0", "powershell.exe").toString(),
                "-NoProfile", "-NonInteractive", "-Command",
                "$s=Get-AuthenticodeSignature -LiteralPath $env:BIRD_INSTALLER; if ($s.Status -ne 'Valid' -or $s.SignerCertificate.Subject -notmatch '(?:^|, )O=Ollama Inc\\.(?:,|$)') {exit 1}");
            verifier.environment().put("BIRD_INSTALLER", file.toString());
            if (verifier.start().waitFor() != 0) throw new IOException("O Windows não confirmou a assinatura do instalador do Ollama.");
            progress.accept("Conclua a instalação na janela do Ollama.");
            if (new ProcessBuilder(file.toString(), "/NORESTART").start().waitFor() != 0)
                throw new IOException("A instalação não foi concluída. Você pode tentar novamente.");
        } finally {
            // Closing Bird Teacher can leave the native installer briefly holding the executable.
            try { Files.deleteIfExists(file); Files.deleteIfExists(temp); }
            catch (IOException locked) { file.toFile().deleteOnExit(); }
        }
    }
}
