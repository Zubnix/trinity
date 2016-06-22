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
package org.westmalle.wayland.nativ.libbcm_host;

import org.freedesktop.jaccall.CType;
import org.freedesktop.jaccall.Field;
import org.freedesktop.jaccall.Struct;

@Struct({
                @Field(name = "width",
                       type = CType.INT),
                @Field(name = "height",
                       type = CType.INT),
                @Field(name = "transform",
                       type = CType.INT),
                @Field(name = "input_format",
                       type = CType.INT),
                @Field(name = "display_num",
                       type = CType.INT),
        })
public final class DISPMANX_MODEINFO_T extends DISPMANX_MODEINFO_T_Jaccall_StructType {}
