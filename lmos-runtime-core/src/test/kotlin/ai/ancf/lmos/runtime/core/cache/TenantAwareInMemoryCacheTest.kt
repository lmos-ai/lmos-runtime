/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */
package ai.ancf.lmos.runtime.core.cache

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TenantAwareInMemoryCacheTest {
    private lateinit var cache: TenantAwareInMemoryCache<String>

    @BeforeEach
    fun setUp() {
        cache = TenantAwareInMemoryCache()
    }

    @Test
    fun testSaveAndGet() {
        // Arrange
        val tenantId = "tenant1"
        val prefix = "prefix1"
        val key = "key1"
        val value = "value1"

        // Act
        cache.save(tenantId, prefix, key, value)
        val result = cache.get(tenantId, prefix, key)

        // Assert
        assertEquals(value, result)
    }

    @Test
    fun testSaveWithTimeoutAndGet() {
        // Arrange
        val tenantId = "tenant1"
        val prefix = "prefix1"
        val key = "key1"
        val value = "value1"
        val timeout = 1000L // Timeout parameter, although not used in implementation

        // Act
        cache.save(tenantId, prefix, key, value, timeout)
        val result = cache.get(tenantId, prefix, key)

        // Assert
        assertEquals(value, result)
    }

    @Test
    fun testGetNonExistentKey() {
        // Arrange
        val tenantId = "tenant1"
        val prefix = "prefix1"
        val key = "key1"

        // Act
        val result = cache.get(tenantId, prefix, key)

        // Assert
        assertNull(result)
    }

    @Test
    fun testDelete() {
        // Arrange
        val tenantId = "tenant1"
        val prefix = "prefix1"
        val key = "key1"
        val value = "value1"
        cache.save(tenantId, prefix, key, value)

        // Act
        cache.delete(tenantId, prefix, key)
        val result = cache.get(tenantId, prefix, key)

        // Assert
        assertNull(result)
    }

    @Test
    fun testDeleteNonExistentKey() {
        // Arrange
        val tenantId = "tenant1"
        val prefix = "prefix1"
        val key = "key1"

        // Act
        assertThrows<IllegalArgumentException> { cache.delete(tenantId, prefix, key) }
    }
}
