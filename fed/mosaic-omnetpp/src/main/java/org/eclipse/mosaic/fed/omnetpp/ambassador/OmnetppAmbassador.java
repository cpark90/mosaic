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

package org.eclipse.mosaic.fed.omnetpp.ambassador;

import org.eclipse.mosaic.lib.coupling.AbstractNetworkAmbassador;
import org.eclipse.mosaic.rti.api.FederateExecutor;
import org.eclipse.mosaic.rti.api.federatestarter.DockerFederateExecutor;
import org.eclipse.mosaic.rti.api.federatestarter.ExecutableFederateExecutor;
import org.eclipse.mosaic.rti.api.federatestarter.NopFederateExecutor;
import org.eclipse.mosaic.rti.api.parameters.AmbassadorParameter;
import org.eclipse.mosaic.rti.config.CLocalHost.OperatingSystem;

import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;

/**
 * Implementation of the ambassador for the OMNeT++ network simulator.
 */
public class OmnetppAmbassador extends AbstractNetworkAmbassador {

    /**
     * Creates a new {@link OmnetppAmbassador} object.
     *
     * @param ambassadorParameter Parameter to specify the ambassador.
     */
    public OmnetppAmbassador(AmbassadorParameter ambassadorParameter) {
        super(ambassadorParameter, "OMNeT++ Ambassador", "OMNeT++ Federate");
    }

    @Nonnull
    @Override
    public FederateExecutor createFederateExecutor(String host, int port, OperatingSystem os) {
        switch (os) {
            case LINUX:
                String omnetppConfigFileName = ObjectUtils.defaultIfNull(config.federateConfigurationFile, "omnetpp.ini");
                String omnetppConfigFilePath = "omnetpp-federate/simulations/" + omnetppConfigFileName;
                String inetSourceDirectories = "inet:omnetpp-federate/src";
                return new ExecutableFederateExecutor(this.descriptor, "omnetpp-federate/omnetpp-federate",
                        "-u", "Cmdenv",
                        "-f", omnetppConfigFilePath,
                        "-n", inetSourceDirectories,
                        "--mosaiceventscheduler-host=" + host,
                        "--mosaiceventscheduler-port=" + port
                );
            case WINDOWS:
            case UNKNOWN:
            default:
                log.error("Operating system not supported");
                break;
        }
        return new NopFederateExecutor();
    }

    @Override
    public DockerFederateExecutor createDockerFederateExecutor(String federateDockerImage, int port, OperatingSystem os) {

        String omnetppConfigFileName = ObjectUtils.defaultIfNull(config.federateConfigurationFile, "omnetpp.ini");
        String omnetppConfigFilePath = "/home/mosaic/shared/omnetpp/omnetpp-federate/simulations/" + omnetppConfigFileName;
        String inetSourceDirectories = "inet:omnetpp-federate/src";

        this.dockerFederateExecutor = new DockerFederateExecutor(
                federateDockerImage,
                "docker-volume:mosaic",
                "/home/mosaic/shared",
                "omnetpp-federate/omnetpp-federate",
                "-u", "Cmdenv",
                "-f", omnetppConfigFilePath,
                "-n", inetSourceDirectories,
                "--mosaiceventscheduler-host=" + "0.0.0.0",
                "--mosaiceventscheduler-port=" + port,
                "--mosaiccmd-port=" + (port + 1)
        );
        this.dockerFederateExecutor.addPortBinding(port, port);
        this.dockerFederateExecutor.addPortBinding(port + 1, port + 1);

        return dockerFederateExecutor;
    }
}
