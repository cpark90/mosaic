#!/bin/bash

#x-terminal-emulator
cd ../../../bridge

x-terminal-emulator -e python3 carla_mosaic_bridge.py --bridge-server-port 8914 -m Town04 /home/mosaic/shared/carla/carla-federate/simulations/Town04.net.xml --step-length 0.1 --tls-manager sumo
