package br.birdteacher.domain;

import java.io.IOException;

public interface Recording {
    void saveAudio(Clip clip, short[] pcm) throws IOException;
    short[] readAudio(Clip clip) throws IOException;
    void saveText(Transcript row) throws IOException;
}
