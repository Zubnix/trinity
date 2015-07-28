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
package org.westmalle.wayland.x11;

import com.google.common.eventbus.Subscribe;

import org.freedesktop.wayland.shared.WlKeyboardKeyState;
import org.freedesktop.wayland.shared.WlPointerButtonState;
import org.westmalle.wayland.nativ.libxcb.Libxcb;
import org.westmalle.wayland.nativ.libxcb.xcb_button_press_event_t;
import org.westmalle.wayland.nativ.libxcb.xcb_button_release_event_t;
import org.westmalle.wayland.nativ.libxcb.xcb_key_press_event_t;
import org.westmalle.wayland.nativ.libxcb.xcb_key_release_event_t;
import org.westmalle.wayland.nativ.libxcb.xcb_motion_notify_event_t;
import org.westmalle.wayland.protocol.WlSeat;

import javax.annotation.Nonnull;

import static org.westmalle.wayland.nativ.Input.BTN_LEFT;
import static org.westmalle.wayland.nativ.Input.BTN_MIDDLE;
import static org.westmalle.wayland.nativ.Input.BTN_RIGHT;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_CURSOR_NONE;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_EVENT_MASK_BUTTON_PRESS;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_EVENT_MASK_BUTTON_RELEASE;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_EVENT_MASK_ENTER_WINDOW;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_EVENT_MASK_LEAVE_WINDOW;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_EVENT_MASK_POINTER_MOTION;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_GRAB_MODE_ASYNC;

public class X11Seat {

    @Nonnull
    private final Libxcb     libxcb;
    @Nonnull
    private final X11Output  x11Output;
    @Nonnull
    private final WlSeat     wlSeat;

    X11Seat(@Nonnull final Libxcb libxcb,
            @Nonnull final X11Output x11Output,
            @Nonnull final WlSeat wlSeat) {
        this.libxcb = libxcb;
        this.x11Output = x11Output;
        this.wlSeat = wlSeat;
    }

    @Subscribe
    public void handle(final xcb_key_press_event_t event) {
        deliverKey(event.detail,
                   true);
    }

    private void deliverKey(final short eventDetail,
                            final boolean pressed) {
        final WlKeyboardKeyState wlKeyboardKeyState = wlKeyboardKeyState(pressed);
        final int                key                = toLinuxKey(eventDetail);
        this.wlSeat.getOptionalWlKeyboard()
                   .ifPresent(wlKeyboard -> wlKeyboard.getKeyboardDevice()
                                                      .key(wlKeyboard.getResources(),
                                                           key,
                                                           wlKeyboardKeyState));
    }

    private WlKeyboardKeyState wlKeyboardKeyState(final boolean pressed) {
        final WlKeyboardKeyState wlKeyboardKeyState;
        if (pressed) {
            wlKeyboardKeyState = WlKeyboardKeyState.PRESSED;
        }
        else {
            wlKeyboardKeyState = WlKeyboardKeyState.RELEASED;
        }
        return wlKeyboardKeyState;
    }

    private int toLinuxKey(final short eventDetail) {
        //TODO convert from X keycodes to input.h keycodes, -> properly use xkbcommon
        return eventDetail;
    }

    @Subscribe
    public void handle(final xcb_button_press_event_t event) {
        deliverButton(event.time,
                      event.detail,
                      true);
    }

    private void deliverButton(final int buttonTime,
                               final short eventDetail,
                               final boolean pressed) {

        final WlPointerButtonState wlPointerButtonState = wlPointerButtonState(buttonTime,
                                                                               pressed);
        final int button = toLinuxButton(eventDetail);
        this.wlSeat.getOptionalWlPointer()
                   .ifPresent(wlPointer -> wlPointer.getPointerDevice()
                                                    .button(wlPointer.getResources(),
                                                            button,
                                                            wlPointerButtonState));
    }

    private WlPointerButtonState wlPointerButtonState(final int buttonTime,
                                                      final boolean pressed) {
        final WlPointerButtonState wlPointerButtonState;
        if (pressed) {
            wlPointerButtonState = WlPointerButtonState.PRESSED;
            this.libxcb.xcb_grab_pointer(this.x11Output.getXcbConnection(),
                                         (byte) 0,
                                         this.x11Output.getxWindow(),
                                         (short) (XCB_EVENT_MASK_BUTTON_PRESS |
                                                  XCB_EVENT_MASK_BUTTON_RELEASE |
                                                  XCB_EVENT_MASK_POINTER_MOTION |
                                                  XCB_EVENT_MASK_ENTER_WINDOW |
                                                  XCB_EVENT_MASK_LEAVE_WINDOW),
                                         (byte) XCB_GRAB_MODE_ASYNC,
                                         (byte) XCB_GRAB_MODE_ASYNC,
                                         this.x11Output.getxWindow(),
                                         XCB_CURSOR_NONE,
                                         buttonTime);
        }
        else {
            this.libxcb.xcb_ungrab_pointer(this.x11Output.getXcbConnection(),
                                           buttonTime);
            wlPointerButtonState = WlPointerButtonState.RELEASED;
        }
        return wlPointerButtonState;
    }

    private int toLinuxButton(final int eventDetail) {
        final int button;
        switch (eventDetail) {
            case 1:
                button = BTN_LEFT;
                break;
            case 2:
                button = BTN_MIDDLE;
                break;
            case 3:
                button = BTN_RIGHT;
                break;
            default:
                button = 0;
        }
        return button;
    }

    @Subscribe
    public void handle(final xcb_key_release_event_t event) {
        deliverKey(event.detail,
                   false);
    }

    @Subscribe
    public void handle(final xcb_button_release_event_t event) {
        deliverButton(event.time,
                      event.detail,
                      false);
    }

    @Subscribe
    public void handle(final xcb_motion_notify_event_t event) {
        final int x = event.event_x;
        final int y = event.event_y;

        this.wlSeat.getOptionalWlPointer()
                   .ifPresent(wlPointer -> wlPointer.getPointerDevice()
                                                    .motion(wlPointer.getResources(),
                                                            x,
                                                            y));
    }
}
