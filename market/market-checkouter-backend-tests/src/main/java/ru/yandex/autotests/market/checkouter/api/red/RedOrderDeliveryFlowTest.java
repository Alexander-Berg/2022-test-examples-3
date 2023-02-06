package ru.yandex.autotests.market.checkouter.api.red;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.checkouter.api.data.requests.orders.delivery.DeliveryChangeRequests;
import ru.yandex.autotests.market.checkouter.api.steps.OrderDeliverySteps;
import ru.yandex.autotests.market.checkouter.beans.Color;
import ru.yandex.autotests.market.checkouter.beans.DeliveryServiceStatus;
import ru.yandex.autotests.market.checkouter.beans.ParcelItem;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.SubStatus;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataShipment;
import ru.yandex.autotests.market.checkouter.client.body.response.order.OrderResponseBody;
import ru.yandex.autotests.market.common.wait.FluentWait;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@Aqua.Test(title = "Тест на интеграцию с Красной доставкой")
@Features("RED")
@Issue("https://st.yandex-team.ru/MARKETCHECKOUT-9633")
public class RedOrderDeliveryFlowTest extends AbstractRedOrderTest {
    private static final Duration MDB_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration POLLING_TIMEOUT = Duration.ofSeconds(5);

    private OrderDeliverySteps deliverySteps = new OrderDeliverySteps();

    @Test
    public void shouldCreateCrossdockDeliveryOrder() {
        TestDataOrder order = checkoutRedOrder(shopId);
        assertThat("Not red order", order.getRgb(), equalTo(Color.RED));
        assertThat("Not crossdock order", order.getFulfilment(), equalTo(false));

        paySteps.payOrder(order, false);
        Long orderId = order.getId();
        OrderResponseBody orderById = ordersSteps.getOrderById(orderId);
        assertThat("No correct status after Pay", orderById.getStatus(), equalTo(Status.PENDING));
        assertThat("No correct substatus after Pay", orderById.getSubStatus(), equalTo(SubStatus.AWAIT_CONFIRMATION));

        ordersStatusSteps.changeOrderStatus(order, Status.PROCESSING);

        Long parcelId = order.getDelivery().getParcels().get(0).getId();
        assertThat("No parcel id was specified", parcelId, notNullValue());

        OrderResponseBody orderResponse = deliverySteps.changeDeliveryByRequest(new DeliveryChangeRequests(orderId)
                .addParcelItems(parcelId, makeParcelItemsForOrder(order)));

        assertThat("Parcel items were not saved", orderResponse.getDelivery().getParcels().get(0).getItems(), hasSize(order.getItems().size()));

        awaitParcelStatus(orderId, DeliveryServiceStatus.CREATED, DeliveryServiceStatus.READY_TO_SHIP);

        order = ordersSteps.getOrderById(orderId).toEntityBean();

        TestDataShipment parcel = order.getDelivery().getParcels().get(0);
        Assert.assertThat(parcel.getTracks(), hasSize(1));

        awaitParcelStatus(orderId, DeliveryServiceStatus.READY_TO_SHIP);

        order = ordersSteps.getOrderById(orderId).toEntityBean();

        parcel = order.getDelivery().getParcels().get(0);
        Assert.assertThat(parcel.getLabelURL(), notNullValue());

    }

    @Test
    public void shouldCreateFulfilmentDeliveryOrder() {
        TestDataOrder order = checkoutRedFFOrder();
        assertThat("Not red order", order.getRgb(), equalTo(Color.RED));
        assertThat("Not fulfilment order", order.getFulfilment(), equalTo(true));

        paySteps.payOrder(order, false);
        Long orderId = order.getId();
        OrderResponseBody orderById = ordersSteps.getOrderById(orderId);
        assertThat("No correct status after Pay", orderById.getStatus(), equalTo(Status.PENDING));
        assertThat("No correct substatus after Pay", orderById.getSubStatus(), equalTo(SubStatus.AWAIT_CONFIRMATION));

        ordersStatusSteps.changeOrderStatus(order, Status.PROCESSING);

        awaitParcelStatus(orderId, DeliveryServiceStatus.READY_TO_SHIP);

        order = ordersSteps.getOrderById(orderId).toEntityBean();
        assertThat("Not enough tracks after READY_TO_SHIP", order.getDelivery().getParcels().get(0).getTracks(), hasSize(2));
    }

    private void awaitParcelStatus(Long orderId, DeliveryServiceStatus... parcelStatuses) {
        new FluentWait<>(orderId)
                .withTimeout(MDB_TIMEOUT)
                .pollingEvery(POLLING_TIMEOUT)
                .until((Predicate<Long>) id -> {
                    OrderResponseBody tmpOrder = ordersSteps.getOrderById(id);
                    DeliveryServiceStatus parcelStatus = tmpOrder.getDelivery().getParcels().get(0).getStatus();
                    return Stream.of(parcelStatuses).anyMatch(Predicate.isEqual(parcelStatus));
                });
    }

    private List<ParcelItem> makeParcelItemsForOrder(TestDataOrder order) {
        return order.getItems().stream()
                .map(oi -> {
                    ParcelItem parcelItem = new ParcelItem();
                    parcelItem.setCount(oi.getCount());
                    parcelItem.setItemId(oi.getId());
                    return parcelItem;
                }).collect(Collectors.toList());
    }
}
