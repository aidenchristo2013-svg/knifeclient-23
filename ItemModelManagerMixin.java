package com.example.knifeclient.mixin;

import com.example.knifeclient.Knives;
import com.example.knifeclient.Modules;
import net.minecraft.client.MinecraftClient; import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third person: show the knife in your main hand when it is empty.
 * The class lives in a different package depending on the mappings, so both names are listed;
 * @Pseudo means the game just skips whichever one does not exist.
 */
@Mixin(ItemModelManager.class)
public abstract class ItemModelManagerMixin {
    @Shadow
    public abstract void updateForLivingEntity(ItemRenderState renderState, ItemStack stack,
                                               ModelTransformationMode mode, boolean leftHanded, LivingEntity entity);

    @Inject(method = "updateForLivingEntity", at = @At("HEAD"), cancellable = true)
    private void knifeclient$knife(ItemRenderState renderState, ItemStack stack, ModelTransformationMode mode,
                                   boolean leftHanded, LivingEntity entity, CallbackInfo ci) {
        if (!Modules.KNIVES.enabled || !Modules.KNIVES_THIRD.value || !stack.isEmpty()) return;
        if (entity != MinecraftClient.getInstance().player) return;
        if (mode != ModelTransformationMode.THIRD_PERSON_RIGHT_HAND
                && mode != ModelTransformationMode.THIRD_PERSON_LEFT_HAND) return;
        if (leftHanded != (entity.getMainArm() == Arm.LEFT)) return;
        this.updateForLivingEntity(renderState, Knives.current(), mode, leftHanded, entity);
        ci.cancel();
    }
}
