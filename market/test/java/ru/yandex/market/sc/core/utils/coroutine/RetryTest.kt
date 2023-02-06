package ru.yandex.market.sc.core.utils.coroutine

import com.google.common.truth.Truth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test

class RetryTest {

    @Test
    fun `Retries block until success`() = runTest {
        withTimeout(timeMillis = 1000) {
            val result: String = retry {
                if (currentAttempt.number < 10) {
                    throw RuntimeException()
                } else {
                    "42"
                }
            }

            Truth.assertThat(result).isEqualTo("42")
        }
    }

    @Test
    fun `Stops retries when call stop method`() = runTest {
        withTimeout(timeMillis = 1000) {
            var timesExecuted = 0

            val error: Throwable? = try {
                retry {
                    onRetryAttempt {
                        if (number > 4) stopRetries()
                    }
                    timesExecuted++
                    throw RuntimeException()
                }
                null
            } catch (t: Throwable) {
                t
            }

            Truth.assertThat(timesExecuted).isEqualTo(5)
            Truth.assertThat(error).isInstanceOf(RuntimeException::class.java)
        }
    }

    @Test(expected = CancellationException::class)
    fun `Rethrows cancellation exception`() = runTest {
        withTimeout(timeMillis = 1000) {
            retry {
                throw CancellationException()
            }
        }
    }

    @Test
    fun `Executes onRetryAttempt on each retry attempt`() = runTest {
        withTimeout(timeMillis = 1000) {
            var timesCalled = 0
            try {
                retry {
                    onRetryAttempt {
                        timesCalled++
                        if (number >= 3) stopRetries()
                    }
                    throw RuntimeException()
                }
            } catch (t: Throwable) {
                // no-op
            }

            Truth.assertThat(timesCalled).isEqualTo(3)
        }
    }

    @Test
    fun `Not executes onRetryAttempt on initial attempt`() = runTest {
        withTimeout(timeMillis = 1000) {
            var timesCalled = 0
            try {
                retry {
                    onRetryAttempt {
                        timesCalled++
                    }
                    ""
                }
            } catch (t: Throwable) {
                // no-op
            }

            Truth.assertThat(timesCalled).isEqualTo(0)
        }
    }

    @Test
    fun `Increments attempt number`() = runTest {
        withTimeout(timeMillis = 1000) {
            val attempts = mutableListOf<RetryScope.Attempt>()
            try {
                retry {
                    onRetryAttempt {
                        if (number >= 4) stopRetries()
                    }
                    attempts.add(currentAttempt)
                    throw RuntimeException()
                }
            } catch (t: Throwable) {
                // no-op
            }

            Truth.assertThat(attempts.map { it.number }).containsExactly(0, 1, 2, 3)
        }
    }
}
