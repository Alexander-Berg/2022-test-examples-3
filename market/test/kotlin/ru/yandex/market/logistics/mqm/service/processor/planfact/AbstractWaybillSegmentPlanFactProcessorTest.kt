package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.entity.lom.CancellationOrderRequest
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.Recipient
import ru.yandex.market.logistics.mqm.entity.lom.enums.CancellationOrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.utils.SHOOTING_UID
import ru.yandex.market.logistics.mqm.utils.isOrderDelivered
import ru.yandex.market.logistics.mqm.utils.isOrderInCancellationOrLost
import java.time.Instant

class AbstractWaybillSegmentPlanFactProcessorTest {
    private val processor = object : AbstractWaybillSegmentPlanFactProcessor(SegmentStatus.STARTED) {
        override fun calculateExpectedDatetime(waybillSegment: WaybillSegment) = Instant.now()
        override fun isEligibleInternal(waybillSegment: WaybillSegment) = true
    }

    @DisplayName("Проверка фильтрации для подходящего заказа")
    @Test
    fun isEligibleSuccess() {
        val lomOrder = mockOrder()
        val waybillSegment = WaybillSegment().apply { order = lomOrder }
        processor.isEligible(waybillSegment) shouldBe true
    }

    @DisplayName("Проверка фильтрации для фэйкового заказа")
    @Test
    fun isEligibleFake() {
        val lomOrder = mockOrder().apply { fake = true }
        val waybillSegment = WaybillSegment().apply { order = lomOrder }
        processor.isEligible(waybillSegment) shouldBe false
    }

    @DisplayName("Проверка фильтрации для стрельбового заказа")
    @Test
    fun isEligibleShooting() {
        val lomOrder = mockOrder().apply { recipient = Recipient(uid = SHOOTING_UID) }
        val waybillSegment = WaybillSegment().apply { order = lomOrder }
        processor.isEligible(waybillSegment) shouldBe false
    }

    @DisplayName("Проверка фильтрации для daas заказа")
    @Test
    fun isEligibleYaDo() {
        val lomOrder = mockOrder().apply { platformClientId = PlatformClient.YANDEX_DELIVERY.id }
        val waybillSegment = WaybillSegment().apply { order = lomOrder }
        processor.isEligible(waybillSegment) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(OrderStatus::class)
    @DisplayName("Проверка фильтрации по статусу заказа")
    fun isEligibleByOrderStatus(status: OrderStatus) {
        val lomOrder = mockOrder().apply { this.status = status }
        val waybillSegment = WaybillSegment().apply { order = lomOrder }
        if (isOrderInCancellationOrLost(status) || isOrderDelivered(status)) {
            processor.isEligible(waybillSegment) shouldBe false
        } else {
            processor.isEligible(waybillSegment) shouldBe true
        }
    }

    @DisplayName("Проверка фильтрации по наличию заявки на отмену")
    @Test
    fun isEligibleByExistingCancellationRequest() {
        val lomOrder = mockOrder().apply {
            cancellationOrderRequests = mutableSetOf(
                CancellationOrderRequest(status = CancellationOrderStatus.PROCESSING)
            )
        }
        val waybillSegment = WaybillSegment().apply { order = lomOrder }
        processor.isEligible(waybillSegment) shouldBe false
    }

    private fun mockOrder() = LomOrder().apply {
        fake = false
        recipient = Recipient(uid = 123L)
        platformClientId = PlatformClient.BERU.id
        status = OrderStatus.PROCESSING
    }
}
