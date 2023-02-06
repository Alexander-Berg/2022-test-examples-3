package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.Recipient
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnService
import ru.yandex.market.logistics.mqm.utils.isOrderDelivered
import ru.yandex.market.logistics.mqm.utils.isOrderReturnedOrLost
import java.time.Instant

class ReturnPlanFactProcessorTest {
    private val lrmReturnService: LrmReturnService = mock()

    private val processor = object : ReturnPlanFactProcessor(SegmentStatus.RETURNED, lrmReturnService) {
        override fun calculateExpectedDatetime(waybillSegment: WaybillSegment): Instant = Instant.now()
        override fun isEligibleInternal(waybillSegment: WaybillSegment): Boolean = true
    }

    @DisplayName("Не создавать планфакт если он есть в ЛРМ")
    @Test
    fun createPlanFactIfExistingActivePlanFactNotFromThisProducer() {
        val lomOrder = LomOrder(
            status = OrderStatus.DRAFT
        ).apply {
            fake = false
            platformClientId = PlatformClient.BERU.id
            recipient = Recipient(uid = 123L)
        }
        val segment = WaybillSegment().apply { order = lomOrder }

        whenever(lrmReturnService.findByExternalOrderId(any())).thenReturn(mock())
        assert(!processor.isEligible(segment))
    }


    @DisplayName("Проверка применимости процессора по статусу заказа")
    @EnumSource(OrderStatus::class)
    @ParameterizedTest
    fun isEligibleByOrderStatus(status: OrderStatus) {
        val lomOrder = LomOrder(
            status = status
        ).apply {
            fake = false
            platformClientId = PlatformClient.BERU.id
            recipient = Recipient(uid = 123L)
        }
        val segment = WaybillSegment().apply { order = lomOrder }

        if (isOrderReturnedOrLost(status) || isOrderDelivered(status)) {
            assertSoftly {
                processor.isEligible(segment) shouldBe false
            }
        } else {
            assertSoftly {
                processor.isEligible(segment) shouldBe true
            }
        }
    }
}
