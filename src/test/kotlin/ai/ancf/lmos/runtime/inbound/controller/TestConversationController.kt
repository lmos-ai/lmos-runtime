/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.inbound.controller

import org.eclipse.lmos.runtime.core.constants.ApiConstants.Endpoints.BASE_PATH
import org.eclipse.lmos.runtime.core.constants.ApiConstants.Endpoints.CHAT_URL
import org.eclipse.lmos.runtime.core.constants.ApiConstants.Headers.TURN_ID
import org.eclipse.lmos.runtime.core.model.AssistantMessage
import org.eclipse.lmos.runtime.core.model.Conversation
import org.eclipse.lmos.runtime.core.service.inbound.ConversationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BASE_PATH)
class TestConversationController(private val conversationService: ConversationService) {
    @PostMapping(CHAT_URL)
    suspend fun chat(
        @RequestBody conversation: Conversation,
        @PathVariable conversationId: String,
        @PathVariable tenantId: String,
        @RequestHeader(TURN_ID) turnId: String,
    ): ResponseEntity<AssistantMessage> {
        return ResponseEntity.ok(conversationService.processConversation(conversation, conversationId, tenantId, turnId))
    }
}
