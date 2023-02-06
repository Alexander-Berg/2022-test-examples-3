package ru.yandex.market.mbi.orderservice.tms.service.monitoring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.InvalidLogisticEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.InvalidLogisticEventKey
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.DLQRepository
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest

class LogisticDLQSizeMetricCollectorTest : FunctionalTest() {

    @Autowired
    lateinit var dlqRepository: DLQRepository<InvalidLogisticEventKey, InvalidLogisticEventEntity>

    @BeforeEach
    fun setUp() {
        val events =
            this::class.loadTestEntities<InvalidLogisticEventEntity>("logisticDlqSizeMetricCollectorTest.before.json")
        dlqRepository.addInvalidLogisticEvents(events)
    }

    @Test
    fun `verify event count`() {
        assertThat(dlqRepository.getInvalidEventsCount()).isEqualTo(2)
    }
}
