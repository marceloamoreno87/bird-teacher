package br.birdteacher.infra;

import br.birdteacher.domain.*;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class OllamaTest {
    HttpServer server;
    Ollama coach;
    final AtomicReference<String> details = new AtomicReference<>("{}");
    final AtomicInteger requests = new AtomicInteger();
    final List<Line> dialogue = List.of(new Line(new Clip(0, Source.OUTPUT), "Where?"),
            new Line(new Clip(1000, Source.MIC), "I go yesterday."));
    final AtomicReference<String> chatRequest = new AtomicReference<>();

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/show", e -> send(e, details.get()));
        server.createContext("/api/chat", e -> {
            chatRequest.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requests.incrementAndGet();
            var items = List.of(new Suggestion(dialogue.getLast().clip().id(), "I go yesterday.", "I went yesterday.", "Use o passado."),
                    new Suggestion(dialogue.getFirst().clip().id(), "Where?", "Where was it?", "Fala alheia."));
            var json = new Gson();
            String content = json.toJson(Map.of("suggestions", items)).replace("\"explanation\"", "\"explicacao_em_portugues\"");
            send(e, json.toJson(Map.of("message", Map.of("content", content))));
        });
        server.start();
        coach = new Ollama(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
    }
    void send(com.sun.net.httpserver.HttpExchange exchange, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) { out.write(bytes); }
    }
    @AfterEach void stop() { server.stop(0); }

    @Test void reportFiltersWrongSpeakerAndIncludesRemoteContext() throws Exception {
        String report = coach.analyze(dialogue, "local");
        assertTrue(report.contains("I went yesterday."));
        assertFalse(report.contains("Fala alheia."));
        assertTrue(chatRequest.get().contains("Where?"));
    }
    @Test void trainingRequiresLiteralStudentEvidenceAndARealChange() throws Exception {
        server.removeContext("/api/chat");
        server.createContext("/api/chat", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            chatRequest.set(body);
            var valid = Map.of("id", dialogue.getLast().clip().id(), "focus", "FINISHED_PAST",
                    "original_quote", "I go yesterday.", "corrected_english", "I went yesterday.",
                    "explicacao_em_portugues", "Use o passado.");
            var invented = new HashMap<>(valid);
            invented.put("original_quote", "I swam.");
            var punctuation = new HashMap<>(valid);
            punctuation.put("corrected_english", "I GO yesterday!");
            var unrelated = new HashMap<>(valid);
            unrelated.put("focus", "general");
            var json = new Gson();
            var content = json.toJson(Map.of("suggestions", List.of(valid, invented, punctuation, unrelated)));
            send(exchange, json.toJson(Map.of("message", Map.of("content", content))));
        });
        var findings = coach.trainingFindings(dialogue, "local");
        assertEquals(1, findings.size());
        assertEquals(Training.Focus.FINISHED_PAST, findings.getFirst().focus());
        assertFalse(chatRequest.get().contains("Where?"));
    }

    @Test void aRunawayTrainingResponseIsBoundedAndRetriedInSmallerGroups() throws Exception {
        server.removeContext("/api/chat");
        var sizes = new ArrayList<Integer>();
        var limits = new ArrayList<Integer>();
        server.createContext("/api/chat", exchange -> {
            var request = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
            var messages = request.getAsJsonArray("messages");
            var prompt = messages.get(messages.size() - 1).getAsJsonObject().get("content").getAsString();
            int size = JsonParser.parseString(prompt).getAsJsonArray().size();
            sizes.add(size);
            limits.add(request.getAsJsonObject("options").get("num_predict").getAsInt());
            var json = new Gson();
            send(exchange, json.toJson(Map.of("done_reason", size > 2 ? "length" : "stop",
                    "message", Map.of("content", size > 2 ? "unfinished" : "{\"suggestions\":[]}"))));
        });
        var input = new ArrayList<Line>();
        for (int i = 0; i < 4; i++) input.add(new Line(new Clip(i, Source.MIC), "I agree."));
        assertTrue(coach.trainingFindings(input, "local").isEmpty());
        assertEquals(List.of(4, 2, 2), sizes);
        assertTrue(limits.stream().allMatch(limit -> limit > 0 && limit <= 2048));
    }

    @Test void remoteModelCannotReceiveSessionEvenUnderLocalAlias() {
        details.set("{\"remote_host\":\"example.org\"}");
        assertThrows(IOException.class, () -> coach.analyze(dialogue, "alias"));
        assertThrows(IOException.class, () -> coach.trainingFindings(dialogue, "alias"));
        assertEquals(0, requests.get());
    }
    @Test void setupPullsTheChosenModelAndShowsLayerProgress() throws Exception {
        server.createContext("/api/tags", e -> send(e, "{\"models\":[]}"));
        var pulled = new AtomicReference<String>();
        server.createContext("/api/pull", e -> {
            var body = JsonParser.parseString(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            pulled.set(body.get("model").getAsString());
            send(e, "{\"status\":\"pulling layer\",\"completed\":50,\"total\":100}\n{\"status\":\"success\"}\n");
        });
        var progress = new ArrayList<String>();
        coach.prepareModel("gemma3:1b", progress::add);
        assertEquals("gemma3:1b", pulled.get());
        assertTrue(progress.stream().anyMatch(p -> p.contains("50%")));
    }

    @Test void interruptedPullCannotActivateAnIncompleteModel() throws Exception {
        server.createContext("/api/tags", e -> send(e, "{\"models\":[]}"));
        server.createContext("/api/pull", e -> send(e, "{\"status\":\"pulling manifest\"}\n"));
        assertThrows(IOException.class, () -> coach.prepareModel("gemma3:1b", ignored -> {}));
    }

    @Test void setupUsesInstalledModelsOfflineAndRejectsCloudAliases() throws Exception {
        server.createContext("/api/tags", e -> send(e, "{\"models\":[{\"name\":\"custom:latest\"}]}"));
        coach.prepareModel("custom", ignored -> {});
        details.set("{\"remote_model\":\"remote\"}");
        assertThrows(IOException.class, () -> coach.prepareModel("custom", ignored -> {}));
    }
    @Test void noStudentSpeechNeedsNoModel() throws Exception {
        assertTrue(coach.analyze(List.of(dialogue.getFirst()), "").contains("não há fala"));
        assertEquals(0, requests.get());
    }
}
