/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.service.outbound

import ai.ancf.lmos.runtime.core.model.Agent
import ai.ancf.lmos.runtime.core.model.Conversation

interface AgentRoutingService {
    suspend fun resolveAgentForConversation(
        conversation: Conversation,
        agentList: List<Agent>,
    ): Agent
}
