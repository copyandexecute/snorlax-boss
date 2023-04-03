package de.hglabor.snorlaxboss.mixin.entity;

import de.hglabor.snorlaxboss.entity.BiggerFallingBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity implements BiggerFallingBlock {
    @Shadow
    private BlockState block;
    private static final TrackedData<Float> SIZE = DataTracker.registerData(FallingBlockEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private Entity shooter;

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

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/FallingBlockEntity;discard()V"))
    private void tickInjection(CallbackInfo ci) {
        if (getScaleSize() == 0) return;
        var size = this.getScaleSize() / 5;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    var blockPos = getBlockPos().add(x, y, z);
                    world.setBlockState(blockPos, block);
                    world.playSound(null, blockPos, block.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS);
                }
            }
        }
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        super.onPlayerCollision(player);
        if (!world.isClient) {
            //TODO probably more damage bzw scale with difficuzlty
            player.damage(DamageSource.fallingBlock(shooter),5.0f);
        }
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

    @Override
    public void setShooter(@Nullable Entity entity) {
        this.shooter = entity;
    }

    @Nullable
    @Override
    public Entity getShooter() {
        return shooter;
    }
}
