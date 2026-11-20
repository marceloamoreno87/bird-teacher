package br.birdteacher.domain;

public interface Speech {
    default String preview(short[] pcm) throws Exception { return recognize(pcm); }
    String recognize(short[] pcm) throws Exception;
}
