package ru.yandex.market.tpl.courier.arch.coroutine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeExactly
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test

class CoroutinesKtTest {

    @Test
    fun `Перезапускает блок пока не будет вызван reject у RetryAttempt`() = runBlockingTest {
        var invoked = 0
        val maxAttemptsCount = 5
        val work = { retryAttempt: RetryAttempt? ->
            if (retryAttempt != null) {
                if (retryAttempt.number >= maxAttemptsCount) {
                    retryAttempt.reject()
                }
            }
            invoked++
            throw RuntimeException()
        }

        shouldThrow<RuntimeException> {
            retry { work(it) }
        }

        invoked shouldBeExactly maxAttemptsCount
    }
}