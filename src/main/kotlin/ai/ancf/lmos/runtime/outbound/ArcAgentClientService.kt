/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.outbound

import ai.ancf.lmos.arc.agent.client.graphql.GraphQlAgentClient
import ai.ancf.lmos.arc.api.AgentRequest
import ai.ancf.lmos.arc.api.ConversationContext
import ai.ancf.lmos.arc.api.ProfileEntry
import ai.ancf.lmos.arc.api.SystemContextEntry
import ai.ancf.lmos.arc.api.UserContext
import ai.ancf.lmos.runtime.core.constants.ApiConstants
import ai.ancf.lmos.runtime.core.exception.AgentClientException
import ai.ancf.lmos.runtime.core.model.Address
import ai.ancf.lmos.runtime.core.model.AssistantMessage
import ai.ancf.lmos.runtime.core.model.Conversation
import ai.ancf.lmos.runtime.core.service.outbound.AgentClientService
import kotlinx.coroutines.flow.toCollection
import org.slf4j.LoggerFactory

class ArcAgentClientService : AgentClientService {
    private val log = LoggerFactory.getLogger(ArcAgentClientService::class.java)

    override suspend fun askAgent(
        conversation: Conversation,
        conversationId: String,
        turnId: String,
        agentName: String,
        agentAddress: Address,
        subset: String?,
    ): AssistantMessage {
        return createGraphQlAgentClient(agentAddress).use { graphQlAgentClient ->

            val subsetHeader = subset?.let { mapOf(ApiConstants.Headers.SUBSET to subset) } ?: emptyMap()
            val agentResponse =
                try {
                    graphQlAgentClient.callAgent(
                        AgentRequest(
                            conversationContext =
                                ConversationContext(
                                    conversationId = conversationId,
                                    anonymizationEntities = conversation.inputContext.anonymizationEntities,
                                ),
                            systemContext =
                                conversation.systemContext.contextParams.map { (key, value) ->
                                    SystemContextEntry(key, value)
                                }.toList(),
                            userContext =
                                UserContext(
                                    userId = conversation.userContext.userId,
                                    userToken = conversation.userContext.userToken,
                                    profile =
                                        conversation.userContext.contextParams.map { (key, value) ->
                                            ProfileEntry(key, value)
                                        }.toList(),
                                ),
                            messages = conversation.inputContext.messages,
                        ),
                        requestHeaders = subsetHeader,
                    ).toCollection(mutableListOf())
                } catch (e: Exception) {
                    log.error("Error response from ArcAgentClient", e)
                    throw AgentClientException(e.message)
                }

            return AssistantMessage(
                agentResponse.first().messages[0].content,
                agentResponse.first().anonymizationEntities,
            )
        }
    }

    internal fun createGraphQlAgentClient(agentAddress: Address): GraphQlAgentClient {
        // TODO - remove hardcoded parts of agent url
        val agentUrl = "ws://${agentAddress.uri}:8080/subscriptions"
        log.info("Creating GraphQlAgentClient with url $agentUrl")
        val graphQlAgentClient = GraphQlAgentClient(agentUrl)
        return graphQlAgentClient
    }
}
