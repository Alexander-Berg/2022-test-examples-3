package ru.yandex.market.checkout.checkouter.receipt;

import java.math.BigDecimal;
import java.util.Collections;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.StubMdsS3ServiceImpl;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.checkouter.pay.RefundReason.ORDER_CANCELLED;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.ACCEPTED;
import static ru.yandex.market.checkout.helpers.RefundHelper.assertRefund;

public class StorageReceiptServiceTest extends AbstractWebTestBase {

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private OrderStatusHelper orderStatusHelper;

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Создание GENERATED чека должно генерить RECEIPT_GENERATED эвент")
    @Test
    public void shouldGenerateReceiptEvent() {
        Order order = createTestOrder();
        Long orderId = order.getId();

        //сохранить GENERATED чек
        ReceiptItem receiptItem = new ReceiptItem(orderId);
        receiptItem.setAmount(BigDecimal.ONE);
        receiptItem.setCount(1);
        receiptItem.setItemId(Iterables.getFirst(order.getItems(), null).getId());
        receiptItem.setItemTitle("woop");
        receiptItem.setPrice(BigDecimal.ONE);
        Receipt receipt = new Receipt();
        receipt.setStatus(ReceiptStatus.GENERATED);
        receipt.setPaymentId(order.getPayment().getId());
        receipt.setType(ReceiptType.INCOME_RETURN);
        receipt.setItems(Collections.singletonList(receiptItem));
        long receiptId = receiptService.createReceipt(receipt, orderId);

        //проверяем что чек сохранен в базу
        receipt = receiptService.getReceipt(orderId, receiptId);
        Assertions.assertNotNull(receipt);
        Assertions.assertNotNull(receipt.getType());
        Assertions.assertEquals(ReceiptStatus.GENERATED, receipt.getStatus());

        //проверяем что есть эвент
        PagedEvents orderHistoryEvents = eventService.getPagedOrderHistoryEvents(
                orderId,
                Pager.atPage(1, 10),
                null,
                null,
                Collections.singleton(HistoryEventType.RECEIPT_GENERATED),
                false,
                ClientInfo.SYSTEM,
                null
        );
        Assertions.assertEquals(1, orderHistoryEvents.getItems().size());
        Assertions.assertEquals(
                HistoryEventType.RECEIPT_GENERATED,
                Iterables.getFirst(orderHistoryEvents.getItems(), null).getType()
        );
    }

    @Test
    public void shouldSaveMdsUrl() {
        Order order = createTestOrder();
        Receipt receipt = insertTestReceipt(order);
        receiptService.saveReceiptPdfToMds(receipt);
        receipt = receiptService.getReceipt(order.getId(), receipt.getId());
        assertThat(receipt.getPdfUrl(),
                equalTo(StubMdsS3ServiceImpl.STUB_MDS_URL_PREFIX + "receipt-" + receipt.getId() + ".pdf"));
    }

    @Test
    public void getByOrderIdAndReceiptIdTest() {
        Order order1 = createTestOrder();
        Receipt receipt1 = receiptService.findByOrder(order1.getId()).get(0);
        Order order2 = createTestOrder();
        Receipt receipt2 = receiptService.findByOrder(order2.getId()).get(0);

        Receipt receipt = receiptService.getReceipt(order1.getId(), receipt1.getId());
        assertNotNull(receipt);

        assertThrows(ReceiptNotFoundException.class, () -> {
            receiptService.getReceipt(order1.getId(), receipt2.getId());
        });
    }

    @Test
    @Disabled("Can't refund DELIVERED order(6) in BLUE market")
    public void shouldGenerateOnlyOneRefundReceiptEventPerMultiOrder() {
        var multiOrder = createTestMultiOrder();
        var order = multiOrder.getCarts().get(0);
        var orderId = order.getId();
        var refund = refundHelper.anyRefundFor(order, PaymentGoal.ORDER_PREPAY);
        assertRefund(refund, ACCEPTED, ORDER_CANCELLED);
        //сохранить GENERATED чек
        var receipt = new Receipt();
        receipt.setStatus(ReceiptStatus.GENERATED);
        receipt.setType(ReceiptType.INCOME_RETURN);
        receipt.setRefundId(refund.getId());
        long receiptId = receiptService.createReceipt(receipt, orderId);
        //проверяем что чек сохранен в базу
        receipt = receiptService.getReceipt(orderId, receiptId);
        Assertions.assertNotNull(receipt);
        Assertions.assertNotNull(receipt.getType());
        Assertions.assertEquals(ReceiptStatus.GENERATED, receipt.getStatus());
        //проверяем что есть эвент
        var orderHistoryEvents = eventService.getPagedOrderHistoryEvents(
                orderId,
                Pager.atPage(1, 10),
                null,
                null,
                Collections.singleton(HistoryEventType.RECEIPT_GENERATED),
                false,
                ClientInfo.SYSTEM,
                null
        );
        Assertions.assertEquals(1, orderHistoryEvents.getItems().size());
        Assertions.assertEquals(
                HistoryEventType.RECEIPT_GENERATED,
                Iterables.getFirst(orderHistoryEvents.getItems(), null).getType()
        );
    }

    private Receipt insertTestReceipt(Order order) {
        Receipt receipt = new Receipt();
        receipt.setTrustPayload("someunreadabletrustpayload");
        receipt.setType(ReceiptType.INCOME);
        receipt.setStatus(ReceiptStatus.PRINTED);
        receipt.setPaymentId(order.getPaymentId());
        receiptService.createReceipt(receipt, order.getId());
        return receipt;
    }

    private Order createTestOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.addShopMetaData(
                parameters.getOrder().getShopId(),
                ShopSettingsHelper.getDefaultMeta()
        );

        Order order = orderCreateHelper.createOrder(parameters);
        Long orderId = order.getId();

        //оплатить заказ
        orderPayHelper.payForOrder(order);
        return orderService.getOrder(orderId);
    }

    private MultiOrder createTestMultiOrder() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParameters());
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.addShopMetaData(
                parameters.getOrder().getShopId(),
                ShopSettingsHelper.getDefaultMeta()
        );
        var multiOrder = orderCreateHelper.createMultiOrder(parameters);
        var order = multiOrder.getCarts().get(0);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        orderPayHelper.refund(order);
        return multiOrder;
    }
}
