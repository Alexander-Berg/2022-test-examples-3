package ru.yandex.market.logistics.mqm.service.lom

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.repository.LomOrderRepository
import ru.yandex.market.logistics.mqm.repository.LomWaybillSegmentRepository

@DisplayName("Проверка сервиса работы с сегментами заказов LOM")
class LomWaybillSegmentServiceImplTest: AbstractContextualTest() {
    @Autowired
    lateinit var lomWaybillSegmentService: LomWaybillSegmentService

    @Autowired
    lateinit var lomWaybillSegmentRepository: LomWaybillSegmentRepository

    @Autowired
    lateinit var lomOrderRepository: LomOrderRepository

    @Autowired
    lateinit var lomWaybillSegmentStatusHistoryService: LomWaybillSegmentStatusHistoryService

    @DisplayName("Проверка получения предыдущих сегментов заказа по текущему сегменту")
    @Test
    @DatabaseSetup("/service/lom/waybill_segment/before/setup.xml")
    fun getOrderPreviousSegmentIds() {
        lomWaybillSegmentService.getOrderPreviousSegmentIds(102) shouldBe listOf(101L)
    }

    //В этот тест можно добавлять поля для проверки загрузки из бд, чтобы не писать по тесту на поле
    @DisplayName("Правильно загружать check_save_waybill_segment")
    @Test
    @DatabaseSetup("/service/lom/waybill_segment/before/setup_waybill_segment.xml")
    fun loadWaybillSegmentCorrectly() {
        val waybillSegment = lomWaybillSegmentService.getByIdOrThrow(1)

        waybillSegment.shipment.dateTime shouldBe Instant.parse("2021-03-30T10:00:00.00Z")
        waybillSegment.combinatorSegmentIds shouldContainExactlyInAnyOrder listOf(200L, 300L, 400L)
    }

    //В этот тест можно добавлять поля для проверки сохранения в бд, чтобы не писать по тесту на поле
    @DisplayName("Правильно сохранять check_save_waybill_segment")
    @Test
    @DatabaseSetup("/service/lom/waybill_segment/before/setup_only_order.xml")
    @ExpectedDatabase(
        value = "/service/lom/waybill_segment/after/check_save_waybill_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveWaybillSegmentCorrectly() {
        val lomOrder = lomOrderRepository.findById(1).get()
        val waybillSegment = WaybillSegment(
            partnerId = 1,
            segmentType = SegmentType.FULFILLMENT,
            shipment = WaybillShipment(dateTime = Instant.parse("2021-03-30T10:00:00.00Z")),
            combinatorSegmentIds = listOf(200, 300, 400),
        ).apply { order = lomOrder }
        lomWaybillSegmentRepository.save(waybillSegment)
    }

    @DisplayName("Находит дату первой отгрузки для указанного заказа")
    @Test
    @DatabaseSetup("/service/lom/waybill_segment/before/setup_waybill_with_history.xml")
    fun getFistShipmentDate() {
        val firstShipmentDateByBarcode = lomWaybillSegmentStatusHistoryService.getFirstShipmentDateByBarcode("order1")

        assert(Instant.parse("2019-05-30T22:00:00.00Z").equals(firstShipmentDateByBarcode))
    }
}
