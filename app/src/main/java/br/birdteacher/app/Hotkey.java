package br.birdteacher.app;

import com.github.kwhat.jnativehook.*;
import com.github.kwhat.jnativehook.keyboard.*;

import java.util.logging.*;

final class Hotkey implements NativeKeyListener {
    private final Runnable toggle;

    enum Binding {
        DEFAULT("Ctrl + Shift + F9", NativeKeyEvent.VC_F9, NativeInputEvent.SHIFT_MASK),
        F10("Ctrl + Shift + F10", NativeKeyEvent.VC_F10, NativeInputEvent.SHIFT_MASK),
        LETTER("Ctrl + Alt + B", NativeKeyEvent.VC_B, NativeInputEvent.ALT_MASK);
        final String label;
        final int key, modifier;

        Binding(String label, int key, int modifier) {
            this.label = label;
            this.key = key;
            this.modifier = modifier;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private Binding binding = Binding.DEFAULT;
    private boolean pressed;

    synchronized void use(Binding value) {
        binding = value;
        pressed = false;
    }

    Hotkey(Runnable toggle) {
        this.toggle = toggle;
    }

    void register() throws NativeHookException {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackageName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(this);
    }

    void close() throws NativeHookException {
        GlobalScreen.removeNativeKeyListener(this);
        if (GlobalScreen.isNativeHookRegistered()) GlobalScreen.unregisterNativeHook();
    }

    @Override
    public synchronized void nativeKeyPressed(NativeKeyEvent e) {
        int mods = e.getModifiers();
        if (e.getKeyCode() == binding.key
                && !pressed
                && (mods & NativeInputEvent.CTRL_MASK) != 0
                && (mods & binding.modifier) != 0) {
            pressed = true;
            toggle.run();
        }
    }

    @Override
    public synchronized void nativeKeyReleased(NativeKeyEvent e) {
        if (e.getKeyCode() == binding.key) pressed = false;
    }
}
