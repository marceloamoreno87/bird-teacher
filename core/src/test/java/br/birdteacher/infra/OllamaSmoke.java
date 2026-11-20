package br.birdteacher.infra;

import br.birdteacher.domain.*;
import java.net.URI;
import java.util.List;

/** Runs the real model with a synthetic dialogue, independently of a microphone. */
public final class OllamaSmoke {
    public static void main(String[] args) throws Exception {
        var transcript = List.of(new Line(new Clip(0, Source.OUTPUT), "How did you get here yesterday?"),
                new Line(new Clip(2200, Source.MIC), "Yesterday I goed to school by bus."));
        String report = new Ollama(URI.create("http://127.0.0.1:" + Integer.parseInt(args[0])))
                .analyze(transcript, args.length > 1 ? args[1] : "qwen3:4b-instruct");
        System.out.println(report);
        if (!report.contains("goed") || !report.toLowerCase().contains("went"))
            throw new AssertionError("A resposta precisa citar a fala e corrigir o verbo.");
    }
}
