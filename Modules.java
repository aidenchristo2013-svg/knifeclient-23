package com.example.knifeclient;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class Modules {
    public static final List<ClientModule> ALL = new ArrayList<>();

    public static final ClientModule GLOW, HEADLESS, KORBLOX, KNIVES, HEADSIZE;
    public static final ClientModule FPS, CPS, COORDS, PING, CLOCK, KEYS, ARMOR, ZOOM, SPRINT, UI;

    public static final ColorGroup GLOW_COLOR, KORBLOX_COLOR, KNIVES_COLOR, UI_ACCENT;
    public static final Setting.Bool KNIVES_ANIM, KNIVES_DRAW, KNIVES_SWING, KNIVES_SWAY;
    public static final Setting.Bool GLOW_PULSE, HEADLESS_HAT, KNIVES_THIRD, CPS_RIGHT, COORDS_FACING;
    public static final Setting.Bool CLOCK_SEC, KEYS_MOUSE, KEYS_SPACE, ARMOR_TEXT, ZOOM_SMOOTH, SPRINT_STATUS, UI_ANIM;
    public static final Setting.Num KNIVES_ANIM_SPEED;
    public static final Setting.Num GLOW_PULSE_SPEED, HEAD_SIZE, COORDS_DEC, ZOOM_LEVEL, UI_SPEED, UI_DIM;
    public static final Setting.Choice UI_THEME;
    public static final Setting.Choice KORBLOX_MODE, KNIVES_KNIFE, CLOCK_FMT, ARMOR_LAYOUT;
    public static final HudStyle FPS_STYLE, CPS_STYLE, COORDS_STYLE, PING_STYLE, CLOCK_STYLE, KEYS_STYLE, ARMOR_STYLE, SPRINT_STYLE;

    private static ClientModule reg(ClientModule m) {
        ALL.add(m);
        return m;
    }

    static {
        // ---- cosmetics ----
        GLOW = reg(new ClientModule("glow", "Glow Skin", "Glowing outline around you", true, false).cat("Cosmetics"));
        GLOW_COLOR = new ColorGroup(GLOW, 200, 0.85, 1.0, false);
        GLOW_PULSE = GLOW.add(new Setting.Bool("pulse", "Pulse", false));
        GLOW_PULSE_SPEED = GLOW.add(new Setting.Num("pspeed", "Pulse Speed", 1.0, 0.2, 5.0, 0.1, "x"));

        HEADLESS = reg(new ClientModule("headless", "Headless", "Hide your head", true, false).cat("Cosmetics"));
        HEADLESS_HAT = HEADLESS.add(new Setting.Bool("hat", "Hide Hat Layer Too", true));

        KORBLOX = reg(new ClientModule("korblox", "Korblox Leg", "Hidden or dark-red right leg", true, false).cat("Cosmetics"));
        KORBLOX_MODE = KORBLOX.add(new Setting.Choice("mode", "Mode", 1, "Hidden", "Tinted"));
        KORBLOX_COLOR = new ColorGroup(KORBLOX, 0, 0.9, 0.35, false);

        KNIVES = reg(new ClientModule("knives", "Knives", "Knife in your empty hand", true, true).cat("Knives"));
        KNIVES_KNIFE = KNIVES.add(new Setting.Choice("knife", "Knife", 0,
                Knives.NAMES));
        KNIVES_ANIM = KNIVES.add(new Setting.Bool("anim", "Animations", true));
        KNIVES_DRAW = KNIVES.add(new Setting.Bool("draw", "Draw Animation", true));
        KNIVES_SWING = KNIVES.add(new Setting.Bool("swing", "Custom Swing", true));
        KNIVES_SWAY = KNIVES.add(new Setting.Bool("sway", "Idle + Walk Sway", true));
        KNIVES_ANIM_SPEED = KNIVES.add(new Setting.Num("aspeed", "Animation Speed", 1.0, 0.5, 2.0, 0.05, "x"));
        KNIVES_THIRD = KNIVES.add(new Setting.Bool("third", "Show In Third Person", true));
        KNIVES_COLOR = new ColorGroup(KNIVES, 0, 0.0, 1.0, false);

        HEADSIZE = reg(new ClientModule("headsize", "Head Size", "Make your head bigger or smaller", true, false).cat("Cosmetics"));
        HEAD_SIZE = HEADSIZE.add(new Setting.Num("size", "Head Size", 1.5, 0.5, 2.5, 0.05, "x"));

        // ---- HUD ----
        FPS = reg(new ClientModule("fps", "FPS Counter", "Shows frames per second", true, false).cat("HUD"));
        FPS_STYLE = new HudStyle(FPS, 1, 1);

        CPS = reg(new ClientModule("cps", "CPS Counter", "Clicks per second", true, false).cat("HUD"));
        CPS_RIGHT = CPS.add(new Setting.Bool("right", "Show Right Click", true));
        CPS_STYLE = new HudStyle(CPS, 1, 7);

        COORDS = reg(new ClientModule("coords", "Coordinates", "Position and direction", true, false).cat("HUD"));
        COORDS_DEC = COORDS.add(new Setting.Num("dec", "Decimals", 0, 0, 2, 1, ""));
        COORDS_FACING = COORDS.add(new Setting.Bool("facing", "Show Direction", true));
        COORDS_STYLE = new HudStyle(COORDS, 1, 13);

        PING = reg(new ClientModule("ping", "Ping", "Server latency", true, false).cat("HUD"));
        PING_STYLE = new HudStyle(PING, 1, 19);

        CLOCK = reg(new ClientModule("clock", "Clock", "Real-world time", true, false).cat("HUD"));
        CLOCK_FMT = CLOCK.add(new Setting.Choice("fmt", "Format", 0, "24 Hour", "12 Hour"));
        CLOCK_SEC = CLOCK.add(new Setting.Bool("sec", "Show Seconds", false));
        CLOCK_STYLE = new HudStyle(CLOCK, 1, 25);

        KEYS = reg(new ClientModule("keys", "Keystrokes", "WASD and mouse display", true, false).cat("HUD"));
        KEYS_MOUSE = KEYS.add(new Setting.Bool("mouse", "Show Mouse Buttons", true));
        KEYS_SPACE = KEYS.add(new Setting.Bool("space", "Show Space Bar", true));
        KEYS_STYLE = new HudStyle(KEYS, 1, 40);

        ARMOR = reg(new ClientModule("armor", "Armor HUD", "Armor and held item", true, false).cat("HUD"));
        ARMOR_LAYOUT = ARMOR.add(new Setting.Choice("layout", "Layout", 0, "Vertical", "Horizontal"));
        ARMOR_TEXT = ARMOR.add(new Setting.Bool("text", "Durability Numbers", true));
        ARMOR_STYLE = new HudStyle(ARMOR, 99, 45);

        // ---- utility ----
        ZOOM = reg(new ClientModule("zoom", "Zoom", "Hold C to zoom in", true, false).cat("Utility"));
        ZOOM_LEVEL = ZOOM.add(new Setting.Num("level", "Zoom Level", 4.0, 1.5, 10.0, 0.1, "x"));
        ZOOM_SMOOTH = ZOOM.add(new Setting.Bool("smooth", "Smooth Zoom", true));

        SPRINT = reg(new ClientModule("sprint", "Toggle Sprint", "Auto-sprint while walking forward", true, false).cat("Utility"));
        SPRINT_STATUS = SPRINT.add(new Setting.Bool("status", "Show Status", true));
        SPRINT_STYLE = new HudStyle(SPRINT, 50, 86);

        UI = reg(new ClientModule("ui", "Interface", "Menu colours and animations", false, true).cat("Interface"));
        UI_THEME = UI.add(new Setting.Choice("theme", "Theme", 1, "Custom Colour", "Ocean", "Crimson", "Emerald", "Violet", "Sunset"));
        UI_ACCENT = new ColorGroup(UI, 190, 0.7, 1.0, false);
        UI_ANIM = UI.add(new Setting.Bool("anim", "Animations", true));
        UI_SPEED = UI.add(new Setting.Num("speed", "Animation Speed", 1.0, 0.5, 2.5, 0.1, "x"));
        UI_DIM = UI.add(new Setting.Num("dim", "Background Dim", 0.45, 0.0, 0.85, 0.05, ""));
    }

    private static final int[] THEME_A = {0, 0x2FC8FF, 0xFF3B54, 0x2BE08A, 0xA066FF, 0xFF8A3D};
    private static final int[] THEME_B = {0, 0x6A5BFF, 0xFF9A3B, 0x2FA8FF, 0xFF4FB8, 0xFF3B7A};

    /** Main menu accent colour. */
    public static int accent() {
        int t = UI_THEME.index;
        return t == 0 ? UI_ACCENT.rgb(0.0) : THEME_A[t];
    }

    /** Second menu colour (used for gradients). */
    public static int accent2() {
        int t = UI_THEME.index;
        if (t == 0) return ColorGroup.hsb((float) (UI_ACCENT.hue.value / 360.0 + 0.12), (float) UI_ACCENT.sat.value, (float) UI_ACCENT.bri.value);
        return THEME_B[t];
    }

    /** Colour of the glow outline (pulsing if enabled). */
    public static int glowColor() {
        int rgb = GLOW_COLOR.rgb(0);
        if (GLOW_PULSE.value) {
            double t = System.currentTimeMillis() / 1000.0;
            double m = 0.55 + 0.45 * Math.sin(t * GLOW_PULSE_SPEED.value * Math.PI * 2);
            int r = (int) (((rgb >> 16) & 255) * m);
            int g = (int) (((rgb >> 8) & 255) * m);
            int b = (int) ((rgb & 255) * m);
            return (r << 16) | (g << 8) | b;
        }
        return rgb;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("knifeclient.properties");
    }

    public static void load() {
        try {
            Path p = path();
            if (!Files.exists(p)) return;
            Properties pr = new Properties();
            try (InputStream in = Files.newInputStream(p)) { pr.load(in); }
            for (ClientModule m : ALL) {
                String e = pr.getProperty(m.id + ".enabled");
                if (e != null && m.toggleable) m.enabled = Boolean.parseBoolean(e);
                for (Setting s : m.settings) {
                    String v = pr.getProperty(m.id + "." + s.id);
                    if (v == null) continue;
                    try { s.load(v); } catch (Exception ignored) { }
                }
            }
        } catch (Exception ignored) { }
    }

    public static void save() {
        try {
            Properties pr = new Properties();
            for (ClientModule m : ALL) {
                pr.setProperty(m.id + ".enabled", Boolean.toString(m.enabled));
                for (Setting s : m.settings) pr.setProperty(m.id + "." + s.id, s.save());
            }
            try (OutputStream out = Files.newOutputStream(path())) { pr.store(out, "Knife Client"); }
        } catch (Exception ignored) { }
    }
}
