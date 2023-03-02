package de.hglabor.snorlaxboss.render.camera

import com.google.common.util.concurrent.AtomicDouble
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import kotlinx.serialization.Serializable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.Util
import kotlin.random.Random

//Credits to https://github.com/LoganDark/fabric-camera-shake
object CameraShaker {
    var avgX: Double = 0.0
    var avgY: Double = 0.0
    private var smooth: Int = 3
    private var pastI: Int = 0
    private val pastX: DoubleArray = DoubleArray(smooth)
    private val pastY: DoubleArray = DoubleArray(smooth)
    private val events: MutableSet<CameraShakeEvent> = HashSet()
    private val providers: MutableSet<CameraShakeProvider> = HashSet()
    private var eventInceptions = Object2LongOpenHashMap<CameraShakeEvent>()

    fun newFrame() {
        val magnitude: Double = getCameraShakeMagnitude(MinecraftClient.getInstance().player)

        val x: Double = (Random.nextDouble() - .5) * magnitude
        val y: Double = (Random.nextDouble() - .5) * magnitude

        pastX[pastI] = x
        pastY[pastI++] = y
        pastI %= smooth

        calculateAvg()
    }

    private fun calculateAvg() {
        avgX = .0
        avgY = .0
        for (i in 0 until smooth) {
            avgX += pastX[i]
            avgY += pastY[i]
        }
        avgX /= smooth.toDouble()
        avgY /= smooth.toDouble()
    }

    interface CameraShakeEvent {
        fun isValid(t: Double): Boolean
        fun getCameraShakeMagnitude(t: Double): Double
    }

    interface CameraShakeProvider {
        fun getCameraShakeMagnitude(player: ClientPlayerEntity?): Double
    }

    @Serializable
    data class BoomShake(private var magnitude: Double, private var sustain: Double, private var fade: Double) :
        CameraShakeEvent {
        override fun isValid(t: Double): Boolean = t < sustain + fade
        override fun getCameraShakeMagnitude(t: Double): Double {
            return when {
                t <= sustain -> magnitude
                else -> magnitude * (1 - (t - sustain) / fade)
            }
        }
    }

    private fun getCameraShakeMagnitude(player: ClientPlayerEntity?): Double {
        val magnitude = AtomicDouble()
        val eventIterator: MutableIterator<CameraShakeEvent> = events.iterator()
        eventIterator.forEachRemaining { event: CameraShakeEvent ->
            val t: Double
            try {
                t = getTime(event)
            } catch (e: IllegalArgumentException) {
                eventIterator.remove()
                return@forEachRemaining
            }
            if (!event.isValid(t)) {
                eventIterator.remove()
                onEventRemoved(event)
            } else {
                magnitude.addAndGet(event.getCameraShakeMagnitude(t))
            }
        }
        for (provider in providers) {
            magnitude.addAndGet(provider.getCameraShakeMagnitude(player))
        }
        return magnitude.get()
    }

    private fun <E : CameraShakeEvent> onEventRemoved(event: E) {
        eventInceptions.removeLong(event)
    }

    private fun <E : CameraShakeEvent?> onEventAdded(event: E) {
        eventInceptions.put(event, Util.getMeasuringTimeNano())
    }

    private fun getTime(event: CameraShakeEvent): Double {
        require(eventInceptions.containsKey(event)) { "Passed event was not added to this CameraShakeManager" }
        val then: Long = eventInceptions.getLong(event)
        val now = Util.getMeasuringTimeNano()
        val delta = now - then
        return delta.toDouble() / 1000000000.0
    }

    fun <E : CameraShakeEvent> addEvent(event: E): E? {
        if (events.add(event)) {
            onEventAdded(event)
            return event
        }
        return null
    }
}
