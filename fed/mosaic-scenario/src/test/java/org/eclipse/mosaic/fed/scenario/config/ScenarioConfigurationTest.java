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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.mosaic.lib.util.objects.ObjectInstantiation;

import org.junit.Test;

public class ScenarioConfigurationTest {

    /**
     * Test case using a properly formed json configuration and asserting, that all
     * values were properly deserialized.
     *
     * @throws InstantiationException if configuration couldn't be properly
     *                                deserialized, under normal circumstances this
     *                                should not occur
     */
    @Test
    public void readConfig_assertProperties() throws InstantiationException {
        // SETUP + RUN
        String validConfig = "/scenario_config.json";
        ScenarioConfiguration scenarioConfiguration = getScenarioConfiguration(validConfig);
        // ASSERT
        assertNotNull(scenarioConfiguration); // assert that configuration is created
        assertEquals(Long.valueOf(200L), scenarioConfiguration.updateInterval);
        assertEquals("D:/CARLA_0.9.10/", scenarioConfiguration.scenarioBinPath);
        assertEquals(8913, scenarioConfiguration.scenarioConnectionPort);
    }

    /**
     * Small helper class, which returns the instantiated object of a
     * json-configuration.
     *
     * @param filePath the path to the configuration
     * @return the instantiated object
     * @throws InstantiationException if there was an error during
     *                                deserialization/instantiation
     */
    private ScenarioConfiguration getScenarioConfiguration(String filePath) throws InstantiationException {
        return new ObjectInstantiation<>(ScenarioConfiguration.class).read(getClass().getResourceAsStream(filePath));
    }

}