/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.config

import ai.ancf.lmos.runtime.core.cache.LmosRuntimeTenantAwareCache
import ai.ancf.lmos.runtime.core.cache.TenantAwareInMemoryCache
import ai.ancf.lmos.runtime.core.inbound.ConversationHandler
import ai.ancf.lmos.runtime.core.inbound.DefaultConversationHandler
import ai.ancf.lmos.runtime.core.service.outbound.AgentClientService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRegistryService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRoutingService
import ai.ancf.lmos.runtime.core.service.routing.ExplicitAgentRoutingService
import ai.ancf.lmos.runtime.outbound.ArcAgentClientService
import ai.ancf.lmos.runtime.outbound.LmosAgentRoutingService
import ai.ancf.lmos.runtime.outbound.LmosOperatorAgentRegistry
import ai.ancf.lmos.runtime.outbound.RoutingInformation
import ai.ancf.lmos.runtime.properties.LmosRuntimeProperties
import ai.ancf.lmos.runtime.properties.RouterType
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(LmosRuntimeProperties::class)
open class LmosRuntimeAutoConfiguration(
    private val lmosRuntimeProperties: LmosRuntimeProperties,
) {
    @Bean
    @ConditionalOnMissingBean(AgentClientService::class)
    open fun agentClientService(): AgentClientService {
        return ArcAgentClientService()
    }

    @Bean
    @ConditionalOnMissingBean(AgentRoutingService::class)
    open fun agentRegistryService(lmosRuntimeTenantAwareCache: LmosRuntimeTenantAwareCache<RoutingInformation>): AgentRegistryService {
        return LmosOperatorAgentRegistry(lmosRuntimeProperties, lmosRuntimeTenantAwareCache)
    }

    @Bean
    @ConditionalOnMissingBean(LmosRuntimeTenantAwareCache::class)
    open fun <V : Any> lmosRuntimeTenantAwareCache(): LmosRuntimeTenantAwareCache<V> {
        return TenantAwareInMemoryCache()
    }

    @Bean
    @ConditionalOnMissingBean(AgentRoutingService::class)
    open fun agentRoutingService(): AgentRoutingService {
        return when (lmosRuntimeProperties.router.type) {
            RouterType.EXPLICIT -> ExplicitAgentRoutingService()
            RouterType.LLM -> {
                lmosRuntimeProperties.openAi ?: throw IllegalArgumentException("openAI configuration key is null")
                LmosAgentRoutingService(lmosRuntimeProperties)
            }
        }
    }

    @Bean
    @ConditionalOnMissingBean(ConversationHandler::class)
    open fun conversationHandler(
        agentRegistryService: AgentRegistryService,
        agentRoutingService: AgentRoutingService,
        agentClientService: AgentClientService,
        lmosRuntimeTenantAwareCache: LmosRuntimeTenantAwareCache<String>,
    ): ConversationHandler {
        return DefaultConversationHandler(
            agentRegistryService,
            agentRoutingService,
            agentClientService,
            lmosRuntimeTenantAwareCache,
            lmosRuntimeProperties,
        )
    }
}
