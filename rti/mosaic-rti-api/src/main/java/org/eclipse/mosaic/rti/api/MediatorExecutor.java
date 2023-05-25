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

package org.eclipse.mosaic.rti.api;

import org.eclipse.mosaic.rti.config.CLocalHost;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * <p>The {@link FederateExecutor} is used to start the mediator the ambassador
 * is associated with. The {@link FederateExecutor} should be able to start the
 * mediator and close it locally, as well as remotely.</p>
 */
public interface MediatorExecutor {

    /**
     * Starts the mediator locally in the given working dir.
     *
     * @param fedDir the working directory
     *
     * @return the local process of the mediator
     * @throws MediatorStarterException if something went wrong during starting the mediator
     */
    Process startLocalMediator(File fedDir) throws MediatorStarterException;

    /**
     * Stops the previously locally started mediator.
     *
     * @throws MediatorStarterException if the start of the mediator failed
     */
    void stopLocalMediator() throws MediatorStarterException;

    /**
     * Starts the mediator locally in the given working dir.
     *
     * @param host information about the remote host the mediator is started on
     * @param sshStreamOut the ssh output stream to send commands to the host
     * @param sshStreamIn the ssh input stream to read the console output of the remote host shell
     *
     * @return the local process of the mediator
     * @throws MediatorStarterException if something went wrong during starting the mediator
     */
    int startRemoteMediator(CLocalHost host, PrintStream sshStreamOut, InputStream sshStreamIn) throws MediatorStarterException;

    /**
     * Stops the previously remotely started mediator.
     *
     * @param sshStreamOut the ssh output stream to send commands to the host
     * @throws MediatorStarterException if something went wrong during starting the mediator
     */
    void stopRemoteMediator(PrintStream sshStreamOut) throws MediatorStarterException;

    class MediatorStarterException extends Exception {

        private static final long serialVersionUID = 1L;

        public MediatorStarterException(Throwable cause) {
            super(cause);
        }

        public MediatorStarterException(String message) {
            super(message);
        }
    }
}
