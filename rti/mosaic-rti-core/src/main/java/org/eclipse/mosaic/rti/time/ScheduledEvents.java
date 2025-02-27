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

import org.eclipse.mosaic.rti.api.time.FederateEvent;

import java.util.concurrent.PriorityBlockingQueue;

class ScheduledEvents {

    final Object accessMutex = new Object();
    final Object isEmptyMutex = new Object();

    /**
     * Priority queue for federateEvents.
     */
    private final PriorityBlockingQueue<FederateEvent> federateEvents = new PriorityBlockingQueue<>();

    /**
     * Priority queue for lookahead values.
     */
    private final PriorityBlockingQueue<Long> lookahead = new PriorityBlockingQueue<>();

    /**
     * Check, whether federateEvent queue is empty.
     *
     * @return true, if the federateEvent queue contains no elements.
     */
    boolean isEmpty() {
        return this.federateEvents.isEmpty();
    }

    /**
     * Clear the PriorityBlockingQueue's federateEvents and lookahead.
     */
    void clear() {
        this.federateEvents.clear();
        this.lookahead.clear();
    }

    /**
     * Appends the specified element to the PriorityBlockingQueue's federateEvents and lookahead.
     *
     * @param federateEvent element to be appended.
     */
    void addEvent(FederateEvent federateEvent) {
        this.federateEvents.add(federateEvent);
        this.lookahead.add(federateEvent.getRequestedTime() + federateEvent.getLookahead());
    }

    /**
     * Returns the next federateEvent in the queue (the head) and removes it from the queue.
     *
     * @return the federateEvent that will be removed
     */
    FederateEvent getNextScheduledEvent() {
        return this.federateEvents.remove();
    }

    /**
     * Removes the federateEvent depending on input federateEvent.
     *
     * @param federateEvent federateEvent to be stored
     */
    void setEventProcessed(FederateEvent federateEvent) {
        this.lookahead.remove(federateEvent.getRequestedTime() + federateEvent.getLookahead());
    }

    /**
     * Returns the Long MAX_VALUE if the queue is empty, else the peek of the PriorityBlockingQueue lookahead.
     *
     * @return the maximum valid time
     */
    long getMaximumValidTime() {
        synchronized (isEmptyMutex) {
            if (this.lookahead.isEmpty()) {
                return Long.MAX_VALUE;
            } else {
                return this.lookahead.peek();
            }
        }
    }
}
