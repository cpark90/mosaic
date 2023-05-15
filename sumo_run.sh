parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )

docker run -it --rm\
    --network=host \
    --gpus=all \
    --env="DISPLAY" \
    --volume="/tmp/.X11-unix:/tmp/.X11-unix:rw" \
    -v $parent_path:/home/sumo/mount:rw \
    --user=root \
    sumo:mosaic \
    $*
