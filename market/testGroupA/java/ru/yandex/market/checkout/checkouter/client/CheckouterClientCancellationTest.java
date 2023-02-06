package ru.yandex.market.checkout.checkouter.client;

import java.util.List;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.CancellationRules;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.CompatibleCancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHOP_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class CheckouterClientCancellationTest extends AbstractWebTestBase {

    private static final String NOTES = "notes";
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void postCancellationRequest() {
        Order order = createOrder();
        CompatibleCancellationRequest cancellationRequest = new CompatibleCancellationRequest(
                USER_CHANGED_MIND.name(), NOTES);
        Order response = client.createCancellationRequest(
                order.getId(),
                cancellationRequest,
                ClientRole.USER,
                BuyerProvider.UID
        );
        assertEquals(response.getCancellationRequest().getSubstatus(), USER_CHANGED_MIND);
        assertEquals(response.getCancellationRequest().getNotes(), NOTES);
    }

    /**
     * Проверяет отмену заказа с множественным clientId.
     */
    @Test
    public void postMultiClientCancellationRequest() {
        Order order = createOrder();
        CompatibleCancellationRequest cancellationRequest = new CompatibleCancellationRequest(SHOP_FAILED.name(), null);
        Order response = client.createCancellationRequest(
                order.getId(),
                cancellationRequest,
                RequestClientInfo.builder(ClientRole.SHOP)
                        .withClientIds(Set.of(1L, SHOP_ID_WITH_SORTING_CENTER, 2L))
                        .build(),
                List.of(Color.BLUE)
        );
        assertEquals(response.getCancellationRequest().getSubstatus(), SHOP_FAILED);
        assertNull(response.getCancellationRequest().getNotes());
    }

    @Test
    public void getCancellationRules() {
        CancellationRules cancellationRules = client.getCancellationRules(ClientRole.USER);
        cancellationRules.getContent().forEach(rule -> {
            Assertions.assertNotNull(rule.getStatus());
            Assertions.assertNotNull(rule.getSubstatuses());
            Assertions.assertFalse(rule.getSubstatuses().isEmpty());
        });
        MatcherAssert.assertThat(cancellationRules.getContent(), containsInAnyOrder(
                hasProperty("status", is(UNPAID)),
                hasProperty("status", is(PENDING)),
                hasProperty("status", is(PROCESSING)),
                hasProperty("status", is(DELIVERY)),
                hasProperty("status", is(PICKUP))
        ));

        cancellationRules.getContent()
                .stream()
                .flatMap(rules -> rules.getSubstatuses().stream())
                .forEach(describedSubstatus -> {
                    assertEquals(
                            describedSubstatus.getOrderSubstatus().name(),
                            describedSubstatus.getNotSerializedSubstatus()
                    );
                });
    }

    @Test
    public void getCancellationRulesFromGraph() {
        CancellationRules cancellationRules = client.getCancellationRules(ClientRole.USER);
        cancellationRules.getContent().forEach(rule -> {
            Assertions.assertNotNull(rule.getStatus());
            Assertions.assertNotNull(rule.getSubstatuses());
            Assertions.assertFalse(rule.getSubstatuses().isEmpty());
        });
        MatcherAssert.assertThat(cancellationRules.getContent(), containsInAnyOrder(
                hasProperty("status", is(UNPAID)),
                hasProperty("status", is(PENDING)),
                hasProperty("status", is(PROCESSING)),
                hasProperty("status", is(DELIVERY)),
                hasProperty("status", is(PICKUP))
        ));

        cancellationRules.getContent()
                .stream()
                .flatMap(rules -> rules.getSubstatuses().stream())
                .forEach(describedSubstatus -> {
                    assertEquals(
                            describedSubstatus.getOrderSubstatus().name(),
                            describedSubstatus.getNotSerializedSubstatus()
                    );
                });
    }


    private Order createOrder() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        return orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
    }
}
