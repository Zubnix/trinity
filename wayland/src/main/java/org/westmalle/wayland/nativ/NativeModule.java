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

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class NativeModule {

    @Singleton
    @Provides
    Libpixman1 provideLibpixman1() {
        return new Libpixman1();
    }

    @Singleton
    @Provides
    Libc provideLibc() {
        return new Libc();
    }

    @Singleton
    @Provides
    LibEGL provideLibegl() {
        return new LibEGL();
    }

    @Singleton
    @Provides
    LibGLESv2 provideLibgles2() {
        return new LibGLESv2();
    }

    @Singleton
    @Provides
    LibX11 provideLibX11() {
        return new LibX11();
    }

    @Singleton
    @Provides
    Libxcb provideLibxcb() {
        return new Libxcb();
    }

    @Singleton
    @Provides
    LibX11xcb provideLibX11xcb() {
        return new LibX11xcb();
    }
}