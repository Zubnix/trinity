//Copyright 2016 Erik De Rijcke
//
//Licensed under the Apache License,Version2.0(the"License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,software
//distributed under the License is distributed on an"AS IS"BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
package org.westmalle.wayland.drm.egl;


import org.freedesktop.jaccall.Pointer;
import org.westmalle.wayland.core.GlRenderer;
import org.westmalle.wayland.drm.DrmConnector;
import org.westmalle.wayland.drm.DrmPlatform;
import org.westmalle.wayland.nativ.libEGL.EglGetPlatformDisplayEXT;
import org.westmalle.wayland.nativ.libEGL.LibEGL;
import org.westmalle.wayland.nativ.libdrm.Libdrm;
import org.westmalle.wayland.nativ.libgbm.Libgbm;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.util.logging.Logger;

import static java.lang.String.format;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_CLIENT_APIS;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_CONTEXT_CLIENT_VERSION;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_EXTENSIONS;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_NONE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_NO_CONTEXT;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_NO_DISPLAY;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_PLATFORM_GBM_KHR;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_VENDOR;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_VERSION;
import static org.westmalle.wayland.nativ.libgbm.Libgbm.GBM_BO_USE_RENDERING;
import static org.westmalle.wayland.nativ.libgbm.Libgbm.GBM_BO_USE_SCANOUT;
import static org.westmalle.wayland.nativ.libgbm.Libgbm.GBM_FORMAT_XRGB8888;

public class GbmEglPlatformFactory {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @Nonnull
    private final PrivateGbmEglPlatformFactory privateGbmEglPlatformFactory;
    @Nonnull
    private final Libdrm                       libdrm;
    @Nonnull
    private final Libgbm                       libgbm;
    @Nonnull
    private final LibEGL                       libEGL;
    @Nonnull
    private final DrmPlatform                  drmPlatform;
    @Nonnull
    private final GbmEglConnectorFactory       eglGbmConnectorFactory;
    @Nonnull
    private final GlRenderer                   glRenderer;

    @Inject
    GbmEglPlatformFactory(@Nonnull final PrivateGbmEglPlatformFactory privateGbmEglPlatformFactory,
                          @Nonnull final Libdrm libdrm,
                          @Nonnull final Libgbm libgbm,
                          @Nonnull final LibEGL libEGL,
                          @Nonnull final DrmPlatform drmPlatform,
                          @Nonnull final GbmEglConnectorFactory eglGbmConnectorFactory,
                          @Nonnull final GlRenderer glRenderer) {
        this.privateGbmEglPlatformFactory = privateGbmEglPlatformFactory;
        this.libdrm = libdrm;
        this.libgbm = libgbm;
        this.libEGL = libEGL;
        this.drmPlatform = drmPlatform;
        this.eglGbmConnectorFactory = eglGbmConnectorFactory;
        this.glRenderer = glRenderer;
    }

    public GbmEglPlatform create() {
        final long gbmDevice = this.libgbm.gbm_create_device(this.drmPlatform.getDrmFd());

        final DrmConnector[]    drmConnectors    = this.drmPlatform.getConnectors();
        final GbmEglConnector[] gbmEglConnectors = new GbmEglConnector[drmConnectors.length];

        for (int i = 0; i < drmConnectors.length; i++) {
            final DrmConnector drmConnector = drmConnectors[i];
            gbmEglConnectors[i] = createGbmEglConnector(gbmDevice,
                                                        drmConnector);
        }

        final long eglDisplay = createEglDisplay(gbmDevice);

        final String eglExtensions = Pointer.wrap(String.class,
                                                  this.libEGL.eglQueryString(eglDisplay,
                                                                             EGL_EXTENSIONS))
                                            .dref();
        final String eglClientApis = Pointer.wrap(String.class,
                                                  this.libEGL.eglQueryString(eglDisplay,
                                                                             EGL_CLIENT_APIS))
                                            .dref();
        final String eglVendor = Pointer.wrap(String.class,
                                              this.libEGL.eglQueryString(eglDisplay,
                                                                         EGL_VENDOR))
                                        .dref();
        final String eglVersion = Pointer.wrap(String.class,
                                               this.libEGL.eglQueryString(eglDisplay,
                                                                          EGL_VERSION))
                                         .dref();

        LOGGER.info(format("Creating X11 EGL output:\n"
                           + "\tEGL client apis: %s\n"
                           + "\tEGL vendor: %s\n"
                           + "\tEGL version: %s\n"
                           + "\tEGL extensions: %s",
                           eglClientApis,
                           eglVendor,
                           eglVersion,
                           eglExtensions));

        final long config = this.glRenderer.eglConfig(eglDisplay,
                                                      eglExtensions);
        final long eglContext = createEglContext(eglDisplay,
                                                 config);

        return this.privateGbmEglPlatformFactory.create(gbmDevice,
                                                        eglDisplay,
                                                        eglContext,
                                                        eglExtensions,
                                                        gbmEglConnectors);
    }

    private long createEglContext(final long eglDisplay,
                                  final long config) {
        final Pointer<?> eglContextAttribs = Pointer.nref(
                //@formatter:off
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL_NONE
                //@formatter:on
                                                         );
        final long context = this.libEGL.eglCreateContext(eglDisplay,
                                                          config,
                                                          EGL_NO_CONTEXT,
                                                          eglContextAttribs.address);
        if (context == 0L) {
            throw new RuntimeException("eglCreateContext() failed");
        }
        return context;
    }

    private GbmEglConnector createGbmEglConnector(final long gbmDevice,
                                                  final DrmConnector drmConnector) {

        final long gbmSurface = this.libgbm.gbm_surface_create(gbmDevice,
                                                               drmConnector.getMode()
                                                                           .hdisplay(),
                                                               drmConnector.getMode()
                                                                           .vdisplay(),
                                                               GBM_FORMAT_XRGB8888,
                                                               GBM_BO_USE_SCANOUT | GBM_BO_USE_RENDERING);

        final long gbmBo = this.libgbm.gbm_surface_lock_front_buffer(gbmSurface);

        final GbmEglConnector gbmEglConnector = this.eglGbmConnectorFactory.create(this.drmPlatform.getDrmFd(),
                                                                                   gbmBo,
                                                                                   gbmSurface,
                                                                                   drmConnector);
        final int fbId = gbmEglConnector.getFbId(gbmBo);
        this.libdrm.drmModeSetCrtc(this.drmPlatform.getDrmFd(),
                                   drmConnector.getCrtcId(),
                                   fbId,
                                   0,
                                   0,
                                   Pointer.nref(drmConnector.getDrmModeConnector()
                                                            .connector_id()).address,
                                   1,
                                   Pointer.ref(drmConnector.getMode()).address);

        return gbmEglConnector;
    }

    private long createEglDisplay(final long gbmDevice) {

        final Pointer<String> noDisplayExtensions = Pointer.wrap(String.class,
                                                                 this.libEGL.eglQueryString(EGL_NO_DISPLAY,
                                                                                            EGL_EXTENSIONS));
        if (noDisplayExtensions.address == 0L) {
            throw new RuntimeException("Could not query egl extensions.");
        }
        final String extensions = noDisplayExtensions.dref();

        if (!extensions.contains("EGL_EXT_platform_gbm")) {
            throw new RuntimeException("Required extension EGL_EXT_platform_gbm not available.");
        }

        final Pointer<EglGetPlatformDisplayEXT> eglGetPlatformDisplayEXT = Pointer.wrap(EglGetPlatformDisplayEXT.class,
                                                                                        this.libEGL.eglGetProcAddress(Pointer.nref("eglGetPlatformDisplayEXT").address));

        final long eglDisplay = eglGetPlatformDisplayEXT.dref()
                                                        .$(EGL_PLATFORM_GBM_KHR,
                                                           gbmDevice,
                                                           0L);
        if (eglDisplay == 0L) {
            throw new RuntimeException("eglGetDisplay() failed");
        }
        if (this.libEGL.eglInitialize(eglDisplay,
                                      0L,
                                      0L) == 0) {
            throw new RuntimeException("eglInitialize() failed");
        }

        return eglDisplay;
    }
}