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
package org.eclipse.mosaic.fed.scenario.ambassador;

import org.junit.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.mosaic.lib.util.junit.TestFileRule;
import org.eclipse.mosaic.rti.TIME;
import org.eclipse.mosaic.rti.api.RtiAmbassador;
import org.eclipse.mosaic.rti.api.parameters.AmbassadorParameter;
import org.eclipse.mosaic.rti.api.parameters.FederateDescriptor;
import org.eclipse.mosaic.rti.config.CLocalHost;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.net.URL;

/**
 * Tests for {@link ScenarioAmbassador}.
 */
public class ScenarioAmbassadorTest {

    private final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final TestFileRule testFileRule = new TestFileRule(temporaryFolder).basedir("scenario");

    @Rule
    public RuleChain chain = RuleChain.outerRule(temporaryFolder).around(testFileRule);

    private RtiAmbassador rtiMock;

    private ScenarioAmbassador ambassador;

    @Before
    public void setup() throws IOException, Exception {

        rtiMock = mock(RtiAmbassador.class);

        FederateDescriptor handleMock = mock(FederateDescriptor.class);

        File workingDir = temporaryFolder.getRoot();

        Path sourcePath = new File("/scenario_config.json").toPath();
        Path targetPath = temporaryFolder.newFile("scenario_config.json").toPath();

        Files.copy(
                sourcePath,
                targetPath.resolve(sourcePath.relativize(sourcePath)),
                StandardCopyOption.COPY_ATTRIBUTES,
                StandardCopyOption.REPLACE_EXISTING,
                LinkOption.NOFOLLOW_LINKS
        );
        CLocalHost testHostConfig = new CLocalHost();

        testHostConfig.workingDirectory = workingDir.getAbsolutePath();

        when(handleMock.getHost()).thenReturn(testHostConfig);

        when(handleMock.getId()).thenReturn("scenario");

        ambassador = new ScenarioAmbassador(
                new AmbassadorParameter("scenario", targetPath.toFile()));

        ambassador.setRtiAmbassador(rtiMock);

        ambassador.setFederateDescriptor(handleMock);
    }

    @Test
    public void initialize() throws Throwable {

        // RUN
        ambassador.initialize(0, 100 * TIME.SECOND);
        // ASSERT
        verify(rtiMock, times(1)).requestAdvanceTime(eq(0L), eq(0L), eq((byte) 0));
        
        assertEquals(ambassador.scenarioConfig.updateInterval, Long.valueOf(200L));
        
    }

}
