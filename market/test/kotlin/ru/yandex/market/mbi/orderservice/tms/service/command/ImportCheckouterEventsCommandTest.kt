package ru.yandex.market.mbi.orderservice.tms.service.command

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.commandInvocationTest
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineKey
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.external.CheckouterApiService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderEventService
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.tms.service.logbroker.events.CheckouterEventProcessor

@CleanupTables(
    [
        OrderEntity::class,
        OrderLineEntity::class,
        OrderEvent::class,
    ]
)
class ImportCheckouterEventsCommandTest : FunctionalTest() {

    @Autowired
    lateinit var checkouterEventProcessor: CheckouterEventProcessor

    @Autowired
    lateinit var checkouterApiService: CheckouterApiService

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var orderEventService: OrderEventService

    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `check import for a single order`() {
        val command = ImportCheckouterEventsCommand(checkouterApiService, checkouterEventProcessor)
        val mockResponse = this::class.loadTestEntities<OrderHistoryEvent>(
            "events/test-events.json",
            objectMapper
        )
        whenever(checkouterApiService.getEventsByOrderId(eq(79914680), any())).thenReturn(mockResponse)
        whenever(checkouterApiService.getEventsByOrderId(eq(69914680), eq(false)))
            .thenThrow(OrderNotFoundException(69914680))
        whenever(checkouterApiService.getEventsByOrderId(eq(69914680), eq(true)))
            .thenReturn(mockResponse)

        commandInvocationTest(
            command,
            "import-checkouter-events 79914680"
        )

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(1223579, 79914680)))
            .isNotNull
        assertThat(orderRepository.orderLineEntityRepository.lookupRow(OrderLineKey(1223579, 79914680, 138268465)))
            .isNotNull
        assertThat(orderEventService.findEventsByOrderKey(OrderKey(1223579, 79914680)))
            .hasSize(8)

        commandInvocationTest(
            command,
            "import-checkouter-events 69914680"
        )

        verify(checkouterApiService, times(1)).getEventsByOrderId(69914680, false)
        verify(checkouterApiService, times(1)).getEventsByOrderId(69914680, true)
    }
}
