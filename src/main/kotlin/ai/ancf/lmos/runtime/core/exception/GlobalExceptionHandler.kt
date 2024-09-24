/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.exception

import ai.ancf.lmos.router.core.AgentRoutingSpecResolverException
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log: Logger = org.slf4j.LoggerFactory.getLogger(this.javaClass)

    @ExceptionHandler(NoRoutingInfoFoundException::class, AgentRoutingSpecResolverException::class)
    fun handleIllegalArgumentException(
        exchange: ServerWebExchange,
        ex: Exception,
    ): ResponseEntity<ErrorMessage> {
        log.error("${ex.message}, ${ex.printStackTrace()}")
        return ResponseEntity(
            ErrorMessage(
                "AGENT_NOT_FOUND",
                ex.message,
            ),
            HttpStatus.NOT_FOUND,
        )
    }

    @ExceptionHandler(AgentClientException::class)
    fun handleAgentClientException(
        exchange: ServerWebExchange,
        ex: Exception,
    ): ResponseEntity<ErrorMessage> {
        log.error("${ex.message}, ${ex.printStackTrace()}")
        return ResponseEntity(
            ErrorMessage(
                "AGENT_CLIENT_EXCEPTION",
                ex.message,
            ),
            HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        exchange: ServerWebExchange,
        ex: IllegalArgumentException,
    ): ResponseEntity<ErrorMessage> {
        log.error("INVALID_REQUEST Illegal Argument: ${ex.message}, ${ex.printStackTrace()}")
        return ResponseEntity(
            ErrorMessage(
                "INVALID_REQUEST",
                ex.message,
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleInvalidInputException(
        exchange: ServerWebExchange,
        ex: ServerWebInputException,
    ): ResponseEntity<ErrorMessage> {
        val errorMessage: String =
            if (ex.cause != null) {
                log.error(ex.cause?.localizedMessage?.toString(), ex.printStackTrace())
                ex.cause?.toString()?.split(':')?.get(2).toString()
            } else {
                log.error("INVALID_REQUEST: ${ex.message}, ${ex.printStackTrace()}")
                ex.message ?: ""
            }
        return ResponseEntity(
            ErrorMessage(
                "INVALID_REQUEST",
                errorMessage,
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleHttpMessageNotReadableException(
        exchange: ServerWebExchange,
        ex: MethodArgumentNotValidException,
    ): ResponseEntity<ErrorMessage> {
        log.error("INVALID_REQUEST. Field errors: ${ex.message}, ${ex.printStackTrace()}")
        val bindingResult = ex.bindingResult
        val fieldErrors = bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return ResponseEntity(
            ErrorMessage(
                "INVALID_REQUEST",
                "Invalid request. Field errors: $fieldErrors",
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        exchange: ServerWebExchange,
        ex: HttpMessageNotReadableException,
    ): ResponseEntity<ErrorMessage> {
        log.error("INVALID_REQUEST, MessageNotReadable: ${ex.message}, , ${ex.printStackTrace()}")
        return ResponseEntity(
            ErrorMessage(
                "INVALID_REQUEST",
                ex.message,
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(
        exchange: ServerWebExchange,
        ex: Exception,
    ): ResponseEntity<ErrorMessage> {
        log.error("Exception occurred in request: ${ex.message}, ${ex.printStackTrace()}")
        return ResponseEntity(
            ErrorMessage(
                "INTERNAL_SERVER_ERROR",
                ex.message,
            ),
            HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }
}
