package com.example.knifeclient;

public final class Zoom {
    public static boolean active = false;
    private static double current = 1.0;
    private static long last = System.nanoTime();

    /** Current zoom factor (1.0 = no zoom). Smoothly eases toward the target. */
    public static double factor() {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - last) / 1.0e9);
        last = now;
        double target = (Modules.ZOOM.enabled && active) ? Modules.ZOOM_LEVEL.value : 1.0;
        if (Modules.ZOOM_SMOOTH.value) {
            current += (target - current) * Math.min(1.0, dt * 14.0);
        } else {
            current = target;
        }
        return current;
    }
}
