package ru.yandex.market.checkout.checkouter.service;

import java.util.Collections;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Either;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.service.bind.BindFailReason;
import ru.yandex.market.checkout.checkouter.service.bind.OrderBindService;
import ru.yandex.market.checkout.test.providers.OrderProvider;

// названо *IntegrationTest, чтобы не было коллизий для тех кто не использует separate sourceSets
public class OrderBindServiceImplIntegrationTest extends AbstractServicesTestBase {

    private static final String BIND_KEY = "asdasd";
    private static final long UID = 1L;
    private static final long MUID = 1234567890121111111L;
    private static final long ANOTHER_MUID = 1234567890121111112L;
    private static final String USER_PHONE = "+79272234562";
    private static final String SIMILAR_PHONE = "+7(927)223-45-62";

    @Autowired
    private OrderBindService orderBindService;

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDER_BY_BINDKEY)
    @DisplayName("Чекаутер должен возвращать заказ по ключу привязки и MUID")
    @Test
    public void shouldSuccessfullyReturnOrder() {
        long orderId = orderCreateService.createOrder(OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey(BIND_KEY);
            o.getBuyer().setUid(MUID);
            o.getBuyer().setMuid(MUID);
        }), ClientInfo.SYSTEM);

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                Collections.singletonList(USER_PHONE), UID, null);

        Assertions.assertTrue(orderByBindKey.isLeftNotRight());
        Assertions.assertEquals(orderId, orderByBindKey.asLeft().getId().longValue());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDER_BY_BINDKEY)
    @DisplayName("Чекаутер должен возвращать заказ по ключу привязки и MUID")
    @Test
    public void shouldSuccessfullyReturnOrderIfNoAuthAndUidIsNull() {
        long orderId = orderCreateService.createOrder(OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey(BIND_KEY);
            o.getBuyer().setUid(MUID);
            o.getBuyer().setMuid(MUID);
        }), ClientInfo.SYSTEM);

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                Collections.singletonList(USER_PHONE), null, true);

        Assertions.assertTrue(orderByBindKey.isLeftNotRight());
        Assertions.assertEquals(orderId, orderByBindKey.asLeft().getId().longValue());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDER_BY_BINDKEY)
    @DisplayName("Чекаутер должен возвращать заказ по ключу привязки и телефону")
    @Test
    public void shouldSuccessfullyReturnOrderByPhone() {
        long orderId = createOrder(USER_PHONE);

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                Collections.singletonList(USER_PHONE), UID, null);

        Assertions.assertTrue(orderByBindKey.isLeftNotRight());
        Assertions.assertEquals(orderId, orderByBindKey.asLeft().getId().longValue());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDER_BY_BINDKEY)
    @DisplayName("Чекаутер должен возвращать заказ по ключу привязки и похожему телефону")
    @Test
    public void shouldSuccessfullyReturnOrderBySimilarPhone() {
        long orderId = createOrder(SIMILAR_PHONE);

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                Collections.singletonList(USER_PHONE), UID, null);

        Assertions.assertTrue(orderByBindKey.isLeftNotRight());
        Assertions.assertEquals(orderId, orderByBindKey.asLeft().getId().longValue());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDER_BY_BINDKEY)
    @DisplayName("Чекаутер не должен возвращать заказ по ключу привязки, если ни телефон ни muid не совпадают")
    @Test
    public void shouldDenyIfNeitherMuidNorPhoneMatches() {
        long orderId = orderCreateService.createOrder(OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey(BIND_KEY);
            o.getBuyer().setUid(ANOTHER_MUID);
            o.getBuyer().setMuid(ANOTHER_MUID);
        }), ClientInfo.SYSTEM);

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                Collections.singletonList(USER_PHONE), UID, null);

        Assertions.assertFalse(orderByBindKey.isLeftNotRight());
        Assertions.assertEquals(BindFailReason.NOT_ENOUGH_CREDENTIALS, orderByBindKey.asRight());
    }

    private long createOrder(String phone) {
        return orderCreateService.createOrder(OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey(BIND_KEY);
            o.getBuyer().setUid(ANOTHER_MUID);
            o.getBuyer().setMuid(ANOTHER_MUID);
            o.getBuyer().setPhone(phone);
        }), ClientInfo.SYSTEM);
    }
}
