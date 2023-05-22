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

/**
 * An exception that thrown within the execution of a mediator.
 */
public class InternalMediatorException extends Exception {
    private static final long serialVersionUID = 1L;

    public InternalMediatorException() {
        super();
        this.fillInStackTrace();
    }

    /**
     * Constructor based on an error message.
     *
     * @param message string describing the exception
     */
    public InternalMediatorException(String message) {
        super(message);
    }

    /**
     * Constructor based on an error message.
     *
     * @param simTime simulation time at which the exception occurred
     * @param message string describing the exception
     */
    public InternalMediatorException(long simTime, String message) {
        super(simTime + " " + message);
    }

    /**
     * Constructor based on a thrown exception.
     *
     * @param ex exception that signal that a parameter cannot be modified
     */
    public InternalMediatorException(Exception ex) {
        super(ex.getMessage());
        initCause(ex);
    }

    /**
     * Constructor based on a thrown exception.
     *
     * @param simTime simulation time at which the exception occurred
     * @param ex      exception that signal that a parameter cannot be modified
     */
    public InternalMediatorException(long simTime, Exception ex) {
        super(simTime + " " + ex.getMessage());
        initCause(ex);
    }

    /**
     * Constructor based on a message and a thrown exception.
     *
     * @param ex      exception that signal that a parameter cannot be modified
     * @param message string describing the exception
     */
    public InternalMediatorException(String message, Exception ex) {
        super(message);
        initCause(ex);
    }

}