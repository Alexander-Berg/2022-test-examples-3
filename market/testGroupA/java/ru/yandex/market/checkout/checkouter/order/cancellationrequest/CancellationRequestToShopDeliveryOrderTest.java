package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.ClientHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PICKUP_EXPIRED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHOP_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;


public class CancellationRequestToShopDeliveryOrderTest extends AbstractWebTestBase {

    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;

    @Test
    public void checkForShopCanCancelFromProcessing() throws Exception {
        Order order = createShopDeliveryOrderAndProceedToProcessing();
        ClientInfo clientInfo = ClientHelper.shopClientFor(order);
        shouldCancel(order, clientInfo, USER_CHANGED_MIND);
    }

    @Test
    public void checkForShopCanCancelFromDelivery() throws Exception {
        Order order = createShopDeliveryOrderAndProceedToDelivery();
        ClientInfo clientInfo = ClientHelper.shopClientFor(order);
        shouldCancel(order, clientInfo, SHOP_FAILED);
    }

    @Test
    public void checkForShopCanCancelFromPickup() throws Exception {
        Order order = createShopDeliveryOrderAndProceedToPickup();
        ClientInfo clientInfo = ClientHelper.shopClientFor(order);
        shouldCancel(order, clientInfo, PICKUP_EXPIRED);
    }

    private Order createShopDeliveryOrderAndProceedToPickup() {
        Order order = createShopDeliveryOrderAndProceedToDelivery();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);
        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), is(PICKUP));
        return order;
    }

    private Order createShopDeliveryOrderAndProceedToDelivery() {
        Order order = createShopDeliveryOrderAndProceedToProcessing();
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), is(DELIVERY));
        return order;
    }

    private Order createShopDeliveryOrderAndProceedToProcessing() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder().build());
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDelivery());

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), is(PROCESSING));
        return order;
    }

    private void createShopCancellationRequest(Order order, ClientInfo clientInfo, OrderSubstatus substatus)
            throws Exception {
        CancellationRequest cancellationRequest = new CancellationRequest(substatus, "notes");
        cancellationRequestHelper.createCancellationRequestByEditApi(order.getId(), cancellationRequest, clientInfo);
    }

    private void shouldCancel(Order order, ClientInfo clientInfo, OrderSubstatus substatus) throws Exception {
        createShopCancellationRequest(order, clientInfo, substatus);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(CANCELLED));
        assertThat(order.getSubstatus(), equalTo(substatus));
        assertThat(order.getCancellationRequest().getSubstatus(), equalTo(substatus));
    }
}
