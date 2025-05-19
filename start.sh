#!/bin/bash

# Usage: ./start.sh --flag1 | --flag2 | --flag3

case "$1" in
    --server)
        mvn yamcs:run
        ;;
    --uart-connection)
        if [ -z "$2" ]; then
            echo "Error: Please provide a port argument for --uart-connection"
            echo "Usage: $0 --uart-connection <port>"
            exit 1
        fi
        cd communication
        python3 messageHandler.py --port "$2"
        cd ..
        ;;
    --gnuradio-connection)
        echo "Starting GNURadio connection..."
        echo "Please ensure that the GNURadio flowgraph is running."
        echo "If you wish to change the port configuration, please edit the contrib/config.yaml file."
        echo "Starting SatNOGS COMMS Gateway..."
        cd communication/satnogs-comms-getaway/build
        ./satnogs-comms-gateway -c ../contrib/config.yaml
        cd ../../..
        ;;
    *)
        echo "Usage: $0 --server | --uart-connection | --gnuradio-connection"
        exit 1
        ;;
esac