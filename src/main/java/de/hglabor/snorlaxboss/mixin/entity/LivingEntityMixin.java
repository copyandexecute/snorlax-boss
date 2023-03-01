package de.hglabor.snorlaxboss.mixin.entity;

import de.hglabor.snorlaxboss.entity.IPauseEntityMovement;
import de.hglabor.snorlaxboss.entity.player.ModifiedPlayerManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements IPauseEntityMovement {
    private static final TrackedData<Boolean> IS_PAUSED = DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initDataTrackerInjection(CallbackInfo ci) {
        this.dataTracker.startTracking(IS_PAUSED, false);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void travelInjectionInjection(CallbackInfo ci) {
        if (this.isPaused()) {
            ci.cancel();
        }
    }

    @Override
    public void pause() {
        this.dataTracker.set(IS_PAUSED, true);
    }

    @Override
    public void unpause() {
        this.dataTracker.set(IS_PAUSED, false);
    }

    @Override
    public boolean isPaused() {
        return this.dataTracker.get(IS_PAUSED);
    }
}
