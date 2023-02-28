package de.hglabor.snorlaxboss.mixin.render;

import de.hglabor.snorlaxboss.render.camera.CameraShaker;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//Credits to https://github.com/LoganDark/fabric-camera-shake
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void moveBy(double x, double y, double z);

    @Inject(
            method = "update",
            at = @At(
                    // Inject before the call to clipToSpace
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V",
                    shift = At.Shift.BY,
                    by = 1
            )
    )
    private void camerashake$onUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        double x = CameraShaker.INSTANCE.getAvgX();
        double y = CameraShaker.INSTANCE.getAvgY();

        moveBy(.0, y, x);
    }
}
