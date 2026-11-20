package br.birdteacher.infra;

import com.sun.jna.Pointer;

/** Downmix and rate conversion carry their phase across native packet boundaries. */
final class MixFormat {
    private final int channels, rate, stride, bits;
    private final boolean floating;
    private double sum;
    private int phase, count;

    MixFormat(Pointer format) {
        int tag = format.getShort(0) & 0xffff;
        channels = format.getShort(2) & 0xffff;
        rate = format.getInt(4);
        stride = format.getShort(12) & 0xffff;
        bits = format.getShort(14) & 0xffff;
        if (tag == 0xfffe) tag = format.getInt(24); // WAVEFORMATEXTENSIBLE.SubFormat.Data1
        floating = tag == 3;
        boolean supported = floating ? bits == 32 : tag == 1 && (bits == 16 || bits == 24 || bits == 32);
        if (!supported || channels < 1 || rate < 1 || stride < channels * (bits / 8))
            throw new IllegalStateException("Formato WASAPI incompatível: " + tag + ", " + bits + " bits.");
    }

    long duration(int frames) { return frames * 1_000_000_000L / rate; }

    short[] convert(Pointer data, int frames) {
        short[] result = new short[(int) (((long) frames * 16000 + phase) / rate)];
        int used = 0;
        for (int frame = 0; frame < frames; frame++) {
            double mono = 0;
            if (data != null) {
                for (int ch = 0; ch < channels; ch++)
                    mono += sample(data, (long) frame * stride + ch * (bits / 8));
            }
            sum += mono / channels;
            count++;
            phase += 16000;
            if (phase < rate) continue;
            short value = (short) Math.clamp(Math.round(sum / count), -32768, 32767);
            while (phase >= rate) {
                result[used++] = value;
                phase -= rate;
            }
            sum = 0;
            count = 0;
        }
        return result;
    }

    private double sample(Pointer p, long offset) {
        if (floating) return p.getFloat(offset) * 32768.0;
        return switch (bits) {
            case 16 -> p.getShort(offset);
            case 24 -> (p.getByte(offset + 2) << 8) | (p.getByte(offset + 1) & 255);
            default -> p.getInt(offset) >> 16;
        };
    }
}
