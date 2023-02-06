package ru.yandex.market.logistics.mqm.service.monitoringevent

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.dto.MonitoringEventStatsPerTypeDto
import ru.yandex.market.logistics.mqm.monitoringevent.event.EventType
import ru.yandex.market.logistics.mqm.monitoringevent.payload.AbstractMonitoringEventPayload

class MonitoringEventServiceTest : AbstractContextualTest() {

    @Autowired
    private lateinit var monitoringEventService: MonitoringEventService<AbstractMonitoringEventPayload?>

    @Test
    @DatabaseSetup("/service/monitoringevent/before/get_stats.xml")
    fun getStats() {
        val result = monitoringEventService.getStatsByType()

        assertSoftly {
            result shouldContainExactlyInAnyOrder listOf(
                MonitoringEventStatsPerTypeDto(EventType.CREATE_STARTREK_ISSUE, 2, 1),
                MonitoringEventStatsPerTypeDto(EventType.WRITE_MESSAGE_TO_LOG, 2, 1),
                MonitoringEventStatsPerTypeDto(EventType.SEND_TELEGRAM_MESSAGE, 2, 0),
            )
        }
    }
}
