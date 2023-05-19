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

package org.eclipse.mosaic.fed.carla.config;

import org.eclipse.mosaic.lib.util.gson.TimeFieldAdapter;

import com.google.gson.annotations.JsonAdapter;

import java.io.Serializable;

/**
 * The CARLA Ambassador configuration class.
 */
public class CCarla implements Serializable {

    private static final long serialVersionUID = 1479294781446446539L;

    /**
     * The Interval after which vehicle info is updated. Define the size of one
     * simulation step in carla (minimal value: 100). The default value is 1000
     * (1s). Unit: [ms].
     */
    @JsonAdapter(TimeFieldAdapter.LegacyMilliSeconds.class)
    public Long updateInterval = 1000L;

    /**
     * CARLA executable path
     */
    public String carlaUE4Path;

    /**
     * path to connection mediator
     */
    public String mediatorPath;

    /**
     * Carla connection port
     */
    public int carlaConnectionPort;

    /**
     * Name of the main CARLA scenario configuration (*.carlacfg). If this member
     * equals null, the CARLA ambassador will try to find a ".carlacfg" file.
     */
    public String carlaConfigurationFile;

    /**
     * If too many vehicles try to enter the simulation, SUMO might skip some
     * vehicles and tries to enter them later again. This behavior can lead to
     * wrong simulation results. This parameter defines, if the ambassador should try
     * to continue the simulation in such cases. Quit SUMO, if an error occurs
     * while inserting a new vehicle (e.g. due to high vehicle densities)
     * (recommended: true).
     */
    public boolean exitOnInsertionError = true;

    /**
     * Add additional parameter to the SUMO start command. Prepend always a
     * blank. The default is a seed of 100000. Set a particular seed for the
     * random number generator. By using different values you can have different
     * but still reproducible simulation runs. Ignore possible waiting times by
     * setting time-to-teleport to 0. This avoid unmoved "vehicles" (in our case
     * also RSUs) being removed from simulation.
     */
    public String additionalCarlaParameters = "";

    /**
     * This offset is added to all time-gap related parametrizations of vehicles.
     * (e.g. declaring vehicle types to SUMO, changing time-gap/reaction time during simulation)
     * This could be helpful as IDM should be parametrized with lower time gaps to achieve specific time gap values.
     */
    public double timeGapOffset = 0;

    /**
     * If set to {@code true} all vehicles will be subscribed (see
     * {@link org.eclipse.mosaic.fed.carla.mediator.facades.SimulationFacade#subscribeForVehicle(String, long, long)}).
     * If set to {@code false} only vehicles with applications mapped to them will be subscribed.
     */
    public boolean subscribeToAllVehicles = true;

    /**
     * Prints out all carla calls.
     */
    public boolean debugCalls = false;

    /**
     * A optional list of subscriptions for each vehicle in the simulation. The less subscriptions given,
     * the faster the simulation. Per default (if this list is set to null), all subscriptions are activated.
     * Please note, that some components expect specific information, such as the road position. If those information
     * is not subscribed, these components may fail.
     * <br/><br/>
     * Possible values are: "roadposition", "signals", "emissions", "leader"
     */
    public Collection<String> subscriptions;

    /**
     * Subscription identifier for everything which is related to the position of the vehicle on the road,
     * such as the ID of the road.
     */
    public final static String SUBSCRIPTION_ROAD_POSITION = "roadposition";

    /**
     * Subscription identifier for everything which is related to the signals on the vehicle.
     */
    public final static String SUBSCRIPTION_SIGNALS = "signals";

    /**
     * Allows configuring specialised vType parameters, which can't be configured via Mapping.
     * E.g. parameters for the lane change model of vehicles.
     */
    public Map<String, Map<String, String>> additionalVehicleTypeParameters = new HashMap<>();

    /**
     * Configure the mode with which a vehicle is moved to a explicit postion with the SUMO command moveToXY().
     * (SWITCH_ROUTE, KEEP_ROUTE or EXACT_POSITION)
     */
    public VehicleSetMoveToXY.Mode moveToXyMode = org.eclipse.mosaic.fed.carla.mediator.api.VehicleSetMoveToXY.Mode.SWITCH_ROUTE;

    public final static String HIGHLIGHT_CHANGE_LANE = "changeLane";
    public final static String HIGHLIGHT_CHANGE_ROUTE = "changeRoute";
}
