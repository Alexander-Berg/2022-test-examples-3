package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.time.Instant

@DisplayName("Тест джобы мониторинга состояния очередей")
class QueueStatisticsExecutorTest: AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @Autowired
    private lateinit var queueStatisticsExecutor: QueueStatisticsExecutor

    @Test
    @DatabaseSetup("/tms/queueStatisticsExecutor/before/setup.xml")
    @DisplayName("Успешная обработка")
    fun doJob() {
        clock.setFixed(Instant.parse("2021-07-01T15:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        queueStatisticsExecutor.doJob(null)
        val log = backLogCaptor.results?.toString()
        val checks = setOf(
            "level=INFO\t" +
                "format=plain\t" +
                "code=DB_QUEUE_TASKS\t" +
                "payload=Found 2 tasks with type LOM_ORDER_STATUS_CHANGED in DbQueue\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,type\t" +
                "extra_values=2,LOM_ORDER_STATUS_CHANGED\n",
            "level=INFO\t" +
                "format=plain\t" +
                "code=DB_QUEUE_TASKS\t" +
                "payload=Found 1 tasks with type LOM_WAYBILL_SEGMENT_STATUS_ADDED in DbQueue\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,type\t" +
                "extra_values=1,LOM_WAYBILL_SEGMENT_STATUS_ADDED\n",
            "level=INFO\t" +
                "format=plain\t" +
                "code=DB_QUEUE_TASKS\t" +
                "payload=Found 3 tasks with type RETRIEVE_LOM_ORDER_COMBINATOR_ROUTE in DbQueue\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=amount,type\t" +
                "extra_values=3,RETRIEVE_LOM_ORDER_COMBINATOR_ROUTE\n",
            "level=INFO\t" +
                "format=plain\t" +
                "code=LOM_ORDER_EVENTS\t" +
                "payload=Found 1 events in LomOrderEvents Queue\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=STATISTICS\t" +
                "extra_keys=maxDelay,amount\t" +
                "extra_values=10800,1\n",
        )
        assertSoftly {
            checks.forEach { log shouldContain it }
        }
    }

}
