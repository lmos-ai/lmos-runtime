/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */
package ai.ancf.lmos.runtime.inbound

import ai.ancf.lmos.runtime.config.LmosRuntimeAutoConfiguration
import ai.ancf.lmos.runtime.core.cache.LmosRuntimeTenantAwareCache
import ai.ancf.lmos.runtime.core.cache.TenantAwareInMemoryCache
import ai.ancf.lmos.runtime.core.inbound.ConversationHandler
import ai.ancf.lmos.runtime.core.inbound.DefaultConversationHandler
import ai.ancf.lmos.runtime.core.model.Address
import ai.ancf.lmos.runtime.core.model.Agent
import ai.ancf.lmos.runtime.core.model.AssistantMessage
import ai.ancf.lmos.runtime.core.model.Conversation
import ai.ancf.lmos.runtime.core.service.outbound.AgentClientService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRegistryService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRoutingService
import ai.ancf.lmos.runtime.outbound.ArcAgentClientService
import ai.ancf.lmos.runtime.outbound.LmosAgentRoutingService
import ai.ancf.lmos.runtime.outbound.RoutingInformation
import ai.ancf.lmos.runtime.properties.LmosRuntimeProperties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [LmosRuntimeAutoConfiguration::class])
@ActiveProfiles("test")
@TestPropertySource(properties = ["lmos.runtime.router.type=LLM", "lmos.runtime.openAi=dummyOpenAiKey"])
@Import(LmosRuntimeAutoConfigurationCustomBeansTest.CustomBeanConfig::class)
class LmosRuntimeAutoConfigurationCustomBeansTest {
    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var lmosRuntimeProperties: LmosRuntimeProperties

    @Test
    fun `should not load ArcAgentClientService as AgentClientService`() {
        val agentClientService = applicationContext.getBean(AgentClientService::class.java)
        assertFalse(agentClientService is ArcAgentClientService)
    }

    @Test
    fun `should not load LmosAgentRoutingService as AgentRoutingService`() {
        val agentRoutingService = applicationContext.getBean(AgentRoutingService::class.java)
        assertFalse(agentRoutingService is LmosAgentRoutingService)
    }

    @Test
    fun `should not load DefaultConversationHandler as ConversationService`() {
        val conversationService = applicationContext.getBean(ConversationHandler::class.java)
        assertFalse(conversationService is DefaultConversationHandler)
    }

    @Test
    fun `should not load TenantAwareInMemoryCache as LmosRuntimeTenantAwareCache`() {
        val cache = applicationContext.getBean(LmosRuntimeTenantAwareCache::class.java)
        assertFalse(cache is TenantAwareInMemoryCache)
    }

    @TestConfiguration
    open class CustomBeanConfig {
        @Bean
        open fun agentClientService(): AgentClientService =
            object : AgentClientService {
                override suspend fun askAgent(
                    conversation: Conversation,
                    conversationId: String,
                    turnId: String,
                    agentName: String,
                    agentAddress: Address,
                    subset: String?,
                ): AssistantMessage {
                    TODO("Not yet implemented")
                }
            }

        @Bean
        open fun agentRoutingService(): AgentRoutingService =
            object : AgentRoutingService {
                override suspend fun resolveAgentForConversation(
                    conversation: Conversation,
                    agentList: List<Agent>,
                ): Agent {
                    TODO("Not yet implemented")
                }
            }

        @Bean
        open fun agentRegistryService(): AgentRegistryService =
            object : AgentRegistryService {
                override suspend fun getRoutingInformation(
                    tenantId: String,
                    channelId: String,
                    subset: String?,
                ): RoutingInformation {
                    TODO("Not yet implemented")
                }
            }

        @Bean
        open fun lmosRuntimeTenantAwareCache(): LmosRuntimeTenantAwareCache<String> =
            object : LmosRuntimeTenantAwareCache<String> {
                override fun save(
                    tenantId: String,
                    prefix: String,
                    key: String,
                    value: String,
                ) {
                    TODO("Not yet implemented")
                }

                override fun save(
                    tenantId: String,
                    prefix: String,
                    key: String,
                    value: String,
                    timeout: Long,
                ) {
                    TODO("Not yet implemented")
                }

                override fun get(
                    tenantId: String,
                    prefix: String,
                    key: String,
                ): String? {
                    TODO("Not yet implemented")
                }

                override fun delete(
                    tenantId: String,
                    prefix: String,
                    key: String,
                ) {
                    TODO("Not yet implemented")
                }
            }

        @Bean
        open fun conversationService(): ConversationHandler =
            object : ConversationHandler {
                override suspend fun handleConversation(
                    conversation: Conversation,
                    conversationId: String,
                    tenantId: String,
                    turnId: String,
                ): AssistantMessage {
                    TODO("Not yet implemented")
                }
            }
    }
}
