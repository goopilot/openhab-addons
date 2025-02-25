/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.bluetooth.bluegiga.internal.command.security;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.enumeration.BgApiResponse;

/**
 * Class to implement the BlueGiga command <b>whitelistBonds</b>.
 * <p>
 * This command will add all bonded devices with a known public or static address to the local
 * devices white list. Previous entries in the white list will be first cleared. This command
 * can't be used while advertising, scanning or being connected.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaWhitelistBondsResponse extends BlueGigaResponse {
    public static final int COMMAND_CLASS = 0x05;
    public static final int COMMAND_METHOD = 0x07;

    /**
     * Command result
     * <p>
     * BlueGiga API type is <i>BgApiResponse</i> - Java type is {@link BgApiResponse}
     */
    private BgApiResponse result;

    /**
     * Number of whitelisted bonds
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int count;

    /**
     * Response constructor
     */
    public BlueGigaWhitelistBondsResponse(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        event = (inputBuffer[0] & 0x80) != 0;

        // Deserialize the fields
        result = deserializeBgApiResponse();
        count = deserializeUInt8();
    }

    /**
     * Command result
     * <p>
     * BlueGiga API type is <i>BgApiResponse</i> - Java type is {@link BgApiResponse}
     *
     * @return the current result as {@link BgApiResponse}
     */
    public BgApiResponse getResult() {
        return result;
    }

    /**
     * Number of whitelisted bonds
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     *
     * @return the current count as {@link int}
     */
    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaWhitelistBondsResponse [result=");
        builder.append(result);
        builder.append(", count=");
        builder.append(count);
        builder.append(']');
        return builder.toString();
    }
}
