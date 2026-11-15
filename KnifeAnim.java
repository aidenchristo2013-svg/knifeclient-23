package com.example.knifeclient;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/**
 * First-person knife animations: draw, inspect, swing, idle sway and walk sway.
 * Everything is built from small translations and rotations applied around the hand position,
 * so the knife keeps its normal place on screen and just moves like it is being handled.
 */
public final class KnifeAnim {
    private KnifeAnim() { }

    // where the hand sits on screen (view space); rotations happen around this point
    private static final double PX = 0.45, PY = -0.42, PZ = -0.75;

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float smooth(float t) { t = clamp01(t); return t * t * (3f - 2f * t); }
    private static float easeOut(float t) { t = clamp01(t); float u = 1f - t; return 1f - u * u * u; }
    private static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.5f, c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }
    private static float seg(float t, float a, float b) { return smooth((t - a) / (b - a)); }

    private static void rx(MatrixStack m, float deg) { if (deg != 0f) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(deg)); }
    private static void ry(MatrixStack m, float deg) { if (deg != 0f) m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(deg)); }
    private static void rz(MatrixStack m, float deg) { if (deg != 0f) m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(deg)); }

    /** Applies all animations to the matrix stack. Returns nothing; call push()/pop() around it yourself. */
    public static void apply(MatrixStack m, int knife, float swing, AbstractClientPlayerEntity player) {
        if (!Modules.KNIVES_ANIM.value) return;
        int style = Knives.STYLE[Math.floorMod(knife, Knives.STYLE.length)];
        float speed = (float) Modules.KNIVES_ANIM_SPEED.value;
        long now = System.nanoTime();
        double secs = now / 1.0e9;

        m.translate(PX, PY, PZ);

        // ---------------------------------------------------- idle + walk sway
        if (Modules.KNIVES_SWAY.value) {
            rz(m, (float) Math.sin(secs * 1.3) * 0.9f);
            rx(m, (float) Math.sin(secs * 0.9 + 1.0) * 0.6f);
            m.translate(0.0, Math.sin(secs * 1.7) * 0.004, 0.0);
            Vec3d v = player.getVelocity();
            float hs = (float) Math.min(1.0, Math.hypot(v.x, v.z) / 0.22);
            if (hs > 0.01f) {
                float w = (float) Math.sin(secs * 7.5 * (player.isSprinting() ? 1.25 : 1.0));
                m.translate(w * 0.012 * hs, -Math.abs(w) * 0.03 * hs, 0.0);
                rz(m, w * 5.0f * hs);
                ry(m, w * 3.0f * hs);
            }
        }

        // ---------------------------------------------------- draw
        if (Modules.KNIVES_DRAW.value && Knives.drawStart != 0L) {
            float t = (float) ((now - Knives.drawStart) / 1.0e9) * speed / 0.6f;
            if (t < 1f) {
                float e = easeOutBack(t);
                float k = 1f - e;
                float roll = style == 0 ? 360f : (style == 1 ? 180f : 0f);
                m.translate(0.0, -k * 0.38, k * 0.18);
                rz(m, k * -32f + (1f - easeOut(t)) * roll);
                rx(m, k * 42f);
                ry(m, k * 28f);
            }
        }

        // ---------------------------------------------------- inspect
        if (Knives.inspectStart != 0L) {
            if (swing > 0f) {
                Knives.inspectStart = 0L;
            } else {
                float t = (float) ((now - Knives.inspectStart) / 1.0e9) * speed / 2.8f;
                if (t >= 1f) {
                    Knives.inspectStart = 0L;
                } else {
                    float env = seg(t, 0f, 0.16f) * (1f - seg(t, 0.86f, 1f));
                    float p = seg(t, 0.16f, 0.86f);
                    m.translate(-0.10 * env, 0.10 * env, 0.28 * env);
                    ry(m, 28f * env);
                    rx(m, -18f * env);
                    switch (style) {
                        case 0 -> {            // karambit: finger spin
                            rz(m, 720f * p);
                            ry(m, (float) Math.sin(p * Math.PI * 2) * 35f * env);
                        }
                        case 1 -> {            // folding knives: flip
                            rz(m, 720f * p);
                            rx(m, (float) Math.sin(p * Math.PI * 4) * 25f * env);
                        }
                        case 2 -> {            // pointy knives: turn over, show the other side
                            ry(m, (float) Math.sin(p * Math.PI) * 160f);
                            rz(m, (float) Math.sin(p * Math.PI * 2) * 18f * env);
                        }
                        default -> {           // heavy knives: slow turntable
                            ry(m, 360f * p);
                            rx(m, (float) Math.sin(p * Math.PI * 2) * 14f * env);
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------- swing (replaces the vanilla swing)
        if (Modules.KNIVES_SWING.value && swing > 0f) {
            float s = clamp01(swing);
            // f: wind up (-1), strike through (+1), recover (0)
            float f;
            if (s < 0.22f) f = -easeOut(s / 0.22f);
            else if (s < 0.50f) f = -1f + 2f * smooth((s - 0.22f) / 0.28f);
            else f = 1f - smooth((s - 0.50f) / 0.50f);
            float w = (float) Math.sin(s * Math.PI);
            switch (style) {
                case 0 -> {                // hook: sweeping slash across
                    m.translate(f * -0.14, f * 0.04, -w * 0.20);
                    rz(m, f * 34f);
                    ry(m, f * -20f);
                    rx(m, w * -14f);
                }
                case 1 -> {                // flip: quick wrist flick
                    m.translate(f * -0.08, w * 0.04, -w * 0.18);
                    rz(m, f * 26f);
                    rx(m, f * -24f);
                    ry(m, f * -12f);
                }
                case 2 -> {                // stab: pull back then thrust
                    m.translate(f * 0.02, f * -0.04, f * -0.30 - w * 0.06);
                    rx(m, f * -16f);
                    ry(m, f * 6f);
                }
                default -> {               // chop: big overhead arc
                    m.translate(f * -0.10, -f * 0.06, -w * 0.22);
                    rx(m, f * -48f);
                    rz(m, f * 22f);
                    ry(m, f * -10f);
                }
            }
        }

        m.translate(-PX, -PY, -PZ);
    }
}
