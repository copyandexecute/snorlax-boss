package de.hglabor.snorlaxboss.render.gui.hud

import com.mojang.blaze3d.systems.RenderSystem
import de.hglabor.snorlaxboss.entity.player.ModifiedPlayer
import de.hglabor.snorlaxboss.gui.screen.ForceSleepingScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.util.math.MatrixStack

object SleepRenderer {
    fun handleSleepRendering(matrices: MatrixStack, scaledWidth: Int, scaledHeight: Int, client: MinecraftClient) {
        val player = client.player as ModifiedPlayer
        if (player.sleepTicks > 0) {
            client.profiler.push("forcesleep")
            RenderSystem.disableDepthTest()
            val h = player.sleepTicks.toFloat()
            var j: Float = h / player.maxSleepTicks.toFloat()
            if (j > 1.0f) {
                j = 1.0f - (h - player.maxSleepTicks.toFloat()) / 10.0f
            }
            val k = (220.0f * j).toInt() shl 24 or 1052704
            DrawableHelper.fill(matrices, 0, 0, scaledWidth, scaledHeight, k)
            RenderSystem.enableDepthTest()
            client.profiler.pop()
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        }
    }

    fun MinecraftClient.startSleepScreen() {
        if ((player as? ModifiedPlayer?)?.isForceSleeping == true && world != null) {
            setScreen(ForceSleepingScreen())
        }
    }

    fun MinecraftClient.stopSleepScreen() {
        if (currentScreen is ForceSleepingScreen && (player as? ModifiedPlayer?)?.isForceSleeping == false) {
            setScreen(null)
        }
    }
}
