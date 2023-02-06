package ru.yandex.market.logistics.mqm.service.lom

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute
import ru.yandex.market.logistics.lom.model.enums.PartnerType
import ru.yandex.market.logistics.lom.model.enums.PointType
import ru.yandex.market.logistics.lom.model.enums.RoutePointServiceType
import ru.yandex.market.logistics.lom.model.enums.ServiceCodeName
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.repository.LomOrderRepository
import ru.yandex.market.logistics.mqm.repository.PlanFactRepository

class LomOrderServiceTest: AbstractContextualTest() {

    @Autowired
    private lateinit var lomOrderService: LomOrderService

    @Autowired
    private lateinit var lomOrderRepository: LomOrderRepository

    @Autowired
    private lateinit var planFactRepository: PlanFactRepository

    @Autowired
    private lateinit var transactionOperations: TransactionOperations

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-05-20T11:40:51.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("План-факт для обратной логистики помечается как NOT_ACTUAL для заказа перешедшего в RETURNED")
    @Test
    @DatabaseSetup("/service/lom/order/before/setup_returned.xml")
    fun markReturnLogisticsPlanFactNotActualForReturnedOrder() {
        transactionOperations.executeWithoutResult {
            val returnedOrder = lomOrderRepository.getOne(1)
            lomOrderService.markPlanFactsNotActual(returnedOrder)
            val planFact = planFactRepository.getOne(101)
            assertSoftly {
                planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            }
        }
    }

    @DisplayName("План-факт для обратной логистики не помечается NOT_ACTUAL для заказа перешедшего в RETURNING")
    @Test
    @DatabaseSetup("/service/lom/order/before/setup_returning.xml")
    fun doNotMarkReturnLogisticsPlanFactNotActualForReturningOrder() {
        transactionOperations.executeWithoutResult {
            val returningOrder = lomOrderRepository.getOne(1)
            lomOrderService.markPlanFactsNotActual(returningOrder)
            val planFact = planFactRepository.getOne(101)
            assertSoftly {
                planFact.planFactStatus shouldBe PlanFactStatus.ACTIVE
            }
        }
    }

    @DisplayName("План-факт для обратной логистики не помечается NOT_ACTUAL для заказа перешедшего в RETURNING")
    @Test
    @DatabaseSetup("/service/lom/order/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/lom/order/after/save_combinator_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveCombinatorRoute() {
        val combinatorRoute = CombinatorRoute().setRoute(
            CombinatorRoute.DeliveryRoute()
                .setCost(BigDecimal.ONE)
                .setDateFrom(CombinatorRoute.Date().setDay(1).setMonth(1).setYear(2021))
                .setTariffId(12L)
                .setPoints(
                    mutableListOf(
                        CombinatorRoute.Point()
                            .setIds(
                                CombinatorRoute.PointIds()
                                    .setRegionId(2)
                                    .setPartnerId(125)
                            )
                            .setSegmentType(PointType.WAREHOUSE)
                            .setPartnerType(PartnerType.FULFILLMENT)
                            .setSegmentId(2)
                            .setPartnerName("PR2")
                            .setServices(
                                mutableListOf(
                                    CombinatorRoute.DeliveryService()
                                        .setId(1002)
                                        .setType(RoutePointServiceType.INBOUND)
                                        .setCode(ServiceCodeName.DELIVERY)
                                        .setItems(mutableListOf(CombinatorRoute.ProcessedItem().setItemIndex(1).setQuantity(1)))
                                        .setCost(BigDecimal.valueOf(1))
                                        .setStartTime(CombinatorRoute.Timestamp().setSeconds(42))
                                        .setDuration(CombinatorRoute.Timestamp().setSeconds(43))
                                        .setDeliveryIntervals(mutableListOf(CombinatorRoute.DeliveryInterval().setFrom(CombinatorRoute.Time().setHour(1)).setTo(CombinatorRoute.Time().setHour(2))))
                                        .setServiceMeta(mutableListOf(CombinatorRoute.ServiceMeta().setKey("k").setValue("v")))
                                        .setLogisticDate(CombinatorRoute.Date().setDay(1).setMonth(2).setYear(3))
                                        .setScheduleStartTime(CombinatorRoute.Timestamp().setSeconds(1))
                                        .setScheduleEndTime(CombinatorRoute.Timestamp().setSeconds(1))
                                        .setTzOffset(42)
                                        .setDurationDelta(1)
                                        .setWorkingSchedule(mutableListOf(createScheduleDay()))
                                )
                            )
                    )
                )
        )
        val lomOrder = 123L;
        transactionOperations.executeWithoutResult {
            lomOrderService.saveCombinatorRoute(lomOrder, combinatorRoute)
        }
    }

    @DisplayName("Метод findLomOrderByExternalId находит заказ по barcode")
    @Test
    @DatabaseSetup("/service/lom/order/before/setup_find_find_order_by_barcode.xml")
    fun findLomOrderByExternalIdWithBarcode() {
        val lomOrder = lomOrderService.findLomOrderByExternalId("12345")!!
        lomOrder.id shouldBe 1
    }

    private fun createScheduleDay(): CombinatorRoute.ScheduleDay {
        return CombinatorRoute.ScheduleDay()
            .setDaysOfWeek(listOf(0, 1, 2, 3, 4, 5, 6))
            .setTimeWindows(listOf(CombinatorRoute.TimeWindow().setStartTime(1).setEndTime(2)))
    }
}
