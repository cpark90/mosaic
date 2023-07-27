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

package org.eclipse.mosaic.fed.scenario.config;

import org.eclipse.mosaic.lib.util.gson.TimeFieldAdapter;

import com.google.gson.annotations.JsonAdapter;

import java.io.Serializable;

/**
 * The CARLA Ambassador configuration class.
 */
public class ScenarioConfiguration implements Serializable {

    private static final long serialVersionUID = 1479294781446446539L;

    /**
     * The Interval after which vehicle info is updated. Define the size of one
     * simulation step in carla (minimal value: 100). The default value is 1000
     * (1s). Unit: [ms].
     */
    @JsonAdapter(TimeFieldAdapter.LegacyMilliSeconds.class)
    public Long updateInterval = 1000L;

    /**
     * Scenario executable path
     */
    public String scenarioBinPath;

    /**
     * path to connection mediator
     */
    public String mediatorMap;
    public String mediatorNetFile;
    public double mediatorStepLength;
    public String mediatorTlsManager;

    /**
     * Scenario connection port
     */
    public int scenarioConnectionPort;

}
