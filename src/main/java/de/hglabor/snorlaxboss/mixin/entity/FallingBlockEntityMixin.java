package de.hglabor.snorlaxboss.mixin.entity;

import de.hglabor.snorlaxboss.entity.BiggerFallingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity implements BiggerFallingBlock {
    private static final TrackedData<Float> SIZE = DataTracker.registerData(FallingBlockEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public FallingBlockEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (SIZE.equals(data)) {
            this.calculateDimensions();
        }
        super.onTrackedDataSet(data);
    }

    public EntityDimensions getDimensions(EntityPose pose) {
        EntityDimensions entityDimensions = super.getDimensions(pose);
        float f = (entityDimensions.width + 0.2F * this.getScaleSize()) / entityDimensions.width;
        return entityDimensions.scaled(f);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initDataTrackerInjection(CallbackInfo ci) {
        this.dataTracker.startTracking(SIZE, 0f);
    }

    @Override
    public float getScaleSize() {
        return this.dataTracker.get(SIZE);
    }

    @Override
    public void setScaleSize(float v) {
        this.dataTracker.set(SIZE, v);
    }
}
