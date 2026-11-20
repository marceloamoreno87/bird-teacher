package br.birdteacher.infra;

import br.birdteacher.domain.*;
import com.sun.jna.*;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.ptr.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static br.birdteacher.infra.ComScope.*;

public final class Wasapi implements AudioInput {
    private static final GUID AUDIO_CLIENT = new GUID("{1CB9AD4C-DBFA-4c32-B178-C2F568A703B2}");
    private static final GUID CAPTURE_CLIENT = new GUID("{C8ADBD64-E71E-48a0-A4DE-185C395CD317}");

    @Override public List<Device> devices() {
        List<Device> devices = new ArrayList<>();
        try (var scope = new ComScope()) {
            Pointer enumerator = scope.enumerator();
            for (Source source : Source.values()) enumerate(scope, enumerator, source, devices);
        }
        return devices;
    }

    private void enumerate(ComScope scope, Pointer enumerator, Source source, List<Device> devices) {
        var out = new PointerByReference();
        int flow = source == Source.MIC ? 1 : 0;
        check("EnumAudioEndpoints", call(enumerator, 3, flow, 1, out));
        Pointer collection = scope.own(out);
        var count = new IntByReference();
        check("GetCount", call(collection, 3, count));
        for (int index = 0; index < count.getValue(); index++) {
            check("Item", call(collection, 4, index, out));
            Pointer device = scope.own(out);
            check("GetId", call(device, 5, out));
            String id = out.getValue().getWideString(0);
            Ole32.INSTANCE.CoTaskMemFree(out.getValue());
            devices.add(new Device(id, friendlyName(scope, device), source));
        }
    }

    private String friendlyName(ComScope scope, Pointer device) {
        var out = new PointerByReference();
        check("OpenPropertyStore", call(device, 4, 0, out));
        Pointer store = scope.own(out);
        // PKEY_Device_FriendlyName, VT_LPWSTR stored at byte 8 of PROPVARIANT.
        var fmt = new GUID("{A45C254E-DF1C-4EFD-8020-67D146A850E0}");
        fmt.write();
        try (var key = new Memory(20); var value = new Memory(24)) {
            key.write(0, fmt.getPointer().getByteArray(0, 16), 0, 16);
            key.setInt(16, 14);
            value.clear();
            check("GetValue", call(store, 5, key, value));
            if (value.getShort(0) != 31) return "Dispositivo de áudio";
            Pointer name = value.getPointer(8);
            try { return name.getWideString(0); }
            finally { Ole32.INSTANCE.CoTaskMemFree(name); }
        }
    }

    @Override public void capture(Device device, AtomicBoolean stopped, Listener listener) throws Exception {
        try (var scope = new ComScope()) {
            var out = new PointerByReference();
            var enumerator = scope.enumerator();
            if (device.followsWindows()) {
                int flow = device.source() == Source.MIC ? 1 : 0;
                check("Dispositivo padrão do Windows", call(enumerator, 4, flow, 1, out)); // eMultimedia
            } else {
                check("GetDevice", call(enumerator, 5, new WString(device.id()), out));
            }
            Pointer endpoint = scope.own(out);
            check("Activate", call(endpoint, 3, AUDIO_CLIENT, 1, null, out));
            Pointer client = scope.own(out);
            check("GetMixFormat", call(client, 8, out));
            Pointer format = out.getValue();
            MixFormat converter;
            try {
                converter = new MixFormat(format);
                int flags = device.source() == Source.OUTPUT ? 0x20000 : 0; // AUDCLNT_STREAMFLAGS_LOOPBACK
                check("Initialize", call(client, 3, 0, flags, 2_000_000L, 0L, format, null));
            } finally { Ole32.INSTANCE.CoTaskMemFree(format); }
            check("GetService", call(client, 14, CAPTURE_CLIENT, out));
            Pointer reader = scope.own(out);
            listener.ready();
            if (stopped.get()) return;
            check("Start", call(client, 10));
            try { read(reader, converter, stopped, listener); }
            finally { call(client, 11); }
        }
    }

    private void read(Pointer reader, MixFormat converter, AtomicBoolean stopped, Listener listener) throws Exception {
        var frames = new IntByReference();
        var flags = new IntByReference();
        var data = new PointerByReference();
        var qpc = new LongByReference();
        long clockOffset = Long.MIN_VALUE;
        while (!stopped.get()) {
            check("GetBuffer", call(reader, 3, data, frames, flags, null, qpc));
            int length = frames.getValue();
            long now = System.nanoTime();
            if (length == 0) {
                listener.idle(now);
                Thread.sleep(8);
                continue;
            }
            short[] pcm;
            try { pcm = converter.convert((flags.getValue() & 2) == 0 ? data.getValue() : null, length); }
            finally { check("ReleaseBuffer", call(reader, 4, length)); }
            long stamp = now - converter.duration(length);
            if ((flags.getValue() & 4) == 0) {
                long counter = qpc.getValue() * 100L;
                if (clockOffset == Long.MIN_VALUE) clockOffset = stamp - counter;
                stamp = counter + clockOffset;
            }
            listener.samples(stamp, pcm);
        }
    }
}
