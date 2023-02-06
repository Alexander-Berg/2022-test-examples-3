package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class GetOrdersByDateTzTest extends AbstractWebTestBase {

    public static final ZoneId EUROPE_MOSCOW = ZoneId.of("Europe/Moscow");

    private Date fromDate;

    @BeforeEach
    public void setUp() {
        LocalDateTime firstDate = LocalDateTime.of(2018, Month.JANUARY, 23, 23, 0, 0);
        LocalDateTime secondDate = LocalDateTime.of(2018, Month.JANUARY, 24, 2, 0, 0);
        LocalDateTime thirdDate = LocalDateTime.of(2018, Month.JANUARY, 24, 4, 0, 0);

        createOrder(firstDate);
        createOrder(secondDate);
        createOrder(thirdDate);

        fromDate = Date.from(
                LocalDateTime.of(2018, Month.JANUARY, 24, 0, 0, 0)
                        .atZone(EUROPE_MOSCOW)
                        .toInstant()
        );
    }

    @Test
    public void shouldFilterByDateCorrectly() {
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withFromDate(fromDate)
                .build();
        PagedOrders orders = client.getOrders(request, ClientRole.SYSTEM, 0L);

        assertThat(orders.getItems(), hasSize(2));
    }

    @Test
    public void shouldFilterByStatusUpdateDateCorrectly() {
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withStatusUpdateFromDate(fromDate)
                .build();

        PagedOrders orders = client.getOrders(request, ClientRole.SYSTEM, 0L);
        assertThat(orders.getItems(), hasSize(2));
    }


    private Order createOrder(LocalDateTime creationDate) {
        setFixedTime(creationDate.atZone(EUROPE_MOSCOW).toInstant(), EUROPE_MOSCOW);
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder()
                .addDelivery(DeliveryProvider.yandexDelivery()
                        .courier(false)
                        .dates(DeliveryDates.deliveryDates(getClock(), 0, 2))
                        .buildActualDeliveryOption(getClock()))
                .build());
        params.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        Order order = orderCreateHelper.createOrder(params);

        LocalDateTime createdDate = LocalDateTime.ofInstant(order.getStatusUpdateDate().toInstant(), EUROPE_MOSCOW);

        Assertions.assertEquals(creationDate, createdDate);

        return order;
    }
}
