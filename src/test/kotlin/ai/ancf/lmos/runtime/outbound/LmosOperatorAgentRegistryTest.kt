/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.outbound

import ai.ancf.lmos.runtime.core.exception.NoRoutingInfoFoundException
import ai.ancf.lmos.runtime.core.properties.LmosRuntimeProperties
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LmosOperatorAgentRegistryTest {
    @Test
    fun test_get_agents_success() =
        runBlocking {
            val mockResponse =
                """
                {
                    "apiVersion": "v1",
                    "kind": "ChannelRouting",
                    "metadata": {
                        "creationTimestamp": "2023-01-01T00:00:00Z",
                        "generation": 1,
                        "labels": {
                            "channel": "test-channel",
                            "subset": "test-subset",
                            "tenant": "test-tenant",
                            "version": "1.0"
                        },
                        "name": "test-name",
                        "namespace": "test-namespace",
                        "resourceVersion": "1",
                        "uid": "12345"
                    },
                    "spec": {
                        "capabilityGroups": [
                            {
                                "name": "test-agent",
                                "description": "Test Agent",
                                "capabilities": [
                                    {
                                        "name": "test-capability",
                                        "requiredVersion": "1.0",
                                        "providedVersion": "1.0",
                                        "description": "Test Capability",
                                        "host": "http://localhost"
                                        "subset": "test-subset"
                                    }
                                ]
                            }
                        ]
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = mockResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client =
                HttpClient(mockEngine) {
                }

            val properties =
                LmosRuntimeProperties(
                    agentRegistry = LmosRuntimeProperties.AgentRegistry(baseUrl = "http://localhost"),
                    openAI = LmosRuntimeProperties.OpenAI(url = "http://localhost", key = "openaiKey", "", 1, 0.0, "json_model"),
                    router = LmosRuntimeProperties.Router(type = LmosRuntimeProperties.RouterType.LLM),
                )

            val registry = LmosOperatorAgentRegistry(properties)
            registry.client = client

            val routingInformation: RoutingInformation =
                registry.getRoutingInformation(
                    "test-tenant",
                    "test-channel",
                    "test-subset",
                )

            assertEquals(1, routingInformation.agentList.size)
            assertEquals("test-agent", routingInformation.agentList[0].name)
        }

    @Test
    fun `returns NoRoutingInfoFoundException when no agent found`(): Unit =
        runBlocking {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = "mockResponse",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client =
                HttpClient(mockEngine) {
                }

            val properties =
                LmosRuntimeProperties(
                    agentRegistry = LmosRuntimeProperties.AgentRegistry(baseUrl = "http://localhost"),
                    openAI = LmosRuntimeProperties.OpenAI("http://localhost", key = "openaiKey", "", 1, 0.0, "json_model"),
                    router = LmosRuntimeProperties.Router(type = LmosRuntimeProperties.RouterType.LLM),
                )

            val registry = LmosOperatorAgentRegistry(properties)
            registry.client = client

            assertThrows(NoRoutingInfoFoundException::class.java) {
                runBlocking { registry.getRoutingInformation("test-tenant", "test-channel", "test-subset") }
            }
        }
}
