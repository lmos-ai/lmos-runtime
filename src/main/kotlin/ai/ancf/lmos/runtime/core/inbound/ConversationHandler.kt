/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.inbound

import ai.ancf.lmos.runtime.core.model.AssistantMessage
import ai.ancf.lmos.runtime.core.model.Conversation
import ai.ancf.lmos.runtime.core.service.inbound.ConversationService

interface ConversationHandler {
    suspend fun handleConversation(
        conversation: Conversation,
        conversationId: String,
        tenantId: String,
        turnId: String,
    ): AssistantMessage
}

class DefaultConversationHandler(private val conversationService: ConversationService) : ConversationHandler {
    override suspend fun handleConversation(
        conversation: Conversation,
        conversationId: String,
        tenantId: String,
        turnId: String,
    ): AssistantMessage {
        return conversationService.processConversation(conversation, conversationId, tenantId, turnId)
    }
}
