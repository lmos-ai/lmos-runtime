/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.core.config

import org.eclipse.lmos.runtime.core.inbound.ConversationHandler
import org.eclipse.lmos.runtime.core.inbound.DefaultConversationHandler
import org.eclipse.lmos.runtime.core.properties.LmosRuntimeProperties
import org.eclipse.lmos.runtime.core.service.ExplicitAgentRoutingService
import org.eclipse.lmos.runtime.core.service.cache.LmosRuntimeTenantAwareCache
import org.eclipse.lmos.runtime.core.service.cache.TenantAwareInMemoryCache
import org.eclipse.lmos.runtime.core.service.inbound.ConversationService
import org.eclipse.lmos.runtime.core.service.inbound.DefaultConversationService
import org.eclipse.lmos.runtime.core.service.outbound.AgentClientService
import org.eclipse.lmos.runtime.core.service.outbound.AgentRegistryService
import org.eclipse.lmos.runtime.core.service.outbound.AgentRoutingService
import org.eclipse.lmos.runtime.outbound.ArcAgentClientService
import org.eclipse.lmos.runtime.outbound.LmosAgentRoutingService
import org.eclipse.lmos.runtime.outbound.LmosOperatorAgentRegistry
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
            LmosRuntimeProperties.RouterType.LLM -> {
                lmosRuntimeProperties.openAi ?: throw IllegalArgumentException("openAI configuration key is null")
                LmosAgentRoutingService(lmosRuntimeProperties)
            }
        }
    }

    @Bean
    @ConditionalOnMissingBean(ConversationHandler::class)
    open fun conversationExecutor(agentConversationService: ConversationService): ConversationHandler {
        return DefaultConversationHandler(agentConversationService)
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
