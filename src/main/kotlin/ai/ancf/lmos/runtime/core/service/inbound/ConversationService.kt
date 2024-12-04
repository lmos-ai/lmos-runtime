/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.service.inbound

import ai.ancf.lmos.runtime.core.constants.ApiConstants.Headers.SUBSET
import ai.ancf.lmos.runtime.core.model.Agent
import ai.ancf.lmos.runtime.core.model.AssistantMessage
import ai.ancf.lmos.runtime.core.model.Conversation
import ai.ancf.lmos.runtime.core.service.cache.LmosRuntimeTenantAwareCache
import ai.ancf.lmos.runtime.core.service.outbound.AgentClientService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRegistryService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRoutingService
import org.slf4j.LoggerFactory

public interface ConversationService {
    suspend fun processConversation(
        conversation: Conversation,
        conversationId: String,
        tenantId: String,
        turnId: String,
    ): AssistantMessage
}

class DefaultConversationService(
    private val agentRegistryService: AgentRegistryService,
    private val agentRoutingService: AgentRoutingService,
    private val agentClientService: AgentClientService,
    private val lmosRuntimeTenantAwareCache: LmosRuntimeTenantAwareCache<String>,
) : ConversationService {
    private val log = LoggerFactory.getLogger(DefaultConversationService::class.java)

    override suspend fun processConversation(
        conversation: Conversation,
        conversationId: String,
        tenantId: String,
        turnId: String,
    ): AssistantMessage {
        val subset = lmosRuntimeTenantAwareCache.get(tenantId, SUBSET, conversationId)
        log.info("Request Received, conversationId: $conversationId, turnId: $turnId, subset: $subset")
        val routingInformation = agentRegistryService.getRoutingInformation(tenantId, conversation.systemContext.channelId, subset)
        if (routingInformation.subset != null) {
            lmosRuntimeTenantAwareCache.save(tenantId, SUBSET, conversationId, routingInformation.subset)
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
