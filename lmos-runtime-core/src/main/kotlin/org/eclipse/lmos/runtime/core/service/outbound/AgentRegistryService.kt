/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.core.service.outbound

import org.eclipse.lmos.runtime.outbound.RoutingInformation

interface AgentRegistryService {
    suspend fun getRoutingInformation(
        tenantId: String,
        channelId: String,
    ): RoutingInformation
}
