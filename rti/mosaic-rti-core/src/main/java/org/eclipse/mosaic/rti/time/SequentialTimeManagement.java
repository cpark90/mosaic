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

package org.eclipse.mosaic.rti.time;

import org.eclipse.mosaic.rti.MosaicComponentParameters;
import org.eclipse.mosaic.rti.api.ComponentProvider;
import org.eclipse.mosaic.rti.api.FederateAmbassador;
import org.eclipse.mosaic.rti.api.IllegalValueException;
import org.eclipse.mosaic.rti.api.InternalFederateException;
import org.eclipse.mosaic.rti.api.TimeManagement;
import org.eclipse.mosaic.rti.api.time.FederateEvent;

/**
 * This class is a sequential implementation of the <code>TimeManagement</code>
 * interface.
 */
public class SequentialTimeManagement extends AbstractTimeManagement {

    private final int realtimeBrake;

    /**
     * Creates a new instance of the sequential time management.
     *
     * @param federation    reference to the <code>ComponentFactory</code> to access simulation components
     * @param componentParameters parameters specifically for this {@link TimeManagement, e.g. the target realtime factor for the simulation.
     */
    public SequentialTimeManagement(ComponentProvider federation, MosaicComponentParameters componentParameters) {
        super(federation, componentParameters);
        this.realtimeBrake = componentParameters.getRealTimeBreak();
    }

    /**
     * Runs the simulation sequentially.
     *
     * @throws InternalFederateException an exception inside of a joined federate occurs
     * @throws IllegalValueException     a parameter has an invalid value
     * @see TimeManagement
     */
    @Override
    public void runSimulation() throws InternalFederateException, IllegalValueException {
        federation.getMonitor().onBeginSimulation(federation.getFederationManagement(), this, 1);

        this.prepareSimulationRun();


        final PerformanceCalculator performanceCalculator = new PerformanceCalculator();
        final RealtimeSynchronisation realtimeSync = new RealtimeSynchronisation(realtimeBrake);

        long currentRealtimeNs;
        FederateEvent federateEvent;
        FederateAmbassador ambassador;

        while (this.time <= getEndTime()) {
            // the end time is inclusive, in order to schedule federateEvents in the last simulation time step

            // sync with real time
            if (this.time > 0) {
                realtimeSync.sync(this.time);
            }

            // remove all federateEvents at the head of the queue that are created by the same federate
            synchronized (this.federateEvents) {
                federateEvent = this.federateEvents.poll();
                // advance global time
                if (federateEvent == null || federateEvent.getRequestedTime() > getEndTime()) {
                    this.time = getEndTime();
                    break;
                } else {
                    this.time = federateEvent.getRequestedTime();
                }
            }

            // call ambassador associated with the scheduled federateEvent
            ambassador = federation.getFederationManagement().getAmbassador(federateEvent.getFederateId());
            if (ambassador != null) {
                federation.getMonitor().onBeginActivity(federateEvent);
                long startTime = System.currentTimeMillis();
                ambassador.advanceTime(federateEvent.getRequestedTime());
                federation.getMonitor().onEndActivity(federateEvent, System.currentTimeMillis() - startTime);

                // check, if federateEvent queue is empty after the last time advance.
                // If no more federateEvents are in the list, the simulation can be skipped to the endTime.
                if (this.federateEvents.isEmpty()) {
                    logger.debug("No federateEvents anymore, skipping to end time: {}", getEndTime());

                    federation.getMonitor().onBeginActivity(federateEvent);
                    startTime = System.currentTimeMillis();
                    ambassador.advanceTime(getEndTime());
                    federation.getMonitor().onEndActivity(federateEvent, System.currentTimeMillis() - startTime);
                }
            }
            currentRealtimeNs = System.nanoTime();

            final PerformanceInformation performanceInformation =
                    performanceCalculator.update(time, getEndTime(), currentRealtimeNs);

            printProgress(currentRealtimeNs, performanceInformation);
            updateWatchDog();
        }

        this.finishSimulationRun(STATUS_CODE_SUCCESS);
    }


}

