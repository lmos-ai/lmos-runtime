package ai.ancf.lmos.runtime.core.outbound    // Handles successful agent response and returns an AssistantMessage

import ai.ancf.lmos.arc.agent.client.graphql.GraphQlAgentClient
import ai.ancf.lmos.arc.api.AgentResult
import ai.ancf.lmos.arc.api.Message
import ai.ancf.lmos.runtime.core.model.Address
import ai.ancf.lmos.runtime.core.model.AssistantMessage
import ai.ancf.lmos.runtime.core.model.Conversation
import ai.ancf.lmos.runtime.core.model.InputContext
import ai.ancf.lmos.runtime.core.model.SystemContext
import ai.ancf.lmos.runtime.core.model.UserContext
import ai.ancf.lmos.runtime.outbound.ArcAgentClientService
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Flow
import kotlin.test.Test
import kotlin.test.assertEquals

class ArcAgentClientServiceTest {

    @Test
    fun test_ask_agent_returns_assistant_message_on_successful_response() = runBlocking {
        val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()
        val service = spyk<ArcAgentClientService>()
        val conversation = Conversation(
            inputContext = InputContext(messages = listOf(Message("user", "Hello"))),
            systemContext = SystemContext(channelId = "testChannel"),
            userContext = UserContext(userId = "user123", userToken = "token123")
        )
        val address = Address(uri = "localhost")

        coEvery { service.createGraphQlAgentClient(address) } returns mockGraphQlAgentClient
        coEvery { mockGraphQlAgentClient.callAgent(any()) } returns flow {
                emit(
                    AgentResult(
                        messages =
                            listOf(
                                Message(role = "assistant", content = "Response from agent"),
                            ),
                    ),
                )
            }

        val result: AssistantMessage = service.askAgent(
            conversation,
            "conversationId",
            "turnId",
            "agentName",
            address,
            null
        )

        assertEquals("Response from agent", result.content)
    }
}