package ru.yandex.market.logistics.mqm.checker

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import org.springframework.stereotype.Component
import ru.yandex.market.logistics.mqm.monitoringevent.event.EventType
import ru.yandex.market.logistics.mqm.monitoringevent.payload.AbstractMonitoringEventPayload
import ru.yandex.market.logistics.mqm.repository.MonitoringEventRepository

@Component
class MonitoringEventTaskChecker(
    private val objectMapper: ObjectMapper,
    private val monitoringEventRepository: MonitoringEventRepository,
) {

    fun assertExactlyQueueTask(eventType: EventType, payload: AbstractMonitoringEventPayload) {
        val queueTasks = monitoringEventRepository.getByTypeAndPayload(
            eventType,
            objectMapper.writeValueAsString(payload)
        )

        assertSoftly {
            queueTasks shouldHaveSize 1
        }
    }
}
