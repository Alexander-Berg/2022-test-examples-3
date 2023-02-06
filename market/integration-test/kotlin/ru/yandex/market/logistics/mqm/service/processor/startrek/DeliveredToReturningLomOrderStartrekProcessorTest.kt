package ru.yandex.market.logistics.mqm.service.processor.startrek

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.service.lom.LomOrderService
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import java.time.Instant

class DeliveredToReturningLomOrderStartrekProcessorTest : AbstractContextualTest() {

    @Autowired
    lateinit var processor: DeliveredToReturningLomOrderStartrekProcessor

    @Autowired
    lateinit var lomOrderService: LomOrderService

    @Test
    @DatabaseSetup("/service/processor/startrek/delivered_to_returning/before.xml")
    @ExpectedDatabase(
        "/service/processor/startrek/delivered_to_returning/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Transactional
    fun success() {
        clock.setFixed(Instant.parse("2021-12-20T20:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        val order = lomOrderService.findAllByBarcodeIn(setOf("777")).get(0)
        processor.lomOrderStatusChanged(LomOrderStatusChangedContext(order, OrderStatus.RETURNING, listOf()))
    }
}
