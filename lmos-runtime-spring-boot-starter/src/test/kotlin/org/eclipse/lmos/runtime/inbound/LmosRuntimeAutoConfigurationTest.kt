/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.inbound

import org.eclipse.lmos.runtime.config.LmosRuntimeAutoConfiguration
import org.eclipse.lmos.runtime.core.cache.LmosRuntimeTenantAwareCache
import org.eclipse.lmos.runtime.core.cache.TenantAwareInMemoryCache
import org.eclipse.lmos.runtime.core.inbound.ConversationHandler
import org.eclipse.lmos.runtime.core.inbound.DefaultConversationHandler
import org.eclipse.lmos.runtime.core.service.outbound.AgentClientService
import org.eclipse.lmos.runtime.core.service.outbound.AgentRoutingService
import org.eclipse.lmos.runtime.core.service.routing.ExplicitAgentRoutingService
import org.eclipse.lmos.runtime.outbound.ArcAgentClientService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [LmosRuntimeAutoConfiguration::class])
@ActiveProfiles("test")
class LmosRuntimeAutoConfigurationTest {
    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Test
    fun `should load ArcAgentClientService as AgentClientService`() {
        val agentClientService = applicationContext.getBean(AgentClientService::class.java)
        assertTrue(agentClientService is ArcAgentClientService)
    }

    @Test
    fun `should load ExplicitAgentRoutingService as AgentRoutingService`() {
        val agentRoutingService = applicationContext.getBean(AgentRoutingService::class.java)
        assertTrue(agentRoutingService is ExplicitAgentRoutingService)
    }

    @Test
    fun `should load DefaultConversationHandler as ConversationHandler`() {
        val conversationHandler = applicationContext.getBean(ConversationHandler::class.java)
        assertTrue(conversationHandler is DefaultConversationHandler)
    }

    @Test
    fun `should load TenantAwareInMemoryCache as LmosRuntimeTenantAwareCache`() {
        val cache = applicationContext.getBean(LmosRuntimeTenantAwareCache::class.java)
        assertTrue(cache is TenantAwareInMemoryCache)
    }
}
