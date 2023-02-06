package ru.yandex.autotests.market.checkouter.api.red;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.checkouter.beans.Color;
import ru.yandex.autotests.market.checkouter.beans.common.OrderItem;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataItem;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.checkouter.client.body.response.order.OrderResponseBody;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.market.checkouter.beans.Status.DELIVERY;
import static ru.yandex.autotests.market.checkouter.beans.Status.PENDING;
import static ru.yandex.autotests.market.checkouter.beans.Status.PROCESSING;
import static ru.yandex.autotests.market.checkouter.beans.SubStatus.AWAIT_CONFIRMATION;

/**
 * @author kukabara
 */
@Aqua.Test(title = "Тест на создание Красного заказа")
@Features("RED")
@Issue("https://st.yandex-team.ru/MARKETCHECKOUT-6349")
@RunWith(Parameterized.class)
public class CreateRedOrderTest extends AbstractRedOrderTest {

    @Title("Создание, оплата, перевод в доставку Красного заказа")
    public void redOrderPayDelivery() {
        TestDataOrder order = checkoutRedOrder(shopId);

        paySteps.payOrder(order, sandbox);
        Long orderId = order.getId();
        OrderResponseBody orderById = ordersSteps.getOrderById(orderId);
        assertThat("No correct order status after Pay", orderById.getStatus(), equalTo(PENDING));
        assertThat("No correct order substatus after Pay", orderById.getSubStatus(), equalTo(AWAIT_CONFIRMATION));

        ordersStatusSteps.changeOrderStatus(order, PROCESSING);
        for (OrderItem orderItem : orderById.getItems()) {
            Assert.assertThat(orderItem.getCargoType(), Matchers.allOf(Matchers.notNullValue(), Matchers.not(0)));
            Assert.assertThat(orderItem.getItemDescriptionEnglish(), Matchers.not(Matchers.isEmptyOrNullString()));
        }

        ordersStatusSteps.changeOrderStatus(order, DELIVERY);

        orderById = ordersSteps.getOrderById(orderId);
        assertThat("No correct order status after Delivery", orderById.getStatus(), equalTo(DELIVERY));
    }

    @Test
    @Title("Создание, оплата, перевод в доставку Красного ФФ заказа")
    public void redFulfilmentOrderPayDelivery() {
        TestDataOrder order = checkoutRedFFOrder();

        Assert.assertThat(order.getRgb(), equalTo(Color.RED));
        Assert.assertThat(order.getFulfilment(), equalTo(true));

        for (TestDataItem orderItem : order.getItems()) {
            Assert.assertThat(orderItem.getItemDescriptionEnglish(), Matchers.not(Matchers.isEmptyOrNullString()));
        }

        paySteps.payOrder(order, sandbox);

        Long orderId = order.getId();
        order = ordersSteps.getOrderById(orderId).toEntityBean();
        Assert.assertThat("No correct order status after Pay", order.getStatus(), equalTo(PENDING));
        Assert.assertThat("No correct order substatus after Pay", order.getSubStatus(), equalTo(AWAIT_CONFIRMATION));

        ordersStatusSteps.changeOrderStatus(order, PROCESSING);
        ordersStatusSteps.changeOrderStatus(order, DELIVERY);

        order = ordersSteps.getOrderById(orderId).toEntityBean();
        assertThat("No correct order status after Delivery", order.getStatus(), equalTo(DELIVERY));
    }

}
