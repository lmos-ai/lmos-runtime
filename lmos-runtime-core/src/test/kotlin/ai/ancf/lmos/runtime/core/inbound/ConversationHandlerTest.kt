/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */
package ai.ancf.lmos.runtime.core.inbound

import ai.ancf.lmos.arc.api.Message
import ai.ancf.lmos.runtime.core.LmosRuntimeConfig
import ai.ancf.lmos.runtime.core.cache.LmosRuntimeTenantAwareCache
import ai.ancf.lmos.runtime.core.cache.TenantAwareInMemoryCache
import ai.ancf.lmos.runtime.core.constants.LmosRuntimeConstants.Cache.ROUTES
import ai.ancf.lmos.runtime.core.exception.AgentClientException
import ai.ancf.lmos.runtime.core.exception.NoRoutingInfoFoundException
import ai.ancf.lmos.runtime.core.model.*
import ai.ancf.lmos.runtime.core.service.outbound.AgentClientService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRegistryService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRoutingService
import ai.ancf.lmos.runtime.core.service.routing.ExplicitAgentRoutingService
import ai.ancf.lmos.runtime.outbound.RoutingInformation
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ConversationHandlerTest {
    private lateinit var agentRegistryService: AgentRegistryService
    private lateinit var agentRoutingService: AgentRoutingService
    private lateinit var agentClientService: AgentClientService
    private lateinit var lmosRuntimeTenantAwareCache: LmosRuntimeTenantAwareCache<RoutingInformation>
    private lateinit var conversationHandler: ConversationHandler
    private lateinit var lmosRuntimeConfig: LmosRuntimeConfig

    @BeforeEach
    fun setUp() {
        agentClientService = mockk<AgentClientService>()
        agentRegistryService = mockk<AgentRegistryService>()

        agentRoutingService = ExplicitAgentRoutingService()
        lmosRuntimeTenantAwareCache = spyk(TenantAwareInMemoryCache())
        lmosRuntimeConfig =
            LmosRuntimeConfig(
                mockk<LmosRuntimeConfig.AgentRegistry>(),
                cache = LmosRuntimeConfig.Cache(ttl = 6000),
            )
        conversationHandler =
            DefaultConversationHandler(
                agentRegistryService,
                agentRoutingService,
                agentClientService,
                lmosRuntimeConfig,
                lmosRuntimeTenantAwareCache,
            )
    }

    @Test
    fun `test handleConversation with null subset`() =
        runBlocking {
            val conversationId = "testConversationId"
            val tenantId = "testTenantId"
            val turnId = "testTurnId"

            val conversation = conversation()
            val routingInformation = routingInformation()

            val resolvedAgent = routingInformation.agentList[0]
            val agentResponse = AssistantMessage("response")

            mockAgentRegistry(tenantId, conversation.systemContext.channelId, routingInformation)
            mockAgentClient(
                conversation,
                conversationId,
                turnId,
                resolvedAgent.name,
                Address(uri = "http://localhost:8080/"),
                null,
                agentResponse,
            )

            // Invoke method
            val result = conversationHandler.handleConversation(conversation, conversationId, tenantId, turnId)

            // Assertions
            assertEquals(agentResponse, result)
        }

    @Test
    fun `test subset returned by routing information cached and used in agent call`() =
        runBlocking {
            val conversationId = "conv1"
            val tenantId = "tenant1"
            val turnId = "turn1"
            val subset = "non-null-subset"

            val conversation = conversation()
            val routingInformation = routingInformation(subset)

            val resolvedAgent = routingInformation.agentList[0]
            val assistantMessage = AssistantMessage("Response from agent", listOf())

            mockAgentClient(
                conversation,
                conversationId,
                turnId,
                resolvedAgent.name,
                resolvedAgent.addresses.first(),
                subset,
                assistantMessage,
            )

            // Execute the method
            conversationHandler.handleConversation(conversation, conversationId, tenantId, turnId)

            coVerify {
                agentClientService.askAgent(
                    conversation,
                    conversationId,
                    turnId,
                    resolvedAgent.name,
                    resolvedAgent.addresses.first(),
                    subset,
                )
            }
        }

    @Test
    fun `test routing information is cached`() =
        runBlocking {
            // Arrange
            val conversationId = "conv-124"
            val tenantId = "tenant-1"
            val turnId = "turn-1"
            val cachedSubset = "cached-subset"

            val conversation = conversation()
            val routingInformation = routingInformation(cachedSubset)

            val resolvedAgent = routingInformation.agentList[0]
            val expectedAgentResponse = AssistantMessage(content = "Test response")

            mockAgentRegistry(tenantId, conversation.systemContext.channelId, routingInformation)
            mockAgentClient(
                conversation,
                conversationId,
                turnId,
                resolvedAgent.name,
                resolvedAgent.addresses.first(),
                cachedSubset,
                expectedAgentResponse,
            )

            val result =
                conversationHandler.handleConversation(
                    conversation,
                    conversationId,
                    tenantId,
                    turnId,
                )

            assertEquals(expectedAgentResponse, result)

            coVerify(exactly = 1) {
                lmosRuntimeTenantAwareCache.save(
                    tenantId,
                    ROUTES,
                    conversationId,
                    routingInformation,
                    any(),
                )
            }
        }

    @Test
    fun `test cached routing information is used`() =
        runBlocking {
            // Arrange
            val conversationId = "conv-124"
            val tenantId = "tenant-1"
            val turnId = "turn-1"
            val cachedSubset = "cached-subset"

            val conversation = conversation()
            val routingInformation = routingInformation(cachedSubset)

            val resolvedAgent = routingInformation.agentList[0]
            val expectedAgentResponse = AssistantMessage(content = "Test response")

            lmosRuntimeTenantAwareCache.save(tenantId, ROUTES, conversationId, routingInformation)
            clearAllMocks()

            mockAgentRegistry(tenantId, conversation.systemContext.channelId, routingInformation)
            mockAgentClient(
                conversation,
                conversationId,
                turnId,
                resolvedAgent.name,
                resolvedAgent.addresses.first(),
                cachedSubset,
                expectedAgentResponse,
            )

            val result =
                conversationHandler.handleConversation(
                    conversation,
                    conversationId,
                    tenantId,
                    turnId,
                )

            assertEquals(expectedAgentResponse, result)

            coVerify(exactly = 0) {
                lmosRuntimeTenantAwareCache.save(
                    tenantId,
                    ROUTES,
                    conversationId,
                    routingInformation,
                )
            }
        }

    @Test
    fun `when agent registry returns error then throws exception`() {
        // Setup
        val conversation = conversation()
        val conversationId = "testConversationId"
        val tenantId = "testTenantId"
        val turnId = "testTurnId"

        coEvery {
            agentRegistryService.getRoutingInformation(tenantId, conversation.systemContext.channelId)
        } throws NoRoutingInfoFoundException("Registry Error")

        assertThrows<NoRoutingInfoFoundException> {
            runBlocking {
                conversationHandler.handleConversation(conversation, conversationId, tenantId, turnId)
            }
        }
    }

    @Test
    fun `when agent client returns error then throws exception`() {
        // Setup
        val conversationId = "testConversationId"
        val tenantId = "testTenantId"
        val turnId = "testTurnId"

        val conversation = conversation()
        val routingInformation = routingInformation()

        mockAgentRegistry(tenantId, conversation.systemContext.channelId, routingInformation)
        coEvery {
            agentClientService.askAgent(conversation, conversationId, turnId, any(), any(), null)
        } throws AgentClientException("Agent Communication Error")

        assertThrows<AgentClientException> {
            runBlocking {
                conversationHandler.handleConversation(conversation, conversationId, tenantId, turnId)
            }
        }
    }

    @Test
    fun `handleConversation should successfully route and get agent response`() =
        runBlocking {
            // Arrange
            val conversationId = "conv-123"
            val tenantId = "tenant-1"
            val turnId = "turn-1"
            val subset = "subset-1"

            val conversation = conversation()
            val routingInformation = routingInformation(subset)
            val resolvedAgent = routingInformation.agentList[0]

            val expectedAgentResponse = AssistantMessage(content = "Test response")

            mockAgentRegistry(tenantId, conversation.systemContext.channelId, routingInformation)
            mockAgentClient(
                conversation,
                conversationId,
                turnId,
                resolvedAgent.name,
                resolvedAgent.addresses.first(),
                subset,
                expectedAgentResponse,
            )

            // Act
            val result =
                conversationHandler.handleConversation(
                    conversation,
                    conversationId,
                    tenantId,
                    turnId,
                )

            // Assert
            assertEquals(expectedAgentResponse, result)
        }

    private fun conversation(): Conversation {
        val conversation =
            Conversation(
                inputContext =
                    InputContext(
                        messages = listOf(Message("user", "Hello")),
                        explicitAgent = "agent1",
                    ),
                systemContext = SystemContext(channelId = "channel1"),
                userContext = UserContext(userId = "user1", userToken = "token1"),
            )
        return conversation
    }

    private fun routingInformation(subset: String? = null): RoutingInformation {
        val routingInformation =
            RoutingInformation(
                agentList = listOf(Agent("agent1", "v1", "desc", listOf(), setOf(Address(uri = "http://localhost:8080/")))),
                subset = subset,
            )
        return routingInformation
    }

    private fun mockAgentClient(
        conversation: Conversation,
        conversationId: String,
        turnId: String,
        agentName: String,
        address: Address,
        subset: String?,
        agentResponse: AssistantMessage,
    ) {
        coEvery { agentClientService.askAgent(conversation, conversationId, turnId, agentName, address, subset) } returns agentResponse
    }

    private fun mockAgentRegistry(
        tenantId: String,
        channelId: String,
        routingInformation: RoutingInformation,
    ) {
        coEvery { agentRegistryService.getRoutingInformation(tenantId, channelId) } returns routingInformation
    }
}
