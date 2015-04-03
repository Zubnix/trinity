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
package org.westmalle.wayland.protocol;

import com.google.auto.factory.AutoFactory;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.freedesktop.wayland.server.Client;
import org.freedesktop.wayland.server.WlTouchRequestsV3;
import org.freedesktop.wayland.server.WlTouchResource;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Set;
import java.util.WeakHashMap;

@AutoFactory(className = "WlTouchFactory")
public class WlTouch implements WlTouchRequestsV3, ProtocolObject<WlTouchResource> {

    private final Set<WlTouchResource> resources = Sets.newSetFromMap(new WeakHashMap<>());

    @Inject
    WlTouch() {
    }

    @Override
    public void release(final WlTouchResource resource) {
        resource.destroy();
    }

    @Nonnull
    @Override
    public Set<WlTouchResource> getResources() {
        return this.resources;
    }

    @Nonnull
    @Override
    public WlTouchResource create(@Nonnull final Client client,
                                  @Nonnegative final int version,
                                  final int id) {
        return new WlTouchResource(client,
                                   version,
                                   id,
                                   this);
    }
}
