package com.example.knifeclient;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Knife Client menu. Pages on the left, search at the top, animated background.
 * Left click a card = toggle, right click = settings drawer. On the Knives page click a knife to pick it.
 */
public class ModuleScreen extends Screen {
    private record Card(ClientModule m, int x, int y, int w, int h) { }
    private record Hit(Setting s, int x, int y, int w, int h) { }
    private record Tab(String name, String icon) { }

    private static final Tab[] TABS = {
            new Tab("Home", "H"), new Tab("Cosmetics", "C"), new Tab("Knives", "K"),
            new Tab("HUD", "#"), new Tab("Utility", "U"), new Tab("Interface", "I")};
    private static int lastTab = 0;

    private final long openTime = System.currentTimeMillis();
    private long lastFrame = openTime;
    private float dt = 0.016f;
    private final Map<Object, Float> anims = new HashMap<>();
    private final List<Card> cards = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<int[]> tabRects = new ArrayList<>();
    private final List<int[]> tileRects = new ArrayList<>();

    private int tab = lastTab;
    private long tabTime = openTime;
    private String query = "";
    private ClientModule selected;       // module shown in the settings drawer
    private long selectedTime;
    private int scroll, maxScroll;
    private int pageScroll, pageMax;
    private Hit dragging;

    // layout of the current frame
    private int wx, wy, ww, wh, sideW, cx0, cy0, cw0, ch0;
    private int drawerX, drawerY, drawerW, drawerH;
    private int setX, setY, setW, setH;      // area that settings hits live in
    private int closeX, resetX, resetW, drawerHeadY;

    public ModuleScreen() {
        super(Text.literal("Knife Client"));
    }

    // ------------------------------------------------------------ helpers
    private static int argb(int a, int rgb) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Filled rectangle with the four corner pixels cut, for a softly rounded look. */
    private static void rrect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private float anim(Object key, float target, float init) {
        float cur = anims.getOrDefault(key, init);
        if (!Modules.UI_ANIM.value) {
            cur = target;
        } else {
            cur += (target - cur) * Math.min(1f, dt * 16f * (float) Modules.UI_SPEED.value);
        }
        anims.put(key, cur);
        return cur;
    }

    private float intro(long now, int index) {
        if (!Modules.UI_ANIM.value) return 1f;
        return ease((now - openTime - index * 22L) / (360f / (float) Modules.UI_SPEED.value));
    }

    private static int rowH(Setting s) {
        return (s instanceof Setting.Num) ? 28 : 18;
    }

    private static float hash(int i, int salt) {
        int h = i * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535f;
    }

    private List<ClientModule> modulesFor(String category) {
        List<ClientModule> out = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT);
        for (ClientModule m : Modules.ALL) {
            if (!q.isEmpty()) {
                if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.desc.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            } else if (m.category.equals(category)) {
                out.add(m);
            }
        }
        return out;
    }

    // ------------------------------------------------------------ render
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        TextRenderer tr = this.textRenderer;
        int accent = Modules.accent();
        int accent2 = Modules.accent2();
        float in0 = intro(now, 0);

        drawBackground(ctx, now, accent, accent2, in0);

        // ---- window geometry
        ww = Math.min(width - 16, 500);
        wh = Math.min(height - 16, 310);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
        sideW = ww < 360 ? 70 : 92;
        cx0 = wx + sideW + 8;
        cy0 = wy + 34;
        cw0 = ww - sideW - 16;
        ch0 = wh - 34 - 8;

        float pop = Modules.UI_ANIM.value ? 0.94f + 0.06f * in0 : 1f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0f);
        ctx.getMatrices().scale(pop, pop, 1f);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0f);
        int a = (int) (255 * in0);

        // ---- window frame + glow
        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 900.0);
        for (int g = 3; g >= 1; g--) {
            ctx.drawBorder(wx - g, wy - g, ww + g * 2, wh + g * 2, argb((int) (a * (0.05f + 0.04f * pulse) / g), accent));
        }
        rrect(ctx, wx, wy, ww, wh, argb((int) (a * 0.95f), 0x0B0D14));
        rrect(ctx, wx, wy, sideW, wh, argb((int) (a * 0.97f), 0x0F121B));
        ctx.fill(wx + sideW, wy + 1, wx + sideW + 1, wy + wh - 1, argb((int) (a * 0.6f), 0x252A3A));
        hgradient(ctx, wx + 1, wy, ww - 2, 2, accent, accent2, a);

        // ---- logo
        String logo = "KNIFE";
        ctx.getMatrices().push();
        ctx.getMatrices().translate(wx + 10, wy + 9, 0f);
        ctx.getMatrices().scale(1.6f, 1.6f, 1f);
        ctx.drawText(tr, logo, 0, 0, argb(a, 0xFFFFFF), true);
        ctx.getMatrices().pop();
        ctx.drawText(tr, "CLIENT", wx + 10 + (int) (tr.getWidth(logo) * 1.6f) + 3, wy + 17, argb(a, accent), true);

        // ---- tabs
        tabRects.clear();
        int ty = wy + 34;
        for (int i = 0; i < TABS.length; i++) {
            int th = 22;
            float in = intro(now, i + 1);
            int tx = wx + 6 - (int) ((1f - in) * 20f);
            boolean sel = i == tab && query.isEmpty();
            boolean hov = inside(mx, my, wx + 6, ty, sideW - 12, th);
            float s = anim("tab" + i, sel ? 1f : 0f, sel ? 1f : 0f);
            float h = anim("tabh" + i, hov ? 1f : 0f, 0f);
            int ta = (int) (a * in);
            if (s > 0.02f || h > 0.02f) {
                rrect(ctx, tx, ty, sideW - 12, th, argb((int) (ta * Math.max(0.16f * s + 0.10f * h, 0.05f)), mix(0xFFFFFF, accent, s)));
            }
            ctx.fill(tx, ty + 4, tx + 2, ty + th - 4, argb((int) (ta * s), accent));
            ctx.drawText(tr, TABS[i].icon(), tx + 8, ty + 7, argb(ta, mix(0x707A90, accent, s)), false);
            if (sideW > 80) ctx.drawText(tr, TABS[i].name(), tx + 22, ty + 7, argb(ta, mix(0xA8B0C0, 0xFFFFFF, Math.max(s, h))), false);
            tabRects.add(new int[]{wx + 6, ty, sideW - 12, th});
            ty += th + 3;
        }
        // side footer
        int enabledCount = 0;
        for (ClientModule m : Modules.ALL) if (m.toggleable && m.enabled) enabledCount++;
        ctx.drawText(tr, enabledCount + " module" + (enabledCount == 1 ? "" : "s") + " on", wx + 10, wy + wh - 22, argb((int) (a * 0.65f), 0xA8B0C0), false);
        ctx.drawText(tr, "v2.1  R-Shift", wx + 10, wy + wh - 12, argb((int) (a * 0.35f), 0xA8B0C0), false);

        // ---- header: page title + search
        String pageTitle = query.isEmpty() ? TABS[tab].name() : "Search";
        ctx.drawText(tr, pageTitle, cx0, wy + 12, argb(a, 0xFFFFFF), true);
        int sbW = Math.min(150, cw0 - tr.getWidth(pageTitle) - 20);
        if (sbW > 50) {
            int sbx = wx + ww - 8 - sbW;
            rrect(ctx, sbx, wy + 8, sbW, 16, argb((int) (a * 0.9f), 0x151926));
            if (!query.isEmpty()) ctx.drawBorder(sbx, wy + 8, sbW, 16, argb((int) (a * 0.7f), accent));
            String shown = query.isEmpty() ? "Type to search..." : query + ((now / 450) % 2 == 0 ? "_" : "");
            while (tr.getWidth(shown) > sbW - 10 && shown.length() > 1) shown = shown.substring(1);
            ctx.drawText(tr, shown, sbx + 5, wy + 12, argb(a, query.isEmpty() ? 0x5C6479 : 0xFFFFFF), false);
        }
        hgradient(ctx, cx0, wy + 28, cw0, 1, accent, 0x0B0D14, (int) (a * 0.55f));

        // ---- page content
        cards.clear();
        tileRects.clear();
        hits.clear();
        float pt = ease((now - tabTime) / (260f / (float) Modules.UI_SPEED.value));
        int pa = (int) (a * (Modules.UI_ANIM.value ? pt : 1f));
        int slide = Modules.UI_ANIM.value ? (int) ((1f - pt) * 14f) : 0;

        ctx.enableScissor(cx0 - 2, cy0, cx0 + cw0 + 2, cy0 + ch0);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0f, slide - pageScroll, 0f);
        int contentH;
        if (!query.isEmpty()) contentH = renderCards(ctx, mx, my, modulesFor(""), cx0, cy0, cw0, pa, accent, now);
        else switch (tab) {
            case 0 -> contentH = renderHome(ctx, mx, my, pa, accent, accent2, now);
            case 2 -> contentH = renderKnives(ctx, mx, my, pa, accent, accent2, now);
            case 5 -> contentH = renderInline(ctx, mx, my, Modules.UI, pa, accent);
            default -> contentH = renderCards(ctx, mx, my, modulesFor(TABS[tab].name()), cx0, cy0, cw0, pa, accent, now);
        }
        ctx.getMatrices().pop();
        ctx.disableScissor();
        pageMax = Math.max(0, contentH - ch0 + 4);
        pageScroll = Math.max(0, Math.min(pageMax, pageScroll));

        ctx.drawCenteredTextWithShadow(tr, "Left click: toggle   Right click: settings   V: inspect knife",
                wx + ww / 2, wy + wh + 4, argb((int) (a * 0.5f), 0xFFFFFF));

        renderDrawer(ctx, mx, my, now, accent, accent2);
        ctx.getMatrices().pop();
    }

    private void hgradient(DrawContext ctx, int x, int y, int w, int h, int c1, int c2, int a) {
        int step = 3;
        for (int i = 0; i < w; i += step) {
            ctx.fill(x + i, y, Math.min(x + w, x + i + step), y + h, argb(a, mix(c1, c2, i / (float) Math.max(1, w))));
        }
    }

    private void drawBackground(DrawContext ctx, long now, int accent, int accent2, float in0) {
        int dim = (int) (Modules.UI_DIM.value * 255 * in0);
        ctx.fillGradient(0, 0, width, height, argb(Math.min(255, dim + 90), mix(0x05060B, accent, 0.10f)),
                argb(Math.min(255, dim + 90), mix(0x05060B, accent2, 0.16f)));
        if (!Modules.UI_ANIM.value) return;
        double t = now / 1000.0 * Modules.UI_SPEED.value;
        for (int i = 0; i < 46; i++) {
            float sp = 4f + hash(i, 3) * 14f;
            float x = (hash(i, 1) * width + (float) (t * sp * 0.6)) % width;
            float y = (hash(i, 2) * height - (float) (t * sp)) % height;
            if (y < 0) y += height;
            float tw = 0.4f + 0.6f * (float) Math.abs(Math.sin(t * (0.4 + hash(i, 4)) + i));
            int sz = hash(i, 5) > 0.82f ? 2 : 1;
            int col = mix(accent, accent2, hash(i, 6));
            ctx.fill((int) x, (int) y, (int) x + sz, (int) y + sz, argb((int) (150 * tw * in0), col));
        }
        // soft moving light bar
        int bx = (int) ((t * 40) % (width + 200)) - 100;
        for (int k = 0; k < 4; k++) {
            ctx.fill(bx + k * 3, 0, bx + k * 3 + 3, height, argb((int) (6 * in0), accent));
        }
    }

    // ------------------------------------------------------------ card grids
    private int renderCards(DrawContext ctx, int mx, int my, List<ClientModule> list, int x, int y, int w, int a, int accent, long now) {
        TextRenderer tr = this.textRenderer;
        int cols = Math.max(1, Math.min(3, (w + 6) / 124));
        int gap = 6;
        int cw = (w - (cols - 1) * gap) / cols;
        int ch = 36;
        int vy = my + pageScroll - (int) 0;
        if (list.isEmpty()) {
            ctx.drawText(tr, "Nothing found.", x, y + 6, argb(a, 0x7C859A), false);
            return 20;
        }
        for (int i = 0; i < list.size(); i++) {
            ClientModule m = list.get(i);
            int cx = x + (i % cols) * (cw + gap);
            int cy = y + 4 + (i / cols) * (ch + gap);
            float in = Modules.UI_ANIM.value ? ease((System.currentTimeMillis() - tabTime - i * 30L) / 260f) : 1f;
            int ca = (int) (a * in);
            cards.add(new Card(m, cx, cy, cw, ch));
            boolean hovered = selected == null && inside(mx, my + pageScroll, cx, cy, cw, ch) && inside(mx, my, cx0, cy0, cw0, ch0);
            float hov = anim(m.id + ":h", hovered ? 1f : 0f, 0f);
            float on = anim(m.id + ":t", m.enabled ? 1f : 0f, m.enabled ? 1f : 0f);
            int yy = cy + (int) ((1f - in) * 10f) - (int) (hov * 1.5f);
            rrect(ctx, cx, yy, cw, ch, argb((int) (ca * 0.92f), mix(0x141824, 0x1E2438, hov)));
            if (on > 0.02f) ctx.fillGradient(cx + 1, yy + 1, cx + cw - 1, yy + ch - 1, argb((int) (ca * 0.16f * on), accent), argb(0, accent));
            ctx.fill(cx, yy + 2, cx + 2, yy + ch - 2, argb(ca, mix(0x3A4052, accent, on)));
            if (hov > 0.02f) ctx.drawBorder(cx, yy, cw, ch, argb((int) (ca * hov * 0.55f), accent));
            ctx.drawText(tr, m.name, cx + 9, yy + 6, argb(ca, 0xFFFFFF), false);
            String d = m.desc;
            int maxW = cw - 18 - (m.toggleable ? 24 : 0);
            while (tr.getWidth(d) > maxW && d.length() > 4) d = d.substring(0, d.length() - 2);
            if (!d.equals(m.desc)) d = d.trim() + "..";
            ctx.drawText(tr, d, cx + 9, yy + 20, argb((int) (ca * 0.5f), 0xFFFFFF), false);
            if (m.toggleable) {
                int sx = cx + cw - 28, sy = yy + 7;
                rrect(ctx, sx, sy, 22, 11, argb(ca, mix(0x353B4B, accent, on)));
                int kx = sx + 1 + (int) (on * 11);
                rrect(ctx, kx, sy + 1, 9, 9, argb(ca, 0xFFFFFF));
            } else {
                ctx.drawText(tr, "...", cx + cw - 20, yy + 6, argb(ca, accent), false);
            }
        }
        int rows = (list.size() + cols - 1) / cols;
        return 4 + rows * (ch + gap);
    }

    // ------------------------------------------------------------ home
    private int renderHome(DrawContext ctx, int mx, int my, int a, int accent, int accent2, long now) {
        TextRenderer tr = this.textRenderer;
        int x = cx0, y = cy0 + 4, w = cw0;
        // hero card
        int hh = 70;
        rrect(ctx, x, y, w, hh, argb((int) (a * 0.92f), 0x141824));
        ctx.fillGradient(x + 1, y + 1, x + w - 1, y + hh - 1, argb((int) (a * 0.22f), accent), argb((int) (a * 0.04f), accent2));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + 12, y + 10, 0f);
        ctx.getMatrices().scale(1.8f, 1.8f, 1f);
        ctx.drawText(tr, "Welcome back", 0, 0, argb(a, 0xFFFFFF), true);
        ctx.getMatrices().pop();
        ctx.drawText(tr, "16 knives, animations, cosmetics, HUD and utilities.", x + 12, y + 32, argb((int) (a * 0.7f), 0xFFFFFF), false);
        ctx.drawText(tr, "Pick a page on the left, or just type to search.", x + 12, y + 44, argb((int) (a * 0.5f), 0xFFFFFF), false);
        // current knife showcase
        drawKnife(ctx, Knives.index(), x + w - 46, y + 35, 3.6f, now, a, true);

        // quick toggles
        int qy = y + hh + 10;
        ctx.drawText(tr, "QUICK TOGGLES", x, qy, argb((int) (a * 0.55f), accent), false);
        List<ClientModule> quick = new ArrayList<>();
        for (ClientModule m : Modules.ALL) if (m.toggleable) quick.add(m);
        int used = renderCards(ctx, mx, my, quick, x, qy + 8, w, a, accent, now);
        return (qy + 8 - cy0) + used;
    }

    // ------------------------------------------------------------ knives page
    private int renderKnives(DrawContext ctx, int mx, int my, int a, int accent, int accent2, long now) {
        TextRenderer tr = this.textRenderer;
        int x = cx0, y = cy0 + 2, w = cw0;
        int n = Knives.IDS.length;
        int cols = Math.max(4, Math.min(8, w / 48));
        int gap = 4;
        int tw = (w - (cols - 1) * gap) / cols;
        int th = Math.min(tw, 46);
        int sel = Knives.index();
        for (int i = 0; i < n; i++) {
            int tx = x + (i % cols) * (tw + gap);
            int ty = y + (i / cols) * (th + gap);
            tileRects.add(new int[]{tx, ty, tw, th, i});
            boolean hov = selected == null && inside(mx, my + pageScroll, tx, ty, tw, th) && inside(mx, my, cx0, cy0, cw0, ch0);
            float h = anim("kt" + i, hov ? 1f : 0f, 0f);
            float s = anim("ks" + i, i == sel ? 1f : 0f, i == sel ? 1f : 0f);
            rrect(ctx, tx, ty, tw, th, argb((int) (a * 0.92f), mix(0x141824, 0x1E2438, h)));
            if (s > 0.02f) {
                ctx.fillGradient(tx + 1, ty + 1, tx + tw - 1, ty + th - 1, argb((int) (a * 0.22f * s), accent), argb(0, accent));
                ctx.drawBorder(tx, ty, tw, th, argb((int) (a * s), accent));
            } else if (h > 0.02f) {
                ctx.drawBorder(tx, ty, tw, th, argb((int) (a * h * 0.5f), accent));
            }
            float sc = (th - 10) / 16f;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(tx + tw / 2f - 8 * sc, ty + 5 - h * 1.5f, 0f);
            ctx.getMatrices().scale(sc, sc, 1f);
            ctx.drawItem(Knives.stack(i, Modules.KNIVES_COLOR.rgb(0.0)), 0, 0);
            ctx.getMatrices().pop();
        }
        int gridH = ((n + cols - 1) / cols) * (th + gap);

        // showcase + settings
        int sy = y + gridH + 6;
        int sh = 78;
        int pw = Math.min(150, w / 2);
        rrect(ctx, x, sy, pw, sh, argb((int) (a * 0.92f), 0x141824));
        ctx.fillGradient(x + 1, sy + 1, x + pw - 1, sy + sh - 1, argb((int) (a * 0.18f), accent2), argb(0, accent2));
        drawKnife(ctx, sel, x + pw / 2, sy + 34, 3.4f, now, a, false);
        String nm = Knives.NAMES[sel];
        ctx.drawText(tr, nm, x + pw / 2 - tr.getWidth(nm) / 2, sy + sh - 22, argb(a, 0xFFFFFF), true);
        String st = Knives.STYLE_NAMES[Knives.STYLE[sel]] + " style";
        ctx.drawText(tr, st, x + pw / 2 - tr.getWidth(st) / 2, sy + sh - 11, argb((int) (a * 0.55f), accent), false);
        // arrows
        boolean hl = inside(mx, my + pageScroll, x + 3, sy + 28, 12, 14) && selected == null;
        boolean hr = inside(mx, my + pageScroll, x + pw - 15, sy + 28, 12, 14) && selected == null;
        ctx.drawText(tr, "<", x + 6, sy + 31, argb(a, hl ? accent : 0x8A93A8), true);
        ctx.drawText(tr, ">", x + pw - 12, sy + 31, argb(a, hr ? accent : 0x8A93A8), true);
        tileRects.add(new int[]{x + 3, sy + 28, 12, 14, -1});
        tileRects.add(new int[]{x + pw - 15, sy + 28, 12, 14, -2});

        int ix = x + pw + 8;
        int iw = w - pw - 8;
        int used = renderSettingsBlock(ctx, mx, my, Modules.KNIVES, ix, sy, iw, a, accent, true);
        return (sy - cy0) + Math.max(sh, used) + 6;
    }

    /** Spinning knife preview: gently rocks and bobs. */
    private void drawKnife(DrawContext ctx, int idx, int cx, int cy, float scale, long now, int a, boolean tilt) {
        double t = now / 1000.0;
        float rock = (float) Math.sin(t * 1.6) * 8f;
        float bob = (float) Math.sin(t * 2.1) * 1.5f;
        float squeeze = 0.86f + 0.14f * (float) Math.cos(t * 1.6);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy + bob, 0f);
        ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rock + (tilt ? -10f : 0f)));
        ctx.getMatrices().scale(scale * squeeze, scale, 1f);
        ctx.drawItem(Knives.stack(idx, Modules.KNIVES_COLOR.rgb(0.0)), -8, -8);
        ctx.getMatrices().pop();
    }

    // ------------------------------------------------------------ settings rendering
    private int renderInline(DrawContext ctx, int mx, int my, ClientModule m, int a, int accent) {
        return renderSettingsBlock(ctx, mx, my, m, cx0, cy0 + 4, cw0, a, accent, false);
    }

    /** Draws a module's settings as rows. Returns the height used. Hits are stored in page coordinates. */
    private int renderSettingsBlock(DrawContext ctx, int mx, int my, ClientModule m, int x, int y, int w, int a, int accent, boolean skipKnifeChoice) {
        TextRenderer tr = this.textRenderer;
        int yy = y;
        if (m.toggleable) {
            // master switch row
            float on = anim("m" + m.id, m.enabled ? 1f : 0f, m.enabled ? 1f : 0f);
            ctx.drawText(tr, m.name + " enabled", x + 4, yy + 5, argb(a, 0xE8E8E8), false);
            int sx = x + w - 28;
            rrect(ctx, sx, yy + 4, 22, 11, argb(a, mix(0x353B4B, accent, on)));
            int kx = sx + 1 + (int) (on * 11);
            rrect(ctx, kx, yy + 5, 9, 9, argb(a, 0xFFFFFF));
            hits.add(new Hit(null, x, yy, w, 18));
            yy += 20;
        }
        for (Setting s : m.settings) {
            if (skipKnifeChoice && s == Modules.KNIVES_KNIFE) continue;
            int rh = rowH(s);
            boolean over = selected == null && inside(mx, my + pageScroll, x, yy, w, rh) && inside(mx, my, cx0, cy0, cw0, ch0);
            if (over) rrect(ctx, x, yy, w, rh - 1, argb((int) (a * 0.35f), 0x2A2F45));
            drawSettingRow(ctx, s, x, yy, w, a, accent);
            hits.add(new Hit(s, x, yy, w, rh));
            yy += rh;
        }
        return yy - y;
    }

    private void drawSettingRow(DrawContext ctx, Setting s, int x, int y, int w, int a, int accent) {
        TextRenderer tr = this.textRenderer;
        if (s instanceof Setting.Bool b) {
            float on = anim(b, b.value ? 1f : 0f, b.value ? 1f : 0f);
            ctx.drawText(tr, b.label, x + 4, y + 5, argb(a, 0xE8E8E8), false);
            int sx = x + w - 28, sy = y + 4;
            rrect(ctx, sx, sy, 22, 11, argb(a, mix(0x353B4B, accent, on)));
            int kx = sx + 1 + (int) (on * 11);
            rrect(ctx, kx, sy + 1, 9, 9, argb(a, 0xFFFFFF));
        } else if (s instanceof Setting.Choice c) {
            ctx.drawText(tr, c.label, x + 4, y + 5, argb(a, 0xE8E8E8), false);
            String t = "< " + c.text() + " >";
            ctx.drawText(tr, t, x + w - 4 - tr.getWidth(t), y + 5, argb(a, accent), false);
        } else if (s instanceof Setting.Num n) {
            ctx.drawText(tr, n.label, x + 4, y + 3, argb(a, 0xE8E8E8), false);
            String t = n.text();
            ctx.drawText(tr, t, x + w - 4 - tr.getWidth(t), y + 3, argb(a, accent), false);
            int tx = x + 4, ty = y + 16, tw2 = w - 8;
            track(ctx, n, tx, ty, tw2, 6, accent, a);
            double frac = (n.value - n.min) / (n.max - n.min);
            int kx = tx + (int) (frac * (tw2 - 4));
            ctx.fill(kx, ty - 2, kx + 4, ty + 8, argb(a, 0xFFFFFF));
        }
    }

    private void track(DrawContext ctx, Setting.Num n, int x, int y, int w, int h, int accent, int a) {
        if (n.kind == Setting.Num.NORMAL || n.group == null) {
            ctx.fill(x, y, x + w, y + h, argb(a, 0x2A2F3D));
            int fw = (int) (w * (n.value - n.min) / (n.max - n.min));
            ctx.fill(x, y, x + fw, y + h, argb(a, accent));
            return;
        }
        float hue = (float) (n.group.hue.value / 360.0);
        float sat = (float) n.group.sat.value;
        for (int i = 0; i < w; i++) {
            float t = i / (float) Math.max(1, w - 1);
            int c;
            if (n.kind == Setting.Num.HUE) c = ColorGroup.hsb(t, 1f, 1f);
            else if (n.kind == Setting.Num.SAT) c = ColorGroup.hsb(hue, t, 1f);
            else c = ColorGroup.hsb(hue, sat, t);
            ctx.fill(x + i, y, x + i + 1, y + h, argb(a, c));
        }
    }

    // ------------------------------------------------------------ drawer (right-click settings)
    private void renderDrawer(DrawContext ctx, int mx, int my, long now, int accent, int accent2) {
        if (selected == null) return;
        TextRenderer tr = this.textRenderer;
        float p = Modules.UI_ANIM.value ? ease((now - selectedTime) / (260f / (float) Modules.UI_SPEED.value)) : 1f;
        int a = (int) (255 * p);

        ctx.fill(wx, wy, wx + ww, wy + wh, argb((int) (150 * p), 0x000000));

        drawerW = Math.min(230, ww - 24);
        drawerH = wh - 16;
        drawerX = wx + ww - drawerW - 8 + (int) ((1f - p) * 40f);
        drawerY = wy + 8;
        rrect(ctx, drawerX, drawerY, drawerW, drawerH, argb(a, 0x0E1118));
        ctx.drawBorder(drawerX, drawerY, drawerW, drawerH, argb(a, accent));
        hgradient(ctx, drawerX + 1, drawerY + 1, drawerW - 2, 2, accent, accent2, a);
        ctx.drawText(tr, selected.name, drawerX + 8, drawerY + 8, argb(a, accent), true);
        String d = selected.desc;
        while (tr.getWidth(d) > drawerW - 16 && d.length() > 4) d = d.substring(0, d.length() - 2);
        ctx.drawText(tr, d, drawerX + 8, drawerY + 20, argb((int) (a * 0.5f), 0xFFFFFF), false);

        String reset = "Reset";
        resetW = tr.getWidth(reset) + 8;
        closeX = drawerX + drawerW - 20;
        resetX = closeX - resetW - 4;
        drawerHeadY = drawerY + 5;
        boolean overReset = inside(mx, my, resetX, drawerHeadY, resetW, 15);
        boolean overClose = inside(mx, my, closeX, drawerHeadY, 14, 15);
        rrect(ctx, resetX, drawerHeadY, resetW, 15, argb(a, overReset ? 0x3A3F55 : 0x232838));
        ctx.drawText(tr, reset, resetX + 4, drawerHeadY + 4, argb(a, 0xFFFFFF), false);
        rrect(ctx, closeX, drawerHeadY, 14, 15, argb(a, overClose ? 0x7A2A2A : 0x232838));
        ctx.drawText(tr, "X", closeX + 4, drawerHeadY + 4, argb(a, 0xFFFFFF), false);

        int viewTop = drawerY + 34;
        int viewBot = drawerY + drawerH - 4;
        setX = drawerX + 6;
        setY = viewTop;
        setW = drawerW - 12;
        setH = viewBot - viewTop;

        int total = 0;
        for (Setting s : selected.settings) total += rowH(s);
        maxScroll = Math.max(0, total + 6 - setH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        hits.clear();
        ctx.enableScissor(drawerX + 1, viewTop, drawerX + drawerW - 1, viewBot);
        int y = viewTop + 2 - scroll;
        for (Setting s : selected.settings) {
            int rh = rowH(s);
            boolean over = inside(mx, my, setX, y, setW, rh) && my >= viewTop && my < viewBot;
            if (over) rrect(ctx, setX, y, setW, rh - 1, argb((int) (a * 0.35f), 0x2A2F45));
            drawSettingRow(ctx, s, setX, y, setW, a, accent);
            hits.add(new Hit(s, setX, y, setW, rh));
            y += rh;
        }
        ctx.disableScissor();
    }

    // ------------------------------------------------------------ input
    private void setFromMouse(Hit h, double mx) {
        Setting.Num n = (Setting.Num) h.s();
        double t = (mx - (h.x() + 2 + 4)) / (double) (h.w() - 8 - 4);
        t = Math.max(0.0, Math.min(1.0, t));
        n.set(n.min + t * (n.max - n.min));
    }

    private void clickSetting(Hit h, int button, double mx, ClientModule owner) {
        Setting s = h.s();
        if (s == null) {
            owner.enabled = !owner.enabled;
            Modules.save();
        } else if (s instanceof Setting.Bool b) {
            b.value = !b.value;
            Modules.save();
        } else if (s instanceof Setting.Choice c) {
            if (button == 1) c.prev(); else c.next();
            Modules.save();
        } else if (s instanceof Setting.Num) {
            dragging = h;
            setFromMouse(h, mx);
        }
    }

    private void switchTab(int t) {
        tab = t;
        lastTab = t;
        tabTime = System.currentTimeMillis();
        pageScroll = 0;
        query = "";
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (selected != null) {
            if (inside(mx, my, drawerX, drawerY, drawerW, drawerH)) {
                if (inside(mx, my, closeX, drawerHeadY, 14, 15)) {
                    selected = null;
                    Modules.save();
                } else if (inside(mx, my, resetX, drawerHeadY, resetW, 15)) {
                    selected.resetSettings();
                    Modules.save();
                } else if (inside(mx, my, setX, setY, setW, setH)) {
                    for (Hit h : hits) {
                        if (inside(mx, my, h.x(), h.y(), h.w(), h.h())) {
                            clickSetting(h, button, mx, selected);
                            break;
                        }
                    }
                }
                return true;
            }
            selected = null;
            Modules.save();
            return true;
        }

        // tabs
        for (int i = 0; i < tabRects.size(); i++) {
            int[] r = tabRects.get(i);
            if (inside(mx, my, r[0], r[1], r[2], r[3])) {
                switchTab(i);
                return true;
            }
        }
        if (!inside(mx, my, cx0, cy0, cw0, ch0)) return super.mouseClicked(mx, my, button);

        double pyy = my + pageScroll;
        // knife tiles + arrows
        for (int[] r : tileRects) {
            if (inside(mx, pyy, r[0], r[1], r[2], r[3])) {
                int idx = r[4];
                if (idx == -1) Modules.KNIVES_KNIFE.prev();
                else if (idx == -2) Modules.KNIVES_KNIFE.next();
                else Modules.KNIVES_KNIFE.index = idx;
                Knives.drawStart = System.nanoTime();
                Modules.save();
                return true;
            }
        }
        // module cards
        for (Card c : cards) {
            if (inside(mx, pyy, c.x(), c.y(), c.w(), c.h())) {
                if (button == 0 && c.m().toggleable) {
                    c.m().enabled = !c.m().enabled;
                    Modules.save();
                } else {
                    selected = c.m();
                    selectedTime = System.currentTimeMillis();
                    scroll = 0;
                }
                return true;
            }
        }
        // inline settings (Knives / Interface pages)
        ClientModule owner = (query.isEmpty() && tab == 2) ? Modules.KNIVES : ((query.isEmpty() && tab == 5) ? Modules.UI : null);
        if (owner != null) {
            for (Hit h : hits) {
                if (inside(mx, pyy, h.x(), h.y(), h.w(), h.h())) {
                    clickSetting(h, button, mx, owner);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            setFromMouse(dragging, mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            Modules.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (selected != null) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (vAmount * 16)));
            return true;
        }
        pageScroll = Math.max(0, Math.min(pageMax, pageScroll - (int) (vAmount * 18)));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (selected == null && chr >= 32 && chr != 127 && query.length() < 24) {
            query += chr;
            pageScroll = 0;
            tabTime = System.currentTimeMillis();
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (selected != null) {
                selected = null;
                Modules.save();
                return true;
            }
            if (!query.isEmpty()) {
                query = "";
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && selected == null && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        Modules.save();
    }
}
