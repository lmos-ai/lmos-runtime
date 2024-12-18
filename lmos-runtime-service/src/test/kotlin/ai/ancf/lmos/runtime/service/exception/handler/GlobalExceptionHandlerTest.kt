/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */
package ai.ancf.lmos.runtime.service.exception.handler

import ai.ancf.lmos.runtime.core.exception.AgentClientException
import ai.ancf.lmos.runtime.core.exception.NoRoutingInfoFoundException
import io.mockk.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlobalExceptionHandlerTest {
    private val globalExceptionHandler = GlobalExceptionHandler()
    private val mockExchange: ServerWebExchange = mockk(relaxed = true)

    @Test
    fun `handleIllegalArgumentException for NoRoutingInfoFoundException`() {
        val exception = NoRoutingInfoFoundException("No routing info")

        val response = globalExceptionHandler.handleIllegalArgumentException(mockExchange, exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("AGENT_NOT_FOUND", response.body?.errorCode)
        assertEquals("No routing info", response.body?.message)
    }

    @Test
    fun `handleAgentClientException handles correctly`() {
        val exception = AgentClientException("Client error occurred")

        val response = globalExceptionHandler.handleAgentClientException(mockExchange, exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("AGENT_CLIENT_EXCEPTION", response.body?.errorCode)
        assertEquals("Client error occurred", response.body?.message)
    }

    @Test
    fun `handleInvalidInputException with cause`() {
        val rootCause = IllegalArgumentException("Invalid input details")
        val exception = ServerWebInputException("Input error", null, rootCause)

        val response = globalExceptionHandler.handleInvalidInputException(mockExchange, exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("INVALID_REQUEST", response.body?.errorCode)
    }

    @Test
    fun `handleMethodArgumentNotValidException collects field errors`() {
        val mockBindingResult: BindingResult =
            mockk {
                every { fieldErrors } returns
                    listOf(
                        FieldError("objectName", "field1", "error1"),
                        FieldError("objectName", "field2", "error2"),
                    )
            }

        val exception: MethodArgumentNotValidException =
            mockk {
                every { bindingResult } returns mockBindingResult
                every { message } returns "MethodArgumentNotValidException"
                every { printStackTrace() } just runs
            }

        val response = globalExceptionHandler.handleHttpMessageNotReadableException(mockExchange, exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("INVALID_REQUEST", response.body?.errorCode)
        assertTrue(response.body?.message?.contains("field1") == true)
        assertTrue(response.body?.message?.contains("field2") == true)
    }

    @Test
    fun `handleAllExceptions catches unexpected errors`() {
        val unexpectedException = RuntimeException("Unexpected error")

        val response = globalExceptionHandler.handleAllExceptions(mockExchange, unexpectedException)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("INTERNAL_SERVER_ERROR", response.body?.errorCode)
        assertEquals("Unexpected error", response.body?.message)
    }
}
