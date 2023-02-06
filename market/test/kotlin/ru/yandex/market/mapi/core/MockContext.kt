package ru.yandex.market.mapi.core

import org.mockito.Mockito
import org.mockito.kotlin.mock
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.01.2022
 */
object MockContext {
    val MOCKS = ConcurrentHashMap<Any, Runnable>()

    fun resetMocks() {
        for ((key, value) in MOCKS) {
            Mockito.reset(key)
            value.run()
        }
    }

    inline fun <reified T : Any> registerMock(initializer: Consumer<T>? = null): T {
        val result = mock<T>()
        initializer?.accept(result)
        MOCKS[result] = Runnable { initializer?.accept(result) }
        return result
    }
}