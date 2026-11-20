package br.birdteacher.domain;

import java.util.*;

public final class Training {
    private Training() {}

    // Stable keys let different sessions contribute to the same narrowly defined lesson.
    public enum Focus {
        THIRD_PERSON_S("Usar a forma do verbo com he, she e it no presente"),
        DID_BASE_VERB("Usar o verbo na forma base depois de did"),
        IRREGULAR_PAST("Formar o passado simples de verbos irregulares"),
        FINISHED_PAST("Usar o passado simples com um momento já encerrado"),
        SINCE_FOR("Escolher since ou for para indicar duração"),
        PRESENT_PERFECT_DURATION("Descrever algo que começou no passado e continua hoje"),
        BE_AGREEMENT("Combinar o verbo be com o sujeito"),
        MODAL_BASE_VERB("Usar a forma base do verbo depois de um modal"),
        PLURAL_AFTER_NUMBER("Usar o plural de substantivos contáveis após números"),
        UNCOUNTABLE_PLURAL("Usar substantivos incontáveis sem plural"),
        MUCH_MANY("Escolher much ou many conforme o substantivo"),
        INDEFINITE_ARTICLE("Escolher a ou an pelo som da palavra seguinte"),
        AUXILIARY_QUESTION("Formar perguntas diretas com do ou does"),
        DEPEND_ON("Usar a preposição on com depend"),
        LISTEN_TO("Usar to antes do objeto de listen"),
        AGREE_WITHOUT_BE("Usar agree como verbo");

        public final String title;
        Focus(String title) { this.title = title; }
    }

    public record Finding(Focus focus, Suggestion change) {
        public boolean supportedBy(List<Line> lines) {
            return focus != null && change != null && change.matchesStudent(lines)
                    && !words(change.quote()).equals(words(change.english()));
        }
        private static String words(String text) {
            return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
        }
    }

    public record Example(String session, Finding finding) {
        public String key() { return session + "/" + finding.change().id(); }
        public Suggestion change() { return finding.change(); }
    }

    public record Lesson(Focus focus, List<Example> examples) {
        public Example audio() { return examples.getFirst(); }
        public Example writing() {
            return examples.stream().skip(1)
                    .filter(e -> !e.change().quote().equalsIgnoreCase(audio().change().quote()))
                    .findFirst().orElse(examples.get(1));
        }
    }

    public static List<Lesson> available(List<Example> examples, Map<Focus, Set<String>> consumed) {
        Map<Focus, Map<String, Example>> groups = new EnumMap<>(Focus.class);
        for (var example : examples) {
            var focus = example.finding().focus();
            if (!consumed.getOrDefault(focus, Set.of()).contains(example.key()))
                groups.computeIfAbsent(focus, ignored -> new LinkedHashMap<>()).putIfAbsent(example.key(), example);
        }
        return groups.entrySet().stream().filter(group -> group.getValue().size() >= 4)
                .sorted(Comparator.comparingInt(group -> -group.getValue().size()))
                .map(group -> new Lesson(group.getKey(), List.copyOf(group.getValue().values())))
                .toList();
    }
}
