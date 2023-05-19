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

package org.eclipse.mosaic.fed.carla.mediator;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum CarlaVersion {
    UNKNOWN("0.0.0"),

    API_12("0.9.12"),
    API_13("0.9.13"),
    API_14("0.9.14"),
    
    /**
     * the lowest version supported by this client.
     */
    LOWEST(API_12.getCarlaVersion()),

    /**
     * the highest version supported by this client.
     */
    HIGHEST(API_14.getCarlaVersion());

    private final String carlaVersion;
    private final int major;
    private final int minor;

    CarlaVersion(String carlaVersion) {
        this.carlaVersion = carlaVersion;
        this.major = Integer.parseInt(StringUtils.substringBefore(carlaVersion, "."));
        this.minor = Integer.parseInt(StringUtils.substringBetween(carlaVersion, ".", "."));
    }

    public String carlaVersion() {
        return carlaVersion;
    }

    public static CarlaVersion getCarlaVersion(String carlaVersion) {
        for (CarlaVersion version : CarlaVersion.values()) {
            if (matches(carlaVersion, version.getCarlaVersion())) {
                return version;
            }
        }
        return UNKNOWN;
    }

    private final static Pattern VERSION_PATTERN = Pattern.compile("^CARLA v?([0-9]\\.[0-9]+)\\..*$");

    private static boolean matches(String carlaVersionString, String carlaVersionPattern) {
        final Matcher matcher = VERSION_PATTERN.matcher(carlaVersionString);
        return matcher.matches() && (matcher.group(1) + ".*").equals(carlaVersionPattern);

    }

    public boolean isGreaterOrEqualThan(CarlaVersion other) {
        return this.major > other.major || (this.major == other.major && this.minor >= other.minor);
    }
}
