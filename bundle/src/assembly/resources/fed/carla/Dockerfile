FROM carlasim/carla:0.9.14

ENV CARLA_VERSION 0_9_14
ENV NVIDIA_DRIVER_VERSION 470

USER root

# Install system dependencies.
RUN apt-get update && apt-get -qq install \
    vim \
    mesa-utils \
    libgl1-mesa-glx \
    python3-pip \
    libnvidia-gl-${NVIDIA_DRIVER_VERSION} \
    git

RUN python3 -m pip install lxml

USER carla

WORKDIR /home/carla

ENV CARLA_HOME="$PWD"

COPY --chown=carla:carla ./bridge /home/carla/bridge
