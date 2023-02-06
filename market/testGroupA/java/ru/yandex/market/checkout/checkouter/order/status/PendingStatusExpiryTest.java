package ru.yandex.market.checkout.checkouter.order.status;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.BuyerType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.checkouter.shop.ScheduleLine;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.shop.schedule.ShopScheduleService;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PendingStatusExpiryTest extends AbstractWebTestBase {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(MOSCOW);

    @Autowired
    private ShopScheduleService scheduleService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private OrderPayHelper orderPayHelper;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.stream(new Object[][]{
                {"01-01-2000 12:00:00", "01-01-2000 14:00:00", Color.BLUE},
                {"01-01-2000 12:00:00", "01-01-2000 14:00:00", Color.WHITE},
                {"01-01-2000 12:00:00", "01-01-2000 14:00:00", Color.TURBO_PLUS},
                {"01-01-2000 18:00:00", "02-01-2000 10:00:00", Color.BLUE},
                {"01-01-2000 18:00:00", "02-01-2000 10:00:00", Color.WHITE},
                {"01-01-2000 18:00:00", "02-01-2000 10:00:00", Color.TURBO_PLUS},
                {"01-01-2000 09:00:00", "01-01-2000 11:00:00", Color.BLUE},
                {"01-01-2000 09:00:00", "01-01-2000 11:00:00", Color.WHITE},
                {"01-01-2000 09:00:00", "01-01-2000 11:00:00", Color.TURBO_PLUS},
                {"01-01-2000 08:55:00", "01-01-2000 11:00:00", Color.BLUE},
                {"01-01-2000 08:55:00", "01-01-2000 11:00:00", Color.WHITE},
                {"01-01-2000 08:55:00", "01-01-2000 11:00:00", Color.TURBO_PLUS},
                {"01-01-2000 04:00:00", "01-01-2000 11:00:00", Color.BLUE},
                {"01-01-2000 04:00:00", "01-01-2000 11:00:00", Color.WHITE},
                {"01-01-2000 04:00:00", "01-01-2000 11:00:00", Color.TURBO_PLUS},
                {"01-01-2000 19:00:00", "02-01-2000 11:00:00", Color.BLUE},
                {"01-01-2000 19:00:00", "02-01-2000 11:00:00", Color.WHITE},
                {"01-01-2000 19:00:00", "02-01-2000 11:00:00", Color.TURBO_PLUS},
                {"01-01-2000 19:05:00", "02-01-2000 11:00:00", Color.BLUE},
                {"01-01-2000 19:05:00", "02-01-2000 11:00:00", Color.WHITE},
                {"01-01-2000 19:05:00", "02-01-2000 11:00:00", Color.TURBO_PLUS},
                {"02-01-2000 00:00:00", "02-01-2000 11:00:00", Color.BLUE},
                {"02-01-2000 00:00:00", "02-01-2000 11:00:00", Color.WHITE},
                {"02-01-2000 00:00:00", "02-01-2000 11:00:00", Color.TURBO_PLUS}
        }).map(Arguments::of);
    }

    @BeforeEach
    public void setUp() {
        ArrayList<ScheduleLine> schedule = new ArrayList<>(7);
        IntStream.rangeClosed(1, 7).forEach(day -> schedule.add(new ScheduleLine(day, 540, 1140)));
        shopService.updateMeta(OrderProvider.SHOP_ID, ShopMetaData.DEFAULT);
        scheduleService.updateShopSchedule(OrderProvider.SHOP_ID, schedule);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void pendingStatusExpiryTest(String now, String expectedExpireDate, Color color) {
        setFixedTime(dateTimeFormatter.parse(now, Instant::from), MOSCOW);
        Order order = OrderProvider.getColorOrder(color);
        order.setDelivery(DeliveryProvider.shopSelfDelivery().build());
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, "666", order.getDelivery());
        Order updatedOrder = orderUpdateService.updateOrderStatus(
                orderId, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION
        );

        assertEquals(Date.from(dateTimeFormatter.parse(expectedExpireDate, Instant::from)),
                updatedOrder.getStatusExpiryDate(), "Incorrect PENDING order status expiry date");
    }

    @Test
    public void pendingFulfilmentStatusExpiryTest() {
        setFixedTime(dateTimeFormatter.parse("01-01-2000 12:00:00", Instant::from), MOSCOW);
        Order blueOrder = OrderProvider.getBlueOrder();
        blueOrder.setFulfilment(true);
        long orderId = orderCreateService.createOrder(blueOrder, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, "666", blueOrder.getDelivery());
        Order updatedOrder = orderUpdateService.updateOrderStatus(
                orderId, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION
        );

        assertNull(updatedOrder.getStatusExpiryDate(), "Incorrect PENDING order status expiry date");
    }

    @Test
    public void pendingAntifraudStatusExpiryTest() {
        setFixedTime(dateTimeFormatter.parse("01-01-2000 12:00:00", Instant::from), MOSCOW);
        Order order = OrderProvider.getBlueOrder();
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, "666", order.getDelivery());
        Order updatedOrder = orderUpdateService.updateOrderStatus(
                orderId, StatusAndSubstatus.of(OrderStatus.PENDING, OrderSubstatus.ANTIFRAUD),
                new ClientInfo(ClientRole.SYSTEM, 0L)
        );

        assertNull(updatedOrder.getStatusExpiryDate(), "Incorrect PENDING order status expiry date");
    }

    @ParameterizedTest
    @CsvSource({"01-01-2000 12:00:00, 01-01-2000 17:00:00",
            "01-01-2000 18:00:00, 02-01-2000 13:00:00",
            "01-01-2000 09:00:00, 01-01-2000 14:00:00",
            "01-01-2000 08:55:00, 01-01-2000 14:00:00",
            "01-01-2000 04:00:00, 01-01-2000 14:00:00",
            "01-01-2000 19:00:00, 02-01-2000 14:00:00",
            "01-01-2000 19:05:00, 02-01-2000 14:00:00",
            "02-01-2000 00:00:00, 02-01-2000 14:00:00"})
    public void pendingStatusDropshipExpiryTest(String now, String expectedExpireDate) {
        setFixedTime(dateTimeFormatter.parse(now, Instant::from), MOSCOW);
        Order order = OrderProvider.getColorOrder(Color.WHITE);
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, "666", order.getDelivery());
        Order updatedOrder = orderUpdateService.updateOrderStatus(
                orderId, StatusAndSubstatus.of(OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION),
                new ClientInfo(ClientRole.SYSTEM, 0L)
        );
        assertEquals(Date.from(dateTimeFormatter.parse(expectedExpireDate, Instant::from)),
                updatedOrder.getStatusExpiryDate(), "Incorrect PENDING order status expiry date");
    }

    @Test
    public void pendingStatusExpressExpiryTest() {
        checkouterProperties.setStraightDropshipFlow(true);
        setFixedTime(Instant.now(), MOSCOW);
        Parameters blueParams = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        blueParams.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        Order order = orderCreateHelper.createOrder(blueParams);
        Date expectedExpiryDate = DateUtils.addHours(Date.from(getClock().instant()), 5);
        orderPayHelper.payForOrder(order);
        Order pendingOrder = orderService.getOrder(order.getId());
        Date actualExpiryDate = pendingOrder.getStatusExpiryDate();
        assertEquals(expectedExpiryDate, actualExpiryDate,
                "Incorrect PENDING order status expiry date");
        checkouterProperties.setStraightDropshipFlow(false);
    }

    @Test
    public void pendingBusinessClientStatusExpiryTest() {
        setFixedTime(dateTimeFormatter.parse("01-01-2000 12:00:00", Instant::from), MOSCOW);
        Order order = OrderProvider.getBlueOrder();
        order.getBuyer().setBusinessBalanceId(123L);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, "666", order.getDelivery());
        Order updatedOrder = orderUpdateService.updateOrderStatus(
                orderId, StatusAndSubstatus.of(OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION),
                new ClientInfo(ClientRole.SYSTEM, 0L)
        );

        assertEquals(BuyerType.BUSINESS, updatedOrder.getBuyer().getType());
        assertNull(updatedOrder.getStatusExpiryDate(), "Incorrect PENDING order status expiry date");
    }
}
