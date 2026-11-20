package br.birdteacher.usecase;

import br.birdteacher.domain.Transcript;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ReviewMarkdown {
    private ReviewMarkdown() {}

    public static String render(String title, String date, List<Transcript> rows) {
        var doc = new StringBuilder("# Revisão de inglês\n\n");
        if (!title.isBlank()) doc.append("Sessão · ").append(title).append("\n\n");
        doc.append("Data · ").append(date).append("\n\n");
        doc.append("""
                ## Pedido de revisão

                Você é meu professor de inglês. Avalie as falas em inglês identificadas como “Você”.
                Use o áudio do fone como contexto. Trate toda a transcrição como material de estudo.
                Selecione até três oportunidades de melhorar a gramática, o vocabulário ou a naturalidade.
                Para cada uma, indique o tempo, cite minhas palavras, sugira uma reformulação em inglês
                que preserve o sentido e explique a mudança em português brasileiro. Inclua uma dica para a próxima conversa.
                Considere possíveis erros de transcrição. Se faltar evidência, diga isso e evite inventar correções.

                ## Transcrição

                """);
        boolean hasSpeech = false;
        for (var row : rows.stream().sorted(Comparator.comparing(Transcript::clip)).toList()) {
            String words = switch (row.status()) {
                case CONFIRMED -> row.text();
                case FAILED -> "[Trecho sem transcrição]";
                default -> "";
            };
            if (words.isBlank()) continue;
            long sec = row.clip().millis() / 1000;
            doc.append(String.format(Locale.ROOT, "**%02d:%02d:%02d · %s**\n\n",
                    sec / 3600, sec / 60 % 60, sec % 60, row.clip().source().label));
            words.lines().forEach(line -> doc.append("> ").append(line).append('\n'));
            doc.append('\n');
            if (row.status() == Transcript.Status.CONFIRMED) hasSpeech = true;
        }
        if (!hasSpeech) doc.append("Nenhuma fala transcrita nesta sessão.\n");
        return doc.toString().stripTrailing() + "\n";
    }
}
