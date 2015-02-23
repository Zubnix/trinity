Westmalle
=====================

A Wayland OpenGL compositor written in pure Java.
This is a work in progress and not ready for day to day use, however
certain features like client movement and render output on X are considered ready.

Javadoc
=======
Not available yet.

Building
========
Run `maven install` in the root of the project.

Dependencies
============

 - JDK8.
 - Jogl. Available on maven central.
 - Google Auto-Factory. Available on maven central.
 - Google Dagger. Available on maven central.
 - Google Guava. Available on maven central.
 - jsr305. Available on maven central.
 - SLF4J. Available on maven central.
 - Wayland-Java-Bindings. Available on maven central.
 - Pixman-Java-Bindings. Available through [jitpack](https://jitpack.io/).
 - Jglm. Available on maven central.

State
=====
[![Build Status](https://travis-ci.org/Zubnix/westmalle.svg?branch=master)](https://travis-ci.org/Zubnix/westmalle)

| Functionality               | Implemented        |
| :-------------------------: | :----------------: |
| OpenGL on X                 | :heavy_check_mark: |
| OpenGL on KMS               | :x:                |
| Software rendering on X     | :x:                |
| Software rendering on KMS   | :x:                |
| Window moving               | :heavy_check_mark: |
| Window resizing             | :x:                |
| Mouse input                 | :heavy_check_mark: |
| Keyboard input              | :x:                |
| Touch input                 | :x:                |

Known Issues
============
 - None.

Roadmap
====
| Topic        | Progress   |
| :----------: | :--------: |
| unit tests   | 90%        |
| wl_shell     | 30%        |
| sw rendering | 0%         |
| xdg_shell    | 0%         |
| xwayland     | 0%         |
| DRM/KMS      | 0%         |
| multi seat   | 0%         |

License
=======
   Copyright 2015 Erik De Rijcke

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
