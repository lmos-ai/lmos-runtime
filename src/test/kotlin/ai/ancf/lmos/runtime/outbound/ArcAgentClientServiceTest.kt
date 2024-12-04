/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.outbound

import ai.ancf.lmos.arc.agent.client.graphql.GraphQlAgentClient
import ai.ancf.lmos.arc.api.AgentResult
import ai.ancf.lmos.arc.api.Message
import ai.ancf.lmos.runtime.core.constants.ApiConstants
import ai.ancf.lmos.runtime.core.model.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class ArcAgentClientServiceTest {
    @Test
    fun `test askAgent method`() {
        // Mocking necessary dependencies
        val conversation =
            Conversation(
                InputContext(emptyList()),
                SystemContext("testChannelId"),
                UserContext("testUserId", "testUserToken"),
            )
        val conversationId = "testConversationId"
        val turnId = "testTurnId"
        val agentName = "testAgentName"
        val agentAddress = Address("http", "test-uri")
        val subset = "test-subset"

        val graphQlAgentClient = mockk<GraphQlAgentClient>()
        coEvery {
            graphQlAgentClient.callAgent(
                agentRequest = any(),
                requestHeaders = mapOf(ApiConstants.Headers.SUBSET to subset),
            )
        } returns
            flow {
                emit(
                    AgentResult(
                        messages =
                            listOf(
                                Message(role = "assistant", content = "Agent Response"),
                            ),
                    ),
                )
            }

        val service = spyk(ArcAgentClientService())
        every { service.createGraphQlAgentClient(any()) } returns graphQlAgentClient

        // Call the method under test
        val result =
            runBlocking {
                service.askAgent(conversation, conversationId, turnId, agentName, agentAddress, subset)
            }

        // Assertions
        assertEquals("Agent Response", result.content)
    }

    @Test
    fun `test askAgent with createGraphQlAgentClient throwing exception`() {
        // Mock the Address object
        val mockAddress = Address(uri = "mockUri")

        // Create a spy of the ArcAgentClientService
        val arcAgentClientService = spyk(ArcAgentClientService())

        // Mock the createGraphQlAgentClient function to throw an exception
        every { arcAgentClientService.createGraphQlAgentClient(any()) } throws Exception("Agent Client exception")

        // Create a sample Conversation object
        val conversation =
            Conversation(
                inputContext = InputContext(messages = emptyList()),
                systemContext = SystemContext(channelId = "mockChannelId"),
                userContext = UserContext(userId = "mockUserId", userToken = "mockUserToken"),
            )

        // Perform the test
        assertThrows(Exception::class.java) {
            runBlocking {
                arcAgentClientService.askAgent(
                    conversation,
                    "mockConversationId",
                    "mockTurnId",
                    "mockAgentName",
                    mockAddress,
                    "test-subset",
                )
            }
        }
    }
}
