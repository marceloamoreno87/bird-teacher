package br.birdteacher.infra;

import br.birdteacher.domain.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.List;

/** Exports an aligned stereo WAV without holding a complete call in memory. */
public final class StereoRecording {
    public static Path export(LocalSession session, List<Transcript> rows) throws IOException {
        Path target = session.directory().resolve("conversa.wav");
        Path temp = Files.createTempFile(session.directory(), "conversa-", ".part");
        try {
            try (var out = new RandomAccessFile(temp.toFile(), "rw")) {
                out.setLength(44);
                for (var row : rows) write(out, row.clip(), session.readAudio(row.clip()));
                int bytes = (int) (out.length() - 44);
                out.seek(0);
                out.write(ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(0x46464952).putInt(bytes + 36).putInt(0x45564157).putInt(0x20746d66)
                        .putInt(16).putShort((short) 1).putShort((short) 2).putInt(16000).putInt(64000)
                        .putShort((short) 4).putShort((short) 16).putInt(0x61746164).putInt(bytes).array());
            }
            return Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }

    private static void write(RandomAccessFile out, Clip clip, short[] pcm) throws IOException {
        long pos = 44 + clip.millis() * 64;
        byte[] data = new byte[pcm.length * 4];
        if (pos + data.length > 0xffff_ffffL) throw new IOException("A sessão excede o limite do WAV estéreo. Os trechos estão na pasta.");
        out.seek(pos);
        int existing = (int) Math.min(data.length, Math.max(0, out.length() - pos));
        out.readFully(data, 0, existing);
        var buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int channel = clip.source() == Source.MIC ? 0 : 2;
        for (int i = 0; i < pcm.length; i++) buffer.putShort(channel + i * 4, pcm[i]);
        out.seek(pos);
        out.write(data);
    }
}
