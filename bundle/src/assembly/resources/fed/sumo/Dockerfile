FROM ubuntu:18.04

ENV NVIDIA_DRIVER_VERSION 470

USER root

# Install system dependencies.
RUN apt-get update && apt-get -qq install \
    cmake \
    python \
    g++ \
    libxerces-c-dev \
    libfox-1.6-dev \
    libgdal-dev \
    libproj-dev \
    libgl2ps-dev \
    swig \
    vim \
    mesa-utils \
    libgl1-mesa-glx \
    python3-pip \
    libnvidia-gl-${NVIDIA_DRIVER_VERSION} \
    git

RUN python3 -m pip install lxml

RUN useradd -m sumo

USER sumo
WORKDIR /home

ENV SUMO_VERSION 1_16_0

RUN cd /home/sumo && \
  git clone --recursive https://github.com/eclipse/sumo && \
  cd /home/sumo/sumo && \
  git checkout v${SUMO_VERSION} && \
  mkdir build/cmake-build && \
  cd build/cmake-build && \
  cmake ../.. && \
  make -j 4

WORKDIR /home/sumo/sumo

ENV PYTHONPATH=/home/sumo/sumo/tools
ENV PATH=$PATH:/home/sumo/sumo/bin
ENV LIBGL_ALWAYS_INDIRECT=1
ENV SUMO_HOME="$PWD"

RUN mkdir -p /home/sumo/mount

COPY --chown=sumo:sumo . /home/sumo/mount/

WORKDIR /home/sumo/sumo/bin
