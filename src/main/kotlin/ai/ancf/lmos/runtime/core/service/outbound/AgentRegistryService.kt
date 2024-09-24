/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.service.outbound

import ai.ancf.lmos.runtime.core.model.Agent

interface AgentRegistryService {
    suspend fun getAgents(
        tenantId: String,
        channelId: String,
    ): List<Agent>
}
