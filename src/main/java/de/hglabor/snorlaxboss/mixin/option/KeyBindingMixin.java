package de.hglabor.snorlaxboss.mixin.option;

import de.hglabor.snorlaxboss.render.camera.CameraShaker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//Credits to https://github.com/LoganDark/fabric-camera-shake
@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Inject(
            method = "onKeyPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void camerashake$onKeyPressed(InputUtil.Key key, CallbackInfo ci) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            if (key.getCode() == GLFW.GLFW_KEY_K) {
                ci.cancel();
                //noinspection ConstantConditions
                CameraShaker.INSTANCE.addEvent(new CameraShaker.BoomShake(.25, .0, .5));
            }
        }
    }
}
