package de.hglabor.snorlaxboss.mixin.entity;

import de.hglabor.snorlaxboss.entity.ILivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ILivingEntity {
    private static final TrackedData<Boolean> IS_PAUSED = DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private int invulnerableDuration = 20;

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

    @ModifyConstant(method = "damage", constant = @Constant(floatValue = 10.0f, ordinal = 0))
    private float useMaximumNoDamageTicksInjection(float value) {
        return this.invulnerableDuration / 2.0F;
    }

    @Redirect(method = "damage", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;timeUntilRegen:I", opcode = Opcodes.PUTFIELD))
    private void timeUntilRegenInjection(LivingEntity instance, int value) {
        instance.timeUntilRegen = ((ILivingEntity) instance).getMaximumNoDamageTicks();
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

    @Override
    public int getMaximumNoDamageTicks() {
        return invulnerableDuration;
    }

    @Override
    public void setMaximumNoDamageTicks(int ticks) {
        this.invulnerableDuration = ticks;
    }
}
