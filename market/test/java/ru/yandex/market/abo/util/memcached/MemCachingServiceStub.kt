package ru.yandex.market.abo.util.memcached

import ru.yandex.common.cache.memcached.MemCachingService
import ru.yandex.common.cache.memcached.cacheable.BulkMemCacheable
import ru.yandex.common.cache.memcached.cacheable.MemCacheable

/**
 * @author komarovns
 */
open class MemCachingServiceStub : MemCachingService {
    override fun <T : Any?, Q : Any?> query(
        memCacheable: MemCacheable<T, Q>,
        query: Q
    ): T? = memCacheable.queryNonCached(query)

    override fun <T : Any?, Q : Any?> queryBulk(
        bulkMemCacheable: BulkMemCacheable<T, Q>?,
        queries: MutableCollection<Q>?
    ): MutableMap<Q, T> = HashMap()

    override fun <T : Any?, Q : Any?> clean(memCacheable: MemCacheable<T, Q>?, query: Q) {
    }

    override fun <T : Any?, Q : Any?> cache(memCacheable: MemCacheable<T, Q>?, query: Q, result: T) {
    }
}
