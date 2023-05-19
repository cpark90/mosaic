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
import org.eclipse.mosaic.rti.api.time.FederateEvent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadedTimeManagement extends AbstractTimeManagement {

    private static final AtomicInteger idCounter = new AtomicInteger();

    private final ThreadPool threadPool;

    private final ScheduledEvents scheduledEvents;

    private final Semaphore ambassadorRunningSemaphore = new Semaphore(1);

    public MultiThreadedTimeManagement(ComponentProvider federation, MosaicComponentParameters componentParameters) {
        super(federation, componentParameters);
        this.threadPool = new ThreadPool(federation, componentParameters.getNumberOfThreads());
        this.scheduledEvents = new ScheduledEvents();
    }

    @Override
    protected void prepareSimulationRun() throws IllegalValueException, InternalFederateException {
        // initialize thread pool
        this.threadPool.setEventQueue(this.scheduledEvents);
        this.threadPool.initialize();
        // initialize all federates
        super.prepareSimulationRun();
    }

    /**
     * Runs the simulation.
     *
     * @throws InternalFederateException an exception inside of a joined federate occurs
     * @throws IllegalValueException     a parameter has an invalid value
     */
    @Override
    public void runSimulation() throws InternalFederateException, IllegalValueException {
        federation.getMonitor().onBeginSimulation(
                federation.getFederationManagement(),
                this,
                threadPool.getThreadCount()
        );

        this.prepareSimulationRun();

        final PerformanceCalculator performanceCalculator = new PerformanceCalculator();
        long currentRealtimeNs;

        FederateAmbassador ambassador;
        FederateEvent federateEvent;
        byte priority;

        // run while federateEvents are available
        while (this.federateEvents.size() > 0 && this.time < getEndTime()) {

            // remove first federateEvent of queue
            synchronized (this.federateEvents) {
                federateEvent = this.federateEvents.poll();
            }

            if (federateEvent != null) {
                this.time = federateEvent.getRequestedTime();
            } else {
                this.logger.trace("No more messages in federateEvent queue. Finishing simulation run.");
                this.time = getEndTime();
                break;
            }
            priority = federateEvent.getPriority();

            this.logger.trace("New minimum valid simulation time: {}", federateEvent.getRequestedTime());

            // check if other federates can be scheduled in parallel
            if (this.federateEvents.peek() != null
                    && priority == this.federateEvents.peek().getPriority()
                    && federateEvent.getRequestedTime() + federateEvent.getLookahead() >= this.federateEvents.peek().getRequestedTime()
            ) {
                try {
                    ambassadorRunningSemaphore.acquire();
                } catch (InterruptedException ignored) {
                    // ignored
                }

                // schedule next federateEvent
                int id = createEventId(); // Acquire scheduling block id
                federation.getMonitor().onScheduling(id, federateEvent);
                this.scheduledEvents.addEvent(federateEvent);

                // schedule further federateEvents that can be executed in parallel
                while (this.federateEvents.peek() != null
                        && priority == this.federateEvents.peek().getPriority()
                        && scheduledEvents.getMaximumValidTime() >= this.federateEvents.peek().getRequestedTime()
                ) {
                    synchronized (federateEvents) {
                        federateEvent = this.federateEvents.poll();
                    }
                    this.logger.trace("Parallel execution: {} time={} lookahead={}", federateEvent.getFederateId(), federateEvent.getRequestedTime(), federateEvent.getLookahead());
                    federation.getMonitor().onScheduling(id, federateEvent);
                    this.scheduledEvents.addEvent(federateEvent);
                }

                // wait until all federateEvents are processed in parallel
                synchronized (this.scheduledEvents.isEmptyMutex) {
                    try {
                        if (this.threadPool.isActive()) {
                            this.scheduledEvents.isEmptyMutex.wait();
                        }
                    } catch (InterruptedException ignored) {
                        // nop
                    }
                }
                ambassadorRunningSemaphore.release();
            } else {
                // call ambassador associated with the scheduled federateEvent to
                // process until the next globally scheduled federateEvent
                ambassador = federation.getFederationManagement().getAmbassador(federateEvent.getFederateId());
                if (ambassador != null) {
                    this.logger.trace(
                            "Advancing {} to time {}",
                            federation.getFederationManagement().getAmbassador(federateEvent.getFederateId()).getId(),
                            federateEvent.getRequestedTime()
                    );

                    try {
                        ambassadorRunningSemaphore.acquire();
                    } catch (InterruptedException e) {
                        this.logger.trace("Error while acquiring semaphore", e);
                    }
                    federation.getMonitor().onBeginActivity(federateEvent);
                    long startTime = System.currentTimeMillis();

                    ambassador.advanceTime(federateEvent.getRequestedTime());

                    ambassadorRunningSemaphore.release();
                    federation.getMonitor().onEndActivity(federateEvent, System.currentTimeMillis() - startTime);

                    updateWatchDog();
                }
            }
            // check if an exception was thrown
            if (this.threadPool.hasException()) {
                throw this.threadPool.getLastException();
            }

            currentRealtimeNs = System.nanoTime();

            final PerformanceInformation performanceInformation =
                    performanceCalculator.update(time, getEndTime(), currentRealtimeNs);

            printProgress(currentRealtimeNs, performanceInformation);
            updateWatchDog();
        }

        this.logger.trace("{} shutdown", getEndTime());
        this.finishSimulationRun(STATUS_CODE_SUCCESS);
    }

    private static int createEventId() {
        return idCounter.incrementAndGet();
    }

    @Override
    public void finishSimulationRun(int statusCode) throws InternalFederateException {
        this.threadPool.shutdown();
        this.federateEvents.clear();
        super.finishSimulationRun(statusCode);
    }
}
