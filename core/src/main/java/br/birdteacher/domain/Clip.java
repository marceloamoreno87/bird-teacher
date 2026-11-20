package br.birdteacher.domain;

import java.util.Locale;

public record Clip(long millis, Source source) implements Comparable<Clip> {
    public String id() { return String.format(Locale.ROOT, "%012d-%s", millis, source.name()); }

    @Override public int compareTo(Clip other) {
        int order = Long.compare(millis, other.millis);
        return order == 0 ? source.compareTo(other.source) : order;
    }
}
