package de.hglabor.snorlaxboss.mixin.entity;

import de.hglabor.snorlaxboss.entity.player.ModifiedPlayerManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    protected int bodyTrackingIncrements;

    @Shadow
    protected double serverX;

    @Shadow
    protected double serverY;

    @Shadow
    protected double serverZ;

    @Shadow
    protected double serverYaw;

    @Shadow
    protected double serverPitch;

    @Shadow
    public abstract boolean canMoveVoluntarily();

    @Shadow
    protected int headTrackingIncrements;

    @Shadow
    public float headYaw;

    @Shadow
    protected double serverHeadYaw;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void tickMovementInjection(CallbackInfo ci) {
        if (ModifiedPlayerManager.INSTANCE.isActive() && getType() == EntityType.PLAYER) {
            if (this.isLogicalSideForUpdatingMovement()) {
                this.bodyTrackingIncrements = 0;
                this.updateTrackedPosition(this.getX(), this.getY(), this.getZ());
            }

            if (this.bodyTrackingIncrements > 0) {
                double d = this.getX() + (this.serverX - this.getX()) / (double) this.bodyTrackingIncrements;
                double e = this.getY() + (this.serverY - this.getY()) / (double) this.bodyTrackingIncrements;
                double f = this.getZ() + (this.serverZ - this.getZ()) / (double) this.bodyTrackingIncrements;
                double g = MathHelper.wrapDegrees(this.serverYaw - (double) this.getYaw());
                this.setYaw(this.getYaw() + (float) g / (float) this.bodyTrackingIncrements);
                this.setPitch(this.getPitch() + (float) (this.serverPitch - (double) this.getPitch()) / (float) this.bodyTrackingIncrements);
                --this.bodyTrackingIncrements;
                this.setPosition(d, e, f);
                this.setRotation(this.getYaw(), this.getPitch());
            }

            if (this.headTrackingIncrements > 0) {
                this.headYaw += (float) MathHelper.wrapDegrees(this.serverHeadYaw - (double) this.headYaw) / (float) this.headTrackingIncrements;
                --this.headTrackingIncrements;
            }

            ci.cancel();
        }
    }
}
