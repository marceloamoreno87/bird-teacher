package br.birdteacher.infra;

import com.sun.jna.Memory;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MixFormatTest {
    @Test void downmixesStereoAndCarriesOneOutputSampleAcrossPackets() {
        try (var format = new Memory(40); var data = new Memory(16)) {
            format.clear();
            format.setShort(0, (short) 0xfffe);
            format.setShort(2, (short) 2);
            format.setInt(4, 48000);
            format.setShort(12, (short) 8);
            format.setShort(14, (short) 32);
            format.setInt(24, 3);
            data.setFloat(0, .75f); data.setFloat(4, .25f);
            data.setFloat(8, .75f); data.setFloat(12, .25f);
            var mix = new MixFormat(format);
            assertEquals(0, mix.convert(data, 2).length);
            assertArrayEquals(new short[]{16384}, mix.convert(data, 1));
            assertArrayEquals(new short[]{0}, mix.convert(null, 3));
        }
    }
}
