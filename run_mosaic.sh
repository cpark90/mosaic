docker run -it --rm --name mosaic \
	--network host \
	-v /var/run/docker.sock:/var/run/docker.sock \
	-v /tmp:/tmp \
	-v mosaic:/app/tmp \
	-v mosaic-logs:/app/logs \
	-v ~/git/mosaic_bazel/bundle/src/assembly/resources/etc:/app/etc \
	-v ~/git/mosaic_bazel/bundle/src/assembly/resources/scenarios:/app/scenarios \
        -v ~/git/mosaic/bundle/src/assembly/resources/fed:/app/fed \
	bazel/rti/mosaic-starter:mosaic-image -s Tiergarten -w 0 -b 1
#	bazel/rti/mosaic-starter:mosaic-image -s Barnim
#	bazel/rti/mosaic-starter:mosaic-image -s Tiergarten 
#	bazel/rti/mosaic-starter:mosaic-image -s LuST 
#	bazel/rti/mosaic-starter:mosaic-image -s Highway 
#	bazel/rti/mosaic-starter:mosaic-image -s Sievekingplatz 
