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
package org.westmalle.wayland.nativ.libxkbcommon;

import org.freedesktop.jaccall.Lib;
import org.freedesktop.jaccall.Ptr;

import javax.inject.Singleton;

@Singleton
@Lib("xkbcommon")
public class Libxkbcommon {

    /**
     * Do not apply any context flags.
     */
    public static final int XKB_CONTEXT_NO_FLAGS             = 0;
    /**
     * Create this context with an empty include path.
     */
    public static final int XKB_CONTEXT_NO_DEFAULT_INCLUDES  = (1 << 0);
    /**
     * Don't take RMLVO names from the environment.
     *
     * @since 0.3.0
     */
    public static final int XKB_CONTEXT_NO_ENVIRONMENT_NAMES = (1 << 1);

    /**
     * Do not apply any flags.
     */
    public static final int XKB_KEYMAP_COMPILE_NO_FLAGS = 0;

    /**
     * The key was released.
     */
    public static final int XKB_KEY_UP   = 0;
    public static final int XKB_KEY_DOWN = 1;

    /**
     * Depressed modifiers, i.e. a key is physically holding them.
     */
    public static final int XKB_STATE_MODS_DEPRESSED   = (1 << 0);
    /**
     * Latched modifiers, i.e. will be unset after the next non-modifier key press.
     */
    public static final int XKB_STATE_MODS_LATCHED     = (1 << 1);
    /**
     * Locked modifiers, i.e. will be unset after the key provoking the lock has been pressed again.
     */
    public static final int XKB_STATE_MODS_LOCKED      = (1 << 2);
    /**
     * Effective modifiers, i.e. currently active and affect key processing (derived from the other state components).
     * Use this unless you explictly care how the state came about.
     */
    public static final int XKB_STATE_MODS_EFFECTIVE   = (1 << 3);
    /**
     * Depressed layout, i.e. a key is physically holding it.
     */
    public static final int XKB_STATE_LAYOUT_DEPRESSED = (1 << 4);
    /**
     * Latched layout, i.e. will be unset after the next non-modifier key press.
     */
    public static final int XKB_STATE_LAYOUT_LATCHED   = (1 << 5);
    /**
     * Locked layout, i.e. will be unset after the key provoking the lock has been pressed again.
     */
    public static final int XKB_STATE_LAYOUT_LOCKED    = (1 << 6);
    /**
     * Effective layout, i.e. currently active and affects key processing (derived from the other state components). Use
     * this unless you explictly care how the state came about.
     */
    public static final int XKB_STATE_LAYOUT_EFFECTIVE = (1 << 7);
    /**
     * LEDs (derived from the other state components).
     */
    public static final int XKB_STATE_LEDS             = (1 << 8);

    /**
     * The current/classic XKB text format, as generated by xkbcomp -xkb.
     */
    public static final int XKB_KEYMAP_FORMAT_TEXT_V1 = 1;


    /**
     * Create a new context.
     *
     * @param flags Optional flags for the context, or 0.
     *
     * @return A new context, or null on failure.
     */
    @Ptr
    public native long xkb_context_new(int flags);

    /**
     * Create a keymap from RMLVO names.
     * <p/>
     * The primary keymap entry point: creates a new XKB keymap from a set of RMLVO (Rules + Model + Layouts + Variants + Options) names.
     *
     * @param context The context in which to create the keymap.
     * @param names   The RMLVO names to use.
     * @param flags   Optional flags for the keymap, or 0.
     *
     * @return A keymap compiled according to the RMLVO names, or null if the compilation failed.
     */
    @Ptr
    public native long xkb_keymap_new_from_names(@Ptr long context,
                                                 @Ptr long names,
                                                 int flags);

    /**
     * Create a new keyboard state object.
     *
     * @param keymap The keymap which the state will use.
     *
     * @return A new keyboard state object, or null on failure.
     */
    @Ptr
    public native long xkb_state_new(@Ptr long keymap);

    /**
     * Update the keyboard state to reflect a given key being pressed or released.
     * <p/>
     * This entry point is intended for programs which track the keyboard state explictly (like an evdev client).
     * If the state is serialized to you by a master process (like a Wayland compositor) using functions like
     * xkb_state_serialize_mods(), you should use xkb_state_update_mask() instead. The two functins should not
     * generally be used together.
     * <p/>
     * A series of calls to this function should be consistent; that is, a call with XKB_KEY_DOWN for a key should be
     * matched by an XKB_KEY_UP; if a key is pressed twice, it should be released twice; etc. Otherwise (e.g. due to
     * missed input events), situations like "stuck modifiers" may occur.
     * <p/>
     * This function is often used in conjunction with the function xkb_state_key_get_syms() (or
     * xkb_state_key_get_one_sym()), for example, when handling a key event. In this case, you should prefer to get
     * the keysyms before updating the key, such that the keysyms reported for the key event are not affected by the
     * event itself. This is the conventional behavior.
     *
     * @param state
     * @param key
     * @param direction
     *
     * @return A mask of state components that have changed as a result of the update. If nothing in the state has
     * changed, returns 0.
     */
    public native int xkb_state_update_key(@Ptr long state,
                                           int key,
                                           int direction);


    /**
     * The counterpart to xkb_state_update_mask for layouts, to be used on the server side of serialization.
     *
     * @param state      The keyboard state.
     * @param components A mask of the layout state components to serialize. State components other than
     *                   XKB_STATE_LAYOUT_* are ignored. If XKB_STATE_LAYOUT_EFFECTIVE is included, all other state
     *                   components are ignored.
     *
     * @return A layout index representing the given components of the layout state.
     */
    public native int xkb_state_serialize_layout(@Ptr long state,
                                                 int components);

    /**
     * The counterpart to xkb_state_update_mask for modifiers, to be used on the server side of serialization.
     *
     * @param state      The keyboard state.
     * @param components A mask of the modifier state components to serialize. State components other than
     *                   XKB_STATE_MODS_* are ignored. If XKB_STATE_MODS_EFFECTIVE is included, all other state
     *                   components are ignored.
     *
     * @return A xkb_mod_mask_t representing the given components of the modifier state.
     */
    public native int xkb_state_serialize_mods(@Ptr long state,
                                               int components);

    /**
     * Release a reference on a keyboard state object, and possibly free it.
     *
     * @param state The state. If it is null, this function does nothing.
     */
    public native void xkb_state_unref(@Ptr long state);

    /**
     * Release a reference on a keymap, and possibly free it.
     *
     * @param keymap The keymap. If it is null, this function does nothing.
     */
    public native void xkb_keymap_unref(@Ptr long keymap);

    /**
     * Release a reference on a context, and possibly free it.
     *
     * @param context The context. If it is null, this function does nothing.
     */
    public native void xkb_context_unref(@Ptr long context);

    /**
     * Get the compiled keymap as a string.
     *
     * @param keymap The keymap to get as a string.
     * @param format The keymap format to use for the string. You can pass in the special value
     *               XKB_KEYMAP_USE_ORIGINAL_FORMAT to use the format from which the keymap was originally created.
     *
     * @return The keymap as a NUL-terminated string, or NULL if unsuccessful. The returned string is dynamically
     * allocated and should be freed by the caller.
     */
    @Ptr
    public native long xkb_keymap_get_as_string(@Ptr long keymap,
                                                int format);
}