package ru.yandex.market.checkout.checkouter.order.status;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertNull;

public class ProcessingStatusExpiryTest extends AbstractWebTestBase {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.stream(new Object[][]{
                {"01-01-2000 12:00:00", Color.BLUE},
                {"01-01-2000 12:00:00", Color.WHITE},
                {"01-01-2000 12:00:00", Color.TURBO_PLUS}
        }).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void processingStatusExpiryTest(String now, Color color) {
        setFixedTime(dateTimeFormatter.parse(now, Instant::from), ZoneId.systemDefault());
        Order order = OrderProvider.getColorOrder(color);

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        Order updatedOrder = orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING);

        assertNull(updatedOrder.getStatusExpiryDate(), "Incorrect PROCESSING order status expiry date");
    }

    @Test
    public void processingStatusDSBSExpiryTest() {
        setFixedTime(dateTimeFormatter.parse("01-01-2000 12:00:00", Instant::from), ZoneId.systemDefault());
        Order order = OrderProvider.getColorOrder(Color.WHITE);
        order.setDelivery(DeliveryProvider.getShopDelivery());
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        Order updatedOrder = orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING);

        assertNull(updatedOrder.getStatusExpiryDate(), "Incorrect PROCESSING order status expiry date");
    }
}
