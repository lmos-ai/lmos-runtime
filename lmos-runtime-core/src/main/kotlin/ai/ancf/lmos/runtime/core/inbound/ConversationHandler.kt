/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.inbound

import ai.ancf.lmos.runtime.core.LmosRuntimeConfig
import ai.ancf.lmos.runtime.core.cache.LmosRuntimeTenantAwareCache
import ai.ancf.lmos.runtime.core.constants.LmosRuntimeConstants.SUBSET
import ai.ancf.lmos.runtime.core.model.Agent
import ai.ancf.lmos.runtime.core.model.AssistantMessage
import ai.ancf.lmos.runtime.core.model.Conversation
import ai.ancf.lmos.runtime.core.service.outbound.AgentClientService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRegistryService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRoutingService
import org.slf4j.LoggerFactory

interface ConversationHandler {
    suspend fun handleConversation(
        conversation: Conversation,
        conversationId: String,
        tenantId: String,
        turnId: String,
    ): AssistantMessage
}

class DefaultConversationHandler(
    private val agentRegistryService: AgentRegistryService,
    private val agentRoutingService: AgentRoutingService,
    private val agentClientService: AgentClientService,
    private val lmosRuntimeTenantAwareCache: LmosRuntimeTenantAwareCache<String>,
    private val lmosRuntimeConfig: LmosRuntimeConfig,
) : ConversationHandler {
    private val log = LoggerFactory.getLogger(DefaultConversationHandler::class.java)

    override suspend fun handleConversation(
        conversation: Conversation,
        conversationId: String,
        tenantId: String,
        turnId: String,
    ): AssistantMessage {
        var subset = lmosRuntimeTenantAwareCache.get(tenantId, SUBSET, conversationId)
        log.info("Request Received, conversationId: $conversationId, turnId: $turnId, subset: $subset")
        val routingInformation = agentRegistryService.getRoutingInformation(tenantId, conversation.systemContext.channelId, subset)
        routingInformation.subset?.takeIf { it != subset }?.let { newSubset ->
            subset = newSubset
            lmosRuntimeTenantAwareCache.save(tenantId, SUBSET, conversationId, newSubset, lmosRuntimeConfig.cache.ttl)
        }
        log.info("routingInformation: $routingInformation")
        val agent: Agent = agentRoutingService.resolveAgentForConversation(conversation, routingInformation.agentList)
        log.info("Resolved agent: $agent")
        // pass subset here
        val agentResponse = agentClientService.askAgent(conversation, conversationId, turnId, agent.name, agent.addresses.random(), subset)
        log.info("Agent Response: $agentResponse")
        return agentResponse
    }
}
