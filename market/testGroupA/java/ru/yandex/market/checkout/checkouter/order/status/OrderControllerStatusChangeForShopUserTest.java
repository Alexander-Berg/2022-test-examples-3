package ru.yandex.market.checkout.checkouter.order.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ParcelBoxHelper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class OrderControllerStatusChangeForShopUserTest extends AbstractWebTestBase {

    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ParcelBoxHelper parcelBoxHelper;
    private Function<Order, ClientInfo> clientInfoProvider;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{
                        ClientRole.SHOP,
                        (Function<Order, ClientInfo>) (o) -> new ClientInfo(ClientRole.SHOP, o.getShopId())
                },
                new Object[]{
                        ClientRole.SHOP_USER,
                        (Function<Order, ClientInfo>) (o) -> new ClientInfo(
                                ClientRole.SHOP_USER,
                                o.getUid(),
                                o.getShopId()
                        )
                }
        ).stream().map(Arguments::of);
    }

    @ParameterizedTest(name = "for {0}")
    @MethodSource("parameterizedTestData")
    public void pendingSubstatuses(ClientRole role, Function<Order, ClientInfo> clientInfoProvider) throws Exception {
        this.clientInfoProvider = clientInfoProvider;
        Order order = dropshipDeliveryHelper.createDropshipOrder();
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertThat(order.getSubstatus(), is(OrderSubstatus.STARTED));

        createBoxes(order);

        order = changeStatus(order, OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP);
        assertThat(order.getSubstatus(), is(OrderSubstatus.READY_TO_SHIP));

        order = changeStatus(order, OrderStatus.PROCESSING, OrderSubstatus.SHIPPED);
        assertThat(order.getSubstatus(), is(OrderSubstatus.SHIPPED));
    }

    @ParameterizedTest(name = "for {0}")
    @MethodSource("parameterizedTestData")
    public void cancelFromStarted(ClientRole role, Function<Order, ClientInfo> clientInfoProvider) throws Exception {
        this.clientInfoProvider = clientInfoProvider;
        Order order = dropshipDeliveryHelper.createDropshipOrder();
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertThat(order.getSubstatus(), is(OrderSubstatus.STARTED));

        order = changeStatus(order, OrderStatus.CANCELLED, OrderSubstatus.SHOP_FAILED);
        assertThat(order.getStatus(), is(OrderStatus.CANCELLED));
        assertThat(order.getSubstatus(), is(OrderSubstatus.SHOP_FAILED));
    }

    @ParameterizedTest(name = "for {0}")
    @MethodSource("parameterizedTestData")
    public void cancelFromReadyToShip(ClientRole role, Function<Order, ClientInfo> clientInfoProvider)
            throws Exception {
        this.clientInfoProvider = clientInfoProvider;
        Order order = dropshipDeliveryHelper.createDropshipOrder();
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertThat(order.getSubstatus(), is(OrderSubstatus.STARTED));

        createBoxes(order);

        order = changeStatus(order, OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP);
        assertThat(order.getSubstatus(), is(OrderSubstatus.READY_TO_SHIP));

        order = changeStatus(order, OrderStatus.CANCELLED, OrderSubstatus.SHOP_FAILED);
        assertThat(order.getStatus(), is(OrderStatus.CANCELLED));
        assertThat(order.getSubstatus(), is(OrderSubstatus.SHOP_FAILED));
    }

    @ParameterizedTest(name = "for {0}")
    @MethodSource("parameterizedTestData")
    public void cancelFromShipped(ClientRole role, Function<Order, ClientInfo> clientInfoProvider) throws Exception {
        this.clientInfoProvider = clientInfoProvider;
        Order order = dropshipDeliveryHelper.createDropshipOrder();
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertThat(order.getSubstatus(), is(OrderSubstatus.STARTED));

        createBoxes(order);

        order = changeStatus(order, OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP);
        assertThat(order.getSubstatus(), is(OrderSubstatus.READY_TO_SHIP));

        order = changeStatus(order, OrderStatus.PROCESSING, OrderSubstatus.SHIPPED);
        assertThat(order.getSubstatus(), is(OrderSubstatus.SHIPPED));

        order = changeStatus(order, OrderStatus.CANCELLED, OrderSubstatus.SHOP_FAILED);
        assertThat(order.getStatus(), is(OrderStatus.CANCELLED));
        assertThat(order.getSubstatus(), is(OrderSubstatus.SHOP_FAILED));
    }

    private Order changeStatus(Order order, OrderStatus cancelled, OrderSubstatus shopFailed) {
        orderStatusHelper.updateOrderStatus(
                order.getId(),
                clientInfoProvider.apply(order),
                cancelled,
                shopFailed
        );
        order = orderService.getOrder(order.getId());
        return order;
    }

    private void createBoxes(Order order) throws Exception {
        ParcelBox parcelBox = parcelBoxHelper.provideOneBoxForOrder(order);

        parcelBoxHelper.putBoxes(
                order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                Collections.singletonList(parcelBox),
                clientInfoProvider.apply(order)
        );
    }

}
