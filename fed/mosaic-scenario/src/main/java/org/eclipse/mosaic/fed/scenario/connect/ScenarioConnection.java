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

package org.eclipse.mosaic.fed.scenario.connect;

import java.net.*;
import java.io.*;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.mosaic.fed.scenario.ambassador.ScenarioAmbassador;

/**
 * The CarlaConnection used to connect Scenario engine and Scenarioambassador
 */
public class ScenarioConnection implements Runnable {

    // ScenarioConnection socket port
    int scenarioPort = 8913;

    // ScenarioConnection host
    String scenarioHostName = "localhost";

    // the data output stream to Scenario engine
    DataOutputStream toScenarioDataOutputStream;

    // the data input stream from Scenario engine
    DataInputStream fromScenarioDataInputStream;

    // ScenarioConnection server socket
    ServerSocket scenarioConnectionServerSocket;

    // ScenarioConnection client socket
    Socket scenarioSocket = null;

    // the reference to ScenarioAmbassador
    ScenarioAmbassador scenarioAmbassador;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * This is the customized constructor of CarlaConnection
     *
     * @param scenarioHostName This is the CarlaConnection server host name
     *
     * @param scenarioPort     This the CarlaConnection client port
     */
    public ScenarioConnection(String scenarioHostName, int scenarioPort, ScenarioAmbassador fedAmbassador) {
        this.scenarioHostName = scenarioHostName;
        this.scenarioPort = scenarioPort;
        this.scenarioAmbassador = fedAmbassador;
    }

    public void setScenarioPort(int scenarioPort) {
        this.scenarioPort = scenarioPort;
    }

    public void setScenarioHostName(String scenarioHostName) {
        this.scenarioHostName = scenarioHostName;
    }

    /**
     * This method is used to run the ScenarioConnection. After Scenario simulator
     * connected, the message begins to transfer between too ends.
     */
    @Override
    public void run() {
        try {
            // create a scenario connection server socket
            scenarioConnectionServerSocket = new ServerSocket(scenarioPort);
            // Blocking call until Scenario engine is connected.
            scenarioSocket = scenarioConnectionServerSocket.accept();
            log.info("Scenario Connected");
            try {
                InputStream fromScenarioInputStream = scenarioSocket.getInputStream();
                fromScenarioDataInputStream = new DataInputStream(fromScenarioInputStream);
                OutputStream toScenarioOutputStream = scenarioSocket.getOutputStream();
                toScenarioDataOutputStream = new DataOutputStream(toScenarioOutputStream);
                log.info("Begin Co-Simulation");
                // Socket data stream ready for transfer message between Scenario engine and
                // Scenario ambassador
                // message buffer
                byte[] buffer = new byte[65535];
                while (true) {
                    // get message from Scenario simulator
                    int length = fromScenarioInputStream.read(buffer);
                    byte[] protoCommandFromScenario = Arrays.copyOfRange(buffer, 0, length);
                    // trigger

                    scenarioAmbassador.triggerInteraction(length, protoCommandFromScenario);
                }
            } catch (Exception e) {
                log.error("error occurs during data streaming: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("error occurs during socket connection: " + e.getMessage());
        }
    }

    /**
     * Get data output stream of socket
     */
    public synchronized DataOutputStream getDataOutputStream() {
        return toScenarioDataOutputStream;
    }

    /**
     * close scenario socket and ScenarioConnectionServer socket
     */
    public synchronized void closeSocket() {
        try {
            // close scenario socket
            log.info("scenario socket closing");
            if (scenarioSocket != null) {
                scenarioSocket.close();
            }
            // close connection server socket
            log.info("scenario connection server socket closing");
            if (scenarioConnectionServerSocket != null) {
                scenarioConnectionServerSocket.close();
            }
        } catch (Exception e) {
            log.error("error occurs during closing scenario socket: " + e.getMessage());
        }
    }
}
