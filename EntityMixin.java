package com.example.knifeclient.mixin;

import com.example.knifeclient.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Glow Skin: glowing outline (and its colour) only for YOUR player, only on your screen. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void knifeclient$glow(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.GLOW.enabled && (Object) this == MinecraftClient.getInstance().player) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("RETURN"), cancellable = true)
    private void knifeclient$glowColor(CallbackInfoReturnable<Integer> cir) {
        if (Modules.GLOW.enabled && (Object) this == MinecraftClient.getInstance().player) {
            cir.setReturnValue(Modules.glowColor());
        }
    }
}
