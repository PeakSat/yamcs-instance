cmake_minimum_required(VERSION 3.20)
project(satnogs-comms-gateway CXX C)

set(CMAKE_CXX_STANDARD 17)

set(CMAKE_EXPORT_COMPILE_COMMANDS TRUE)

# ##############################################################################
# CMake custom modules
# ##############################################################################
list(INSERT CMAKE_MODULE_PATH 0 ${CMAKE_SOURCE_DIR}/cmake)

# Mandatory requirements
find_package(Threads REQUIRED)
find_package(Boost 1.69.0 REQUIRED COMPONENTS system)
find_package(yaml-cpp REQUIRED)
find_package(cppzmq REQUIRED)
find_package(Gnuradio "3.10" REQUIRED COMPONENTS pmt)

# ##############################################################################
# Setup the include and linker paths
# ##############################################################################
include_directories(${CMAKE_SOURCE_DIR} ${CMAKE_BINARY_DIR})

# ##############################################################################
# Installation details
# ##############################################################################
include(CPack)

add_executable(
  satnogs-comms-gateway src/can_transport.cpp src/gr_zmq_transport.cpp
                        src/main.cpp src/udp_transport.cpp)

target_include_directories(satnogs-comms-gateway
                           PRIVATE "${PROJECT_BINARY_DIR}")

target_link_libraries(
  satnogs-comms-gateway
  PUBLIC m ${CMAKE_THREAD_LIBS_INIT} ${Boost_SYSTEM_LIBRARY} yaml-cpp::yaml-cpp
         cppzmq gnuradio::gnuradio-pmt)
