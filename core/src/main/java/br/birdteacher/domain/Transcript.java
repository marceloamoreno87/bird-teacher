package br.birdteacher.domain;

public record Transcript(Clip clip, Status status, String text) {
    public enum Status { PREVIEW, QUEUED, CONFIRMED, SILENT, FAILED }
}
