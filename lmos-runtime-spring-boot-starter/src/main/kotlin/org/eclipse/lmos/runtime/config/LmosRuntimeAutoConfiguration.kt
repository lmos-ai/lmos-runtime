/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.config

import org.eclipse.lmos.runtime.core.cache.LmosRuntimeTenantAwareCache
import org.eclipse.lmos.runtime.core.cache.TenantAwareInMemoryCache
import org.eclipse.lmos.runtime.core.inbound.ConversationHandler
import org.eclipse.lmos.runtime.core.inbound.DefaultConversationHandler
import org.eclipse.lmos.runtime.core.service.outbound.AgentClientService
import org.eclipse.lmos.runtime.core.service.outbound.AgentRegistryService
import org.eclipse.lmos.runtime.core.service.outbound.AgentRoutingService
import org.eclipse.lmos.runtime.core.service.routing.ExplicitAgentRoutingService
import org.eclipse.lmos.runtime.outbound.ArcAgentClientService
import org.eclipse.lmos.runtime.outbound.LmosAgentRoutingService
import org.eclipse.lmos.runtime.outbound.LmosOperatorAgentRegistry
import org.eclipse.lmos.runtime.outbound.RoutingInformation
import org.eclipse.lmos.runtime.properties.LmosRuntimeProperties
import org.eclipse.lmos.runtime.properties.Type
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
    open fun <V : Any> lmosRuntimeTenantAwareCache(): LmosRuntimeTenantAwareCache<V> {
        return TenantAwareInMemoryCache()
    }

    @Bean
    @ConditionalOnMissingBean(AgentRoutingService::class)
    open fun agentRoutingService(): AgentRoutingService {
        return when (lmosRuntimeProperties.router.type) {
            Type.EXPLICIT -> ExplicitAgentRoutingService()
            Type.LLM -> {
                lmosRuntimeProperties.openAi?.key
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("openAI configuration key is null or empty")
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
        lmosRuntimeProperties: LmosRuntimeProperties,
        lmosRuntimeTenantAwareCache: LmosRuntimeTenantAwareCache<RoutingInformation>,
    ): ConversationHandler {
        return DefaultConversationHandler(
            agentRegistryService,
            agentRoutingService,
            agentClientService,
            lmosRuntimeProperties,
            lmosRuntimeTenantAwareCache,
        )
    }
}
