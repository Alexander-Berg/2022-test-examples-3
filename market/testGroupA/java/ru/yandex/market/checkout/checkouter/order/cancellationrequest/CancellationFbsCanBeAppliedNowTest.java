package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.BaseStatusModelTest;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestProcessor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.CALL_CENTER_OPERATOR;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SHOP;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SHOP_USER;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SYSTEM;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.UNKNOWN;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.USER;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;

/**
 * Правила мгновенной отмены описаны в https://wiki.yandex-team.ru/market/assess/dscancellation/
 */
public class CancellationFbsCanBeAppliedNowTest extends BaseStatusModelTest {

    @Autowired
    private CancellationRequestProcessor cancellationRequestProcessor;

    public static Stream<Arguments> parameterizedFBSTestData() {
        var params = new ArrayList<Object[]>();
        Arrays.stream(new Object[]{USER, SHOP, SHOP_USER, CALL_CENTER_OPERATOR, SYSTEM})
                .forEach(r -> params.addAll(
                        Arrays.stream(new Object[][]{
                                        new Object[]{OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION, true, r},
                                        new Object[]{OrderStatus.PENDING, OrderSubstatus.ANTIFRAUD, true, r},
                                        new Object[]{OrderStatus.PROCESSING, OrderSubstatus.STARTED, true, r},
                                        new Object[]{OrderStatus.PROCESSING, OrderSubstatus.PACKAGING, true, r}})
                                .collect(Collectors.toList())
                ));
        Arrays.stream(new Object[]{USER, CALL_CENTER_OPERATOR, SYSTEM})
                .forEach(r -> params.addAll(
                        Arrays.stream(new Object[][]{
                                new Object[]{OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP, false, r}
                        }).collect(Collectors.toList())
                ));
        Arrays.stream(new Object[]{SHOP, SHOP_USER})
                .forEach(r -> params.addAll(
                        Arrays.stream(new Object[][]{
                                new Object[]{OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP, true, r}
                        }).collect(Collectors.toList())
                ));
        Arrays.stream(ClientRole.values()).filter(role -> role != UNKNOWN)
                .forEach(r -> {
                    params.addAll(
                            Arrays.stream(new Object[][]{
                                    new Object[]{OrderStatus.PROCESSING, OrderSubstatus.SHIPPED, false, r}
                            }).collect(Collectors.toList()));
                    params.addAll(Arrays.stream(new OrderStatus[]{OrderStatus.DELIVERY, OrderStatus.PICKUP})
                            .flatMap(os -> Arrays.stream(OrderSubstatus.values()).filter(oss -> oss.getStatus() == os))
                            .map(oss -> new Object[]{oss.getStatus(), oss, false, r})
                            .collect(Collectors.toList())
                    );
                });
        return params.stream().map(Arguments::of);
    }

    @ParameterizedTest()
    @MethodSource("parameterizedFBSTestData")
    public void canBeAppliedNowTest(OrderStatus fromStatus,
                                    OrderSubstatus fromSubstatus,
                                    boolean shouldBeApplied,
                                    ClientRole clientRole) {
        var order = createFbsOrderWithStatus(fromStatus, fromSubstatus);
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

    private Order createFbsOrderWithStatus(OrderStatus status, OrderSubstatus substatus) {
        var order = new Order();
        order.setStatus(status);
        order.setSubstatus(substatus);
        order.setRgb(BLUE);
        order.setFulfilment(false);
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        order.setDelivery(delivery);
        return order;
    }
}
