package com.example.knifeclient;

/** Common look/position settings for every HUD element. */
public final class HudStyle {
    public final Setting.Num x, y, scale;
    public final Setting.Bool bg;
    public final ColorGroup color;

    public HudStyle(ClientModule m, double px, double py) {
        x = m.add(new Setting.Num("x", "Position X", px, 0, 100, 1, "%"));
        y = m.add(new Setting.Num("y", "Position Y", py, 0, 100, 1, "%"));
        scale = m.add(new Setting.Num("scale", "Size", 1.0, 0.5, 3.0, 0.1, "x"));
        bg = m.add(new Setting.Bool("bg", "Background", true));
        color = new ColorGroup(m, 0, 0, 1.0, false);
    }
}
