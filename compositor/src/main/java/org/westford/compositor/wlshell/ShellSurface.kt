/*
 * Westford Wayland Compositor.
 * Copyright (C) 2016  Erik De Rijcke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.westford.compositor.wlshell

import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import org.freedesktop.wayland.server.Display
import org.freedesktop.wayland.server.EventSource
import org.freedesktop.wayland.server.WlKeyboardResource
import org.freedesktop.wayland.server.WlPointerResource
import org.freedesktop.wayland.server.WlShellSurfaceResource
import org.freedesktop.wayland.server.WlSurfaceResource
import org.freedesktop.wayland.shared.WlShellSurfaceResize
import org.freedesktop.wayland.shared.WlShellSurfaceTransient
import org.freedesktop.wayland.util.Fixed
import org.westford.Slot
import org.westford.compositor.core.Compositor
import org.westford.compositor.core.KeyboardDevice
import org.westford.compositor.core.Point
import org.westford.compositor.core.PointerDevice
import org.westford.compositor.core.Rectangle
import org.westford.compositor.core.Role
import org.westford.compositor.core.RoleVisitor
import org.westford.compositor.core.Scene
import org.westford.compositor.core.Sibling
import org.westford.compositor.core.Surface
import org.westford.compositor.core.SurfaceView
import org.westford.compositor.core.Transforms
import org.westford.compositor.core.calc.Mat4
import org.westford.compositor.core.calc.Vec4
import org.westford.compositor.core.events.KeyboardFocusGained
import org.westford.compositor.core.events.PointerGrab
import org.westford.compositor.protocol.WlKeyboard
import org.westford.compositor.protocol.WlPointer
import org.westford.compositor.protocol.WlSurface
import java.util.EnumSet
import java.util.Optional

@AutoFactory(className = "PrivateShellSurfaceFactory", allowSubclasses = true)
class ShellSurface internal constructor(@Provided display: Display,
                                        @param:Provided private val compositor: Compositor,
                                        @param:Provided private val scene: Scene,
                                        private val surfaceView: SurfaceView,
                                        private val pingSerial: Int) : Role {
    private val timerEventSource: EventSource

    private var keyboardFocusListener = Optional.empty<Slot<KeyboardFocusGained>>()
    var isActive = true
        private set

    var clazz = Optional.empty<String>()
        set(clazz) {
            field = clazz
            this.compositor.requestRender()
        }
    var title = Optional.empty<String>()
        set(title) {
            field = title
            this.compositor.requestRender()
        }

    init {
        this.timerEventSource = display.eventLoop
                .addTimer {
                    this.isActive = false
                    0
                }
    }

    fun pong(wlShellSurfaceResource: WlShellSurfaceResource,
             pingSerial: Int) {
        if (this.pingSerial == pingSerial) {
            this.isActive = true
            wlShellSurfaceResource.ping(pingSerial)
            this.timerEventSource.updateTimer(5000)
        }
    }

    fun move(wlSurfaceResource: WlSurfaceResource,
             wlPointerResource: WlPointerResource,
             grabSerial: Int) {

        val wlSurface = wlSurfaceResource.implementation as WlSurface
        val surface = wlSurface.surface

        val wlPointer = wlPointerResource.implementation as WlPointer
        val pointerDevice = wlPointer.pointerDevice

        val pointerPosition = pointerDevice.position

        pointerDevice.grab
                .filter { grabSurfaceView ->
                    surface.views
                            .contains(grabSurfaceView)
                }
                .ifPresent { grabSurfaceView ->
                    val surfacePosition = grabSurfaceView.global(Point.create(0,
                            0))
                    val pointerOffset = pointerPosition.subtract(surfacePosition)

                    //FIXME pick a surface view based on the pointer position
                    pointerDevice.grabMotion(wlSurfaceResource,
                            grabSerial
                    ) { motion ->
                        grabSurfaceView.setPosition(motion.point
                                .subtract(pointerOffset))
                    }
                }
    }

    fun resize(wlShellSurfaceResource: WlShellSurfaceResource,
               wlSurfaceResource: WlSurfaceResource,
               wlPointerResource: WlPointerResource,
               buttonPressSerial: Int,
               edges: Int) {

        val wlSurface = wlSurfaceResource.implementation as WlSurface
        val surface = wlSurface.surface

        val wlPointer = wlPointerResource.implementation as WlPointer
        val pointerDevice = wlPointer.pointerDevice
        val pointerStartPos = pointerDevice.position

        pointerDevice.grab
                .filter { grabSurfaceView ->
                    surface.views
                            .contains(grabSurfaceView)
                }
                .ifPresent { grabSurfaceView ->
                    val local = grabSurfaceView.local(pointerStartPos)
                    val size = surface.size

                    val quadrant = quadrant(edges)
                    val transform = transform(quadrant,
                            size,
                            local)

                    val inverseTransform = grabSurfaceView.inverseTransform

                    val grabMotionSuccess = pointerDevice.grabMotion(wlSurfaceResource,
                            buttonPressSerial
                    ) { motion ->
                        val motionLocal = inverseTransform.multiply(motion.point
                                .toVec4())
                        val resize = transform.multiply(motionLocal)
                        val width = resize.x.toInt()
                        val height = resize.y.toInt()
                        wlShellSurfaceResource.configure(quadrant.value,
                                if (width < 1) 1 else width,
                                if (height < 1) 1 else height)
                    }

                    if (grabMotionSuccess) {
                        wlPointerResource.leave(pointerDevice.nextLeaveSerial(),
                                wlSurfaceResource)
                        pointerDevice.pointerGrabSignal
                                .connect(object : Slot<PointerGrab> {
                                    override fun handle(event: PointerGrab) {
                                        if (!pointerDevice.grab
                                                .isPresent) {
                                            pointerDevice.pointerGrabSignal
                                                    .disconnect(this)
                                            wlPointerResource.enter(pointerDevice.nextEnterSerial(),
                                                    wlSurfaceResource,
                                                    Fixed.create(local.x),
                                                    Fixed.create(local.y))
                                        }
                                    }
                                })
                    }
                }
    }

    private fun quadrant(edges: Int): WlShellSurfaceResize {
        when (edges) {
            0 -> return WlShellSurfaceResize.NONE
            1 -> return WlShellSurfaceResize.TOP
            2 -> return WlShellSurfaceResize.BOTTOM
            4 -> return WlShellSurfaceResize.LEFT
            5 -> return WlShellSurfaceResize.TOP_LEFT
            6 -> return WlShellSurfaceResize.BOTTOM_LEFT
            8 -> return WlShellSurfaceResize.RIGHT
            9 -> return WlShellSurfaceResize.TOP_RIGHT
            10 -> return WlShellSurfaceResize.BOTTOM_RIGHT
            else -> return WlShellSurfaceResize.NONE
        }
    }

    private fun transform(quadrant: WlShellSurfaceResize,
                          size: Rectangle,
                          pointerLocal: Point): Mat4 {
        val width = size.width
        val height = size.height

        val transformationBuilder: Mat4.Builder
        val transformation: Mat4
        val pointerdx: Float
        val pointerdy: Float
        when (quadrant) {
            WlShellSurfaceResize.TOP -> {
                transformationBuilder = Transforms._180.toBuilder()
                        .m00(0f)
                        .m30(width.toFloat())
                transformation = transformationBuilder.build()
                val pointerLocalTransformed = transformation.multiply(pointerLocal.toVec4())
                pointerdx = 0f
                pointerdy = height - pointerLocalTransformed.y
            }
            WlShellSurfaceResize.TOP_LEFT -> {
                transformationBuilder = Transforms._180.toBuilder()
                        .m30(width.toFloat())
                        .m31(height.toFloat())
                transformation = transformationBuilder.build()
                val localTransformed = transformation.multiply(pointerLocal.toVec4())
                pointerdx = width - localTransformed.x
                pointerdy = height - localTransformed.y
            }
            WlShellSurfaceResize.LEFT -> {
                transformationBuilder = Transforms.FLIPPED.toBuilder()
                        .m11(0f)
                        .m31(height.toFloat())
                transformation = transformationBuilder.build()
                val localTransformed = transformation.multiply(pointerLocal.toVec4())
                pointerdx = width - localTransformed.x
                pointerdy = 0f
            }
            WlShellSurfaceResize.BOTTOM_LEFT -> {
                transformationBuilder = Transforms.FLIPPED.toBuilder()
                        .m30(width.toFloat())
                transformation = transformationBuilder.build()
                val localTransformed = transformation.multiply(pointerLocal.toVec4())
                pointerdx = width - localTransformed.x
                pointerdy = height - localTransformed.y
            }
            WlShellSurfaceResize.RIGHT -> {
                transformationBuilder = Transforms.NORMAL.toBuilder()
                        .m11(0f)
                        .m31(height.toFloat())
                transformation = transformationBuilder.build()
                val localTransformed = transformation.multiply(pointerLocal.toVec4())
                pointerdx = width - localTransformed.x
                pointerdy = 0f
            }
            WlShellSurfaceResize.TOP_RIGHT -> {
                transformationBuilder = Transforms.FLIPPED_180.toBuilder()
                        .m31(height.toFloat())
                transformation = transformationBuilder.build()
                val localTransformed = transformation.multiply(pointerLocal.toVec4())
                pointerdx = width - localTransformed.x
                pointerdy = height - localTransformed.y
            }
            WlShellSurfaceResize.BOTTOM -> {
                transformationBuilder = Transforms.NORMAL.toBuilder()
                        .m00(0f)
                        .m30(width.toFloat())
                transformation = transformationBuilder.build()
                val pointerLocalTransformed = transformation.multiply(pointerLocal.toVec4())
                pointerdx = 0f
                pointerdy = height - pointerLocalTransformed.y
            }
            WlShellSurfaceResize.BOTTOM_RIGHT -> {
                transformationBuilder = Transforms.NORMAL.toBuilder()
                transformation = transformationBuilder.build()
                val localTransformed = pointerLocal.toVec4()
                pointerdx = width - localTransformed.x
                pointerdy = height - localTransformed.y
            }
            else -> {
                transformationBuilder = Transforms.NORMAL.toBuilder()
                transformation = transformationBuilder.build()
                pointerdx = 0f
                pointerdy = 0f
            }
        }

        return transformationBuilder.m30(transformation.m30 + pointerdx)
                .m31(transformation.m31 + pointerdy)
                .build()
    }

    fun setTransient(wlSurfaceResource: WlSurfaceResource,
                     parent: WlSurfaceResource,
                     x: Int,
                     y: Int,
                     flags: EnumSet<WlShellSurfaceTransient>) {
        val wlSurface = wlSurfaceResource.implementation as WlSurface
        val surface = wlSurface.surface

        this.keyboardFocusListener.ifPresent { slot ->
            surface.keyboardFocusGainedSignal
                    .disconnect(slot)
        }

        if (flags.contains(WlShellSurfaceTransient.INACTIVE)) {
            val slot = { keyboardFocusGained ->
                //clean collection of focuses, so they don't get notify of keyboard related events
                surface.keyboardFocuses
                        .clear()
            }
            surface.keyboardFocusGainedSignal
                    .connect(slot)

            //first time focus clearing, also send out leave events
            val keyboardFocuses = surface.keyboardFocuses
            keyboardFocuses.forEach { wlKeyboardResource ->
                val wlKeyboard = wlKeyboardResource.implementation as WlKeyboard
                val keyboardDevice = wlKeyboard.keyboardDevice
                wlKeyboardResource.leave(keyboardDevice.nextKeyboardSerial(),
                        wlSurfaceResource)
            }
            keyboardFocuses.clear()

            this.keyboardFocusListener = Optional.of<Slot<KeyboardFocusGained>>(slot)
        }

        val parentWlSurface = parent.implementation as WlSurface
        val parentSurface = parentWlSurface.surface

        this.scene.removeView(this.surfaceView)
        parentSurface.views
                .forEach(Consumer<SurfaceView> { this.surfaceView.setParent(it) })

        parentSurface.addSibling(Sibling.create(wlSurfaceResource,
                Point.create(x,
                        y)))
    }

    override fun accept(roleVisitor: RoleVisitor) {
        roleVisitor.visit(this)
    }

    fun setTopLevel(wlSurfaceResource: WlSurfaceResource) {
        val wlSurface = wlSurfaceResource.implementation as WlSurface
        val surface = wlSurface.surface

        surface.views
                .forEach { surfaceView ->
                    surfaceView.parent
                            .ifPresent { parentSurfaceView ->
                                val parentWlSurfaceResource = parentSurfaceView.wlSurfaceResource
                                val parentWlSurface = parentWlSurfaceResource.implementation as WlSurface
                                val parentSurface = parentWlSurface.surface
                                parentSurface.removeSibling(Sibling.create(wlSurfaceResource))
                            }
                }

        this.scene.removeView(this.surfaceView)
        this.scene.applicationLayer
                .surfaceViews
                .add(this.surfaceView)
    }
}