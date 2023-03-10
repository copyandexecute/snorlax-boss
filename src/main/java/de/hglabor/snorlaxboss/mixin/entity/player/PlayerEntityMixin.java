package de.hglabor.snorlaxboss.mixin.entity.player;

import de.hglabor.snorlaxboss.entity.damage.DamageManager;
import de.hglabor.snorlaxboss.entity.player.ModifiedPlayer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements ModifiedPlayer {
    private static final TrackedData<Boolean> IS_FLAT = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_SHAKY = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initDataTrackerInjection(CallbackInfo ci) {
        this.dataTracker.startTracking(IS_FLAT, false);
        this.dataTracker.startTracking(IS_SHAKY, false);
    }

    public float getJumpVelocity() {
        if (!isFlat()) {
            return 0.42F * this.getJumpVelocityMultiplier();
        } else {
            return 0.22F * this.getJumpVelocityMultiplier();
        }
    }

    @Override
    public boolean blockedByShield(DamageSource source) {
        if (source == DamageManager.INSTANCE.getHYPERBEAM()) {
            return this.isBlocking();
        } else {
            return super.blockedByShield(source);
        }
    }

    @Override
    public boolean isFlat() {
        return this.dataTracker.get(IS_FLAT);
    }

    @Override
    public void setFlat(boolean flag) {
        this.dataTracker.set(IS_FLAT, flag);
    }

    @Override
    public void setShaky(boolean flag) {
        this.dataTracker.set(IS_SHAKY, flag);
    }

    @Override
    public boolean isShaky() {
        return this.dataTracker.get(IS_SHAKY);
    }
}
