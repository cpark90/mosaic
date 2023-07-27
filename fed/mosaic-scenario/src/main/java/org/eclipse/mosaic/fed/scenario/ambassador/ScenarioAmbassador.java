/*
 * Copyright (c) 2021 Old Dominion University. All rights reserved.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.mosaic.fed.scenario.ambassador;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.mosaic.fed.scenario.connect.ScenarioConnection;
import org.eclipse.mosaic.fed.scenario.config.ScenarioConfiguration;
import org.eclipse.mosaic.lib.util.ProcessLoggingThread;
import org.eclipse.mosaic.lib.util.objects.ObjectInstantiation;
import org.eclipse.mosaic.lib.util.scheduling.DefaultEventScheduler;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;
import org.eclipse.mosaic.lib.util.scheduling.EventScheduler;
import org.eclipse.mosaic.rti.TIME;
import org.eclipse.mosaic.rti.api.AbstractFederateAmbassador;
import org.eclipse.mosaic.rti.api.FederateExecutor;
import org.eclipse.mosaic.rti.api.MediatorExecutor;
import org.eclipse.mosaic.rti.api.IllegalValueException;
import org.eclipse.mosaic.rti.api.Interaction;
import org.eclipse.mosaic.rti.api.InternalFederateException;
import org.eclipse.mosaic.rti.api.federatestarter.DockerFederateExecutor;
import org.eclipse.mosaic.rti.api.federatestarter.ExecutableFederateExecutor;
import org.eclipse.mosaic.rti.api.federatestarter.NopFederateExecutor;
import org.eclipse.mosaic.rti.api.mediatorstarter.DockerMediatorExecutor;
import org.eclipse.mosaic.rti.api.mediatorstarter.ExecutableMediatorExecutor;
import org.eclipse.mosaic.rti.api.mediatorstarter.NopMediatorExecutor;
import org.eclipse.mosaic.rti.api.parameters.AmbassadorParameter;
import org.eclipse.mosaic.rti.api.parameters.FederatePriority;
import org.eclipse.mosaic.rti.config.CLocalHost;

import org.eclipse.mosaic.protos.federate.FederateMessageWrapper;
import org.eclipse.mosaic.protos.federate.SimulationStep;
import org.eclipse.mosaic.protos.federate.SimulationStepResponse;
import org.eclipse.mosaic.protos.federate.MediatorStatus;
import org.eclipse.mosaic.protos.common.StatusType;

/**
 * Implementation of a {@link AbstractFederateAmbassador} for the vehicle
 * simulator Scenario. It is used to visualize the traffic simulation in 3D
 * environment.
 */
public class ScenarioAmbassador extends AbstractFederateAmbassador {

    /**
     * Connection between Scenario federate and Scenario simulator.
     */
    private ScenarioConnection scenarioConnection = null;

    /**
     * Command used to start Scenario simulator.
     */
    FederateExecutor federateExecutor = null;

    DockerFederateExecutor dockerFederateExecutor = null;

    /**
     * Command used to start Scenario simulator.
     */
    MediatorExecutor mediatorExecutor = null;

    DockerMediatorExecutor dockerMediatorExecutor = null;

    /**
     * Simulation time.
     */
    long nextTimeStep;

    /**
     * Scenario configuration file
     */
    ScenarioConfiguration scenarioConfig;

    /**
     * flag for simulation step
     */
    boolean isSimulationStep = false;

    /**
     * Sleep after each connection try. Unit: [ms].
     */
    private final static long SLEEP_AFTER_ATTEMPT = 1000L;

    /**
     * Maximum amount of attempts to connect to Scenario simulator.
     */
    private int connectionAttempts = 5;

    /**
     * Scenario simulator client port
     */
    private int scenarioSimulatorClientPort = -1;

    /**
     * Scenario mediator port
     */
    private int mediatorPort = -1;

    /**
     * The process for running the connection bridge client
     */
    private Process connectionProcess = null;

    /**
     * Indicates whether advance time is called for the first time.
     */
    private boolean firstAdvanceTime = true;
    

    /**
     * An event scheduler which is currently used to change the speed to
     * a given value after slowing down the vehicle.
     */
    private final EventScheduler eventScheduler = new DefaultEventScheduler();

    /**
     * Creates a new {@link CarlaAmbassador} object.
     *
     * @param ambassadorParameter includes parameters for the CARLA Ambassador.
     */
    public ScenarioAmbassador(AmbassadorParameter ambassadorParameter) {
        super(ambassadorParameter);
        try {
            // read the CARLA configuration file
            scenarioConfig = new ObjectInstantiation<>(ScenarioConfiguration.class, log)
                    .readFile(ambassadorParameter.configuration);
        } catch (InstantiationException e) {
            log.error("Configuration object could not be instantiated: ", e);
        }

        log.info("scenarioConfig.updateInterval: " + scenarioConfig.updateInterval);

        // check the carla configuration
        checkConfiguration();
    }

    /**
     * Check the updateInterval is validated.
     */
    private void checkConfiguration() {
        if (scenarioConfig.updateInterval <= 0) {
            throw new RuntimeException("Invalid scenario interval, should be >0");
        }
    }

    /**
     * Creates and sets new federate executor.
     *
     * @param host name of the host (as specified in /etc/hosts.json)
     * @param port port number to be used by this federate
     * @param os   operating system enum
     * @return FederateExecutor.
     */
    @Nonnull
    @Override
    public FederateExecutor createFederateExecutor(String host, int port, CLocalHost.OperatingSystem os) {
        // CARLA needs to start the federate by itself, therefore we need to store the
        // federate starter locally and use it later
        federateExecutor = new ExecutableFederateExecutor(descriptor, getScenarioExecutable("CarlaUE4"),
                getProgramArguments(port));
        this.scenarioSimulatorClientPort = port;
        return new NopFederateExecutor();
    }

    /**
     * Get CARLA simulator executable file location
     *
     * @param executable the name of carla executable file
     * @return the path to CarlaUE4 executable file
     */
    String getScenarioExecutable(String executable) {
        String scenarioHome = null;
        if (scenarioConfig.scenarioBinPath != null) {
            scenarioHome = scenarioConfig.scenarioBinPath;
            log.info("use carla path from configuration file: " + scenarioHome);
        }
        else if (System.getenv("SCENARIO_HOME") != null) {
            scenarioHome = System.getenv("SCENARIO_HOME");
            log.info("use carla path from environmental variable: " + scenarioHome);
        }
        if (StringUtils.isNotBlank(scenarioHome)) {
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            if (isWindows) {
                executable += ".exe";
            } else {
                executable += ".sh";
            }
            return scenarioHome + File.separator + executable;
        }
        return executable;
    }

    @Override
    public DockerFederateExecutor createDockerFederateExecutor(String dockerImage, int port, CLocalHost.OperatingSystem os) {
        List<String> args = getProgramArguments(port);
        args.add(0, "/home/carla/CarlaUE4.sh");

        // TODO: deploy target path 
        // copy data from host to docker volume
        // this.dockerClient.createDockerVolume(imageVolume);

        // if (!imageVolume.isEmpty()) {
        //     this.dockerClient.copyFile(containerName, fedDir.toString(), sharedDirectoryPath);
        // }
        // descriptor.getSimulationId() + "-" + descriptor.getId()
        this.dockerFederateExecutor = new DockerFederateExecutor(
                dockerImage,
                descriptor.getHost().workingDirectory + "/" + descriptor.getSimulationId(),
                "",
                args
        );
        this.dockerFederateExecutor.addPortBinding(port, port);
        this.dockerFederateExecutor.addPortBinding(8913, 8913);
        this.federateExecutor = this.dockerFederateExecutor;
        return this.dockerFederateExecutor;
    }

    /**
     * Creates and sets new mediator executor.
     *
     * @param host name of the host (as specified in /etc/hosts.json)
     * @param port port number to be used by this federate
     * @param os   operating system enum
     * @return MediatorExecutor.
     */
    @Nonnull
    @Override
    public MediatorExecutor createMediatorExecutor(String host, int port, CLocalHost.OperatingSystem os) {
        // CARLA needs to start the federate by itself, therefore we need to store the
        // federate starter locally and use it later
        mediatorExecutor = new ExecutableMediatorExecutor(descriptor, getMediatorExecutable("CarlaUE4"),
                getMediatorArguments(port));
        this.mediatorPort = port;
        return new NopMediatorExecutor();
    }

    /**
     * Get CARLA mediator executable file location
     *
     * @param executable the name of carla executable file
     * @return the path to CarlaUE4 executable file
     */
    String getMediatorExecutable(String executable) {
        // execute mediator in scenario directory
        String mediatorHome = null;
        if (scenarioConfig.scenarioBinPath != null) {
            mediatorHome = scenarioConfig.scenarioBinPath;
            log.info("use scenario mediator path from configuration file: " + mediatorHome);
        }
        else if (System.getenv("MEDIATOR_HOME") != null) {
            mediatorHome = System.getenv("MEDIATOR_HOME");
            log.info("use scenario mediator path from environmental variable: " + mediatorHome);
        }
        if (StringUtils.isNotBlank(mediatorHome)) {
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            if (isWindows) {
                executable += ".exe";
            } else {
                executable += ".sh";
            }
            return mediatorHome + File.separator + executable;
        }
        return executable;
    }

    @Override
    public DockerMediatorExecutor createDockerMediatorExecutor(String dockerImage, int port, CLocalHost.OperatingSystem os) {
        List<String> args = getMediatorArguments(port);

        // TODO: deploy target path 
        this.dockerMediatorExecutor = new DockerMediatorExecutor(
                dockerImage,
                descriptor.getHost().workingDirectory + "/" + descriptor.getSimulationId(),
                "",
                args
        );
        this.dockerMediatorExecutor.addPortBinding(port, port);
        this.dockerMediatorExecutor.addPortBinding(2000, 2000);
        this.mediatorExecutor = this.dockerMediatorExecutor;
        return this.dockerMediatorExecutor;
    }

    /**
     * This method is called to tell the federate the start time and the end time.
     * It is also used to start CARLA, and connect to CARLA.
     *
     * @param startTime Start time of the simulation run in nano seconds.
     * @param endTime   End time of the simulation run in nano seconds.
     * @throws InternalFederateException Exception is thrown if an error is occurred
     *                                   while execute of a federate.
     */
    @Override
    public void initialize(long startTime, long endTime) throws InternalFederateException {
        super.initialize(startTime, endTime);

        nextTimeStep = startTime;
        try {
            rti.requestAdvanceTime(nextTimeStep, 0, FederatePriority.higher(descriptor.getPriority()));
        } catch (IllegalValueException e) {
            log.error("Error during advanceTime request", e);
            throw new InternalFederateException(e);
        }
    }

    /**
     * Connects to Scenario engine using the given host and port.
     *
     * @param host host on which scenario engine is running.
     * @param port port on which Scenario client is listening.
     */
    @Override
    public void connectToFederate(String host, int port) {
        // Start the Scenario connection server
        // TODO: connect scenario client to control carla simulator
    }

    @Override
    public void connectToFederate(String host, InputStream in, InputStream err) {
        this.connectToFederate(host, scenarioSimulatorClientPort);
    }

    /**
     * Connects to Scenario engine mediator using the given host and port.
     *
     * @param host host on which Scenario engine mediator is running.
     * @param port port on which mediator client is listening.
     */
    @Override
    public void connectToMediator(String host, int port) {
        // Start the Scenario engine connection server
        int scenarioConnectionPort = 8913;
        if (scenarioConfig.scenarioConnectionPort != 0)
            scenarioConnectionPort = scenarioConfig.scenarioConnectionPort; // set the carla connection port

        if (scenarioConnection == null) {
            // start the carla connection

            scenarioConnection = new ScenarioConnection("localhost", scenarioConnectionPort, this);
            Thread scenarioThread = new Thread(scenarioConnection);
            scenarioThread.start();
        } else {
            scenarioConnection.closeSocket();
            scenarioConnection.setScenarioPort(scenarioConnectionPort);
            scenarioConnection.setScenarioHostName("localhost");
            Thread scenarioThread = new Thread(scenarioConnection);
            scenarioThread.start();
        }
    }

    @Override
    public void connectToMediator(String host, InputStream in, InputStream err) {
        this.connectToMediator(host, mediatorPort);
    }

    /**
     * Starts the Scenario engine binary locally.
     */
    void startScenarioLocal() throws InternalFederateException {
        if (!descriptor.isToStartAndStop()) {
            return;
        }

        File dir = new File(descriptor.getHost().workingDirectory, descriptor.getId());
        log.info("Start Federate local");
        log.info("Directory: " + dir);

        try {
            federateExecutor.stopLocalFederate();
            Process p = federateExecutor.startLocalFederate(dir);
            connectToFederate("localhost", p.getInputStream(), p.getErrorStream());
            // read error output of process in an extra thread
            new ProcessLoggingThread("scenario", p.getInputStream(), log::debug).start();
            new ProcessLoggingThread("scenario", p.getErrorStream(), line -> {
                log.error(line);
                System.err.println(line); // make sure that we see what's wrong when SUMO cannot start
            }).start();

        } catch (FederateExecutor.FederateStarterException e) {
            log.error("Error while executing command: {}", federateExecutor.toString());
            throw new InternalFederateException("Error while starting Carla: " + e.getLocalizedMessage());
        }

        if (mediatorExecutor != null) {
            try {
                mediatorExecutor.stopLocalMediator();
                Process mediatorProcess = mediatorExecutor.startLocalMediator(dir);
                connectToMediator("localhost", mediatorProcess.getInputStream(), mediatorProcess.getErrorStream());
            } catch (MediatorExecutor.MediatorStarterException e) {
                log.error("Error while executing command: {}", mediatorExecutor.toString());
                throw new InternalFederateException("Error while starting Carla: " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * This method is called by the AbstractFederateAmbassador when a time advance
     * has been granted by the RTI. Before this call is placed, any unprocessed
     * interaction is forwarded to the federate using the processInteraction method.
     *
     * @param time The timestamp towards which the federate can advance it local
     *             time.
     */
    @Override
    public synchronized void processTimeAdvanceGrant(long time) throws InternalFederateException {

        if (time < nextTimeStep) {
            // process time advance only if time is equal or greater than the next
            // simulation time step
            return;
        }
            
        // schedule events, e.g. change speed events
        int scheduled = eventScheduler.scheduleEvents(time);
        // log.debug("scheduled {} events at time {}", scheduled, TIME.format(time));

        try {

            // if the simulation step received from CARLA, advance CARLA federate local
            // simulation time
            if (isSimulationStep) {
                log.debug("ScenarioAmbassador timestep advance");
                nextTimeStep += scenarioConfig.updateInterval * TIME.MILLI_SECOND;
                isSimulationStep = false;
            }

            rti.requestAdvanceTime(nextTimeStep, 0, FederatePriority.higher(descriptor.getPriority()));
        } catch (IllegalValueException e) {
            log.error("Error during advanceTime(" + time + ")", e);
            throw new InternalFederateException(e);
        }
    }

    /**
     * This method is called by the time management service to signal that the
     * simulation is finished.
     */
    @Override
    public void finishSimulation() throws InternalFederateException {
        log.info("Closing Scenario connection.");

        if (scenarioConnection != null) {
            try {
                scenarioConnection.closeSocket();

            } catch (Exception e) {
                log.warn("Could not properly stop scenario connection");
            }
        }

        if (federateExecutor != null) {
            try {
                federateExecutor.stopLocalFederate();
            } catch (FederateExecutor.FederateStarterException e) {
                log.warn("Could not properly stop federate");
            }
        }

        if (mediatorExecutor != null) {
            try {
                mediatorExecutor.stopLocalMediator();
            } catch (MediatorExecutor.MediatorStarterException e) {
                log.warn("Could not properly stop mediator");
            }
        }

        if (connectionProcess != null) {
            try {

                connectionProcess.waitFor(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("Something went wrong when stopping a process", e);
            } finally {
                connectionProcess.destroy();
            }
        }
        log.info("Finished simulation");
    }

    /**
     * get the CARLA command arguments
     *
     * @param port CARLA simulator client port
     * @return the list of CARLA command arguments
     */
    List<String> getProgramArguments(int port) {

        List<String> args = Lists.newArrayList("-scenario-rpc-port", Integer.toString(port), "-RenderOffScreen");
        

        return args;
    }

    /**
     * get the Scenario mediator command arguments
     *
     * @param port Scenario engine mediator client port
     * @return the list of Scenario command arguments
     */
    List<String> getMediatorArguments(int port) {

        List<String> args = Lists.newArrayList("--bridge-server-port", Integer.toString(port), "-m", scenarioConfig.mediatorMap, scenarioConfig.mediatorNetFile, "--step-length", Double.toString(scenarioConfig.mediatorStepLength), "--tls-manager", scenarioConfig.mediatorTlsManager);

        return args;
    }
    
    /**
     * Returns whether this federate is time constrained. Is set if the federate is
     * sensitive towards the correct ordering of events. The federate ambassador
     * will ensure that the message processing happens in time stamp order. If set
     * to false, interactions will be processed will be in receive order.
     *
     * @return {@code true} if this federate is time constrained, else {@code false}
     */
    @Override
    public boolean isTimeConstrained() {
        return true;
    }

    /**
     * Returns whether this federate is time regulating. Is set if the federate
     * influences other federates and can prevent them from advancing their local
     * time.
     *
     * @return {@code true} if this federate is time regulating, {@code false} else
     */
    @Override
    public boolean isTimeRegulating() {
        return true;
    }

    /**
     * Trigger a new CarlaTraciRequest, SimulationStep or ExternalMessage
     * interaction
     *
     * @param length  command length
     * @param command command
     */
    public synchronized void triggerInteraction(int length, byte[] command) throws InternalFederateException {
        try {
            FederateMessageWrapper messageWarpper;

            messageWarpper = FederateMessageWrapper.parseFrom(command);

            switch (messageWarpper.getMessageCase()) {
                // SimulationStep
                case SIMULATIONSTEP:

                    log.debug("SimulationStep in ScenarioAmbassador");
                    rti.triggerInteraction(new org.eclipse.mosaic.interactions.application.SimulationStep(this.nextTimeStep));
                    isSimulationStep = true;

                // MediatorStatus
                case MEDIATORSTATUS:
                    ;
            }

        } catch (IllegalValueException e) {
            throw new InternalFederateException(e);
        }
    }

    /**
     * This method is called by the {@link AbstractFederateAmbassador}s whenever the
     * federate can safely process interactions in its incoming interaction queue.
     * The decision when it is safe to process such an interaction depends on the
     * policies TimeRegulating and TimeConstrained that has to be set by the
     * federate.
     *
     * @param interaction the interaction to be processed
     */
    @Override
    public void processInteraction(Interaction interaction) {
        String type = interaction.getTypeId();
        long interactionTime = interaction.getTime();
        log.trace("Process interaction with type '{}' at time: {}", type, interactionTime);
        if (interaction.getTypeId().equals(org.eclipse.mosaic.interactions.application.SimulationStepResponse.TYPE_ID)) {
            this.receiveInteraction((org.eclipse.mosaic.interactions.application.SimulationStepResponse) interaction);
        }
    }

    /**
     * process the traci response interaction
     *
     * @param interaction Simulation Step Response interaction
     */
    private void receiveInteraction(org.eclipse.mosaic.interactions.application.SimulationStepResponse interaction) {
        try {

            if (scenarioConnection.getDataOutputStream() != null) {
                SimulationStepResponse simulationStepResponse = SimulationStepResponse.newBuilder().setStatus(StatusType.SUCCESS).build();
                FederateMessageWrapper messageWarpper = FederateMessageWrapper.newBuilder().setSimulationStepResponse(simulationStepResponse).build();

                scenarioConnection.getDataOutputStream().writeInt(messageWarpper.getSerializedSize());
                scenarioConnection.getDataOutputStream().write(messageWarpper.toByteArray());
            }

        } catch (Exception e) {
            log.error("error occurs during process simulation step response interaction: " + e.getMessage());
        }
    }
}
