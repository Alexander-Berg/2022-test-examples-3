package ru.yandex.market.tpl.core.domain.test_sc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.fulfillment.Courier;
import ru.yandex.market.logistic.api.model.fulfillment.Delivery;
import ru.yandex.market.logistic.api.model.fulfillment.DeliveryType;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.PaymentType;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.Partner;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kukabara
 */
@CoreTest
class ScLogisticServiceTest {

    private static final String TPL_ORDER_ID = "tplOrderId";
    private static final String YDO_ORDER_ID = "ydoOrderId";

    private static final String COURIER_NAME = "Иванов";

    @Autowired
    private ScLogisticService scLogisticService;
    @Autowired
    private SortingCenterService sortingCenterService;
    @Autowired
    private Clock clock;

    @Test
    void create() {
        Partner partner = sortingCenterService.findSortCenterForDs(DeliveryService.DEFAULT_DS_ID);
        TestScOrder created = scLogisticService.createOrder(getOrder(), partner);

        assertThat(created.getExternalOrderId()).isEqualTo(YDO_ORDER_ID);
        assertThat(created.getYandexId()).isEqualTo(TPL_ORDER_ID);
        assertThat(created.getCourier()).isEqualTo(COURIER_NAME);
        ResourceId resourceId = new ResourceId(TPL_ORDER_ID, null);

        assertThat(scLogisticService.getOrderHistory(resourceId, partner).stream().map(OrderStatus::getStatusCode))
                .containsExactly(OrderStatusType.ORDER_CREATED_FF);
        assertThat(scLogisticService.getOrdersStatus(List.of(resourceId), partner).stream()
                .map(osh -> osh.getHistory().get(0))
                .map(OrderStatus::getStatusCode))
                .containsExactly(OrderStatusType.ORDER_CREATED_FF);
    }

    @Test
    void updateStatus() {
        Partner partner = sortingCenterService.findSortCenterForDs(DeliveryService.DEFAULT_DS_ID);
        scLogisticService.createOrder(getOrder(), partner);

        ResourceId resourceId = new ResourceId(TPL_ORDER_ID, null);

        scLogisticService.updateStatus(resourceId, OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE, false);
        scLogisticService.updateStatus(resourceId, OrderStatusType.SO_GOT_INFO_ABOUT_PLANNED_RETURN, false);

        assertThat(scLogisticService.getOrderHistory(resourceId, partner).stream().map(OrderStatus::getStatusCode))
                .containsExactly(
                        OrderStatusType.ORDER_CREATED_FF,
                        OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE,
                        OrderStatusType.SO_GOT_INFO_ABOUT_PLANNED_RETURN
                );
        assertThat(scLogisticService.getOrdersStatus(List.of(resourceId), partner).stream()
                .map(osh -> osh.getHistory().get(0))
                .map(OrderStatus::getStatusCode))
                .containsExactly(OrderStatusType.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

    private Order getOrder() {
        Person person = new Person.PersonBuilder(COURIER_NAME).build();
        Delivery delivery = new Delivery.DeliveryBuilder("Беру", List.of(), "contract", List.of(), 1)
                .setCourier(new Courier.CourierBuilder(List.of(person)).build())
                .build();

        return new Order.OrderBuilder(new ResourceId(TPL_ORDER_ID, null),
                null,
                List.of(),
                null,
                null, null, PaymentType.CARD, delivery, DeliveryType.COURIER, null,
                null, null, null, null, null, null
        )
                .setDeliveryDate(DateTime.fromLocalDateTime(LocalDateTime.now(clock)))
                .setExternalId(new ResourceId(YDO_ORDER_ID, null))
                .build();
    }

}
