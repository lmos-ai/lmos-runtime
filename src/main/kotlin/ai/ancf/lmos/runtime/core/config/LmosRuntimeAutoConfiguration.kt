/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.config

import ai.ancf.lmos.runtime.core.inbound.ConversationExecutor
import ai.ancf.lmos.runtime.core.inbound.DefaultConversationExecutor
import ai.ancf.lmos.runtime.core.properties.LmosRuntimeProperties
import ai.ancf.lmos.runtime.core.service.ExplicitAgentRoutingService
import ai.ancf.lmos.runtime.core.service.cache.LmosRuntimeTenantAwareCache
import ai.ancf.lmos.runtime.core.service.cache.TenantAwareInMemoryCache
import ai.ancf.lmos.runtime.core.service.inbound.ConversationService
import ai.ancf.lmos.runtime.core.service.inbound.DefaultConversationService
import ai.ancf.lmos.runtime.core.service.outbound.AgentClientService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRegistryService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRoutingService
import ai.ancf.lmos.runtime.outbound.ArcAgentClientService
import ai.ancf.lmos.runtime.outbound.LmosAgentRoutingService
import ai.ancf.lmos.runtime.outbound.LmosOperatorAgentRegistry
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
    open fun agentRegistryService(): AgentRegistryService {
        return LmosOperatorAgentRegistry(lmosRuntimeProperties)
    }

    @Bean
    @ConditionalOnMissingBean(LmosRuntimeTenantAwareCache::class)
    open fun lmosRuntimeTenantAwareCache(): LmosRuntimeTenantAwareCache<String> {
        return TenantAwareInMemoryCache()
    }

    @Bean
    @ConditionalOnMissingBean(AgentRoutingService::class)
    open fun agentRoutingService(): AgentRoutingService {
        return when (lmosRuntimeProperties.router.type) {
            LmosRuntimeProperties.RouterType.EXPLICIT -> ExplicitAgentRoutingService()
            LmosRuntimeProperties.RouterType.LLM -> LmosAgentRoutingService(lmosRuntimeProperties)
        }
    }

    @Bean
    @ConditionalOnMissingBean(ConversationExecutor::class)
    open fun conversationExecutor(agentConversationService: ConversationService): ConversationExecutor {
        return DefaultConversationExecutor(agentConversationService)
    }

    @Bean
    @ConditionalOnMissingBean(ConversationService::class)
    open fun conversationService(
        agentRegistryService: AgentRegistryService,
        agentRoutingService: AgentRoutingService,
        agentClientService: AgentClientService,
        lmosRuntimeTenantAwareCache: LmosRuntimeTenantAwareCache<String>,
    ): ConversationService {
        return DefaultConversationService(agentRegistryService, agentRoutingService, agentClientService, lmosRuntimeTenantAwareCache)
    }
}
