/*
 * Copyright (c) 2020 Fraunhofer FOKUS and others. All rights reserved.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contact: mosaic@fokus.fraunhofer.de
 */

package org.eclipse.mosaic.rti.api.mediatorstarter;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import org.eclipse.mosaic.lib.docker.DockerClient;
import org.eclipse.mosaic.lib.docker.DockerContainer;
import org.eclipse.mosaic.lib.docker.DockerRun;
import org.eclipse.mosaic.rti.api.MediatorExecutor;
import org.eclipse.mosaic.rti.config.CLocalHost;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.annotation.Nullable;

/**
 * A federate executor which starts a docker container which contains the federate.
 */
public class DockerMediatorExecutor implements MediatorExecutor {

    private final String image;
    private final String sharedDirectoryPath;
    private final String imageVolume;
    private final List<String> args;
    private List<Pair<Integer, Integer>> portBindings = new Vector<>();
    private String containerName;

    private final Map<String, Object> parameters = new HashMap<>();
    private DockerClient dockerClient;
    private DockerContainer container;

    /**
     * Creates a new DockerMediatorExecutor instance.
     *
     * @param image               the name of the docker image to create and start a container from
     * @param sharedDirectoryPath the local sub directory to share with container, relative to the working directory
     * @param imageVolume         the path to the image volume which is bound with the sharedDirectoryPath
     */

    public DockerMediatorExecutor(String image, String sharedDirectoryPath, String imageVolume, String... args) {
        this(image, sharedDirectoryPath, imageVolume, Arrays.asList(args));
    }

    public DockerMediatorExecutor(String image, String sharedDirectoryPath, String imageVolume, List<String> args) {
        this.image = image;
        this.containerName = StringUtils.substringBefore(image, ":");
        this.sharedDirectoryPath = sharedDirectoryPath;
        this.imageVolume = imageVolume;
        this.args = args;
    }

    /**
     * Adds an execution parameter which is passed to the docker container.
     */
    public DockerMediatorExecutor addParameter(String key, Object value) {
        parameters.put(key, value);
        return this;
    }

    public DockerMediatorExecutor addPortBinding(int portHost, int portContainer) {
        this.portBindings.add(Pair.of(portHost, portContainer));
        return this;
    }

    public DockerMediatorExecutor setContainerName(String name) {
        containerName = name;
        return this;
    }

    public Process getContainerLogsProcess() {
        return dockerClient.readLogs(containerName);
    }

    @Override
    public Process startLocalMediator(File fedDir) {
        this.dockerClient = new DockerClient();
        final DockerRun run;

        if (sharedDirectoryPath.startsWith("docker-volume:")) {
            run = this.dockerClient
                    .run(image)
                    .name(containerName)
                    // .removeAfterRun()
                    .networkHost()
                    .currentUser()
                    .args(args)
                    .volumeBinding(sharedDirectoryPath.replaceFirst("^docker-volume:", ""), imageVolume);
        } else {
            run = this.dockerClient
                    .run(image)
                    .name(containerName)
                    .removeAfterRun()
                    .currentUser()
                    .args(args)
                    .volumeBinding(new File(fedDir.getParent()), imageVolume);
        }

        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            run.parameter(param.getKey(), param.getValue());
        }

        for (Pair<Integer, Integer> binding : portBindings) {
            run.portBinding(binding.getKey(), binding.getValue());
        }

        this.container = run.execute();

        return this.container.getAppendedProcess();
    }

    /**
     * Returns an instance to the currently running docker container.
     * Returns <code>null</code> if the container has not been started yet.
     *
     * @return the currently running docker container, or <code>null</code> if no container is running
     */
    @Nullable
    public DockerContainer getRunningContainer() {
        return this.container;
    }

    @Override
    public void stopLocalMediator() {
        if (dockerClient != null) {
            dockerClient.killContainer(this.container, true);
            dockerClient.close();
        }
    }

    @Override
    public int startRemoteMediator(CLocalHost host, PrintStream sshStream, InputStream sshStreamIn) {
        throw new UnsupportedOperationException("Starting docker containers remotely is not supported yet.");
    }

    @Override
    public void stopRemoteMediator(PrintStream sshStreamOut) {
        throw new UnsupportedOperationException("Stopping docker containers remotely is not supported yet.");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
                .append("image", image)
                .append("containerName", containerName)
                .append("imageVolume", imageVolume)
                .append("containerSharePath", sharedDirectoryPath)
                .toString();
    }
}
