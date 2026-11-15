package com.example.knifeclient;

/** Five settings that together make a customizable colour (with optional rainbow). */
public final class ColorGroup {
    public final Setting.Bool rainbow;
    public final Setting.Num speed, hue, sat, bri;

    public ColorGroup(ClientModule m, double h, double s, double b, boolean rainbowDefault) {
        rainbow = m.add(new Setting.Bool("rainbow", "Rainbow", rainbowDefault));
        speed = m.add(new Setting.Num("rspeed", "Rainbow Speed", 1.0, 0.1, 5.0, 0.1, "x"));
        hue = m.add(new Setting.Num("hue", "Hue", h, 0, 360, 1, ""));
        sat = m.add(new Setting.Num("sat", "Saturation", s, 0, 1, 0.01, ""));
        bri = m.add(new Setting.Num("bri", "Brightness", b, 0, 1, 0.01, ""));
        hue.kind = Setting.Num.HUE;
        sat.kind = Setting.Num.SAT;
        bri.kind = Setting.Num.BRI;
        hue.group = this;
        sat.group = this;
        bri.group = this;
    }

    /** RGB colour (no alpha). phase offsets the rainbow so different things can differ. */
    public int rgb(double phase) {
        float h = (float) (hue.value / 360.0);
        float s = (float) sat.value;
        if (rainbow.value) {
            double t = System.currentTimeMillis() / 1000.0;
            h = (float) ((t * 0.2 * speed.value + phase) % 1.0);
            s = (float) Math.max(sat.value, 0.65);
        }
        return hsb(h, s, (float) bri.value);
    }

    public static int hsb(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float x = h * 6f;
        int i = (int) x;
        float f = x - i;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }
}
