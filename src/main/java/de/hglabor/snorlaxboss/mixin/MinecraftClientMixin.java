package de.hglabor.snorlaxboss.mixin;

import de.hglabor.snorlaxboss.render.gui.hud.SleepRenderer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isDead()Z"))
    private void forceSleepingScreenInjection(CallbackInfo ci) {
        SleepRenderer.INSTANCE.startSleepScreen((MinecraftClient) (Object) this);
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 3))
    private void forceSleepingScreenInjection2(CallbackInfo ci) {
        SleepRenderer.INSTANCE.stopSleepScreen((MinecraftClient) (Object) this);
    }
}
