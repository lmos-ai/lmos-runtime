/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.outbound

import ai.ancf.lmos.router.core.*
import ai.ancf.lmos.router.llm.DefaultModelClient
import ai.ancf.lmos.router.llm.DefaultModelClientProperties
import ai.ancf.lmos.router.llm.LLMAgentRoutingSpecsResolver
import ai.ancf.lmos.runtime.core.model.Agent
import ai.ancf.lmos.runtime.core.model.AgentBuilder
import ai.ancf.lmos.runtime.core.model.AgentCapability
import ai.ancf.lmos.runtime.core.model.Conversation
import ai.ancf.lmos.runtime.core.properties.LmosRuntimeProperties
import ai.ancf.lmos.runtime.core.service.outbound.AgentRoutingService
import org.slf4j.LoggerFactory

class LmosAgentRoutingService(private val lmosRuntimeProperties: LmosRuntimeProperties) : AgentRoutingService {
    private val log = LoggerFactory.getLogger(LmosAgentRoutingService::class.java)

    override suspend fun resolveAgentForConversation(
        conversation: Conversation,
        agentList: List<Agent>,
    ): Agent {
        val agentRoutingSpecResolver = initializeAgentRouter(agentList)
        val (context, input) = prepareConversationComponents(conversation)

        val agentRoutingSpec =
            resolveAgent(agentRoutingSpecResolver, context, input)
        log.info("Resolved agent: $agentRoutingSpec")
        return agentRoutingSpec?.toAgent() ?: throw AgentRoutingSpecResolverException("No agent resolved for user query")
    }

    fun resolveAgent(
        agentRoutingSpecResolver: AgentRoutingSpecsResolver,
        context: Context,
        input: UserMessage,
    ): AgentRoutingSpec? {
        val agentRoutingSpec =
            agentRoutingSpecResolver.resolve(context, input).getOrThrow()
        return agentRoutingSpec
    }

    fun initializeAgentRouter(agentList: List<Agent>): AgentRoutingSpecsResolver {
        val routingSpecProvider = routingSpecProvider(agentList.toAgentRoutingSpec())
        val agentRoutingSpecResolver = agentRoutingSpecResolver(routingSpecProvider)
        return agentRoutingSpecResolver
    }

    private fun routingSpecProvider(agentRoutingSpec: List<AgentRoutingSpec>): SimpleAgentRoutingSpecProvider {
        return SimpleAgentRoutingSpecProvider().apply {
            agentRoutingSpec.forEach { add(it) }
        }
    }

    private fun agentRoutingSpecResolver(agentRoutingSpecsProvider: AgentRoutingSpecsProvider): AgentRoutingSpecsResolver {
        val openAIConfig = lmosRuntimeProperties.openAi ?: throw IllegalArgumentException("openAI configuration key is null")
        val defaultModelClientProperties =
            DefaultModelClientProperties(
                openAiUrl = openAIConfig.url,
                openAiApiKey = openAIConfig.key,
                model = openAIConfig.model,
                maxTokens = openAIConfig.maxTokens,
                temperature = openAIConfig.temperature,
                format = openAIConfig.format,
            )
        return LLMAgentRoutingSpecsResolver(
            agentRoutingSpecsProvider,
            modelClient =
                DefaultModelClient(
                    defaultModelClientProperties,
                ),
        )
    }

    private fun prepareConversationComponents(conversation: Conversation): Pair<Context, UserMessage> {
        val userMessage = conversation.inputContext.messages.last().content
        val conversationHistory =
            conversation.inputContext.messages.dropLast(1).map {
                when (it.role) {
                    "system" -> SystemMessage(it.content)
                    "user" -> UserMessage(it.content)
                    "assistant" -> AssistantMessage(it.content)
                    else -> throw IllegalArgumentException("Unsupported role: ${it.role}")
                }
            }

        val context = Context(conversationHistory)
        val input = UserMessage(userMessage)
        return Pair(context, input)
    }
}

fun List<Agent>.toAgentRoutingSpec(): List<AgentRoutingSpec> {
    return this.map { agent ->
        AgentRoutingSpecBuilder().name(agent.name).version(agent.version).description(agent.description).apply {
            agent.capabilities.map { agentCapability ->
                addCapability(
                    Capability(
                        agentCapability.name,
                        agentCapability.description,
                        agent.version,
                    ),
                )
            }
            agent.addresses.map { address ->
                address(Address(address.protocol, address.uri))
            }
        }.build()
    }
}

fun AgentRoutingSpec.toAgent(): Agent =
    AgentBuilder()
        .name(name)
        .description(description)
        .version(version)
        .addresses(addresses.map { address -> ai.ancf.lmos.runtime.core.model.Address(address.protocol, address.uri) }.toSet())
        .apply {
            capabilities(
                capabilities.map { capability ->
                    AgentCapability(capability.name, capability.version, capability.description)
                },
            )
        }
        .build()
