package br.birdteacher.domain;

public enum Source {
    MIC("Você"), OUTPUT("Áudio do fone");

    public final String label;
    Source(String label) { this.label = label; }
}
