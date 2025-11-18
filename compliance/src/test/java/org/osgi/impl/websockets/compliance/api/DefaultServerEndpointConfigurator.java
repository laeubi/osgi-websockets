/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.osgi.impl.websockets.compliance.api;

import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Default ServerEndpointConfig.Configurator for testing purposes.
 * This is registered via META-INF/services to provide a default
 * configurator when the Builder is used without an explicit one.
 */
public class DefaultServerEndpointConfigurator extends ServerEndpointConfig.Configurator {
    // Default implementation from parent class is sufficient for testing
}
