package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.BaseStatusModelTest;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestProcessor;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.CALL_CENTER_OPERATOR;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SHOP;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SHOP_USER;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SYSTEM;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.USER;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;

/**
 * Правила мгновенной отмены описаны в https://wiki.yandex-team.ru/market/assess/dscancellation/
 */
public class CancellationDbsCanBeAppliedNowTest extends BaseStatusModelTest {

    @Autowired
    private CancellationRequestProcessor cancellationRequestProcessor;

    public static Stream<Arguments> parameterizedDBSTestData() {
        var params = new ArrayList<Object[]>();
        Consumer<Order> deliveryToMarketBrandedOutlet = x -> {
            x.getDelivery().setMarketBranded(true);
        };
        Arrays.stream(new Object[]{PaymentType.PREPAID, PaymentType.POSTPAID})
                .forEach(paymentType -> {
                    Arrays.stream(new Object[]{USER, SHOP, SHOP_USER, CALL_CENTER_OPERATOR, SYSTEM})
                            .forEach(r -> params.addAll(
                                    Arrays.stream(new Object[][]{
                                                    new Object[]{OrderStatus.PENDING,
                                                            OrderSubstatus.AWAIT_CONFIRMATION, true, r, paymentType,
                                                            null},
                                                    new Object[]{OrderStatus.PENDING, OrderSubstatus.ANTIFRAUD, true,
                                                            r, paymentType, null},
                                                    new Object[]{OrderStatus.PROCESSING, OrderSubstatus.STARTED, true,
                                                            r, paymentType, null},
                                                    new Object[]{OrderStatus.PROCESSING, OrderSubstatus.PACKAGING,
                                                            true, r, paymentType, null},
                                                    new Object[]{OrderStatus.PROCESSING, OrderSubstatus.SHIPPED, true,
                                                            r, paymentType, null}})
                                            .collect(Collectors.toList())
                            ));
                    Arrays.stream(new Object[]{SHOP, SHOP_USER})
                            .forEach(r -> params.addAll(Arrays.stream(new OrderStatus[]{OrderStatus.DELIVERY,
                                            OrderStatus.PICKUP})
                                    .flatMap(os -> Arrays.stream(OrderSubstatus.values())
                                            .filter(oss -> oss.getStatus() == os))
                                    .map(oss -> new Object[]{oss.getStatus(), oss, true, r, paymentType, null})
                                    .collect(Collectors.toList())
                            ));
                    Arrays.stream(new Object[]{USER, CALL_CENTER_OPERATOR, SYSTEM})
                            .forEach(r -> params.addAll(Arrays.stream(new OrderStatus[]{OrderStatus.DELIVERY,
                                            OrderStatus.PICKUP})
                                    .flatMap(os -> Arrays.stream(OrderSubstatus.values())
                                            .filter(oss -> oss.getStatus() == os))
                                    .map(oss -> new Object[]{oss.getStatus(), oss,
                                            paymentType == PaymentType.POSTPAID, r, paymentType, null})
                                    .collect(Collectors.toList())
                            ));
                    Arrays.stream(new Object[]{USER, CALL_CENTER_OPERATOR, SYSTEM})
                            .forEach(r -> params.addAll(Arrays.stream(new OrderStatus[]{OrderStatus.DELIVERY,
                                            OrderStatus.PICKUP})
                                    .flatMap(os -> Arrays.stream(OrderSubstatus.values())
                                            .filter(oss -> oss.getStatus() == os))
                                    .map(oss -> new Object[]{oss.getStatus(), oss,
                                            true, r, paymentType, deliveryToMarketBrandedOutlet
                                    })
                                    .collect(Collectors.toList())
                            ));
                });
        return params.stream().map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}, {1}, {2}, {3}, {4}")
    @MethodSource("parameterizedDBSTestData")
    public void canBeAppliedNowDBSTest(OrderStatus fromStatus,
                                       OrderSubstatus fromSubstatus,
                                       boolean shouldBeApplied,
                                       ClientRole clientRole,
                                       PaymentType paymentType,
                                       Consumer<Order> orderConsumer) {
        doReturn(false).when(checkouterFeatureReader).getBoolean(ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE);

        var order = createDbsOrderWithStatus(fromStatus, fromSubstatus, paymentType == PaymentType.PREPAID,
                orderConsumer);
        var isAppliedNow = cancellationRequestProcessor.canBeAppliedNow(
                order,
                new CancellationRequestPayload(
                        OrderSubstatus.USER_CHANGED_MIND,
                        null,
                        null,
                        null),
                new ClientInfo(clientRole, 1L));
        assertThat(isAppliedNow, equalTo(shouldBeApplied));
    }

    private Order createDbsOrderWithStatus(OrderStatus status, OrderSubstatus substatus, boolean prepaid,
                                           Consumer<Order> orderConsumer) {
        var order = new Order();
        order.setStatus(status);
        order.setSubstatus(substatus);
        order.setRgb(WHITE);
        order.setFulfilment(false);
        order.setPaymentMethod(prepaid ? PaymentMethod.YANDEX : PaymentMethod.CARD_ON_DELIVERY);
        order.setDelivery(DeliveryProvider.getShopDeliveryWithPickupType());
        if (orderConsumer != null) {
            orderConsumer.accept(order);
        }
        return order;
    }
}
