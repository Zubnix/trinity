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
package org.westford.compositor.drm


import org.freedesktop.jaccall.Pointer
import org.westford.nativ.libdrm.DrmEventContext
import org.westford.nativ.libdrm.Libdrm
import org.westford.nativ.libdrm.Pointerpage_flip_handler
import org.westford.nativ.libdrm.Pointervblank_handler
import javax.inject.Inject

class DrmEventBusFactory @Inject
internal constructor(private val privateDrmEventBusFactory: PrivateDrmEventBusFactory) {

    fun create(drmFd: Int): DrmEventBus {
        val drmEventContextP = Pointer.malloc<DrmEventContext>(DrmEventContext.SIZE,
                DrmEventContext::class.java!!)

        val drmEventBus = this.privateDrmEventBusFactory.create(drmFd,
                drmEventContextP.address)

        val drmEventContext = drmEventContextP.dref()
        drmEventContext.version(Libdrm.DRM_EVENT_CONTEXT_VERSION)
        drmEventContext.page_flip_handler(Pointerpage_flip_handler.nref(???({ fd, sequence, tv_sec, tv_usec, user_data -> drmEventBus.pageFlipHandler(fd, sequence, tv_sec, tv_usec, user_data) })))
        drmEventContext.vblank_handler(Pointervblank_handler.nref(???({ fd, sequence, tv_sec, tv_usec, user_data -> drmEventBus.vblankHandler(fd, sequence, tv_sec, tv_usec, user_data) })))

        return drmEventBus
    }
}