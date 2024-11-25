package ai.ancf.lmos.runtime.service.inbound.controller

import ai.ancf.lmos.arc.api.Message
import ai.ancf.lmos.runtime.core.exception.ErrorMessage
import ai.ancf.lmos.runtime.core.exception.NoRoutingInfoFoundException
import ai.ancf.lmos.runtime.core.inbound.ConversationHandler
import ai.ancf.lmos.runtime.core.model.*
import ai.ancf.lmos.runtime.service.constants.LmosServiceConstants.Endpoints.BASE_PATH
import ai.ancf.lmos.runtime.service.constants.LmosServiceConstants.Endpoints.CHAT_URL
import ai.ancf.lmos.runtime.service.constants.LmosServiceConstants.Headers.TURN_ID
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@ExtendWith(SpringExtension::class)
@WebFluxTest(controllers = [ConversationController::class])
@Import(ConversationControllerTest.CustomBeanConfig::class)
class ConversationControllerTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var conversationHandler: ConversationHandler

    private lateinit var validConversation: Conversation
    private lateinit var assistantMessage: AssistantMessage

    @BeforeEach
    fun setup() {
        // Setup test data
        validConversation = Conversation(
            inputContext = InputContext(
                messages = listOf(Message("user", "Hello")),
                explicitAgent = "agent1"
            ),
            systemContext = SystemContext(channelId = "channel1"),
            userContext = UserContext(userId = "user1", userToken = "token1")
        )

        assistantMessage = AssistantMessage(
            content = "Test assistant response"
        )
    }

    @Test
    fun `chat endpoint returns successful response`(): Unit = runBlocking {
        val conversationId = "test-conversation-id"
        val tenantId = "test-tenant-id"
        val turnId = "test-turn-id"

        coEvery {
            conversationHandler.handleConversation(
                validConversation,
                conversationId,
                tenantId,
                turnId
            )
        } returns assistantMessage

            webClient.post()
            .uri("$BASE_PATH$CHAT_URL", tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .body(BodyInserters.fromValue(validConversation))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(objectMapper.writeValueAsString(assistantMessage))
    }

    @Test
    fun `chat endpoint handles empty conversation`(): Unit = runBlocking {
        val conversationId = "empty-conversation-id"
        val tenantId = "empty-tenant-id"
        val turnId = "empty-turn-id"

        val emptyConversation = Conversation(
            inputContext = InputContext(
                messages = listOf(),
            ),
            systemContext = SystemContext(channelId = "channel1"),
            userContext = UserContext(userId = "user1", userToken = "token1")
        )

        coEvery {
            conversationHandler.handleConversation(
                emptyConversation,
                conversationId,
                tenantId,
                turnId
            )
        } returns assistantMessage

        webClient.
            post()
            .uri("$BASE_PATH$CHAT_URL", tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .body(BodyInserters.fromValue(emptyConversation))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(objectMapper.writeValueAsString(assistantMessage))
    }

    @Test
    fun `chat endpoint handles missing turn id`(): Unit = runBlocking {
        val conversationId = "no-turn-id-conversation"
        val tenantId = "no-turn-id-tenant"

        webClient
            .post()
            .uri("$BASE_PATH$CHAT_URL", tenantId, conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(validConversation))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `chat endpoint handles invalid request body`(): Unit = runBlocking {
        val conversationId = "invalid-body-conversation"
        val tenantId = "invalid-body-tenant"
        val turnId = "invalid-turn-id"

        webClient
            .post()
            .uri("$BASE_PATH$CHAT_URL", tenantId, conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(TURN_ID, turnId)
            .body(BodyInserters.fromValue("{\"invalid\": \"data\"}"))
            .exchange()
            .expectStatus().isBadRequest

    }

    @Test
    fun `chat endpoint handles NoRoutingInfoFoundException`(): Unit = runBlocking {
        val conversationId = "invalid-body-conversation"
        val tenantId = "invalid-body-tenant"
        val turnId = "invalid-turn-id"

        coEvery {
            conversationHandler.handleConversation(
                validConversation,
                conversationId,
                tenantId,
                turnId
            )
        } throws NoRoutingInfoFoundException("No routing found")

        val errorMessage = ErrorMessage("AGENT_NOT_FOUND", "No routing found")

            webClient
            .post()
            .uri("$BASE_PATH$CHAT_URL", tenantId, conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TURN_ID, turnId)
            .body(BodyInserters.fromValue(validConversation))
            .exchange()
            .expectStatus().isNotFound
            .expectBody().json(objectMapper.writeValueAsString(errorMessage))

    }

    @TestConfiguration
    open class CustomBeanConfig {

        @Bean
        open fun conversationHandler(): ConversationHandler = mockk<ConversationHandler>()
    }

}