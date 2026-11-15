package com.example.knifeclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Locale;

public final class Hud {
    private static final ArrayDeque<Long> LMB = new ArrayDeque<>();
    private static final ArrayDeque<Long> RMB = new ArrayDeque<>();
    private static boolean lastL, lastR;

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        pollClicks(mc);

        if (Modules.FPS.enabled) {
            textBox(ctx, mc, Modules.FPS_STYLE, "FPS: " + mc.getCurrentFps());
        }
        if (Modules.CPS.enabled) {
            String t = "CPS: " + LMB.size();
            if (Modules.CPS_RIGHT.value) t += " | " + RMB.size();
            textBox(ctx, mc, Modules.CPS_STYLE, t);
        }
        if (Modules.COORDS.enabled) {
            int d = (int) Modules.COORDS_DEC.value;
            String f = "%." + d + "f";
            String t = String.format(Locale.ROOT, "XYZ: " + f + " " + f + " " + f,
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            if (Modules.COORDS_FACING.value) {
                String dir = mc.player.getHorizontalFacing().asString();
                t += "  " + Character.toUpperCase(dir.charAt(0)) + dir.substring(1);
            }
            textBox(ctx, mc, Modules.COORDS_STYLE, t);
        }
        if (Modules.PING.enabled) {
            String t;
            if (mc.isInSingleplayer() || mc.getNetworkHandler() == null) {
                t = "Ping: -- (singleplayer)";
            } else {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                t = "Ping: " + (e == null ? "--" : e.getLatency() + "ms");
            }
            textBox(ctx, mc, Modules.PING_STYLE, t);
        }
        if (Modules.CLOCK.enabled) {
            boolean h12 = Modules.CLOCK_FMT.index == 1;
            boolean sec = Modules.CLOCK_SEC.value;
            String pattern = h12 ? (sec ? "h:mm:ss a" : "h:mm a") : (sec ? "HH:mm:ss" : "HH:mm");
            textBox(ctx, mc, Modules.CLOCK_STYLE, LocalTime.now().format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)));
        }
        if (Modules.SPRINT.enabled && Modules.SPRINT_STATUS.value) {
            textBox(ctx, mc, Modules.SPRINT_STYLE, mc.player.isSprinting() ? "[Sprinting]" : "[Walking]");
        }
        if (Modules.KEYS.enabled) keys(ctx, mc);
        if (Modules.ARMOR.enabled) armor(ctx, mc);
    }

    private static void pollClicks(MinecraftClient mc) {
        long win = mc.getWindow().getHandle();
        boolean noScreen = mc.currentScreen == null;
        boolean l = noScreen && GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = noScreen && GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (l && !lastL) LMB.addLast(now);
        if (r && !lastR) RMB.addLast(now);
        lastL = l;
        lastR = r;
        while (!LMB.isEmpty() && now - LMB.peekFirst() > 1000) LMB.pollFirst();
        while (!RMB.isEmpty() && now - RMB.peekFirst() > 1000) RMB.pollFirst();
    }

    private static void begin(DrawContext ctx, HudStyle st, int w, int h) {
        float s = (float) st.scale.value;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        float px = (sw - w * s) * (float) (st.x.value / 100.0);
        float py = (sh - h * s) * (float) (st.y.value / 100.0);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(px, py, 0f);
        ctx.getMatrices().scale(s, s, 1f);
    }

    private static void end(DrawContext ctx) {
        ctx.getMatrices().pop();
    }

    private static void textBox(DrawContext ctx, MinecraftClient mc, HudStyle st, String text) {
        TextRenderer tr = mc.textRenderer;
        int w = tr.getWidth(text) + 8;
        int h = 14;
        begin(ctx, st, w, h);
        if (st.bg.value) ctx.fill(0, 0, w, h, 0x90000000);
        ctx.drawText(tr, text, 4, 3, 0xFF000000 | st.color.rgb(0.0), true);
        end(ctx);
    }

    private static void keys(DrawContext ctx, MinecraftClient mc) {
        HudStyle st = Modules.KEYS_STYLE;
        TextRenderer tr = mc.textRenderer;
        int k = 18, g = 2;
        boolean mouse = Modules.KEYS_MOUSE.value;
        boolean space = Modules.KEYS_SPACE.value;
        int w = 3 * k + 2 * g;
        int h = 2 * k + g + (mouse ? k + g : 0) + (space ? 10 + g : 0);
        begin(ctx, st, w, h);
        int rgb = st.color.rgb(0.0);
        boolean bg = st.bg.value;
        key(ctx, tr, k + g, 0, k, k, "W", mc.options.forwardKey.isPressed(), rgb, bg);
        key(ctx, tr, 0, k + g, k, k, "A", mc.options.leftKey.isPressed(), rgb, bg);
        key(ctx, tr, k + g, k + g, k, k, "S", mc.options.backKey.isPressed(), rgb, bg);
        key(ctx, tr, 2 * (k + g), k + g, k, k, "D", mc.options.rightKey.isPressed(), rgb, bg);
        int y = 2 * (k + g);
        if (mouse) {
            int half = (w - g) / 2;
            key(ctx, tr, 0, y, half, k, "LMB", mc.options.attackKey.isPressed(), rgb, bg);
            key(ctx, tr, half + g, y, w - half - g, k, "RMB", mc.options.useKey.isPressed(), rgb, bg);
            y += k + g;
        }
        if (space) {
            key(ctx, tr, 0, y, w, 10, "-----", mc.options.jumpKey.isPressed(), rgb, bg);
        }
        end(ctx);
    }

    private static void key(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                            String label, boolean down, int rgb, boolean bg) {
        int fill = down ? (0xB0000000 | rgb) : (bg ? 0x90000000 : 0x40000000);
        ctx.fill(x, y, x + w, y + h, fill);
        int tc = down ? 0xFF000000 : (0xFF000000 | rgb);
        ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2, tc, false);
    }

    private static void armor(DrawContext ctx, MinecraftClient mc) {
        HudStyle st = Modules.ARMOR_STYLE;
        TextRenderer tr = mc.textRenderer;
        ItemStack[] items = {
                mc.player.getEquippedStack(EquipmentSlot.HEAD),
                mc.player.getEquippedStack(EquipmentSlot.CHEST),
                mc.player.getEquippedStack(EquipmentSlot.LEGS),
                mc.player.getEquippedStack(EquipmentSlot.FEET),
                mc.player.getMainHandStack()
        };
        boolean vertical = Modules.ARMOR_LAYOUT.index == 0;
        boolean text = Modules.ARMOR_TEXT.value;
        int cell = 18;
        int tw = (vertical && text) ? 26 : 0;
        int w = vertical ? cell + tw + 4 : cell * 5 + 4;
        int h = vertical ? cell * 5 + 4 : cell + 4;
        begin(ctx, st, w, h);
        if (st.bg.value) ctx.fill(0, 0, w, h, 0x90000000);
        int rgb = 0xFF000000 | st.color.rgb(0.0);
        for (int i = 0; i < items.length; i++) {
            ItemStack s = items[i];
            if (s.isEmpty()) continue;
            int x = vertical ? 2 : 2 + i * cell;
            int y = vertical ? 2 + i * cell : 2;
            ctx.drawItem(s, x, y);
            ctx.drawStackOverlay(tr, s, x, y);
            if (vertical && text && s.isDamageable()) {
                ctx.drawText(tr, String.valueOf(s.getMaxDamage() - s.getDamage()), x + cell + 1, y + 5, rgb, true);
            }
        }
        end(ctx);
    }
}
