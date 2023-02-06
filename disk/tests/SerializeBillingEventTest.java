package ru.yandex.chemodan.app.eventloader.serializer.tests;

import org.junit.Test;

import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.TestProduct;
import ru.yandex.chemodan.eventlog.events.billing.BillingEvent;
import ru.yandex.chemodan.eventlog.events.billing.BillingEventType;
import ru.yandex.chemodan.eventlog.events.billing.Payment;
import ru.yandex.chemodan.eventlog.events.billing.PaymentStatus;
import ru.yandex.chemodan.eventlog.events.billing.Product;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class SerializeBillingEventTest extends AbstractSerializeEventTest {
    private static final Product PRODUCT = TestProduct.PAID_10GB_1M_2014.product;

    private static final Payment PAYMENT = new Payment("RUB", "30", PaymentStatus.NONE);

    private static final Payment PAYMENT_WITH_STATUS =
            new Payment("RUB", "30", new PaymentStatus("success", "code"));

    @Test
    public void testOrderNew() {
        new ExpectedJson()
                .withProduct(PRODUCT)
                .withPayment(PAYMENT)
                .serializeAndCheck(new BillingEvent(BillingEventType.ORDER_NEW, METADATA, PRODUCT, PAYMENT, true));
    }

    @Test
    public void testBuyNew() {
        new ExpectedJson()
                .withProduct(PRODUCT)
                .withPayment(PAYMENT)
                .with("reason", "code")
                .serializeAndCheck(
                        new BillingEvent(BillingEventType.BUY_NEW, METADATA, PRODUCT, PAYMENT_WITH_STATUS, true)
                );
    }

    @Test
    public void testSubscribe() {
        new ExpectedJson()
                .withEventType(EventType.BILLING_SUBSCRIBE)
                .withProduct(PRODUCT)
                .withPayment(PAYMENT)
                .with("reason", "code")
                .serializeAndCheck(
                        new BillingEvent(BillingEventType.BUY_NEW, METADATA, PRODUCT, PAYMENT_WITH_STATUS, true)
                );
    }

    @Test
    public void testProlong() {
        new ExpectedJson()
                .withProduct(PRODUCT)
                .withPayment(PAYMENT)
                .with("reason", "code")
                .serializeAndCheck(
                        new BillingEvent(BillingEventType.PROLONG, METADATA, PRODUCT, PAYMENT_WITH_STATUS, true)
                );
    }

    @Test
    public void testUnsubscribe() {
        new ExpectedJson()
                .withProduct(PRODUCT)
                .withPayment(PAYMENT)
                .serializeAndCheck(
                        new BillingEvent(BillingEventType.UNSUBSCRIBE, METADATA, PRODUCT, PAYMENT, false)
                );
    }

    @Test
    public void testDelete() {
        new ExpectedJson()
                .withProduct(PRODUCT)
                .withPayment(PAYMENT)
                .serializeAndCheck(new BillingEvent(BillingEventType.DELETE, METADATA, PRODUCT, PAYMENT, true));
    }
}
