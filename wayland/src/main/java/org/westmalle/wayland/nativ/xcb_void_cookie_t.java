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
package org.westmalle.wayland.nativ;

import com.sun.jna.Structure;

import java.util.Collections;
import java.util.List;

public class xcb_void_cookie_t extends Structure {

    private static final List<?> FIELD_ORDER = Collections.singletonList("sequence");

    public int sequence;

    protected List<?> getFieldOrder() {
        return FIELD_ORDER;
    }

    public static class ByValue extends xcb_void_cookie_t implements Structure.ByValue {

    }
}