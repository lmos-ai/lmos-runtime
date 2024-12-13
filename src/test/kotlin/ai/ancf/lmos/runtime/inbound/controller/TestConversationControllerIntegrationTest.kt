/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.inbound.controller

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.*
import kotlinx.coroutines.flow.flow
import org.eclipse.lmos.arc.agent.client.graphql.GraphQlAgentClient
import org.eclipse.lmos.arc.api.AgentResult
import org.eclipse.lmos.arc.api.Message
import org.eclipse.lmos.router.core.Address
import org.eclipse.lmos.router.core.AgentRoutingSpecBuilder
import org.eclipse.lmos.router.core.AgentRoutingSpecResolverException
import org.eclipse.lmos.runtime.core.constants.ApiConstants.Endpoints.BASE_PATH
import org.eclipse.lmos.runtime.core.constants.ApiConstants.Endpoints.CHAT_URL
import org.eclipse.lmos.runtime.core.constants.ApiConstants.Headers.TURN_ID
import org.eclipse.lmos.runtime.core.model.*
import org.eclipse.lmos.runtime.core.properties.LmosRuntimeProperties
import org.eclipse.lmos.runtime.outbound.ArcAgentClientService
import org.eclipse.lmos.runtime.outbound.LmosAgentRoutingService
import org.eclipse.lmos.runtime.outbound.LmosOperatorAgentRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConversationControllerIntegrationTest.MockConfig::class)
class TestConversationControllerIntegrationTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var agentRegistryService: LmosOperatorAgentRegistry

    @Autowired
    private lateinit var lmosAgentRoutingService: LmosAgentRoutingService

    @Autowired
    private lateinit var arcAgentClientService: ArcAgentClientService

    @BeforeEach
    fun setup() {
        clearAllMocks()

        // Setup the MockEngine
        val mockEngine =
            MockEngine { request ->
                if (request.url.fullPath.contains("tenants/de")) {
                    respond(
                        content = "",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                } else if (request.url.fullPath.contains("tenants/at")) {
                    respond(
                        content = "",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                } else {
                    respond(
                        content = """{
  "apiVersion": "v1",
  "kind": "ChannelRouting",
  "metadata": {
    "creationTimestamp": "2024-08-25T12:34:56Z",
    "generation": 1,
    "labels": {
      "channel": "web",
      "subset": "production",
      "tenant": "en",
      "version": "v1.0"
    },
    "name": "channel-routing-web",
    "namespace": "default",
    "resourceVersion": "123456",
    "uid": "abcd-efgh-ijkl-mnop"
  },
  "spec": {
    "capabilityGroups": [
      {
        "name": "group-1",
        "description": "Handles basic web capabilities",
        "capabilities": [
          {
            "name": "capability-1",
            "requiredVersion": "1.0",
            "providedVersion": "1.1",
            "description": "Provides user authentication",
            "host": "auth-service",
            "subset": "stable"
          },
          {
            "name": "capability-2",
            "requiredVersion": "1.0",
            "providedVersion": "1.0",
            "description": "Manages user profiles",
            "host": "profile-service",
            "subset": "stable"
          }
        ]
      },
      {
        "name": "group-2",
        "description": "Handles advanced web features",
        "capabilities": [
          {
            "name": "capability-3",
            "requiredVersion": "2.0",
            "providedVersion": "2.1",
            "description": "Provides real-time notifications",
            "host": "notification-service",
            "subset": "stable"
          }
        ]
      }
    ]
  }
}
""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            }
        val httpMockClient = HttpClient(mockEngine)
        agentRegistryService.client = httpMockClient
    }

    @Test
    fun `chat returns response from agent`() {
        val conversationId = "conversation-id"
        val tenantId = "en"
        val turnId = "200-turn-id"
        val correlationId = "correlation-id"

        val conversation =
            Conversation(
                systemContext = SystemContext(channelId = "web"),
                userContext = UserContext(userId = "user-id", userToken = "user-token"),
                inputContext = InputContext(messages = listOf(Message(role = "user", content = "Hello"))),
            )

        coEvery { lmosAgentRoutingService.resolveAgent(any(), any(), any()) } returns
            AgentRoutingSpecBuilder()
                .name("mocked-agent")
                .description("Mocked Description")
                .version("1.0")
                .address(Address(protocol = "ws", uri = "localhost:8080/subscriptions"))
                .build()

        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()
        coEvery { arcAgentClientService.createGraphQlAgentClient(any()) } returns mockGraphQlAgentClient
        coEvery { mockGraphQlAgentClient.close() } just Runs
        coEvery { mockGraphQlAgentClient.callAgent(any()) } returns
            flow {
                emit(
                    AgentResult(
                        messages =
                            listOf(
                                Message(role = "assistant", content = "Dummy response from Agent"),
                            ),
                    ),
                )
            }

        // Execute the request
        webTestClient.post()
            .uri("$BASE_PATH$CHAT_URL", tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .header("x-correlation-id", correlationId)
            .bodyValue(conversation)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content").isEqualTo("Dummy response from Agent")

        coVerify(exactly = 1) { lmosAgentRoutingService.resolveAgent(any(), any(), any()) }
        coVerify(exactly = 1) { mockGraphQlAgentClient.callAgent(any()) }
        coVerify(exactly = 1) { mockGraphQlAgentClient.close() }
    }

    @Test
    fun `chat returns 404 when no agent found in agent registry`() {
        val conversationId = "conversation-id"
        val tenantId = "de"
        val turnId = "404-agent-registry"
        val correlationId = "correlation-id"

        val conversation =
            Conversation(
                systemContext = SystemContext(channelId = "web"),
                userContext = UserContext(userId = "user-id", userToken = "user-token"),
                inputContext = InputContext(messages = listOf(Message(role = "user", content = "Hello"))),
            )

        coEvery { lmosAgentRoutingService.resolveAgent(any(), any(), any()) } returns
            AgentRoutingSpecBuilder()
                .name("mocked-agent")
                .description("Mocked Description")
                .version("1.0")
                .address(Address(protocol = "ws", uri = "localhost:8080/subscriptions"))
                .build()

        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()
        coEvery { arcAgentClientService.createGraphQlAgentClient(any()) } returns mockGraphQlAgentClient
        coEvery { mockGraphQlAgentClient.callAgent(any()) } returns
            flow {
                emit(
                    AgentResult(
                        messages =
                            listOf(
                                Message(role = "assistant", content = "Dummy response from Agent"),
                            ),
                    ),
                )
            }

        // Execute the request
        webTestClient.post()
            .uri("$BASE_PATH$CHAT_URL", tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .bodyValue(conversation)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("$.errorCode").isEqualTo("AGENT_NOT_FOUND")

        coVerify(exactly = 0) { lmosAgentRoutingService.resolveAgent(any(), any(), any()) }
        coVerify(exactly = 0) { mockGraphQlAgentClient.callAgent(any()) }
    }

    @Test
    fun `chat returns 500 when agent registry returns error response`() {
        val conversationId = "conversation-id"
        val tenantId = "at"
        val turnId = "500-registry-turn-id"
        val correlationId = "correlation-id"

        val conversation =
            Conversation(
                systemContext = SystemContext(channelId = "web"),
                userContext = UserContext(userId = "user-id", userToken = "user-token"),
                inputContext = InputContext(messages = listOf(Message(role = "user", content = "Hello"))),
            )

        coEvery { lmosAgentRoutingService.resolveAgent(any(), any(), any()) } returns
            AgentRoutingSpecBuilder()
                .name("mocked-agent")
                .description("Mocked Description")
                .version("1.0")
                .address(Address(protocol = "ws", uri = "localhost:8080/subscriptions"))
                .build()

        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()
        coEvery { arcAgentClientService.createGraphQlAgentClient(any()) } returns mockGraphQlAgentClient
        coEvery { mockGraphQlAgentClient.callAgent(any()) } returns
            flow {
                emit(
                    AgentResult(
                        messages =
                            listOf(
                                Message(role = "assistant", content = "Dummy response from Agent"),
                            ),
                    ),
                )
            }

        // Execute the request
        webTestClient.post()
            .uri("$BASE_PATH$CHAT_URL", tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .bodyValue(conversation)
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.errorCode").isEqualTo("INTERNAL_SERVER_ERROR")
            .jsonPath("$.message").isEqualTo("Unexpected response from operator")

        coVerify(exactly = 0) { lmosAgentRoutingService.resolveAgent(any(), any(), any()) }
        coVerify(exactly = 0) { mockGraphQlAgentClient.callAgent(any()) }
    }

    @Test
    fun `chat returns 404 when agent not resolved`() {
        val conversationId = "conversation-id"
        val tenantId = "en"
        val turnId = "turn-id"
        val correlationId = "correlation-id"

        val conversation =
            Conversation(
                systemContext = SystemContext(channelId = "web"),
                userContext = UserContext(userId = "user-id", userToken = "user-token"),
                inputContext = InputContext(messages = listOf(Message(role = "user", content = "Hello"))),
            )

        coEvery {
            lmosAgentRoutingService.resolveAgent(any(), any(), any())
        } throws AgentRoutingSpecResolverException("No agent resolved for user query")

        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()
        coEvery { arcAgentClientService.createGraphQlAgentClient(any()) } returns mockGraphQlAgentClient
        coEvery { mockGraphQlAgentClient.callAgent(any()) } returns
            flow {
                emit(
                    AgentResult(
                        messages =
                            listOf(
                                Message(role = "assistant", content = "Dummy response from Agent"),
                            ),
                    ),
                )
            }

        // Execute the request
        webTestClient.post()
            .uri("$BASE_PATH$CHAT_URL", tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .bodyValue(conversation)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.errorCode").isEqualTo("AGENT_NOT_FOUND")
            .jsonPath("$.message").isEqualTo("No agent resolved for user query")

        coVerify(exactly = 1) { lmosAgentRoutingService.resolveAgent(any(), any(), any()) }
        coVerify(exactly = 0) { mockGraphQlAgentClient.callAgent(any()) }
    }

    @Test
    fun `chat return 500 not found when agent client returns error`() {
        val conversationId = "conversation-id"
        val tenantId = "en"
        val turnId = "turn-id"
        val correlationId = "correlation-id"

        val conversation =
            Conversation(
                systemContext = SystemContext(channelId = "web"),
                userContext = UserContext(userId = "user-id", userToken = "user-token"),
                inputContext = InputContext(messages = listOf(Message(role = "user", content = "Hello"))),
            )

        coEvery { lmosAgentRoutingService.resolveAgent(any(), any(), any()) } returns
            AgentRoutingSpecBuilder()
                .name("mocked-agent")
                .description("Mocked Description")
                .version("1.0")
                .address(Address(protocol = "ws", uri = "localhost:8080/subscriptions"))
                .build()

        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()
        coEvery { arcAgentClientService.createGraphQlAgentClient(any()) } returns mockGraphQlAgentClient
        coEvery { mockGraphQlAgentClient.callAgent(any()) } throws RuntimeException("Something went wrong")

        // Execute the request
        webTestClient.post()
            .uri("$BASE_PATH$CHAT_URL", tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .bodyValue(conversation)
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.errorCode").isEqualTo("AGENT_CLIENT_EXCEPTION")

        coVerify(exactly = 1) { lmosAgentRoutingService.resolveAgent(any(), any(), any()) }
        coVerify(exactly = 1) { mockGraphQlAgentClient.callAgent(any()) }
    }

    @TestConfiguration
    open class MockConfig {
        @Bean
        open fun lmosAgentRoutingService(lmosRuntimeProperties: LmosRuntimeProperties): LmosAgentRoutingService =
            spyk(LmosAgentRoutingService(lmosRuntimeProperties))

        @Bean
        open fun arcAgentClientService(): ArcAgentClientService = spyk()

        @Bean
        open fun agentRoutingService(lmosRuntimeProperties: LmosRuntimeProperties): LmosOperatorAgentRegistry =
            spyk(LmosOperatorAgentRegistry(lmosRuntimeProperties))
    }
}
