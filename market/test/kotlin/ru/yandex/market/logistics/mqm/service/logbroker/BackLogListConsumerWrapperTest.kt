package ru.yandex.market.logistics.mqm.service.logbroker

import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import java.time.Instant
import java.util.function.Consumer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.service.logbroker.BackLogListConsumerWrapper.Companion.CODE
import ru.yandex.market.logistics.mqm.service.logbroker.BackLogListConsumerWrapper.Companion.RESULT_FAIL
import ru.yandex.market.logistics.mqm.service.logbroker.BackLogListConsumerWrapper.Companion.RESULT_SUCCESS
import ru.yandex.market.logistics.mqm.utils.tskvGetCode
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.mqm.utils.tskvGetPayload
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor

@ExtendWith(MockitoExtension::class)
internal class BackLogListConsumerWrapperTest: AbstractTest() {

    @Autowired
    private var clock = TestableClock()

    @Mock
    private lateinit var testConsumer: TestConsumer

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    private lateinit var consumerWrapper: BackLogListConsumerWrapper<List<String>>

    @BeforeEach
    fun setUp() {
        clock.setFixed(TEST_NOW, DateTimeUtils.MOSCOW_ZONE)
        consumerWrapper = BackLogListConsumerWrapper(
            actualConsumer = testConsumer,
            clock = clock,
        )
    }

    @DisplayName("Успешная обработка")
    @Test
    fun acceptOk() {
        doAnswer {
            clock.setFixed(TEST_NOW.plus(TEST_DURATION), DateTimeUtils.MOSCOW_ZONE)
        }.whenever(testConsumer).accept(TEST_EVENTS)

        consumerWrapper.accept(TEST_EVENTS)

        val log = backLogCaptor.results[0]
        verifyLog(log)
        tskvGetExtra(log).toMap()["result"] shouldBe RESULT_SUCCESS
        tskvGetPayload(log) shouldContain "Events has been processed"
    }

    @DisplayName("Неуспешная обработка")
    @Test
    fun acceptError() {
        doAnswer {
            clock.setFixed(TEST_NOW.plus(TEST_DURATION), DateTimeUtils.MOSCOW_ZONE)
            throw RuntimeException()
        }.whenever(testConsumer).accept(TEST_EVENTS)

        consumerWrapper.accept(TEST_EVENTS)

        val log = backLogCaptor.results[0]
        verifyLog(log)
        tskvGetExtra(log).toMap()["result"] shouldBe RESULT_FAIL
        tskvGetExtra(log).toMap()["exception"] shouldBe "RuntimeException"
        tskvGetPayload(log) shouldContain "Failed to process events"
    }

    private fun verifyLog(log: String) {
        tskvGetCode(log) shouldBe CODE
        tskvGetExtra(log).toMap() shouldContainKey "durationMsec"
        tskvGetExtra(log) shouldContainAll listOf(
            Pair("consumer", testConsumer::class.simpleName),
            Pair("count", TEST_EVENTS.size.toString()),
            Pair("durationMsec", TEST_DURATION.toMillis().toString()),
        )
    }

    open class TestConsumer(val clock: TestableClock): Consumer<List<String>> {
        override fun accept(t: List<String>) {
            clock.setFixed(TEST_NOW.plus(TEST_DURATION), DateTimeUtils.MOSCOW_ZONE)
        }
    }

    companion object {
        val TEST_NOW = Instant.parse("2021-03-01T20:00:50.00Z")
        val TEST_DURATION = Duration.ofMillis(145)
        val TEST_EVENTS = listOf("one", "two")
    }
}
