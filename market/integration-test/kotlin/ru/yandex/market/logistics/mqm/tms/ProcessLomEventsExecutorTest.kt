package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest

@DisplayName("Тест обработки событий LOM")
class ProcessLomEventsExecutorTest: AbstractContextualTest() {

    @Autowired
    private lateinit var processLomEventsExecutor: ProcessLomEventsExecutor

    @Test
    @DatabaseSetup("/tms/processLomEventsExecutor/before/setup.xml")
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка")
    fun doJob() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/processLomEventsExecutor/before/setup_dd_delivery.xml")
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_dd_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка изменения даты доставки и изменений с DELIVERY_DATE")
    fun successProcessingDeliveryDateAndChangeOrderRequestWithDelivery() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/processLomEventsExecutor/before/setup_dd_recalculate.xml")
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_dd_recalculate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка изменения даты доставки и изменений с RECALCULATE_ROUTE_DATES")
    fun successProcessingDeliveryDateAndChangeOrderRequestWithRecalculate() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/processLomEventsExecutor/before/setup_dd_several.xml")
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_dd_several.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка изменения даты доставки и изменений c несколькими запросами")
    fun successProcessingDeliveryDateAndChangeOrderRequestWithSeveralRequests() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/processLomEventsExecutor/before/setup_dd_empty.xml")
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_dd_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка изменения даты доставки с пустыми изменениями")
    fun successProcessingDeliveryDateAndChangeOrderRequestWithEmpty() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/processLomEventsExecutor/before/setup_lm_courier_pickup.xml")
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_lm_courier_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка изменения типа доставки с COURIER на PICKUP при изменении статуса COR")
    fun successProcessingDeliveryTypeFromCourierToPickupFromCorChanges() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/processLomEventsExecutor/before/setup_lm_pickup_courier_required_segment_success.xml")
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_lm_pickup_courier_required_segment_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Успешная обработка изменения типа доставки с PICKUP на COURIER при изменении статуса COR, "
            + "переход заявки в статус REQUIRED_SEGMENT_SUCCESS, задача на закрытие ПФ создается"
    )
    fun successProcessingDeliveryTypeFromPickupToCourierFromCorChanges_requiredSegmentSuccess() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup(value = [
        "/tms/processLomEventsExecutor/after/lom_order_lm_pickup_courier_required_segment_success.xml",
        "/tms/processLomEventsExecutor/before/setup_lm_cor_pickup_courier_success.xml"
    ])
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_lm_pickup_courier_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Успешная обработка изменения типа доставки с PICKUP на COURIER при изменении статуса COR, "
            + "переход заявки в статус SUCCESS, задача на закрытие ПФ не создается"
    )
    fun successProcessingDeliveryTypeFromPickupToCourierFromCorChanges_success() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/processLomEventsExecutor/before/setup_lm_pickup_pickup_required_segment_success.xml")
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_lm_pickup_pickup_required_segment_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Успешная обработка изменения типа доставки с PICKUP на PICKUP при изменении статуса COR, "
            + "переход заявки в статус REQUIRED_SEGMENT_SUCCESS, задача на закрытие ПФ создается"
    )
    fun successProcessingDeliveryTypeFromPickupToPickupFromCorChanges_requiredSegmentSuccess() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup(value = [
        "/tms/processLomEventsExecutor/after/lom_order_lm_pickup_pickup_required_segment_success.xml",
        "/tms/processLomEventsExecutor/before/setup_lm_cor_pickup_pickup_success.xml"
    ])
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_lm_pickup_pickup_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Успешная обработка изменения типа доставки с PICKUP на PICKUP при изменении статуса COR, "
            + "переход заявки в статус SUCCESS, задача на закрытие ПФ не создается"
    )
    fun successProcessingDeliveryTypeFromPickupToPickupFromCorChanges_success() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/processLomEventsExecutor/before/setup_route_uuid_changed.xml")
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_route_uuid_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка изменения маршрута заказа")
    fun successProcessingRouteUuidChanged() {
        processLomEventsExecutor.doJob(null)
    }

    @Test
    @DatabaseSetup("/tms/processLomEventsExecutor/before/setup_route_uuid_created.xml")
    @ExpectedDatabase(
        value = "/tms/processLomEventsExecutor/after/lom_order_route_uuid_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Появление маршрута у заказа не создает таску UPDATE_LOM_ORDER_COMBINATOR_ROUTE")
    fun routeUuidChangeFromNullWillNotProduceTask() {
        processLomEventsExecutor.doJob(null)
    }
}
