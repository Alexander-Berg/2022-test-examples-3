package ru.yandex.market.checkout.checkouter.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.order.reservation.OrderCompletionService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.BasketItemPartition;
import ru.yandex.market.checkout.checkouter.pay.IncomingPaymentPartition;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentAgent;
import ru.yandex.market.checkout.checkouter.pay.PaymentMarkup;
import ru.yandex.market.checkout.checkouter.pay.PaymentPartition;
import ru.yandex.market.checkout.checkouter.pay.PaymentPartitions;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItemPartition;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentWritingDao;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.pay.builders.PrepayPaymentBuilder.PLACEHOLDER_PAYMENT_ID;

public class CheckouterClientPaymentTest extends AbstractWebTestBase {
    @Autowired
    private OrderCompletionService orderCompletionService;
    @Autowired
    private PaymentWritingDao paymentWritingDao;
    @Autowired
    private Storage storage;

    @BeforeEach
    public void initTrustMock() {
        trustMockConfigurer.mockWholeTrust();
    }


    @Test
    public void canCreatePaymentWithNullCardId() {
        Order order = prepareOrder();

        Payment payment = client.payments().payOrder(order.getId(),
                order.getBuyer().getUid(),
                "https://market-test.pepelac1ft.yandex.ru/payment/status/" +
                        PLACEHOLDER_PAYMENT_ID + "/",
                null,
                true,
                null);
        checkPayment(payment);
    }

    @Test
    public void canCreatePaymentWithCardId() {
        Order order = prepareOrder();

        Payment payment = client.payments().payOrder(order.getId(),
                order.getBuyer().getUid(),
                "https://market-test.pepelac1ft.yandex.ru/payment/status/" +
                        PLACEHOLDER_PAYMENT_ID + "/",
                "some_CardId-xx55",
                true,
                null);
        checkPayment(payment);
    }

    @Test
    public void canCreatePayment() {
        Order order = prepareOrder();
        Payment payment = client.payments().payOrder(order.getId(),
                order.getBuyer().getUid(),
                "https://market-test.pepelac1ft.yandex.ru/payment/status/" +
                        PLACEHOLDER_PAYMENT_ID + "/", null, null, true,
                null);
        checkPayment(payment);
    }

    /**
     * Check partition output within payment object for external clients
     */
    @Test
    public void checkPaymentPartitionOutputTest() {
        Order order = prepareOrder();

        Payment payment = client.payments().payOrder(order.getId(),
                order.getBuyer().getUid(),
                "https://market-test.pepelac1ft.yandex.ru/payment/status/" +
                        PLACEHOLDER_PAYMENT_ID + "/",
                "some_CardId-xx55",
                true,
                null);

        BigDecimal spasiboTotalAmount = BigDecimal.ONE;
        payment.addPartition(new PaymentPartition(
                PaymentAgent.DEFAULT,
                payment.getTotalAmount().subtract(spasiboTotalAmount)
        ));
        payment.addPartition(new PaymentPartition(PaymentAgent.SBER_SPASIBO, spasiboTotalAmount));

        storage.updateEntityGroup(orderService.entityGroup(order.getId()), () -> {
            paymentWritingDao.updatePaymentPartition(payment, ClientInfo.SYSTEM);
            return null;
        });

        //Serialization/Deserialization check
        Order orderFromClient = client.getOrder(order.getId(), ClientRole.SYSTEM, null);

        assertEquals(BigDecimal.ONE, orderFromClient.getPayment().amountByAgent(PaymentAgent.SBER_SPASIBO));
    }

    @Disabled // сломался при обновлении PG 10->12; поменялся порядок элементов в markup.content, тест стал падать
    @Test
    public void canMarkup() throws Exception {
        Order order = prepareOrder(false);
        OrderItem item1 = order.getItems().iterator().next();

        Payment payment = client.payments().payOrder(order.getId(),
                order.getBuyer().getUid(),
                "https://market-test.pepelac1ft.yandex.ru/payment/status/" +
                        PLACEHOLDER_PAYMENT_ID + "/",
                "some_CardId-xx55",
                false,
                null);

        PaymentPartitions paymentPartitions = new PaymentPartitions(new ArrayList<IncomingPaymentPartition>() {{
            add(new IncomingPaymentPartition(PaymentAgent.SBER_SPASIBO, BigDecimal.TEN));
        }});

        PaymentMarkup markup = client.payments().markupPayment(payment.getBasketKey().getPurchaseToken(),
                paymentPartitions);

        BasketItemPartition basketItemPartition = markup.getContent().stream()
                .filter(i -> i.getOrderItemServiceOrderId().contains("item"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("partition not found"));

        ReceiptItemPartition partition = basketItemPartition.getPartitions().stream()
                .filter(p -> PaymentAgent.SBER_SPASIBO == p.getPaymentAgent()).findFirst().orElse(null);

        Assertions.assertEquals(
                String.format("%d-item-%d", order.getId(), item1.getId()),
                basketItemPartition.getOrderItemServiceOrderId()
        );
        Assertions.assertEquals(BigDecimal.TEN, partition.getAmount());

        Payment paymentFromDB = client.payments().getPayment(payment.getId(), ClientRole.SYSTEM, null);
        Assertions.assertEquals(BigDecimal.TEN, paymentFromDB.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(payment.getTotalAmount().subtract(BigDecimal.TEN),
                paymentFromDB.amountByAgent(PaymentAgent.DEFAULT));

        BasketItemPartition basketItemPartitionForDelivery = markup.getContent().stream()
                .filter(i -> i.getOrderItemServiceOrderId().contains("delivery"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("partition not found"));

        Assertions.assertEquals(
                String.format("%d-delivery", order.getId()),
                basketItemPartitionForDelivery.getOrderItemServiceOrderId()
        );

        ReceiptItemPartition deliveryPartitionSpasibo = basketItemPartitionForDelivery.getPartitions().stream()
                .filter(p -> PaymentAgent.SBER_SPASIBO == p.getPaymentAgent()).findFirst().orElse(null);
        Assertions.assertNull(deliveryPartitionSpasibo);

        ReceiptItemPartition deliveryPartitionYandex = basketItemPartitionForDelivery.getPartitions().stream()
                .filter(p -> PaymentAgent.DEFAULT == p.getPaymentAgent()).findFirst().orElse(null);

        Assertions.assertEquals(BigDecimal.valueOf(100), deliveryPartitionYandex.getAmount());
    }

    @Test
    public void testGetReceiptsWithMarkup() {
        Order order = prepareOrder(false);
        Order order2 = prepareOrder(false);

        Payment payment = client.payments().payOrder(order.getId(),
                order.getBuyer().getUid(),
                "https://market-test.pepelac1ft.yandex.ru/payment/status/" +
                        PLACEHOLDER_PAYMENT_ID + "/",
                "some_CardId-xx55",
                false,
                null);

        Payment payment2 = client.payments().payOrder(order2.getId(),
                order2.getBuyer().getUid(),
                "https://market-test.pepelac1ft.yandex.ru/payment/status/" +
                        PLACEHOLDER_PAYMENT_ID + "/",
                "some_CardId-xx55",
                false,
                null);

        PaymentPartitions paymentPartitions = new PaymentPartitions(new ArrayList<IncomingPaymentPartition>() {{
            add(new IncomingPaymentPartition(PaymentAgent.SBER_SPASIBO, BigDecimal.TEN));
        }});

        client.payments().markupPayment(payment.getBasketKey().getPurchaseToken(), paymentPartitions);
        client.payments().markupPayment(payment2.getBasketKey().getPurchaseToken(), paymentPartitions);

        List<Long> ids = new ArrayList<Long>() {{
            add(order.getId());
            add(order2.getId());
        }};

        Receipts receipts = client.getOrdersReceipts(ids, ClientRole.SYSTEM, null, null);
        Assertions.assertEquals(2, receipts.getContent().size());
        Assertions.assertEquals(4, receipts.getContent().stream()
                .flatMap(r -> r.getItems().stream())
                .filter(ri -> ri.getPartitions() != null)
                .flatMap(i -> i.getPartitions().stream()).count());
    }

    private Order prepareOrder() {
        return prepareOrder(true);
    }

    private Order prepareOrder(boolean fake) {
        Order order = OrderProvider.getPrepaidOrder();
        order.setFake(fake);

        shopService.updateMeta(order.getShopId(), ShopSettingsHelper.getDefaultMeta());

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        order = orderUpdateService.reserveOrder(orderId, String.valueOf(orderId), order.getDelivery());
        orderCompletionService.completeOrder(order, ClientInfo.SYSTEM);
        return order;
    }

    private void checkPayment(Payment payment) {
        assertEquals(PaymentStatus.INIT, payment.getStatus());
        assertNotNull(payment.getId());
        assertNull(payment.getFailReason());
    }

}
