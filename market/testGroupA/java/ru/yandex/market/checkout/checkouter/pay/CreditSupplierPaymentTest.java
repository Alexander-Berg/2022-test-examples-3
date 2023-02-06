package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.service.ServiceOrderIdProvider;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsParamsProvider;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * @author : poluektov
 * date: 2019-05-20.
 */
public class CreditSupplierPaymentTest extends AbstractWebTestBase {

    @Autowired
    OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderHistoryEventsTestHelper eventsTestHelper;
    @Autowired
    private ServiceOrderIdProvider serviceOrderIdProvider;

    private Order order;
    private Payment basePayment;

    @BeforeEach
    public void createOrderWithCreditPayment() {
        Parameters parameterts = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(5000));
        parameterts.setPaymentMethod(PaymentMethod.CREDIT);
        order = orderCreateHelper.createOrder(parameterts);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        basePayment = order.getPayment();
    }

    @Test
    public void testCreditSupplierPaymentCreation() {
        Payment supplierPayment = paymentService.createAndBindSupplierPayment(basePayment.getId());
        assertThat(supplierPayment, notNullValue());
        assertThat(supplierPayment.getType(), equalTo(PaymentGoal.SUPPLIER_PAYMENT));

        Receipt receipt = receiptService.findByPayment(supplierPayment).iterator().next();
        assertThat(receipt.getStatus(), equalTo(ReceiptStatus.NEW));
        assertThat(receipt.getItems(), hasSize(order.getItems().size() + 1));

        loadOrderByPayment(supplierPayment.getId());
        List<OrderHistoryEvent> events =
                eventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.NEW_SUPPLIER_PAYMENT);
        assertThat(events, hasSize(1));
    }

    //https://st.yandex-team.ru/MARKETCHECKOUT-13784 - тест на багу.
    @Test
    public void testCreditSupplierPaymentCreationWithoutDuplication() {
        //создаем саплаерный платеж
        Payment supplierPayment = paymentService.createAndBindSupplierPayment(basePayment.getId());
        //создаем его еще раз и чекаем что чек и ивент выбивается один раз.
        paymentService.createAndBindSupplierPayment(basePayment.getId());
        assertThat(supplierPayment, notNullValue());
        assertThat(supplierPayment.getType(), equalTo(PaymentGoal.SUPPLIER_PAYMENT));
        List<Receipt> receipts = receiptService.findByPayment(supplierPayment);
        assertThat(receipts, hasSize(1));
        loadOrderByPayment(supplierPayment.getId());
        List<OrderHistoryEvent> events =
                eventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.NEW_SUPPLIER_PAYMENT);
        assertThat(events, hasSize(1));
    }

    @Test
    public void checkTrustCalls() {
        trustMockConfigurer.resetRequests();
        Payment supplierPayment = paymentService.createAndBindSupplierPayment(basePayment.getId());
        order = orderService.getOrder(order.getId());
        OneElementBackIterator<ServeEvent> iterator = trustMockConfigurer.eventsIterator();
        OrderItem orderItem = order.getItems().iterator().next();
        TrustCallsChecker.checkLoadPartnerCall(iterator, orderItem.getSupplierId());
        TrustCallsChecker.checkOptionalCreateServiceProductCall(iterator,
                TrustCallsParamsProvider.productFrom3PItem(orderItem));
        TrustCallsChecker.checkOptionalCreateServiceProductCall(iterator,
                TrustCallsParamsProvider.productFromBlueDelivery());
        //TODO: OrdersBatch придумать тулзу для генерации и поюзать. Продолжаем уходить от PaymentTestHelper-a
        iterator.next();
        order.getDelivery().setBalanceOrderId(serviceOrderIdProvider.orderDeliveryServiceOrderId(order.getId()));
        CreateBasketParams createBasket = TrustCallsParamsProvider.createBasketFulfilmentParams(order,
                supplierPayment.getId());
        createBasket.withPayMethodId("sberbank_credit");
        String customParams = "{\"origin_payment_id\":" + basePayment.getId() + ",\"call_preview_payment\":" +
                "\"card_info\"}";
        createBasket.withDeveloperPayload(customParams);
        createBasket.withPassParams(notNullValue(String.class));
        createBasket.withFiscalForce(1);
        TrustBasketKey key = TrustCallsChecker.checkCreateBasketCall(iterator, createBasket);
        TrustCallsChecker.checkPayBasketCall(iterator, order.getUid(), key);
    }

    @Test
    public void testTrustNotify() {
        Payment supplierPayment = paymentService.createAndBindSupplierPayment(basePayment.getId());
        order = orderService.getOrder(order.getId());
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPaymentClear(supplierPayment);

        supplierPayment = paymentService.getPayment(supplierPayment.getId(), ClientInfo.SYSTEM);
        assertThat(supplierPayment.getStatus(), equalTo(PaymentStatus.CLEARED));
        Receipt receipt = receiptService.findByPayment(supplierPayment).iterator().next();
        assertThat(receipt.getStatus(), equalTo(ReceiptStatus.PRINTED));
    }

    @Test
    public void getColorForExistingSupplierPayment() {
        Payment supplierPayment = paymentService.createAndBindSupplierPayment(basePayment.getId());

        Assertions.assertNotEquals(order.getPayment().getId(), supplierPayment.getId());
        final Color color = paymentService.getColor(supplierPayment);
        Assertions.assertEquals(Color.BLUE, color);
    }

    @Test
    public void testCreditSupplierPaymentCreationWithSameTrustToken() {
        List<Order> orders = new ArrayList<>();
        Parameters blueParameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(5000));
        blueParameters.setPaymentMethod(PaymentMethod.CREDIT);
        orders.add(orderCreateHelper.createOrder(blueParameters));
        Parameters whiteParameters = WhiteParametersProvider.simpleWhiteParameters();
        whiteParameters.setPaymentMethod(PaymentMethod.CREDIT);
        orders.add(orderCreateHelper.createOrder(whiteParameters));

        Payment payment = orderPayHelper.payForOrders(orders);

        Payment supplierPayment = paymentService.createAndBindSupplierPayment(payment.getId());
        assertThat(supplierPayment, notNullValue());
        assertThat(supplierPayment.getType(), equalTo(PaymentGoal.SUPPLIER_PAYMENT));

        Receipt receipt = receiptService.findByPayment(supplierPayment).iterator().next();
        Integer expectedReceiptItemsCount = orders.stream()
                .map(order -> order.getItems().size() + 1)
                .reduce(Integer::sum)
                .orElse(0);
        assertThat(receipt.getStatus(), equalTo(ReceiptStatus.NEW));
        assertThat(receipt.getItems(), hasSize(expectedReceiptItemsCount));

        Collection<Order> paidOrders = orderService.getOrdersByPayment(payment.getId(), ClientInfo.SYSTEM);
        assertThat(orders, hasSize(2));

        Set<Long> orderIds = paidOrders.stream().map(Order::getId).collect(Collectors.toSet());
        List<OrderHistoryEvent> events =
                eventsTestHelper.getEventsOfType(orderIds, HistoryEventType.NEW_SUPPLIER_PAYMENT);
        assertThat(events, hasSize(2));
    }

    private void loadOrderByPayment(long paymentId) {
        Collection<Order> orders = orderService.getOrdersByPayment(paymentId, ClientInfo.SYSTEM);
        assertThat(orders, hasSize(1));
        order = orders.iterator().next();
    }
}
