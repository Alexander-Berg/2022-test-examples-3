package ru.yandex.market.contentmapping.services.utility

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.getBean
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.contentmapping.utils.await5s
import java.time.Duration

class ThreadedTaskExecutorTest : BaseAppTestClass() {
    lateinit var threadedTaskExecutor: ThreadedTaskExecutor
    lateinit var call: () -> Boolean

    @Before
    fun setup() {
        threadedTaskExecutor = ThreadedTaskExecutor(
            context.getBean(),
            TaskConfig("test-task", delay = Duration.ofMillis(DELAY_MILLIS)),
            task = { call() })
    }

    @After
    fun cleanup() {
        threadedTaskExecutor.cleanup()
    }

    @Test
    fun `task should run`() {
        var runCount = 0
        call = { runCount += 1; true }
        threadedTaskExecutor.run()

        await5s().untilAsserted {
            runCount shouldBeGreaterThan 0
        }
    }

    @Test
    fun `should wait if task returned false (no more work)`() {
        var runCount = 0
        call = { runCount += 1; false }
        threadedTaskExecutor.run()

        await5s().untilAsserted {
            runCount shouldBeGreaterThan 0
        }

        val callsBefore = runCount
        Thread.sleep(50) // Check it's waiting
        runCount shouldBe callsBefore
    }

    @Test
    fun `shouldn't wait if task returned true (has more work)`() {
        var runCount = 0
        call = { runCount += 1; true }
        threadedTaskExecutor.run()

        await5s().untilAsserted {
            runCount shouldBeGreaterThan 0
        }

        val callsBefore = runCount
        await5s().untilAsserted {
            runCount shouldBeGreaterThan callsBefore
        }
    }

    @Test
    fun `should survive exceptions, even errors`() {
        var runCount = 0
        call = { runCount += 1; throw OutOfMemoryError("Cheated!") }
        threadedTaskExecutor.run()

        await5s().untilAsserted {
            runCount shouldBeGreaterThan 0
        }

        // Keeps running
        val callsBefore = runCount
        await5s().untilAsserted {
            runCount shouldBeGreaterThan callsBefore
        }
    }

    @Test
    fun `should interrupt if asked to`() {
        var runCount = 0
        call = { runCount += 1; throw InterruptedException("Enough of this") }
        threadedTaskExecutor.run()

        await5s().untilAsserted {
            runCount shouldBeGreaterThan 0
        }

        await5s().untilAsserted {
            threadedTaskExecutor.state shouldBe ThreadedTaskExecutor.State.INTERRUPTED
        }
    }

    companion object {
        const val DELAY_MILLIS = 200L
    }
}
