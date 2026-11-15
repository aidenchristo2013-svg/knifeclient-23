package com.example.knifeclient;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.List;

public final class Knives {
    public static final String[] IDS = {
            "karambit", "bayonet", "butterfly", "huntsman", "flip", "falchion", "tanto", "bowie",
            "kukri", "stiletto", "dagger", "machete", "cleaver", "kris", "dirk", "kunai"};
    public static final String[] NAMES = {
            "Karambit", "Bayonet", "Butterfly Knife", "Huntsman Knife", "Flip Knife", "Falchion Knife", "Tanto", "Bowie Knife",
            "Kukri", "Stiletto", "Dagger", "Machete", "Cleaver", "Kris", "Dirk", "Kunai"};
    /** 0 = hook (curved slashes), 1 = flip (folding knives), 2 = stab (pointy), 3 = chop (heavy). */
    public static final int[] STYLE = {0, 2, 1, 2, 1, 3, 2, 3, 3, 2, 2, 3, 3, 2, 2, 2};
    public static final String[] STYLE_NAMES = {"Hook", "Flip", "Stab", "Chop"};

    // ---- animation clock (all times are System.nanoTime based) ----
    private static long lastSeen = 0L;
    private static int lastKnife = -1;
    public static long drawStart = 0L;
    public static long inspectStart = 0L;

    public static int index() {
        return Math.floorMod(Modules.KNIVES_KNIFE.index, IDS.length);
    }

    /** A fresh knife stack with the given model and tint colour. */
    public static ItemStack stack(int i, int rgb) {
        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("knifeclient", IDS[Math.floorMod(i, IDS.length)]));
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                new CustomModelDataComponent(List.of(), List.of(), List.of(), List.of(rgb)));
        return stack;
    }

    /** The knife you currently have selected, in your chosen colour. */
    public static ItemStack current() {
        return stack(index(), Modules.KNIVES_COLOR.rgb(0.0));
    }

    /** Called once per first-person frame while the knife is shown; restarts the draw animation when needed. */
    public static void frame() {
        long now = System.nanoTime();
        int k = index();
        if (now - lastSeen > 250_000_000L || k != lastKnife) {
            drawStart = now;
            inspectStart = 0L;
        }
        lastSeen = now;
        lastKnife = k;
    }

    public static void inspect() {
        if (Modules.KNIVES.enabled) inspectStart = System.nanoTime();
    }
}
