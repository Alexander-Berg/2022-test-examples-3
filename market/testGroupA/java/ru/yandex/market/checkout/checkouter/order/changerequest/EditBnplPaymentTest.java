package ru.yandex.market.checkout.checkouter.order.changerequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BnplTestProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;

/**
 * @author : poluektov
 * date: 2021-07-15.
 */
public class EditBnplPaymentTest extends AbstractWebTestBase {

    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private OrderPayHelper payHelper;

    @BeforeEach
    public void initMocks() {
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
    }

    @Test
    public void shouldChangeBnplFlag() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.setPaymentMethod(YANDEX);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order = orderCreateHelper.createOrder(parameters);

        OrderEditRequest editRequest = new OrderEditRequest();
        PaymentEditRequest paymentEdit = new PaymentEditRequest();
        paymentEdit.setPaymentMethod(YANDEX);
        paymentEdit.setBnpl(false);
        editRequest.setPaymentEditRequest(paymentEdit);
        client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID, singletonList(BLUE), editRequest);

        order = orderService.getOrder(order.getId());
        assertFalse(order.isBnpl());

        order.getItems().forEach(item -> assertFalse(item.isBnpl()));
    }

    @Test
    void shouldChangeFlagBackOnRebind() {
        var parameters = BnplTestProvider.defaultBnplParameters();
        var order = orderCreateHelper.createOrder(parameters);
        var bnplPayment = payHelper.payForOrderWithoutNotification(order);

        // bnpl = false после смены способа оплаты.
        OrderEditRequest editRequest = new OrderEditRequest();
        PaymentEditRequest paymentEdit = new PaymentEditRequest();
        paymentEdit.setPaymentMethod(YANDEX);
        paymentEdit.setBnpl(false);
        editRequest.setPaymentEditRequest(paymentEdit);
        client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID, singletonList(BLUE), editRequest);
        order = orderService.getOrder(order.getId());
        payHelper.payForOrderWithoutNotification(order);
        assertFalse(order.isBnpl());
        order.getItems().forEach(item -> assertFalse(item.isBnpl()));

        // bnpl = true после ребинда заказа обратно на bnpl платеж.
        payHelper.notifyBnplPayment(bnplPayment);
        order = orderService.getOrder(order.getId());
        assertTrue(order.isBnpl());
        order.getItems().forEach(item -> assertTrue(item.isBnpl()));
    }
}
