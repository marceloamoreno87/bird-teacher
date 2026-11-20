package br.birdteacher.infra;

import com.sun.jna.*;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.ptr.PointerByReference;
import java.util.ArrayDeque;
import java.util.Arrays;

/** Owns the WASAPI COM references for one thread, in acquisition order. */
final class ComScope implements AutoCloseable {
    private final ArrayDeque<Pointer> owned = new ArrayDeque<>();
    ComScope() { check("CoInitializeEx", Ole32.INSTANCE.CoInitializeEx(null, 0).intValue()); }

    Pointer enumerator() {
        var result = new PointerByReference();
        check("MMDeviceEnumerator", Ole32.INSTANCE.CoCreateInstance(
                new GUID("{BCDE0395-E52F-467C-8E3D-C4579291692E}"), null, 1,
                new GUID("{A95664D2-9614-4F35-A746-DE8DB63617E6}"), result).intValue());
        return own(result);
    }

    Pointer own(PointerByReference result) {
        owned.push(result.getValue());
        return result.getValue();
    }

    static int call(Pointer object, int slot, Object... arguments) {
        var method = object.getPointer(0).getPointer((long) slot * Native.POINTER_SIZE);
        var args = Arrays.copyOf(arguments, arguments.length + 1);
        System.arraycopy(arguments, 0, args, 1, arguments.length);
        args[0] = object;
        return Function.getFunction(method, Function.ALT_CONVENTION).invokeInt(args);
    }

    static void check(String operation, int hresult) {
        if (hresult < 0) throw new IllegalStateException("%s falhou (HRESULT 0x%08x).".formatted(operation, hresult));
    }

    @Override public void close() {
        while (!owned.isEmpty()) call(owned.pop(), 2); // IUnknown.Release
        Ole32.INSTANCE.CoUninitialize();
    }
}
