package de.hglabor.snorlaxboss.mixin.accessor;

import net.minecraft.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MoveControl.class)
public interface MoveControllAccessor {
    @Invoker("isPosWalkable")
    boolean invokeIsPosWalkable(float x, float z);
}
