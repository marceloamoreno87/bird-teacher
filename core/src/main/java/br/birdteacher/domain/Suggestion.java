package br.birdteacher.domain;

import java.util.List;

public record Suggestion(String id, String quote, String english, String explanation) {
    public boolean matchesStudent(List<Line> conversation) {
        if (id == null || quote == null || quote.isBlank() || english == null || english.isBlank()
                || explanation == null || explanation.isBlank()) return false;
        if (conversation.stream().anyMatch(line -> line.clip().source() == Source.OUTPUT
                && line.text().strip().equalsIgnoreCase(english.strip()))) return false;
        return conversation.stream().anyMatch(line -> line.clip().source() == Source.MIC
                && line.clip().id().equals(id) && line.text().contains(quote));
    }
}
