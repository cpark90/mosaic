#!/bin/bash

#x-terminal-emulator
cd ../../../bridge

x-terminal-emulator -e python3.7 carla_mosaic_bridge.py --bridge-server-port 8913 -m Town04 ../scenarios/Town04_20/sumo/Town04.net.xml --step-length 0.1 --tls-manager sumo
