package br.birdteacher.domain;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface AudioInput {
    record Device(String id, String name, Source source) {
        public static Device systemDefault(Source source) { return new Device("", "Padrão do Windows", source); }
        public boolean followsWindows() { return id.isEmpty(); }
        @Override public String toString() { return name; }
    }
    interface Listener {
        void ready() throws Exception;
        void samples(long nanoTime, short[] pcm) throws Exception;
        void idle(long nanoTime) throws Exception;
    }
    List<Device> devices();
    void capture(Device device, AtomicBoolean stopped, Listener listener) throws Exception;
}
