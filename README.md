SpeedReader
==========

SpeedReader is a read-optimized distributed key-value store written by Alex Dao,
Gautam Hathi, and Joy Patel, based on a fork of DDDFS (now deprecated).

### Building
We use Gradle as our build system. Use the `build.gradle` file to set up with
your IDE of choice.

### Code structure
All source files are located in the /src/ directory. We have also provided a
test suite found in /src/test/. You may compile the paper
directly via the `.tex` file in /doc/ directory. Certain sections from the original DDDFS paper (written by Alex Dao, Jiawei Zhang, and Danny Oh for CS 510 Spring 2016) were reused in this paper (describing performance load balancing, parts of the introduction, and infrastructure). 

### Libraries
App built with the help of these libs:
* [Redis](https://redis.io)
* [Spark](http://sparkjava.com)

License
--------

    Copyright 2016 Alex Dao, Gautam Hathi, Joy Patel.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
