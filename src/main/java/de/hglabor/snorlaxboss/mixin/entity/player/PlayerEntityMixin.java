package de.hglabor.snorlaxboss.mixin.entity.player;

import de.hglabor.snorlaxboss.entity.damage.DamageManager;
import de.hglabor.snorlaxboss.entity.player.ModifiedPlayer;
import de.hglabor.snorlaxboss.entity.player.ModifiedPlayerManager;
import net.minecraft.client.render.entity.model.ParrotEntityModel;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements ModifiedPlayer {
    @Shadow
    public abstract void sendMessage(Text message, boolean overlay);

    private static final TrackedData<Boolean> IS_FLAT = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_SHAKY = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> REACH = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> MAX_SLEEP_TICKS = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> FORCE_SLEEPING = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private int flatJumps;
    private int sleepTicks;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initDataTrackerInjection(CallbackInfo ci) {
        this.dataTracker.startTracking(IS_FLAT, false);
        this.dataTracker.startTracking(IS_SHAKY, false);
        this.dataTracker.startTracking(REACH, 4.5f);
        this.dataTracker.startTracking(MAX_SLEEP_TICKS, 100);
        this.dataTracker.startTracking(FORCE_SLEEPING, false);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (IS_FLAT.equals(data)) {
            this.calculateDimensions();
        } else if (FORCE_SLEEPING.equals(data)) {
            if (isForceSleeping()) {
                this.sleepTicks = 0;
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickInjection(CallbackInfo ci) {
        if (isForceSleeping()) {
            this.sleepTicks++;
            if (sleepTicks >= getMaxSleepTicks()) {
                if (!world.isClient) {
                    this.setForceSleeping(false);
                }
            }
        } else {
            this.sleepTicks = Math.max(0, sleepTicks - 30);
        }
    }

    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    private void getActiveEyeHeightInjection(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (isFlat()) {
            cir.setReturnValue(getDimensions(pose).height);
        }
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void getDimensionsInjection(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (isFlat()) {
            cir.setReturnValue(EntityDimensions.fixed(2.0f, 0.2f));
        }
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        return super.getDimensions(pose);
    }

    public float getJumpVelocity() {
        if (!isFlat()) {
            return 0.42F * this.getJumpVelocityMultiplier();
        } else {
            return 0.22F * this.getJumpVelocityMultiplier();
        }
    }

    @Inject(method = "updatePose", at = @At("TAIL"))
    private void updatePoseInjection(CallbackInfo ci) {
        if (isForceSleeping()) {
            setPose(EntityPose.SLEEPING);
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

    @Redirect(method = "tickRiding", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;stopRiding()V"))
    private void tickRidingInjection(PlayerEntity instance) {
        ModifiedPlayerManager.INSTANCE.handleSnorlaxDismount(instance);
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

    @Override
    public void setFlatJumps(int amount) {
        flatJumps = amount;
    }

    @Override
    public int getFlatJumps() {
        return flatJumps;
    }

    @Override
    public void setNormalReach(float value) {
        this.dataTracker.set(REACH, value);
    }

    @Override
    public float getNormalReach() {
        return this.dataTracker.get(REACH);
    }

    @Override
    public boolean isForceSleeping() {
        return this.dataTracker.get(FORCE_SLEEPING);
    }

    @Override
    public void setForceSleeping(boolean value) {
        this.dataTracker.set(FORCE_SLEEPING, value);
    }

    @Override
    public int getSleepTicks() {
        return sleepTicks;
    }

    @Override
    public void setSleepTicks(int sleepTicks) {
        this.sleepTicks = sleepTicks;
    }

    @Override
    public void setMaxSleepTicks(int i) {
        this.dataTracker.set(MAX_SLEEP_TICKS, i);
    }

    @Override
    public int getMaxSleepTicks() {
        return this.dataTracker.get(MAX_SLEEP_TICKS);
    }
}
