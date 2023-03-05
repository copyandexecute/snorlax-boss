package de.hglabor.snorlaxboss.mixin.gui.hud;


import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @ModifyArg(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"), index = 1)
    private static float getFirstPersonFireY(float x) {
        return FabricLoader.getInstance().isDevelopmentEnvironment() ? -0.6f : x;
    }
}
