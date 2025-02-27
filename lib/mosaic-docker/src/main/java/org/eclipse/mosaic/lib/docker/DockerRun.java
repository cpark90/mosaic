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

package org.eclipse.mosaic.lib.docker;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

/**
 * Provides methods to easily compose a "docker run" command.
 */
public class DockerRun {
    private final static Logger LOG = LoggerFactory.getLogger(DockerRun.class);

    private final DockerClient client;
    private final String image;
    private String name;
    private String workingDir;
    private String fedId;
    private List<Pair<String, Object>> parameters = new Vector<>();
    private List<Pair<Integer, Integer>> portBindings = new Vector<>();
    private String user;
    private List<String> args;
    private List<Pair<File, String>> hostVolumeBindings = new Vector<>();
    private List<Pair<String, String>> dockerVolumeBindings = new Vector<>();
    private boolean gpusAll = false;
    private boolean removeAfterRun = false;
    private boolean removeBeforeRun;

    DockerRun(DockerClient dockerClient, String image) {
        this.client = dockerClient;
        this.image = image;
    }

    /**
     * Used to set the containers name.
     *
     * @param name the name of the container
     */
    public DockerRun name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Marks the docker container to use gpus.
     */
    public DockerRun gpusAll() {
        this.gpusAll = true;
        return this;
    }

    /**
     * Marks the docker container to be removed after it has been run.
     */
    public DockerRun removeAfterRun() {
        this.removeAfterRun = true;
        return this;
    }

    /**
     * If containers with the same name are already running, this method
     * marks those containers to be removed beforehand.
     */
    public DockerRun removeBeforeRun() {
        this.removeBeforeRun = true;
        return this;
    }

    /**
     * Adds an explicit port binding to this docker run command. If
     * no port binding is defined, all exposed ports will be published
     * to random ports on the host.
     *
     * @param portHost      the local port on the host
     * @param portContainer the exposed port within the container, as defined in the Dockerfile
     */
    public DockerRun portBinding(int portHost, int portContainer) {
        this.portBindings.add(Pair.of(portHost, portContainer));
        return this;
    }

    /**
     * Sets the user and group of container. See https://docs.docker.com/engine/reference/run/#user for details.
     *
     * @param user the user and group as string accepted by Docker's CLI parameter "--user".
     */
    public DockerRun user(String user) {
        this.user = user;
        return this;
    }

    /**
     * Sets the args.
     *
     * @param args the args.
     */
    public DockerRun args(List<String> args) {
        this.args = args;
        return this;
    }

    /**
     * Sets user to current user/group.
     */
    public DockerRun currentUser() {
        String user = null;

        // Currently, default user is set on Unix systems, only.
        if (SystemUtils.IS_OS_UNIX) {
            // The class "com.sun.security.auth.module.UnixSystem" is not available on Windows.
            // Therefore, its import statement has to be bypassed on Windows.
            try {
                Class<?> systemClass = Class.forName("com.sun.security.auth.module.UnixSystem");
                Method getUid = systemClass.getDeclaredMethod("getUid");
                Method getGid = systemClass.getDeclaredMethod("getGid");
                Object system = systemClass.newInstance();
                user = String.format("%d:%d", (Long) getUid.invoke(system), (Long) getGid.invoke(system));
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LOG.warn("Cannot fetch user id and group id. User will not be set.");
            }
        }

        return this.user(user);
    }

    /**
     * Adds an explicit volume binding this docker run command. The resulting
     * container can then share files with the host.
     *
     * @param localDir   the local directory which should be bound with the container
     * @param volumePath the path within the container to be bound with
     */
    public DockerRun volumeBinding(File hostDir, String volumePath) {
        this.hostVolumeBindings.add(Pair.of(hostDir, volumePath));
        return this;
    }

    public DockerRun volumeBinding(String volumeName, String volumePath) {
        this.dockerVolumeBindings.add(Pair.of(volumeName, volumePath));
        return this;
    }

    /**
     * Adds an environmental parameter to the docker run command. Those
     * parameters are then available in the running docker container.
     */
    public DockerRun parameter(String key, Object value) {
        this.parameters.add(Pair.of(key, value));
        return this;
    }

    /**
     * Executes the composed docker run command.
     *
     * @return The DockerContainer associated with this docker run command.
     */
    public DockerContainer execute() {
        List<String> options = new Vector<>();

        if (gpusAll) {
            options.add("--gpus");
            options.add("all");
        }

        if (removeAfterRun) {
            options.add("--rm");
        }

        if (user != null && !user.isEmpty()) {
            options.add("--user");
            options.add(user);
        }

        for (Pair<File, String> binding : hostVolumeBindings) {
            options.add("-v");
            options.add(binding.getKey().getAbsolutePath().replace('\\', '/').replace(" ", "\\ ") + ":" + binding.getValue());
        }

        for (Pair<String, String> binding : dockerVolumeBindings) {
            options.add("-v");
            options.add(binding.getKey() + ":" + binding.getValue());
        }

        for (Pair<Integer, Integer> binding : portBindings) {
            options.add("-p");
            options.add(binding.getKey() + ":" + binding.getValue());
        }

        for (Pair<String, Object> param : parameters) {
            options.add("-e");
            options.add(param.getKey() + "=" + param.getValue().toString());
        }

        return this.client.runImage(image, name, options, args, removeBeforeRun);
    }


}
