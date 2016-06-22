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
package org.westmalle.wayland.nativ.libinput;

import org.freedesktop.jaccall.Functor;
import org.freedesktop.jaccall.Ptr;

@Functor
@FunctionalInterface
public interface libinput_log_handler {

    //TODO at va_list (args) support to jaccall
    void $(@Ptr long libinput, int priority, @Ptr(String.class) long format, @Ptr long va_list_args);
}
