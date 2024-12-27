/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.service.inbound.controller

import org.eclipse.lmos.runtime.core.inbound.ConversationHandler
import org.eclipse.lmos.runtime.core.model.AssistantMessage
import org.eclipse.lmos.runtime.core.model.Conversation
import org.eclipse.lmos.runtime.service.constants.LmosServiceConstants.Endpoints.BASE_PATH
import org.eclipse.lmos.runtime.service.constants.LmosServiceConstants.Endpoints.CHAT_URL
import org.eclipse.lmos.runtime.service.constants.LmosServiceConstants.Headers.TURN_ID
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BASE_PATH)
class ConversationController(private val conversationHandler: ConversationHandler) {
    private val log = LoggerFactory.getLogger(ConversationController::class.java)

    @PostMapping(CHAT_URL)
    suspend fun chat(
        @RequestBody conversation: Conversation,
        @PathVariable conversationId: String,
        @PathVariable tenantId: String,
        @RequestHeader(TURN_ID) turnId: String,
    ): ResponseEntity<AssistantMessage> {
        val assistantMessage = conversationHandler.handleConversation(conversation, conversationId, tenantId, turnId)
        log.info("Response generated: ${assistantMessage.content}")
        return ResponseEntity.ok(assistantMessage)
    }
}
