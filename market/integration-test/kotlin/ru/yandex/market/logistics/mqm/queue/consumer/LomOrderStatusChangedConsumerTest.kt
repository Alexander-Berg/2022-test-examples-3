package ru.yandex.market.logistics.mqm.queue.consumer

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.time.Instant
import java.time.LocalTime
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.queue.dto.LomOrderIdStatusDto
import ru.yandex.market.logistics.mqm.queue.producer.CloseIssueLinkProducer
import ru.yandex.market.logistics.mqm.service.PartnerService

@DisplayName("Тест обработки получения нового статуса заказа")
class LomOrderStatusChangedConsumerTest : AbstractContextualTest() {
    @Autowired
    private lateinit var consumer: LomOrderStatusChangedConsumer

    @Autowired
    private lateinit var partnerService: PartnerService

    @Autowired
    private lateinit var transactionOperations: TransactionOperations

    @Autowired
    private lateinit var closeIssueLinkProducer: CloseIssueLinkProducer

    @Test
    @DatabaseSetup("/queue/consumer/before/process_order_status_change/setup.xml")
    @ExpectedDatabase(
        value = "/queue/consumer/after/process_order_status_change/cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Подходящий статус: CANCELLED")
    fun testConsumerCancelled() {
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        transactionOperations.executeWithoutResult {
            consumer.processPayload(LomOrderIdStatusDto(1L, OrderStatus.CANCELLED))
        }
        verify(closeIssueLinkProducer).produceTask(any(), any())
    }

    @Test
    @DatabaseSetup("/queue/consumer/before/process_order_status_change/setup_enqueued.xml")
    @ExpectedDatabase(
        value = "/queue/consumer/after/process_order_status_change/enqueued_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Подходящий статус: ENQUEUED - первый сегмент FF")
    fun consumerEnqueued() {
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        mock()
        transactionOperations.executeWithoutResult {
            consumer.processPayload(LomOrderIdStatusDto(1L, OrderStatus.ENQUEUED))
        }
    }


    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup("/queue/consumer/before/process_order_status_change/setup_enqueued.xml"),
            DatabaseSetup(
                value = ["/queue/consumer/before/process_order_status_change/first_segment_dropship.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/queue/consumer/after/process_order_status_change/enqueued_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Подходящий статус: ENQUEUED - первый сегмент DROPSHIP")
    fun consumerEnqueuedDropship() {
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        mock()
        transactionOperations.executeWithoutResult {
            consumer.processPayload(LomOrderIdStatusDto(1L, OrderStatus.ENQUEUED))
        }
    }

    @Test
    @DatabaseSetup("/queue/consumer/before/process_order_status_change/setup.xml")
    @ExpectedDatabase(
        value = "/queue/consumer/before/process_order_status_change/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Неподходящий статус: PROCESSING")
    fun consumerWithOtherStatus() {
        transactionOperations.executeWithoutResult {
            consumer.processPayload(LomOrderIdStatusDto(1L, OrderStatus.PROCESSING))
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        names = ["CANCELLED", "RETURNING", "RETURNED", "LOST", "DELIVERED"],
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Не вызывать закрытие задач для не финальных статусов")
    @DatabaseSetup("/queue/consumer/before/process_order_status_change/setup_enqueued.xml")
    fun notCallCloseIssuesForNotFinalOrderStatuses(orderStatus: OrderStatus) {
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        mock()
        transactionOperations.executeWithoutResult {
            consumer.processPayload(LomOrderIdStatusDto(1L, orderStatus))
        }
        verify(closeIssueLinkProducer, never()).produceTask(any(), any())
    }

    private fun mock() {
        doReturn(LocalTime.of(0, 0)).whenever(partnerService).findCutoffTime(argThat { it -> it.partnerId == 301L })
        doReturn(LocalTime.of(23, 59)).whenever(partnerService).findCutoffTime(argThat { it -> it.partnerId == 302L })
    }
}
