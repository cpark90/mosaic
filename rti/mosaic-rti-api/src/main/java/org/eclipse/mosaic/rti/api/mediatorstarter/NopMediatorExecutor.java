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

package org.eclipse.mosaic.rti.api.federatestarter;

import org.eclipse.mosaic.rti.api.FederateAmbassador;
import org.eclipse.mosaic.rti.api.MediatorExecutor;
import org.eclipse.mosaic.rti.config.CLocalHost;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * A {@link MediatorExecutor} implementation which does nothing when called (no-operation).
 * This should be used mainly by {@link FederateAmbassador}
 * implementations which do not connect with a separate mediator but which are
 * directly coupled with the RTI.
 */
public class NopMediatorExecutor implements MediatorExecutor {

    @Override
    public Process startLocalMediator(File fedDir) {
        //nop
        return null;
    }

    @Override
    public int startRemoteMediator(CLocalHost host, PrintStream sshStream, InputStream sshStreamIn) {
        //nop
        return -1;
    }

    @Override
    public void stopLocalMediator() {
        //nop
    }

    @Override
    public void stopRemoteMediator(PrintStream sshStreamOut) {
        //nop
    }

    @Override
    public String toString() {
        return "No-Operation MediatorExecutor";
    }
}
