package ru.yandex.market.deliveryintegrationtests.delivery.tests.ondemand.lavka;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dto.requests.checkouter.Address;
import dto.responses.lavka.LavkaParcelState;
import factory.OfferItems;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;

@DisplayName("Blue Lavka order Test")
@Epic("Blue Lavka")
@Slf4j
public class LavkaOrderStatusesTest extends AbstractLavkaTest {

    /**
     * В этом тесте создается заказ ФФ - МК - Лавка.
     *
     * LOM и чекаутер реагируют на чекпоинты от Лавки. Чекпоинты можно проставлять двумя способами:
     * 1. Можно изменять статус заказа в системе партнера и дожидаться,
     * пока трекер опросит партнера и получит чекпоинты.
     * 2. Можно проставлять ожидаемые чекпоинты прямо в трекер из теста.
     *
     * В релизный пайплайн добавляем 2-ой тип тестов.
     */
    public enum Coverage {
        /**
         * Чекпоинты проставляются в трекер из теста.
         */
        MARKET_ONLY,
        /**
         * Изменяется статус заказа в tristero, история статусов маппится в чекпоинты.
         */
        MARKET_AND_TRISTERO,
    }

    private static final Map<Coverage, Map<OrderSubstatus, Consumer<IdHolder>>> STATUS_MODIFIERS = Map.of(
        Coverage.MARKET_ONLY,
        Map.of(
            OrderSubstatus.READY_FOR_LAST_MILE,
            (IdHolder idHolder) -> {
                DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
                    idHolder.getLavkaWaybillSegmentTrackerId(),
                    OrderDeliveryCheckpointStatus.DELIVERY_AT_START
                );
                DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
                    idHolder.getLavkaWaybillSegmentTrackerId(),
                    OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT
                );
            },
            OrderSubstatus.LAST_MILE_STARTED,
            (IdHolder idHolder) -> DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
                idHolder.getLavkaWaybillSegmentTrackerId(),
                OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT
            ),
            OrderSubstatus.USER_RECEIVED,
            (IdHolder idHolder) -> DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
                idHolder.getLavkaWaybillSegmentTrackerId(),
                OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT
            )
        ),
        Coverage.MARKET_AND_TRISTERO,
        Map.of(
            OrderSubstatus.READY_FOR_LAST_MILE,
            (IdHolder idHolder) -> TRISTERO_ORDER_STEPS.setStatus(
                idHolder.getCheckouterOrderId(),
                LavkaParcelState.IN_DEPOT
            ),
            OrderSubstatus.LAST_MILE_STARTED,
            (IdHolder idHolder) -> TRISTERO_ORDER_STEPS.setStatus(
                idHolder.getCheckouterOrderId(),
                LavkaParcelState.DELIVERING
            ),
            OrderSubstatus.USER_RECEIVED,
            (IdHolder idHolder) -> TRISTERO_ORDER_STEPS.setStatus(
                idHolder.getCheckouterOrderId(),
                LavkaParcelState.DELIVERED
            )
        )
    );

    private IdHolder idHolder;

    @BeforeEach
    void setUp() {
        // создаем заказ в чекаутере
        order = TRISTERO_ORDER_STEPS.createLavkaOrder(List.of(OfferItems.FF_172_UNFAIR_STOCK.getItem()), Address.LAVKA);
        orderId = order.getId();

        // ждем пока заказ создастся в LOM и в партнерах
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(
            lomOrderId,
            ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING
        );
        LOM_ORDER_STEPS.verifyTrackerIds(lomOrderId);

        // запоминаем идентификатор трека пооследней СД
        OrderDto orderDto = LOM_ORDER_STEPS.getLomOrderData(order);
        Long lavkaWaybillSegmentTrackerId = CollectionUtils.last(orderDto.getWaybill()).getTrackerId();

        idHolder = IdHolder.of(orderId, lavkaWaybillSegmentTrackerId);
    }

    @Test
    @Tag("LavkaOrderCreationTest")
    @TmsLink("logistic-17")
    @DisplayName("Синий заказ в Лавке: успешное вручение со второй попытки (без tristero)")
    @Description(
        "Создание заказа в Лавке и перевод по статусам до доставки со второй попытки до пользователя. " +
            "Чекпоинты 1-10-45-48-10-45-48-49 проставляются в трекере."
    )
    public void orderDeliveredToRecipientMarketOnly() {
        orderDeliveredToRecipient(Coverage.MARKET_ONLY);
    }

    @Test
    @TmsLink("logistic-19")
    @DisplayName("Синий заказ в Лавке: успешное вручение со второй попытки (с tristero)")
    @Description(
        "Создание заказа в Лавке и перевод по статусам до доставки со второй попытки до пользователя. " +
            "Статусы проставляются в тристеро."
    )
    public void orderDeliveredToRecipientMarketAndTristero() {
        orderDeliveredToRecipient(Coverage.MARKET_AND_TRISTERO);
    }

    private void orderDeliveredToRecipient(Coverage coverage) {
        moveOrderToStatus(OrderSubstatus.READY_FOR_LAST_MILE, coverage);
        moveOrderToStatus(OrderSubstatus.LAST_MILE_STARTED, coverage);
        moveOrderToStatus(OrderSubstatus.USER_RECEIVED, coverage);
        verifyLomOrderLavkaWaybillSegmentStatus(SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT);
    }

    //1-410
    @Test
    @TmsLink("logistic-30")
    @DisplayName("Синий заказ в Лавке: отмена из статуса Processing")
    public void cancelFromProcessingLavkaOrderTest() {
        ORDER_STEPS.cancelOrder(order);
        ORDER_STEPS.verifyForOrderStatus(order, OrderStatus.CANCELLED);
    }

    //1-10-45-70
    @Test
    @TmsLink("logistic-35")
    @DisplayName("Синий заказ в Лавке: отмена из статуса Delivery")
    public void cancelFromDeliveringLavkaOrderTest() {
        moveOrderToStatus(OrderSubstatus.READY_FOR_LAST_MILE);
        ORDER_STEPS.cancelOrder(order);
        ORDER_STEPS.verifyForOrderStatus(order, OrderStatus.CANCELLED);
        verifyLomOrderLavkaWaybillSegmentStatus(SegmentStatus.RETURN_ARRIVED);
    }

    //1-10-45-48-10-45-60-70
    @Test
    @DisplayName("Синий заказ в Лавке: попытка доставки, возврат в лавку, отмена")
    @TmsLink("logistic-31")
    public void cancelAfterFailedDeliveryLavkaOrderTest() {
        moveOrderToStatus(OrderSubstatus.READY_FOR_LAST_MILE);
        moveOrderToStatus(OrderSubstatus.LAST_MILE_STARTED);
        moveOrderToStatus(OrderSubstatus.READY_FOR_LAST_MILE);
        ORDER_STEPS.cancelOrder(order);
        ORDER_STEPS.verifyForOrderStatus(order, OrderStatus.CANCELLED);
        verifyLomOrderLavkaWaybillSegmentStatus(SegmentStatus.RETURN_ARRIVED);
    }

    private void moveOrderToStatus(OrderSubstatus orderSubstatus) {
        moveOrderToStatus(orderSubstatus, Coverage.MARKET_ONLY);
    }

    private void moveOrderToStatus(OrderSubstatus orderSubstatus, Coverage coverage) {
        STATUS_MODIFIERS.get(coverage).get(orderSubstatus).accept(idHolder);
        ORDER_STEPS.verifyForOrderStatus(order, OrderStatus.DELIVERY);
        ORDER_STEPS.verifyForOrderSubStatus(order, orderSubstatus);
    }

    private void verifyLomOrderLavkaWaybillSegmentStatus(SegmentStatus transitTransmittedToRecipient) {
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            transitTransmittedToRecipient
        );
    }

    @Value(staticConstructor = "of")
    public static class IdHolder {
        long checkouterOrderId;
        long lavkaWaybillSegmentTrackerId;
    }
}
