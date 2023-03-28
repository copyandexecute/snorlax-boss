package de.hglabor.snorlaxboss.mixin.network;

import de.hglabor.snorlaxboss.entity.player.ModifiedPlayerManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @ModifyConstant(method = "getReachDistance", constant = @Constant(floatValue = 4.5f))
    private float normalReachInjection(float constant) {
        return ModifiedPlayerManager.INSTANCE.handleNormalReachDistance(constant);
    }
}
