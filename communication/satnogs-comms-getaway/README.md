# satnogs-comms-gateway: A gateway between hardware protocols and UDP for the SatNOGS-COMMS board

This tool serves as a gateway for messages from low-level hardware connections (such as CAN and ZeroMQ) to UDP and vice versa.

## Requirements
* CMake (>= 3.20)
* C++17
* GNU Make
* Linux CAN support
* libboost-system
* yaml-cpp (>= 0.8.0)
* GNU Radio (>= 3.10.0)
* cppzmq


## Build instructions

1. Clone the repository with all the necessary submodules
  ```bash
  git clone https://gitlab.com/librespacefoundation/satnogs-comms/satnogs-comms-yamcs.git
  ```
2. Configure and build the program:

  ```bash
  cd satnogs-comms-yamcs/gateway
  mkdir build
  cd build
  cmake ..
  make -j$(nproc)
  ./satnogs-comms-gateway -c ../contrib/config.yaml
  ```

## Usage
The tool uses a `YAML` configuration file for the parameters.

```bash
Usage: satnogs-comms-gateway [options] ARG1 ARG2
    -h, --help
        shows this help message
    -c, --config
        File path of the configuration file.
```
