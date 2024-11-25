package ai.ancf.lmos.runtime.service.inbound.controller

import ai.ancf.lmos.arc.agent.client.graphql.GraphQlAgentClient
import ai.ancf.lmos.arc.api.AgentRequest
import ai.ancf.lmos.arc.api.AgentResult
import ai.ancf.lmos.arc.api.Message
import ai.ancf.lmos.runtime.core.model.*
import ai.ancf.lmos.runtime.outbound.ArcAgentClientService
import ai.ancf.lmos.runtime.test.BaseWireMockTest
import ai.ancf.lmos.runtime.service.constants.LmosServiceConstants.Endpoints.BASE_PATH
import ai.ancf.lmos.runtime.service.constants.LmosServiceConstants.Endpoints.CHAT_URL
import ai.ancf.lmos.runtime.service.constants.LmosServiceConstants.Headers.TURN_ID
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Import
class ConversationControllerIntegrationTest: BaseWireMockTest() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var agentClientService: ArcAgentClientService

    private lateinit var baseUrl: String

    @BeforeEach
    fun setup() {
        baseUrl = "$BASE_PATH$CHAT_URL"
    }

    @Test
    fun `successful conversation handling with single message`(): Unit = runBlocking {
        // Arrange
        val conversationId = UUID.randomUUID().toString()
        val tenantId = "en"
        val turnId = UUID.randomUUID().toString()

        val conversation = createConversation("SummaryAgent")

        val agentAddress = Address(protocol="http", uri="localhost:8080/summary-agent")

        mockAgentCall(agentAddress)

        webTestClient.post()
            .uri(baseUrl, tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .bodyValue(conversation)
            .exchange()
            .expectStatus().isOk
            .expectBody<AssistantMessage>()
            .consumeWith { response ->
                val assistantMessage = response.responseBody
                assertNotNull(assistantMessage)
                assertTrue(assistantMessage?.content?.isNotBlank() == true)
            }
    }

    @Test
    fun `conversation handling with multiple messages`(): Unit = runBlocking {
        val conversationId = UUID.randomUUID().toString()
        val tenantId = "en"
        val turnId = UUID.randomUUID().toString()

        val conversation = createConversation("SummaryAgent")
        val updatedInputContext = conversation.inputContext.copy(messages = listOf(
            Message(role = "user", content = "Hi"),
            Message(role = "assistant", content = "Hello")
        )
        )
        val updatedConversation = conversation.copy(inputContext = updatedInputContext)

        val agentAddress = Address(protocol="http", uri="localhost:8080/summary-agent")

        mockAgentCall(agentAddress)

        webTestClient.post()
            .uri(baseUrl, tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .bodyValue(updatedConversation)
            .exchange()
            .expectStatus().isOk
            .expectBody<AssistantMessage>()
            .consumeWith { response ->
                val assistantMessage = response.responseBody
                assertNotNull(assistantMessage)
                assertTrue(assistantMessage?.content?.isNotBlank() == true)
            }
    }

    @Test
    fun `multi-turn conversation`(): Unit = runBlocking {
        // Arrange
        val conversationId = UUID.randomUUID().toString()
        val tenantId = "en"
        val turnId = UUID.randomUUID().toString()

        val conversation = createConversation("SummaryAgent")

        val agentAddress = Address(protocol="http", uri="localhost:8080/summary-agent")

        mockAgentCall(agentAddress)

        webTestClient.post()
            .uri(baseUrl, tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .bodyValue(conversation)
            .exchange()
            .expectStatus().isOk
            .expectBody<AssistantMessage>()
            .consumeWith { response ->
                val assistantMessage = response.responseBody
                assertNotNull(assistantMessage)
                assertTrue(assistantMessage?.content?.isNotBlank() == true)
            }

        val updatedInputContext = conversation.inputContext.copy(messages = listOf(
            Message(role = "assistant", content = "Hello"),
            Message(role = "user", content = "summarize today's tech news"),
        )
        )
        val updatedConversation = conversation.copy(inputContext = updatedInputContext)

        webTestClient.post()
            .uri(baseUrl, tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId+"1")
            .bodyValue(updatedConversation)
            .exchange()
            .expectStatus().isOk
            .expectBody<AssistantMessage>()
            .consumeWith { response ->
                val assistantMessage = response.responseBody
                assertNotNull(assistantMessage)
                assertTrue(assistantMessage?.content?.isNotBlank() == true)
            }
    }

    private fun mockAgentCall(agentAddress: Address) {
        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()
        coEvery { agentClientService.createGraphQlAgentClient(agentAddress) } returns mockGraphQlAgentClient
        coEvery { mockGraphQlAgentClient.callAgent(any<AgentRequest>()) } returns
                flow {
                    emit(
                        AgentResult(
                            messages = listOf(
                                Message(
                                    role = "assistant",
                                    content = "Dummy response from Agent"
                                )
                            )
                        )
                    )
                }
    }

    private fun createConversation(agent: String) = Conversation(
        systemContext = SystemContext(channelId = "web"),
        userContext = UserContext(userId = "user-id", userToken = "user-token"),
        inputContext = InputContext(
            messages = listOf(Message(role = "user", content = "Hello")),
            explicitAgent = agent
        )
    )

    @TestConfiguration
    open class TestConfig {

        @Bean
        open fun agentClientService() = spyk<ArcAgentClientService>()

    }

}