/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.core.service.outbound

import org.eclipse.lmos.runtime.core.model.Agent
import org.eclipse.lmos.runtime.core.model.Conversation

interface AgentRoutingService {
    suspend fun resolveAgentForConversation(
        conversation: Conversation,
        agentList: List<Agent>,
    ): Agent
}
