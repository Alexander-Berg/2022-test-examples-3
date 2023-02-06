package ru.yandex.market.logistics.mqm.tms

import io.kotest.assertions.exceptionToMessage
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.Runnable
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.fail
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.quartz.JobExecutionContext
import ru.yandex.market.logistics.mqm.utils.tskvGetByKey
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import ru.yandex.market.tms.quartz2.model.Executor
import java.time.Duration
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
@DisplayName("Проверка на корректность работы базового экзекьютера.")
class ExecutorJobWrapperTest {

    @Mock
    private lateinit var context: JobExecutionContext

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    private val clock: MockClock = MockClock()
    private val registry: MeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, clock)
    private val workDuration = Duration.ofSeconds(3L)

    @Test
    @DisplayName("Проверка метрик при успешном выполнении.")
    fun jobExecutionSuccessTest() {
        val stubExecutor = StubExecutor(clock = clock)
        val executor = mockExecutorWrapper(registry = registry, stubExecutor = stubExecutor)
        val timerOk = registry.getTimerOk()
        val counterOk = registry.getCounterOk()
        val counterError = registry.getCounterError()

        counterError.count() shouldBe 0
        counterOk.count() shouldBe 0
        timerOk.count() shouldBe 0
        timerOk.totalTime(TimeUnit.SECONDS) shouldBe 0.0

        executor.doJob(context)

        counterError.count() shouldBe 0
        counterOk.count() shouldBe 1.0
        timerOk.count() shouldBe 1L
        timerOk.totalTime(TimeUnit.SECONDS) shouldBe workDuration.seconds.toDouble()

        backLogCaptor.results.size shouldBe 1
        val log = backLogCaptor.results[0]

        tskvGetByKey(log, CODE_KEY) shouldBe Pair(CODE_KEY, ExecutorJobWrapper.EXECUTOR_JOB_RESULT)
        tskvGetExtra(log).size shouldBe 1
        tskvGetExtra(log)[0] shouldBe Pair(EXECUTOR_KEY, StubExecutor::class.java.simpleName)
        tskvGetByKey(log, PAYLOAD_KEY) shouldBe Pair(PAYLOAD_KEY, ExecutorJobWrapper.JOB_OK_LOG_MESSAGE)
    }

    @Test
    @DisplayName("Проверка метрик при ошибке.")
    fun jobExecutionErrorTest() {
        val stubExecutor = StubExecutor(isSuccess = false, clock)
        val executor = ExecutorJobWrapper(stubExecutor, registry)

        val timerError = registry.getTimerError()
        val counterOk = registry.getCounterOk()
        val counterError = registry.getCounterError()

        counterError.count() shouldBe 0.0
        counterOk.count() shouldBe 0.0
        timerError.count() shouldBe 0
        timerError.totalTime(TimeUnit.SECONDS) shouldBe 0.0

        try {
            executor.doJob(context)
            fail("Ожидается, что выполнение кода прервется из-за ошибки.")
        } catch (exception: Exception) {
        }

        counterError.count() shouldBe 1.0
        counterOk.count() shouldBe 0.0
        timerError.count() shouldBe 1L
        timerError.totalTime(TimeUnit.SECONDS) shouldBe workDuration.seconds.toDouble()

        backLogCaptor.results.size shouldBe 1

        val log = backLogCaptor.results[0]

        tskvGetByKey(log, CODE_KEY) shouldBe Pair(CODE_KEY, ExecutorJobWrapper.EXECUTOR_JOB_RESULT)
        tskvGetExtra(log).size shouldBe 2
        tskvGetExtra(log) shouldContainExactlyInAnyOrder
            listOf(
                Pair(EXECUTOR_KEY, StubExecutor::class.java.simpleName),
                Pair(EXCEPTION_KEY, RuntimeException::class.java.simpleName)
            )
        tskvGetByKey(log, PAYLOAD_KEY) shouldBe Pair(
            PAYLOAD_KEY,
            exceptionToMessage(RuntimeException(EXCEPTION_MESSAGE))
        )
    }

    private fun MeterRegistry.getTimerError() =
        getTimer(this, ExecutorJobWrapper.ERROR_TAG)

    private fun MeterRegistry.getTimerOk() =
        getTimer(this, ExecutorJobWrapper.OK_TAG)

    private fun MeterRegistry.getCounterOk() =
        getCounter(
            this,
            ExecutorJobWrapper.OK_TAG,
            ExecutorJobWrapper.JOB_OK_COUNTER_NAME
        )

    private fun MeterRegistry.getCounterError() =
        getCounter(
            this,
            ExecutorJobWrapper.ERROR_TAG,
            ExecutorJobWrapper.JOB_ERROR_COUNTER_NAME,
        )

    private fun getTimer(meterRegistry: MeterRegistry, resultTag: Tag) = meterRegistry.timer(
        ExecutorJobWrapper.JOB_TIMER_NAME,
        resultTag.key, resultTag.value,
        ExecutorJobWrapper.EXECUTOR_NAME_TAG_KEY, EXECUTOR_NAME
    )

    private fun getCounter(
        meterRegistry: MeterRegistry,
        resultTag: Tag,
        counterName: String,
    ) =
        meterRegistry.counter(
            counterName,
            resultTag.key, resultTag.value,
            ExecutorJobWrapper.EXECUTOR_NAME_TAG_KEY, EXECUTOR_NAME
        )

    private fun mockExecutorWrapper(
        stubExecutor: Runnable,
        registry: MeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock())
    ) =
        ExecutorJobWrapper(
            stubExecutor,
            meterRegistry = registry,
        )

    inner class StubExecutor(
        var isSuccess: Boolean = true,
        val clock: MockClock
    ): Runnable {
        override fun run() {
            clock.addSeconds(workDuration.seconds)
            if (isSuccess.not()) throw RuntimeException(EXCEPTION_MESSAGE)
        }
    }

    companion object {
        private const val CODE_KEY = "code"
        private const val EXECUTOR_KEY = "executor"
        private const val PAYLOAD_KEY = "payload"
        private const val EXCEPTION_KEY = "exception"
        private const val EXCEPTION_MESSAGE = "Job failed."
        private val EXECUTOR_NAME = StubExecutor::class.simpleName
    }
}
