package ru.yandex.market.logistics.les.service.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.service.queue.task.TaskType
import ru.yandex.market.logistics.les.util.parseTskRow
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor

class DBQueueStatisticsExecutorTest : AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @Autowired
    lateinit var dbQueueStatisticsExecutor: DBQueueStatisticsExecutor

    @Test
    @Disabled
    //TODO(DELIVERY-44262): Временно отключен из-за флапа в CI.
    @DatabaseSetup("/tms/dbqueue_statistics_executor/setup.xml")
    fun sendMetrics() {
        dbQueueStatisticsExecutor.doRealJob(null)
        val statsLogLine = backLogCaptor.results
            .map { log -> parseTskRow(log) }
            .single { log -> log.code == DBQueueStatisticsExecutor.CODE }
        statsLogLine.extra shouldBe listOf(
            Pair("count", "2"),
            Pair("queue", TaskType.EVENT_TASK.name),
        )
    }
}
