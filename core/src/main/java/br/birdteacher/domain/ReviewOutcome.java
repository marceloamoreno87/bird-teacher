package br.birdteacher.domain;

/** The saved report format also stores the two successful analyses without suggestions. */
public enum ReviewOutcome {
    MISSING("Esta sessão ainda não tem uma revisão de inglês."),
    NO_SPEECH("Ainda não há fala sua confirmada para analisar nesta sessão."),
    NO_SUGGESTIONS("O modelo não encontrou uma sugestão com citação verificável nesta sessão."),
    AVAILABLE("Revisão salva nesta sessão.");

    public final String message;

    ReviewOutcome(String message) { this.message = message; }

    public static ReviewOutcome fromReport(String report) {
        String text = report.strip().replaceFirst("^\uFEFF", "").strip();
        if (text.isEmpty()) return MISSING;
        if (text.equals(NO_SPEECH.message)) return NO_SPEECH;
        if (text.equals(NO_SUGGESTIONS.message)) return NO_SUGGESTIONS;
        return AVAILABLE;
    }
}
