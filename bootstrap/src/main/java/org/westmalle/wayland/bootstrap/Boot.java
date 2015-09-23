//Copyright 2015 Erik De Rijcke
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
package org.westmalle.wayland.bootstrap;

import org.westmalle.wayland.Application;
import org.westmalle.wayland.DaggerApplication;
import org.westmalle.wayland.core.Compositor;
import org.westmalle.wayland.core.CompositorFactory;
import org.westmalle.wayland.core.PointerDevice;
import org.westmalle.wayland.core.RenderEngine;
import org.westmalle.wayland.egl.EglRenderEngineFactory;
import org.westmalle.wayland.protocol.WlCompositor;
import org.westmalle.wayland.protocol.WlCompositorFactory;
import org.westmalle.wayland.protocol.WlDataDeviceManagerFactory;
import org.westmalle.wayland.protocol.WlKeyboard;
import org.westmalle.wayland.protocol.WlOutput;
import org.westmalle.wayland.protocol.WlSeat;
import org.westmalle.wayland.protocol.WlShellFactory;
import org.westmalle.wayland.x11.X11OutputFactory;
import org.westmalle.wayland.x11.X11SeatFactory;

import java.util.Arrays;
import java.util.logging.Logger;

class Boot {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(final String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread,
                                                   throwable) -> {
            LOGGER.severe("Got uncaught exception " + throwable.getMessage());
            throwable.printStackTrace();
        });

        LOGGER.info(String.format("Starting Westmalle:\n"
                                  + "\tArguments: %s",
                                  args.length == 0 ? "<none>" : Arrays.toString(args)));

        new Boot().strap(DaggerApplication.create());
    }

    private void strap(final Application application) {
        //get all required factory instances
        final CompositorFactory          compositorFactory          = application.compositorFactory();
        final WlCompositorFactory        wlCompositorFactory        = application.wlCompositorFactory();
        final WlDataDeviceManagerFactory wlDataDeviceManagerFactory = application.wlDataDeviceManagerFactory();
        final WlShellFactory             wlShellFactory             = application.wlShellFactory();

        //setup X11 input/output back-end.
        final X11OutputFactory outputFactory = application.x11()
                                                          .outputFactory();
        final X11SeatFactory seatFactory = application.x11()
                                                      .seatFactory();
        //setup egl render engine
        final EglRenderEngineFactory renderEngineFactory = application.egl()
                                                                      .renderEngineFactory();
        //create an output
        //create an X opengl enabled x11 window
        final WlOutput wlOutput = outputFactory.create(System.getenv("DISPLAY"),
                                                       800,
                                                       600);
        //setup our render engine
        final RenderEngine renderEngine = renderEngineFactory.create();

        //setup compositing for output support
        //create a compositor with shell and scene logic
        final Compositor compositor = compositorFactory.create(renderEngine);

        //add our output to the compositor
        //TODO add output hotplug functionality (eg monitor hotplug)
        compositor.getWlOutputs()
                  .add(wlOutput);

        //create a wayland compositor that delegates it's requests to a compositor implementation.
        final WlCompositor wlCompositor = wlCompositorFactory.create(compositor);

        //create data device manager for drag and drop support
        wlDataDeviceManagerFactory.create();

        //setup seat for input support
        //create a seat that listens for input on the X opengl window and passes it on to a wayland seat.
        //TODO add seat hotplug functionality (eg multiple mouses)
        final WlSeat wlSeat = seatFactory.create(wlOutput,
                                                 compositor);
        //setup keyboard focus tracking to follow mouse pointer
        final WlKeyboard wlKeyboard = wlSeat.getWlKeyboard();
        final PointerDevice pointerDevice = wlSeat.getWlPointer()
                                                  .getPointerDevice();
        pointerDevice.getPointerFocusSignal()
                     .connect(event -> wlKeyboard.getKeyboardDevice()
                                                 .setFocus(wlKeyboard.getResources(),
                                                           pointerDevice.getFocus()));

        //enable wl_shell protocol for minimal desktop-like features (move, resize, cursor changes ...)
        wlShellFactory.create(wlCompositor);

        //enable xdg_shell protocol for full desktop-like features
        //TODO implement xdg_shell protocol

        //start the thingamabah
        application.shellService()
                   .start();
    }
}