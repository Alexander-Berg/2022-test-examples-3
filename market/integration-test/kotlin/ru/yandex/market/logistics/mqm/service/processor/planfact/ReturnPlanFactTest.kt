package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.service.PlanFactService
import java.time.Instant

@DisplayName("Тесты процессоров, создающих план-факты обратной логистики")
class ReturnPlanFactTest : AbstractContextualTest() {

    @Autowired
    private lateinit var planFactService: PlanFactService

    @BeforeEach
    fun setup() {
        clock.setFixed(TEST_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Создание планфакта обратной логистики для приёмки в СД")
    @DatabaseSetup(value = ["/service/return_plan_fact/before/return_ds_intake_sd.xml"])
    @ExpectedDatabase(
        value = "/service/return_plan_fact/after/return_ds_intake_sd.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun returnDsIntakePlanFact() {
        saveFactStatusDatetime(3L, SegmentStatus.RETURNED)
    }

    @Test
    @DisplayName("Создание планфакта обратной логистики для приёмки в СЦ из последней мили")
    @DatabaseSetup(value = ["/service/return_plan_fact/before/return_last_mile_sc_intake.xml"])
    @ExpectedDatabase(
        value = "/service/return_plan_fact/after/return_last_mile_sc_intake.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun returnLastMileScIntakePlanFact() {
        saveFactStatusDatetime(3L, SegmentStatus.RETURNED)
    }

    @Test
    @DisplayName("Создание планфакта обратной логистики для приёмки в СЦ из СД")
    @DatabaseSetup(value = ["/service/return_plan_fact/before/return_ds_sc_intake.xml"])
    @ExpectedDatabase(
        value = "/service/return_plan_fact/after/return_ds_sc_intake.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun returnDsScIntakePlanFact() {
        saveFactStatusDatetime(3L, SegmentStatus.RETURNED)
    }

    @Test
    @DisplayName("Создание план-факта обратной логистики для готовности возвратного заказа к передаче с СЦ")
    @DatabaseSetup(value = ["/service/return_plan_fact/before/return_sc_prepared.xml"])
    @ExpectedDatabase(
        value = "/service/return_plan_fact/after/return_sc_prepared.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun returnScPreparedPlanFact() {
        saveFactStatusDatetime(1L, SegmentStatus.RETURN_ARRIVED)
    }

    @Test
    @DisplayName("Создание план-факта обратной логистики для того, что заказ возвращен отправителю")
    @DatabaseSetup(value = ["/service/return_plan_fact/before/return_sc_returned.xml"])
    @ExpectedDatabase(
        value = "/service/return_plan_fact/after/return_sc_returned.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun returnScReturnedPlanFact() {
        saveFactStatusDatetime(1L, SegmentStatus.RETURN_PREPARING_SENDER)
    }

    @Test
    @DisplayName("Создание план-факта на отгрузку с СД для обратной логистики (70 - 80)")
    @DatabaseSetup(value = ["/service/return_plan_fact/before/return_ds_shipment.xml"])
    @ExpectedDatabase(
        value = "/service/return_plan_fact/after/return_ds_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun returnDsShipmentDefaultPlanFact() {
        saveFactStatusDatetime(2L, SegmentStatus.RETURN_PREPARING_SENDER)
    }

    @Test
    @DisplayName("Создание план-факта на отгрузку с СД для обратной логистики (70 - 80) с контрактной доставкой")
    @DatabaseSetup(value = ["/service/return_plan_fact/before/return_ds_shipment_contract_delivery.xml"])
    @ExpectedDatabase(
        value = "/service/return_plan_fact/after/return_ds_shipment_contract_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun returnDsShipmentContractDeliveryPlanFact() {
        saveFactStatusDatetime(2L, SegmentStatus.RETURN_PREPARING_SENDER)
    }

    private fun saveFactStatusDatetime(segmentId: Long, segmentStatus: SegmentStatus) {
        planFactService.saveFactStatusDatetime(
            EntityType.LOM_WAYBILL_SEGMENT,
            segmentId,
            segmentStatus.name,
            FIXED_TIME,
            null
        )
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-01T00:00:00.00Z")
        private val TEST_TIME = Instant.parse("2020-12-01T00:00:00.00Z")
    }
}
