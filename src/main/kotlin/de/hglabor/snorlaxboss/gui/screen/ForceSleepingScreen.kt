package de.hglabor.snorlaxboss.gui.screen

import de.hglabor.snorlaxboss.entity.player.ModifiedPlayerManager.tryWakeUp
import de.hglabor.snorlaxboss.network.NetworkManager.TRY_WAKE_UP
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class ForceSleepingScreen : Screen(Text.empty()) {
    private lateinit var stopSleepingButton: ButtonWidget

    override fun init() {
        super.init()
        stopSleepingButton = ButtonWidget.builder(
            Text.translatable("multiplayer.snorlaxboss.wakeUp")
        ) { tryWakeUp() }
            .dimensions(width / 2 - 100, height - 40, 200, 20).build()
        addDrawableChild(stopSleepingButton)
    }

    override fun close() = tryWakeUp()
    override fun shouldPause() = false
    private fun tryWakeUp() {
        TRY_WAKE_UP.send(Unit)
        client?.player?.tryWakeUp()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            tryWakeUp()
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}