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
package org.westmalle.wayland.output;

import com.google.auto.factory.AutoFactory;
import com.sun.jna.ptr.IntByReference;
import org.freedesktop.pixman1.Pixman1Library;
import org.freedesktop.pixman1.pixman_box32;
import org.freedesktop.pixman1.pixman_region32;

import javax.annotation.Nonnull;
import javax.media.nativewindow.util.PointImmutable;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.RectangleImmutable;
import java.util.ArrayList;
import java.util.List;

@AutoFactory(className = "RegionFactory")
public class Region {

    private final pixman_region32 pixman_region32 = new pixman_region32();

    Region() {
    }

    public List<RectangleImmutable> asList() {
        //int pointer
        final IntByReference n_rects = new IntByReference();
        final pixman_box32 pixman_box32_array = Pixman1Library.INSTANCE
                                                              .pixman_region32_rectangles(getPixmanRegion32(),
                                                                                          n_rects);
        final int size = n_rects.getValue();
        final pixman_box32[] pixman_box32s = (pixman_box32[]) pixman_box32_array.toArray(size);

        final List<RectangleImmutable> boxes = new ArrayList<>(size);
        for (final pixman_box32 pixman_box32 : pixman_box32s) {
            final int x = pixman_box32.x1;
            final int y = pixman_box32.y1;

            final int width = pixman_box32.x2 - x;
            final int height = pixman_box32.y2 - y;
            boxes.add(new Rectangle(x,
                                    y,
                                    width,
                                    height));
        }
        return boxes;
    }

    public Region add(@Nonnull final RectangleImmutable rectangle) {
        //FIXME check result
        final int result = Pixman1Library.INSTANCE
                                         .pixman_region32_union_rect(getPixmanRegion32(),
                                                                     getPixmanRegion32(),
                                                                     rectangle.getX(),
                                                                     rectangle.getY(),
                                                                     rectangle.getWidth(),
                                                                     rectangle.getHeight());

        return this;
    }

    public Region subtract(@Nonnull final RectangleImmutable rectangle) {
        final pixman_region32 delta_pixman_region32 = new pixman_region32();
        Pixman1Library.INSTANCE
                      .pixman_region32_init_rect(delta_pixman_region32,
                                                 rectangle.getX(),
                                                 rectangle.getY(),
                                                 rectangle.getWidth(),
                                                 rectangle.getHeight());
        //FIXME check result
        final int result = Pixman1Library.INSTANCE
                                         .pixman_region32_subtract(getPixmanRegion32(),
                                                                   getPixmanRegion32(),
                                                                   delta_pixman_region32);
        return this;
    }

    public boolean contains(@Nonnull final PointImmutable point) {
        return Pixman1Library.INSTANCE
                             .pixman_region32_contains_point(getPixmanRegion32(),
                                                             point.getX(),
                                                             point.getY(),
                                                             null) != 0;
    }

    public boolean contains(@Nonnull final RectangleImmutable clipping,
                            @Nonnull final PointImmutable point) {
        //TODO test this method
        //FIXME check result
        final int result = Pixman1Library.INSTANCE
                                         .pixman_region32_intersect_rect(getPixmanRegion32(),
                                                                         getPixmanRegion32(),
                                                                         clipping.getX(),
                                                                         clipping.getY(),
                                                                         clipping.getWidth(),
                                                                         clipping.getHeight());

        return Pixman1Library.INSTANCE.pixman_region32_contains_point(getPixmanRegion32(),
                                                                      point.getX(),
                                                                      point.getY(),
                                                                      null) != 0;
    }

    public pixman_region32 getPixmanRegion32() {
        return this.pixman_region32;
    }
}
