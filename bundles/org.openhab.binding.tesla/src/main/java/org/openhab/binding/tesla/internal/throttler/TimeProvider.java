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
package org.openhab.binding.tesla.internal.throttler;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TimeProvider} provides time stamps
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public interface TimeProvider {
    static final TimeProvider SYSTEM_PROVIDER = new TimeProvider() {
        @Override
        public long getCurrentTimeInMillis() {
            return System.currentTimeMillis();
        }
    };

    long getCurrentTimeInMillis();
}
