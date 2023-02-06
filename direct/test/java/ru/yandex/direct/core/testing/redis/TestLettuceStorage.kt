package ru.yandex.direct.core.testing.redis

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider
import ru.yandex.direct.core.redis.LettuceStorage

/**
 * Реализация Redis'а для тестов, можно использовать, если мокать неудобно.
 */
class TestLettuceStorage(connectionProvider: LettuceConnectionProvider) : LettuceStorage(connectionProvider) {

    private val storage: MutableMap<String, Any> = mutableMapOf()

    override fun incrementAndGet(key: String): Long {
        val oldValue = (storage[key] ?: 0L) as Long
        val newValue = oldValue + 1

        storage[key] = newValue

        return newValue
    }

    override fun expire(key: String, ttl: Long): Boolean {
        return true
    }
}
