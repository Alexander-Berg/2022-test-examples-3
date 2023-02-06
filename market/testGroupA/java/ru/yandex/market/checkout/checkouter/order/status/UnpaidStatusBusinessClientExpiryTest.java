package ru.yandex.market.checkout.checkouter.order.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnpaidStatusBusinessClientExpiryTest extends AbstractWebTestBase {

    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    private Clock clock;

    public static Stream<Arguments> parameterizedTestData() {
         return Arrays.stream(new Object[][]{
                {"03-06-2022 12:00:00", "10-06-2022 23:59:59"},
                {"07-06-2022 10:00:00", "15-06-2022 23:59:59"},
         }).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void pendingStatusExpiryTest(String now, String expectedExpireDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(clock.getZone());
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.parse(expectedExpireDate, formatter));

        setFixedTime(formatter.parse(now, Instant::from), clock.getZone());
        Order order = OrderProvider.getColorOrder(Color.BLUE);
        order.setDelivery(DeliveryProvider.shopSelfDelivery().build());
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        order.getBuyer().setBusinessBalanceId(10000L);

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, "666", order.getDelivery());
        Order updatedOrder = orderUpdateService.updateOrderStatus(
                orderId, StatusAndSubstatus.of(OrderStatus.UNPAID, OrderSubstatus.AWAIT_PAYMENT),  ClientInfo.SYSTEM);

        assertEquals(Date.from(formatter.parse(expectedExpireDate, Instant::from).plusMillis(999)),
                updatedOrder.getStatusExpiryDate(), "Incorrect UNPAID order status expiry date");
    }

    @Test
    public void B2bClientsFailTest() {
        Order order = OrderProvider.getColorOrder(Color.BLUE);
        order.setDelivery(DeliveryProvider.shopSelfDelivery().build());
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        order.getBuyer().setBusinessBalanceId(10000L);

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, "666", order.getDelivery());
        Order updatedOrder = orderUpdateService.updateOrderStatus(
                orderId, StatusAndSubstatus.of(OrderStatus.UNPAID, OrderSubstatus.AWAIT_PAYMENT),  ClientInfo.SYSTEM);

        // если b2b клиент не отвечает, то ставим резерв на 5 дней
        Date expectedDate = Date.from(updatedOrder.getUpdateDate().toInstant().atZone(clock.getZone()).toLocalDate()
                .plusDays(5).atTime(LocalTime.MAX).atZone(clock.getZone()).toInstant());

        assertEquals(expectedDate,
                updatedOrder.getStatusExpiryDate(), "Incorrect default UNPAID order status expiry date");
    }
}
