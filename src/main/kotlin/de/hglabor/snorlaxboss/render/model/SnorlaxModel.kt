package de.hglabor.snorlaxboss.render.model

import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.extension.toId
import net.minecraft.util.Identifier
import software.bernie.geckolib.model.GeoModel

class SnorlaxModel : GeoModel<Snorlax>() {
    companion object {
        val MODEL_ID = "geo/snorlax.geo.json".toId()
        val TEXTURE_ID = "textures/snorlax.png".toId()
        val ANIMATION_ID = "animations/snorlax.animation.json".toId()
    }

    override fun getModelResource(animatable: Snorlax): Identifier = MODEL_ID
    override fun getTextureResource(animatable: Snorlax) = TEXTURE_ID
    override fun getAnimationResource(animatable: Snorlax) = ANIMATION_ID
}
