/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.service.outbound

import ai.ancf.lmos.runtime.outbound.RoutingInformation

interface AgentRegistryService {
    suspend fun getRoutingInformation(
        tenantId: String,
        channelId: String,
    ): RoutingInformation
}
