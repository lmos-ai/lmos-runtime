/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.lmos.runtime.test

import com.github.tomakehurst.wiremock.WireMockServer

abstract class BaseWireMockTest {
    companion object {
        protected lateinit var wireMockServer: WireMockServer

        @JvmStatic
        protected var mockPort: Int = 0

        @JvmStatic
        @org.junit.jupiter.api.BeforeAll
        fun setupWireMockServer() {
            wireMockServer = WireMockServer()
            wireMockServer.start()
            mockPort = wireMockServer.port()
        }

        @JvmStatic
        @org.junit.jupiter.api.AfterAll
        fun stopWireMockServer() {
            if (Companion::wireMockServer.isInitialized) {
                wireMockServer.stop()
            }
        }
    }
}
