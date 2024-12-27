/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.lmos.runtime.core.outbound

import io.mockk.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agent.client.graphql.GraphQlAgentClient
import org.eclipse.lmos.arc.api.AgentResult
import org.eclipse.lmos.arc.api.Message
import org.eclipse.lmos.runtime.core.model.Address
import org.eclipse.lmos.runtime.core.model.AssistantMessage
import org.eclipse.lmos.runtime.core.model.Conversation
import org.eclipse.lmos.runtime.core.model.InputContext
import org.eclipse.lmos.runtime.core.model.SystemContext
import org.eclipse.lmos.runtime.core.model.UserContext
import org.eclipse.lmos.runtime.outbound.ArcAgentClientService
import kotlin.test.Test
import kotlin.test.assertEquals

class ArcAgentClientServiceTest {
    @Test
    fun test_ask_agent_returns_assistant_message_on_successful_response() =
        runBlocking {
            val mockGraphQlAgentClient = mockk<GraphQlAgentClient>()
            val service = spyk<ArcAgentClientService>()
            val conversation =
                Conversation(
                    inputContext = InputContext(messages = listOf(Message("user", "Hello"))),
                    systemContext = SystemContext(channelId = "testChannel"),
                    userContext = UserContext(userId = "user123", userToken = "token123"),
                )
            val address = Address(uri = "localhost")

            coEvery { service.createGraphQlAgentClient(address) } returns mockGraphQlAgentClient
            coEvery { mockGraphQlAgentClient.close() } just runs
            coEvery { mockGraphQlAgentClient.callAgent(any()) } returns
                flow {
                    emit(
                        AgentResult(
                            messages =
                                listOf(
                                    Message(role = "assistant", content = "Response from agent"),
                                ),
                        ),
                    )
                }

            val result: AssistantMessage =
                service.askAgent(
                    conversation,
                    "conversationId",
                    "turnId",
                    "agentName",
                    address,
                    null,
                )

            assertEquals("Response from agent", result.content)
        }
}
