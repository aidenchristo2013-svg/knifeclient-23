package com.example.knifeclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnifeClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("knifeclient");
    private static KeyBinding menuKey;
    private static KeyBinding zoomKey;
    private static KeyBinding inspectKey;
    private static boolean sprintForced = false;

    @Override
    public void onInitializeClient() {
        Modules.load();
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.knifeclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.knifeclient"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.knifeclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.knifeclient"));

        inspectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.knifeclient.inspect", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.knifeclient"));

        ClientTickEvents.END_CLIENT_TICK.register(KnifeClient::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> Hud.render(ctx));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Modules.save());

        // Proof-of-life message so you know the mod loaded.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[Knife Client] Loaded! Right Shift = menu, V = inspect knife."), false);
            }
        }));
        LOGGER.info("Knife Client loaded");
    }

    private static void tick(MinecraftClient client) {
        while (menuKey.wasPressed()) {
            client.setScreen(new ModuleScreen());
        }
        while (inspectKey.wasPressed()) {
            if (client.currentScreen == null) Knives.inspect();
        }
        Zoom.active = Modules.ZOOM.enabled && zoomKey.isPressed() && client.currentScreen == null;

        boolean wantSprint = Modules.SPRINT.enabled && client.player != null && client.currentScreen == null
                && client.options.forwardKey.isPressed() && !client.player.isSneaking();
        if (wantSprint) {
            client.options.sprintKey.setPressed(true);
            sprintForced = true;
        } else if (sprintForced) {
            client.options.sprintKey.setPressed(false);
            sprintForced = false;
        }
    }
}
