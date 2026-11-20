package br.birdteacher.app;

import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.*;

import static org.junit.jupiter.api.Assertions.*;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

class HotkeyTest {
    @Test
    void repeatedKeydownOnlyTogglesOnceUntilRelease() {
        var count = new AtomicInteger();
        var key = new Hotkey(count::incrementAndGet);
        var partial = new NativeKeyEvent(NATIVE_KEY_PRESSED, CTRL_L_MASK, 0, VC_F9, CHAR_UNDEFINED);
        var chord =
                new NativeKeyEvent(
                        NATIVE_KEY_PRESSED, CTRL_R_MASK | SHIFT_L_MASK, 0, VC_F9, CHAR_UNDEFINED);
        key.nativeKeyPressed(partial);
        assertEquals(0, count.get());
        key.nativeKeyPressed(chord);
        key.nativeKeyPressed(chord);
        assertEquals(1, count.get());
        key.nativeKeyReleased(new NativeKeyEvent(NATIVE_KEY_RELEASED, 0, 0, VC_F9, CHAR_UNDEFINED));
        key.nativeKeyPressed(chord);
        assertEquals(2, count.get());
    }

    @Test
    void changingShortcutReplacesThePreviousBinding() {
        var count = new AtomicInteger();
        var key = new Hotkey(count::incrementAndGet);
        key.use(Hotkey.Binding.LETTER);
        key.nativeKeyPressed(
                new NativeKeyEvent(
                        NATIVE_KEY_PRESSED, CTRL_L_MASK | SHIFT_L_MASK, 0, VC_F9, CHAR_UNDEFINED));
        assertEquals(0, count.get());
        key.nativeKeyPressed(
                new NativeKeyEvent(
                        NATIVE_KEY_PRESSED, CTRL_L_MASK | ALT_L_MASK, 0, VC_B, CHAR_UNDEFINED));
        assertEquals(1, count.get());
    }
}
