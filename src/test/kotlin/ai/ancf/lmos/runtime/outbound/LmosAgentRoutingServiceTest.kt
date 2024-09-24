/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.outbound

import ai.ancf.lmos.arc.api.Message
import ai.ancf.lmos.router.core.*
import ai.ancf.lmos.router.core.Capability
import ai.ancf.lmos.runtime.core.model.*
import ai.ancf.lmos.runtime.core.model.Address
import ai.ancf.lmos.runtime.core.properties.LmosRuntimeProperties
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LmosAgentRoutingServiceTest {
    private lateinit var lmosAgentRoutingService: LmosAgentRoutingService
    private lateinit var agentRoutingSpecsResolver: AgentRoutingSpecsResolver
    private lateinit var conversation: Conversation
    private lateinit var agentList: List<Agent>

    @BeforeEach
    fun setup() {
        clearAllMocks()

        val mockProperties = mockk<LmosRuntimeProperties>()
        lmosAgentRoutingService = spyk(LmosAgentRoutingService(mockProperties))
        agentRoutingSpecsResolver = mockk()
        every { lmosAgentRoutingService.initializeAgentRouter(any()) } returns agentRoutingSpecsResolver

        conversation =
            Conversation(
                inputContext =
                    InputContext(
                        messages =
                            listOf(
                                Message(role = "user", content = "Hello"),
                                Message(role = "assistant", content = "Hi there!"),
                            ),
                    ),
                systemContext = SystemContext(channelId = "testChannel"),
                userContext = UserContext(userId = "testUser", userToken = "testToken"),
            )

        agentList =
            listOf(
                AgentBuilder().name("agent1").description("agent description").version("1.0").build(),
            )
    }

    @Test
    fun `resolveAgentForConversation should return correct agent with valid input`() =
        runBlocking {
            val capability1 = ai.ancf.lmos.router.core.Capability("capability1", "desc1", "1.0")
            val capability2 = ai.ancf.lmos.router.core.Capability("capability2", "desc2", "1.1")
            val address1 = ai.ancf.lmos.router.core.Address("http", "http://address1")

            val agentSpec =
                AgentRoutingSpecBuilder()
                    .name("agent1")
                    .description("agent description")
                    .version("1.0")
                    .address(address1)
                    .addCapability(capability1)
                    .addCapability(capability2)
                    .build()

            every { agentRoutingSpecsResolver.resolve(any(), any()) } returns Success(agentSpec)

            // Act
            val result = lmosAgentRoutingService.resolveAgentForConversation(conversation, agentList)

            // Assert
            assertEquals(result.name, agentList[0].name)
            assertEquals(result.description, agentList[0].description)
            assertEquals(result.version, agentList[0].version)

            // Verify
            verify(exactly = 1) { agentRoutingSpecsResolver.resolve(any(), any()) }
        }

    @Test
    fun test_resolve_agent_for_conversation_empty_agent_list() =
        runBlocking {
            every {
                agentRoutingSpecsResolver.resolve(
                    any(),
                    any(),
                )
            } returns Failure(AgentRoutingSpecResolverException("Failed to resolve agent spec"))

            assertThrows(AgentRoutingSpecResolverException::class.java) {
                runBlocking {
                    lmosAgentRoutingService.resolveAgentForConversation(conversation, agentList)
                }
            }

            // Verify the interactions
            verify { agentRoutingSpecsResolver.resolve(any(), any()) }
        }

    @Test
    fun converts_list_of_agents_to_agent_routing_specs() {
        val agent1 =
            Agent(
                name = "Agent1",
                version = "1.0",
                description = "First agent",
                capabilities = listOf(AgentCapability("cap1", "1.0", "Capability 1")),
                addresses = setOf(Address("http", "https://agent1.com")),
            )

        val agent2 =
            Agent(
                name = "Agent2",
                version = "2.0",
                description = "Second agent",
                capabilities = listOf(AgentCapability("cap2", "2.0", "Capability 2")),
                addresses = setOf(Address("https", "https://agent2.com")),
            )

        val agents = listOf(agent1, agent2)
        val agentRoutingSpecs = agents.toAgentRoutingSpec()

        assertEquals(2, agentRoutingSpecs.size)
        assertEquals("Agent1", agentRoutingSpecs[0].name)
        assertEquals("Agent2", agentRoutingSpecs[1].name)
    }

    @Test
    fun handles_empty_list_of_agents() {
        val agents = emptyList<Agent>()
        val agentRoutingSpecs = agents.toAgentRoutingSpec()

        assertTrue(agentRoutingSpecs.isEmpty())
    }

    @Test
    fun test_to_agent_with_all_fields_populated() {
        val capabilities =
            setOf(
                Capability("capability1", "1.0", "description1"),
                Capability("capability2", "2.0", "description2"),
            )
        val addresses =
            setOf(
                ai.ancf.lmos.router.core.Address("http", "example.com"),
                ai.ancf.lmos.router.core.Address("https", "example.com"),
            )
        val spec =
            AgentRoutingSpec(
                name = "AgentName",
                description = "AgentDescription",
                version = "1.0",
                capabilities = capabilities,
                addresses = addresses,
            )

        val agent = spec.toAgent()

        val agentCapabilitiesMap = agent.capabilities.associateBy { it.name }

        assertEquals("AgentName", agent.name)
        assertEquals("AgentDescription", agent.description)
        assertEquals("1.0", agent.version)
        assertEquals(capabilities.size, agent.capabilities.size)
        assertEquals(addresses.size, agent.addresses.size)
        assertTrue(
            capabilities.all { cap ->
                val key = cap.name
                agentCapabilitiesMap[key]?.let { agentCap ->
                    cap.description == agentCap.description
                } ?: false
            },
        )
    }
}
