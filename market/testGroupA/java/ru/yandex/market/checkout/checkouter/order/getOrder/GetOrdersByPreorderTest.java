package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Collection;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByPreorderTest extends AbstractWebTestBase {

    private Order notPreorder;
    private Order preorder;

    @BeforeAll
    public void setUp() {
        notPreorder = orderCreateHelper.createOrder(new Parameters(OrderProvider.getBlueOrder(o -> {
            o.getItems().forEach(oi -> oi.setPreorder(false));
        })));
        assertThat(notPreorder.isPreorder(), is(false));

        Parameters parameters = new Parameters(OrderProvider.getBlueOrder(o -> {
            o.getItems().forEach(oi -> oi.setPreorder(true));
        }));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        preorder = orderCreateHelper.createOrder(parameters);
        assertThat(preorder.isPreorder(), is(true));
    }

    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @Test
    public void shouldFilterByPreorder() {
        OrderSearchRequest request = new OrderSearchRequest();
        request.preorder = true;

        Collection<Long> preorderIds = orderService.getOrderIds(request, ClientInfo.SYSTEM);
        assertThat(preorderIds, hasItem(preorder.getId()));
        assertThat(preorderIds, CoreMatchers.not(hasItem(notPreorder.getId())));
    }

    @Test
    public void shouldFilterByPreorderFalse() {
        OrderSearchRequest nonPreorderRequest = new OrderSearchRequest();
        nonPreorderRequest.preorder = false;

        Collection<Long> notPreorderIds = orderService.getOrderIds(nonPreorderRequest, ClientInfo.SYSTEM);
        assertThat(notPreorderIds, hasItem(notPreorder.getId()));
        assertThat(notPreorderIds, CoreMatchers.not(hasItem(preorder.getId())));
    }
}
