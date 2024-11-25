/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.inbound

import ai.ancf.lmos.arc.agent.client.graphql.GraphQlAgentClient
import ai.ancf.lmos.arc.api.AgentRequest
import ai.ancf.lmos.arc.api.AgentResult
import ai.ancf.lmos.arc.api.Message
import ai.ancf.lmos.runtime.core.LmosRuntimeConfig
import ai.ancf.lmos.runtime.core.cache.TenantAwareInMemoryCache
import ai.ancf.lmos.runtime.core.exception.AgentClientException
import ai.ancf.lmos.runtime.core.exception.AgentNotFoundException
import ai.ancf.lmos.runtime.core.exception.NoRoutingInfoFoundException
import ai.ancf.lmos.runtime.core.model.*
import ai.ancf.lmos.runtime.core.service.routing.ExplicitAgentRoutingService
import ai.ancf.lmos.runtime.outbound.ArcAgentClientService
import ai.ancf.lmos.runtime.outbound.LmosOperatorAgentRegistry
import ai.ancf.lmos.runtime.outbound.RoutingInformation
import ai.ancf.lmos.runtime.test.BaseWireMockTest
import io.mockk.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ConversationHandlerIntegrationTest: BaseWireMockTest() {

    private val lmosRuntimeConfig = LmosRuntimeConfig(
        agentRegistry = LmosRuntimeConfig.AgentRegistry(baseUrl = "http://localhost:$mockPort/agentRegistry"),
        cache = LmosRuntimeConfig.Cache(ttl = 6000)
    )
    private val lmosRuntimeTenantAwareCache = TenantAwareInMemoryCache<String>()
    private val routingInformationCache = TenantAwareInMemoryCache<RoutingInformation>()
    private val agentRegistryService = LmosOperatorAgentRegistry(lmosRuntimeConfig, routingInformationCache)
    private val agentRoutingService = ExplicitAgentRoutingService()
    private val agentClientService = spyk(ArcAgentClientService())

    private val conversationHandler = DefaultConversationHandler(agentRegistryService, agentRoutingService,
        agentClientService, lmosRuntimeTenantAwareCache, lmosRuntimeConfig)

    @Test
    fun `should return agent response when explicitly specified`() = runBlocking {
        val conversationId = "conversation-id-success"
        val tenantId = "en"
        val turnId = "turn-id-success-1"

        val conversation = createConversation("UserManagementAgent")

        val agentAddress = Address(protocol="http", uri="localhost:8080/user-agent")

        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()
        coEvery { agentClientService.createGraphQlAgentClient(agentAddress) } returns mockGraphQlAgentClient
        coEvery { mockGraphQlAgentClient.close() } just runs
        coEvery { mockGraphQlAgentClient.callAgent(any<AgentRequest>()) } returns
                flow {
                    emit(AgentResult(messages = listOf(Message(role = "assistant", content = "Dummy response from Agent"))))
                }

            val assistantMessage =
                conversationHandler.handleConversation(conversation, conversationId, tenantId, turnId)

        assertEquals("Dummy response from Agent", assistantMessage.content)
        coVerify(exactly = 1) { mockGraphQlAgentClient.callAgent(any(), any(), any()) }
    }

    @Test
    fun `should throw NoRoutingInfoFoundException when no agent found in agent registry`() =  runBlocking {
        val conversationId = "conversation-id-404"
        val tenantId = "de"
        val turnId = "404-agent-registry"

        val conversation = createConversation("UserManagementAgent")

        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()

        assertThrows<NoRoutingInfoFoundException> { conversationHandler.handleConversation(conversation, conversationId, tenantId, turnId) }
        coVerify(exactly = 0) { mockGraphQlAgentClient.callAgent(any(), any(), any()) }

    }

    @Test
    fun `should throw AgentNotFoundException when matching agent not found`() = runBlocking {
        val conversationId = "conversation-id"
        val tenantId = "en"
        val turnId = "turn-id"

        val conversation = createConversation("UnconfiguredAgent")

        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()

        assertThrows<AgentNotFoundException> { conversationHandler.handleConversation(conversation, conversationId, tenantId, turnId) }
        coVerify(exactly = 0) { mockGraphQlAgentClient.callAgent(any(), any(), any()) }

    }

    @Test
    fun `should throw AgentClientException when agent returns error`() = runBlocking {
        val conversationId = "conversation-id"
        val tenantId = "en"
        val turnId = "turn-id"

        val conversation = createConversation("UserManagementAgent")

        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()
        coEvery { agentClientService.createGraphQlAgentClient(any()) } returns mockGraphQlAgentClient
        coEvery { mockGraphQlAgentClient.callAgent(any(), any(), any()) } throws RuntimeException("Something went wrong")

        assertThrows<AgentClientException> { conversationHandler.handleConversation(conversation, conversationId, tenantId, turnId) }
        coVerify(exactly = 1) { mockGraphQlAgentClient.callAgent(any(), any(), any()) }
    }

    private fun createConversation(agent: String) = Conversation(
        systemContext = SystemContext(channelId = "web"),
        userContext = UserContext(userId = "user-id", userToken = "user-token"),
        inputContext = InputContext(
            messages = listOf(Message(role = "user", content = "Hello")),
            explicitAgent = agent
        )
    )
}
