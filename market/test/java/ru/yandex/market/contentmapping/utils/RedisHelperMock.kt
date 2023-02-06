package ru.yandex.market.contentmapping.utils

import org.mockito.Mockito
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool
import ru.yandex.market.contentmapping.dto.rules.RulesWithVersion
import ru.yandex.market.contentmapping.kotlin.typealiases.ShopId
import java.util.Optional

open class RedisHelperMock : RedisHelper(Mockito.mock(JedisSentinelPool::class.java)) {
    private val cache: HashMap<Long, RulesWithVersion> = HashMap()
    private val cachedVersions: HashMap<Long, Long> = HashMap()

    override fun get(key: Long): RulesWithVersion {
        return cache[key] ?: RulesWithVersion.noLoaded(key)
    }

    override fun getWithClient(key: Long, client: Jedis): RulesWithVersion {
        return get(key)
    }

    override fun getVersionWithClient(shopId: ShopId, client: Jedis): Long {
        return getVersion(shopId)
    }

    override fun setWithClient(key: Long, value: RulesWithVersion, client: Jedis) {
        set(key, value)
    }

    override fun <T : Any> withLock(key: Long, action: (client: Jedis) -> T): Optional<T> {
        return Optional.of(action(Mockito.mock(Jedis::class.java)))
    }

    override fun getVersion(shopId: ShopId): Long {
        return cachedVersions[shopId] ?: -1
    }

    override fun set(key: Long, value: RulesWithVersion) {
        cache[key] = value
        cachedVersions[key] = value.version
    }

    override fun invalidate(key: Long?) {
        if (key == null) {
            cache.clear()
            cachedVersions.clear()
        } else {
            cache.remove(key)
            cachedVersions.remove(key)
        }
    }
}
