/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.cache

import java.util.concurrent.ConcurrentHashMap

interface LmosRuntimeTenantAwareCache<V : Any> {
    fun save(
        tenantId: String,
        prefix: String,
        key: String,
        value: V,
    )

    fun save(
        tenantId: String,
        prefix: String,
        key: String,
        value: V,
        timeout: Long,
    )

    fun get(
        tenantId: String,
        prefix: String,
        key: String,
    ): V?

    fun delete(
        tenantId: String,
        prefix: String,
        key: String,
    )
}

class TenantAwareInMemoryCache<V : Any> : LmosRuntimeTenantAwareCache<V> {
    private val cache: ConcurrentHashMap<String, V> = ConcurrentHashMap()

    override fun save(
        tenantId: String,
        prefix: String,
        key: String,
        value: V,
    ) {
        val combinedKey = String.format("%s:%s:%s", tenantId, prefix, key)
        cache[combinedKey] = value
    }

    override fun save(
        tenantId: String,
        prefix: String,
        key: String,
        value: V,
        timeout: Long,
    ) {
        val combinedKey = String.format("%s:%s:%s", tenantId, prefix, key)
        cache[combinedKey] = value
    }

    override fun get(
        tenantId: String,
        prefix: String,
        key: String,
    ): V? {
        val combinedKey = String.format("%s:%s:%s", tenantId, prefix, key)
        return cache[combinedKey]
    }

    override fun delete(
        tenantId: String,
        prefix: String,
        key: String,
    ) {
        val combinedKey = String.format("%s:%s:%s", tenantId, prefix, key)
        if (cache.containsKey(combinedKey)) {
            cache.remove(combinedKey)
        } else {
            throw IllegalArgumentException()
        }
    }
}
