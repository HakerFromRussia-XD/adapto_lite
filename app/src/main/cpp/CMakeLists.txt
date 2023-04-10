
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.

project("bt_drv")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             bt_drv

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
        bt_drv.c
        ble_tcp.c
        ble_eth.c
        eth_mem.c
        eth_router.c
        eth_nat.c
        eth_queue.c
        xlib.c
        settings.c
        images.c


#        LWIP
#        lwip/api/api_lib.c
#//        lwip/api/api_msg.c
#//        lwip/api/err.c
#//        lwip/api/if_api.c
#//        lwip/api/tcpip.c
        lwip/dnsproxy.c
        lwip/dhcp_server.c
        lwip/net_raw.c
        lwip/net_topology.c
        lwip/lwip.c
        lwip/eth_router_if_lwip.c
        lwip/core/def.c
        lwip/core/inet_chksum.c
        lwip/core/init.c
        lwip/core/ip.c
        lwip/core/mem.c
        lwip/core/memp.c
        lwip/core/netif.c
        lwip/core/pbuf.c
        lwip/core/raw.c
        lwip/core/stats.c
        lwip/core/sys.c
        lwip/core/tcp.c
        lwip/core/tcp_in.c
        lwip/core/tcp_out.c
        lwip/core/timeouts.c
        lwip/core/udp.c
        lwip/core/ipv4/icmp.c
        lwip/core/ipv4/ip4.c
        lwip/core/ipv4/ip4_addr.c
        lwip/core/ipv4/ip4_frag.c
        lwip/core/ipv4/etharp.c
        lwip/netif/ethernet.c
        )
include_directories(
        .
        lwip
        lwip/include
)
# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       bt_drv

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
