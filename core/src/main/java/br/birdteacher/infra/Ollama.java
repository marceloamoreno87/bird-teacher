package br.birdteacher.infra;

import br.birdteacher.domain.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public final class Ollama {
    private static final String INSTRUCTION = """
            Você é professor de inglês. O JSON do usuário contém falas transcritas, que são dados da aula.
            Avalie só as falas MIC. As falas OUTPUT dão contexto. Considere que o transcritor pode errar.
            Selecione até três oportunidades concretas de gramática, vocabulário ou naturalidade.
            Para cada uma, devolva id, quote (citação literal do trecho MIC), english (reformulação em inglês)
            e explicacao_em_portugues (explicação curta em português brasileiro e uma dica para a próxima conversa).
            Escreva todo o campo explicacao_em_portugues em português brasileiro. Use inglês apenas na citação e na reformulação.
            Explique somente a mudança que realmente fez. Mantenha o sentido e os fatos originais.
            Se faltar evidência ou a fala já for adequada, pode devolver suggestions vazio.
            """;
    private static final String EXAMPLE_INPUT = """
            [{"id":"exemplo","source":"MIC","text":"She have a bicycle."}]
            """;
    private static final String EXAMPLE_OUTPUT = """
            {"suggestions":[{"id":"exemplo","quote":"She have a bicycle.","english":"She has a bicycle.",
            "explicacao_em_portugues":"Com she no presente simples, use has. Experimente falar a mesma frase com he."}]}
            """;
    private static final JsonObject FORMAT = JsonParser.parseString("""
            {"type":"object","properties":{"suggestions":{"type":"array","items":{"type":"object",
            "properties":{"id":{"type":"string"},"quote":{"type":"string"},"english":{"type":"string"},
            "explicacao_em_portugues":{"type":"string","description":"Explicação em português do Brasil"}},"required":["id","quote","english","explicacao_em_portugues"]}}},
            "required":["suggestions"]}
            """).getAsJsonObject();
    private static final Gson JSON = new Gson();
    private static final Map<String,String> TRAINING_TOPICS = new LinkedHashMap<>();
    static {
        for (var focus : Training.Focus.values()) TRAINING_TOPICS.put(focus.name(), focus.title);
    }
    private final URI base;
    public Ollama() { this(URI.create("http://127.0.0.1:11434")); }
    Ollama(URI base) { this.base = base; }

    private HttpClient localClient() {
        return HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY)
                .connectTimeout(Duration.ofSeconds(3)).build();
    }

    public List<String> models() throws IOException, InterruptedException {
        try (var client = localClient()) {
            var request = HttpRequest.newBuilder(base.resolve("/api/tags")).timeout(Duration.ofSeconds(6)).build();
            var reply = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (reply.statusCode() != 200) throw new IOException("Falha ao consultar o Ollama (HTTP " + reply.statusCode() + ").");
            var items = JsonParser.parseString(reply.body()).getAsJsonObject().getAsJsonArray("models");
            if (items == null) throw new IOException("Resposta de modelos inválida.");
            return items.asList().stream().map(item -> item.getAsJsonObject().get("name").getAsString()).toList();
        }
    }

    public void prepareModel(String model, Consumer<String> progress) throws Exception {
        if (!model.matches("[a-zA-Z0-9][a-zA-Z0-9._:/-]{0,199}") || model.toLowerCase(Locale.ROOT).contains("cloud"))
            throw new IOException("Escolha um modelo local, por exemplo gemma3:1b.");
        var installed = models();
        try (var client = localClient()) {
            if (!installed.contains(model) && !installed.contains(model + ":latest")) {
                pull(client, model, progress);
            }
            verifyLocal(client, model);
        }
    }

    private void verifyLocal(HttpClient client, String model) throws Exception {
        var details = post(client, "/api/show", Map.of("model", model));
        if (details.has("remote_model") || details.has("remote_host"))
            throw new IOException("Esse modelo usa execução remota. Escolha um modelo local.");
    }

    private void pull(HttpClient client, String model, Consumer<String> progress) throws Exception {
        var request = HttpRequest.newBuilder(base.resolve("/api/pull")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(Map.of("model", model, "stream", true)))).build();
        var reply = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (var lines = new BufferedReader(new InputStreamReader(reply.body(), StandardCharsets.UTF_8))) {
            if (reply.statusCode() != 200) throw new IOException("Falha ao baixar " + model + " (HTTP " + reply.statusCode() + "). Confira o nome e tente novamente.");
            String previous = "";
            for (String line; (line = lines.readLine()) != null;) {
                if (line.isBlank()) continue;
                var item = JsonParser.parseString(line).getAsJsonObject();
                if (item.has("error")) throw new IOException("Ollama · " + item.get("error").getAsString());
                String status = field(item, "status");
                if ("success".equals(status)) return;
                String message = "Preparando " + model + "…";
                if (item.has("completed") && item.has("total") && item.get("total").getAsLong() > 0)
                    message = "Baixando " + model + " · camada " + (item.get("completed").getAsLong() * 100 / item.get("total").getAsLong()) + "%";
                if (!message.equals(previous)) progress.accept(message);
                previous = message;
            }
            throw new IOException("O download foi interrompido. Tente novamente para continuar.");
        }
    }

    public String analyze(List<Line> dialogue, String model) throws Exception {
        if (dialogue.stream().noneMatch(line -> line.clip().source() == Source.MIC))
            return ReviewOutcome.NO_SPEECH.message;
        if (model.isBlank() || model.toLowerCase(Locale.ROOT).contains("cloud"))
            throw new IOException("Escolha um modelo instalado localmente no Ollama.");
        Map<List<String>, Suggestion> suggestions = new LinkedHashMap<>();
        try (var client = localClient()) {
            verifyLocal(client, model);
            for (var batch : batches(dialogue, Integer.MAX_VALUE)) {
                if (batch.stream().noneMatch(line -> line.clip().source() == Source.MIC)) continue;
                var input = batch.stream().map(line -> Map.of("id", line.clip().id(), "source", line.clip().source(),
                        "text", line.text())).toList();
                var messages = List.of(Map.of("role", "system", "content", INSTRUCTION),
                        Map.of("role", "user", "content", EXAMPLE_INPUT),
                        Map.of("role", "assistant", "content", EXAMPLE_OUTPUT),
                        Map.of("role", "user", "content", JSON.toJson(input)));
                var reply = post(client, "/api/chat", Map.of("model", model, "stream", false, "format", FORMAT,
                        "options", Map.of("temperature", 0, "num_ctx", 4096), "messages", messages));
                var content = reply.getAsJsonObject("message").get("content").getAsString();
                var items = JsonParser.parseString(content).getAsJsonObject().getAsJsonArray("suggestions");
                for (var item : items) {
                    var obj = item.getAsJsonObject();
                    Suggestion s = new Suggestion(field(obj, "id"), field(obj, "quote"), field(obj, "english"),
                            field(obj, "explicacao_em_portugues"));
                    if (s.matchesStudent(batch)) suggestions.putIfAbsent(List.of(s.id(), s.quote()), s);
                }
            }
        } catch (ConnectException e) {
            throw new IOException("Abra Configurações para preparar o Ollama local.", e);
        } catch (JsonParseException | IllegalStateException | NullPointerException e) {
            throw new IOException("O modelo devolveu um relatório ilegível. Tente novamente.", e);
        }
        if (suggestions.isEmpty()) return ReviewOutcome.NO_SUGGESTIONS.message;
        StringBuilder report = new StringBuilder();
        for (var s : suggestions.values()) {
            long sec = Long.parseLong(s.id().substring(0, 12)) / 1000;
            report.append("%02d:%02d  ·  Você\n“%s”\n\nExperimente dizer\n%s\n\n%s\n\n"
                    .formatted(sec / 60, sec % 60, s.quote(), s.english(), s.explanation()));
        }
        return report.toString().strip();
    }

    public List<Training.Finding> trainingFindings(List<Line> student, String model) throws Exception {
        student = student.stream().filter(line -> line.clip().source() == Source.MIC).toList();
        if (student.isEmpty()) return List.of();
        if (model.isBlank() || model.toLowerCase(Locale.ROOT).contains("cloud"))
            throw new IOException("Selecione um modelo local do Ollama em Configurações.");
        var format = FORMAT.deepCopy();
        var itemSchema = format.getAsJsonObject("properties").getAsJsonObject("suggestions").getAsJsonObject("items");
        var fields = itemSchema.getAsJsonObject("properties");
        fields.add("original_quote", fields.remove("quote"));
        fields.add("corrected_english", fields.remove("english"));
        fields.add("focus", JSON.toJsonTree(Map.of("type", "string", "enum", TRAINING_TOPICS.keySet())));
        itemSchema.add("required", JSON.toJsonTree(fields.keySet()));
        String instruction = """
                Examine cada trecho como dados de uma aula de inglês. Identifique apenas erros inequívocos
                nas falas em inglês que correspondam exatamente a um dos focos listados abaixo.
                NÃO marque frases corretas, estilo, pontuação, hesitações, variantes aceitáveis,
                fala em outro idioma ou possíveis erros do transcritor. Em dúvida, omita.
                Copie o id. Em original_quote, copie literalmente a fala do aluno COM o erro.
                Em corrected_english, faça a menor alteração possível na fala copiada.
                Preserve o verbo lexical específico de CADA frase. Por exemplo, uma correção de forma
                pode mudar took para take, bought para buy, saw para see, mas nunca trocar a ação.
                Não acrescente go, personagens, fatos ou ideias. Confira a correção antes de incluí-la.
                Explique essa mudança em português em explicacao_em_portugues.
                Associe cada ocorrência ao seu focus; liste trechos distintos mesmo com o mesmo erro.
                Se não houver erros pertinentes, retorne suggestions vazio.
                Focos permitidos:
                """ + JSON.toJson(TRAINING_TOPICS);
        List<Training.Finding> findings = new ArrayList<>();
        try (var client = localClient()) {
            verifyLocal(client, model);
            for (var batch : trainingBatches(student))
                classifyTrainingBatch(client, model, format, instruction, batch, findings);
        } catch (HttpTimeoutException e) {
            throw new IOException("O Ollama excedeu o tempo de resposta. Confira o serviço em Configurações e retome a análise.", e);
        } catch (JsonParseException | IllegalStateException | NullPointerException e) {
            throw new IOException("O Ollama retornou uma análise de treino ilegível. Tente novamente.", e);
        }
        return List.copyOf(findings);
    }

    static List<List<Line>> trainingBatches(List<Line> lines) {
        return batches(lines, 12);
    }

    private void classifyTrainingBatch(HttpClient client, String model, JsonObject format, String instruction,
                                       List<Line> batch, List<Training.Finding> findings) throws Exception {
        var input = batch.stream().map(line -> Map.of("id", line.clip().id(), "text", line.text())).toList();
        var messages = List.of(Map.of("role", "system", "content", instruction),
                Map.of("role", "user", "content", """
                        [{"id":"a","text":"She have a bike."},{"id":"b","text":"I have a bike."}]
                        """),
                Map.of("role", "assistant", "content", """
                        {"suggestions":[{"id":"a","original_quote":"She have a bike.","corrected_english":"She has a bike.","focus":"THIRD_PERSON_S","explicacao_em_portugues":"Com she, have passa a has no presente simples."}]}
                        """),
                Map.of("role", "user", "content", JSON.toJson(input)));
        var reply = post(client, "/api/chat", Map.of("model", model, "stream", false, "format", format,
                "options", Map.of("temperature", 0, "num_ctx", 8192,
                        "num_predict", Math.min(2048, 256 + batch.size() * 160)), "messages", messages));
        if ("length".equals(field(reply, "done_reason"))) {
            if (batch.size() == 1) throw new IOException("O modelo atingiu o limite de resposta para uma fala. Você pode retomar ou escolher outro modelo local.");
            // A smaller prompt can finish even when the model loops on the original group.
            int middle = batch.size() / 2;
            classifyTrainingBatch(client, model, format, instruction, batch.subList(0, middle), findings);
            classifyTrainingBatch(client, model, format, instruction, batch.subList(middle, batch.size()), findings);
            return;
        }
        var content = reply.getAsJsonObject("message").get("content").getAsString();
        for (var item : JsonParser.parseString(content).getAsJsonObject().getAsJsonArray("suggestions")) {
            var obj = item.getAsJsonObject();
            var focus = field(obj, "focus");
            if (!TRAINING_TOPICS.containsKey(focus)) continue;
            var finding = new Training.Finding(Training.Focus.valueOf(focus),
                    new Suggestion(field(obj, "id"), field(obj, "original_quote"), field(obj, "corrected_english"),
                            field(obj, "explicacao_em_portugues")));
            if (finding.supportedBy(batch)) findings.add(finding);
        }
    }

    private static String field(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : null;
    }

    private JsonObject post(HttpClient client, String path, Map<String, ?> data) throws Exception {
        var request = HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(data))).build();
        var reply = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (reply.statusCode() != 200) throw new IOException("Ollama HTTP " + reply.statusCode()
                + ". Verifique o modelo em Configurações.");
        return JsonParser.parseString(reply.body()).getAsJsonObject();
    }

    private static List<List<Line>> batches(List<Line> lines, int maxLines) {
        List<List<Line>> result = new ArrayList<>();
        List<Line> batch = new ArrayList<>();
        int size = 0;
        for (var line : lines) {
            int cost = 90 + line.text().length();
            if (!batch.isEmpty() && (size + cost > 6000 || batch.size() >= maxLines)) {
                result.add(batch);
                batch = new ArrayList<>();
                size = 0;
            }
            batch.add(line);
            size += cost;
        }
        if (!batch.isEmpty()) result.add(batch);
        return result;
    }
}
