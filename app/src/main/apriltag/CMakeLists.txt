# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)
project(Project)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add.library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library(# Specifies the name of the library.
            apriltag

            # Sets the library as a shared library.
            SHARED

            # Provides a relative path to your source file(s).
            apriltag.c
            apriltag_jni.c
            apriltag_quad_thresh.c
            common/g2d.c
            common/getopt.c
            common/homography.c
            common/image_f32.c
            common/image_u8.c
            common/image_u8x3.c
            common/matd.c
            common/pnm.c
            common/string_util.c
            common/svd22.c
            common/time_util.c
            common/unionfind.c
            common/workerpool.c
            common/zarray.c
            common/zhash.c
            common/zmaxheap.c
            tag16h5.c
            tag25h7.c
            tag25h9.c
            tag36artoolkit.c
            tag36h10.c
            tag36h11.c
        )

STRING(REPLACE "-O0" "-O4" CMAKE_C_FLAGS_DEBUG ${CMAKE_C_FLAGS_DEBUG})
STRING(REPLACE "-O2" "-O4" CMAKE_C_FLAGS_RELEASE ${CMAKE_C_FLAGS_RELEASE})

include_directories(.)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
target_link_libraries(apriltag ${log-lib} ${jnigraphics-lib})
