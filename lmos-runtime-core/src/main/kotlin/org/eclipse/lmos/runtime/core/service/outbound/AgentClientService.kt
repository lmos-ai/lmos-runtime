/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.core.service.outbound

import org.eclipse.lmos.runtime.core.model.Address
import org.eclipse.lmos.runtime.core.model.AssistantMessage
import org.eclipse.lmos.runtime.core.model.Conversation

interface AgentClientService {
    suspend fun askAgent(
        conversation: Conversation,
        conversationId: String,
        turnId: String,
        agentName: String,
        agentAddress: Address,
        subset: String?,
    ): AssistantMessage
}
