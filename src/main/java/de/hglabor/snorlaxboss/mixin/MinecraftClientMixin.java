package de.hglabor.snorlaxboss.mixin;

import de.hglabor.snorlaxboss.entity.player.ModifiedPlayer;
import de.hglabor.snorlaxboss.gui.screen.ForceSleepingScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    @Nullable
    public ClientWorld world;

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Shadow @Nullable public Screen currentScreen;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isDead()Z"))
    private void forceSleepingScreenInjection(CallbackInfo ci) {
        if (player instanceof ModifiedPlayer modifiedPlayer && modifiedPlayer.isForceSleeping() && this.world != null) {
            this.setScreen(new ForceSleepingScreen());
        }
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 3))
    private void forceSleepingScreenInjection2(CallbackInfo ci) {
        if (currentScreen instanceof ForceSleepingScreen && player instanceof ModifiedPlayer modifiedPlayer && !modifiedPlayer.isForceSleeping()) {
            this.setScreen(null);
        }
    }
}
