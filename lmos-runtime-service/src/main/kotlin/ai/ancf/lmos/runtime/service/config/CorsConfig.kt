/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.service.config

import ai.ancf.lmos.runtime.service.properties.LmosRuntimeCorsProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
open class CorsConfig(private val lmosRuntimeCorsProperties: LmosRuntimeCorsProperties) {
    private val log = LoggerFactory.getLogger(CorsConfig::class.java)

    @Bean
    @ConditionalOnProperty(prefix = "lmos.runtime", name = ["cors.enabled"], havingValue = "true", matchIfMissing = false)
    open fun corsWebFilter(): CorsWebFilter {
        val corsConfig =
            CorsConfiguration().apply {
                allowedOrigins = lmosRuntimeCorsProperties.allowedOrigins
                maxAge = lmosRuntimeCorsProperties.maxAge
                allowedMethods = lmosRuntimeCorsProperties.allowedMethods
                allowedHeaders = lmosRuntimeCorsProperties.allowedHeaders
            }

        log.info(
            "CORS Configuration: Allowed Origins: {}, MaxAge: {}, Allowed Methods: {}, Allowed Headers: {}",
            corsConfig.allowedOrigins?.joinToString(", "),
            corsConfig.maxAge,
            corsConfig.allowedMethods?.joinToString(", "),
            corsConfig.allowedHeaders?.joinToString(", "),
        )

        val source =
            UrlBasedCorsConfigurationSource().apply {
                lmosRuntimeCorsProperties.patterns.forEach { pattern ->
                    registerCorsConfiguration(pattern, corsConfig)
                }
            }
        return CorsWebFilter(source)
    }
}
