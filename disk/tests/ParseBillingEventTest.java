package ru.yandex.chemodan.eventlog.log.tests;

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
public class ParseBillingEventTest extends AbstractParseEventTest {
    private static final Payment PAYMENT = new Payment("RUB", "200", PaymentStatus.NONE);

    private static final Payment SUCCESS_PAYMENT =
            new Payment("RUB", "200", new PaymentStatus("success"));

    private static final Payment ERROR_PAYMENT =
            new Payment("RUB", "200", new PaymentStatus("error", "error_code"));

    private static final Product PRODUCT = TestProduct.PAID_1TB_1Y_2015.product;

    private static final String TSKV_LINE = TestProduct.PAID_1TB_1Y_2015 +
            "\tcurrency=" + PAYMENT.currency +
            "\tprice=" + PAYMENT.price +
            "\tauto=False";

    private static final String SUCCESS_TSKV_LINE = TSKV_LINE + "\tstatus=success\tstatus_code=";

    private static final String ERROR_TSKV_LINE = TSKV_LINE + "\tstatus=error\tstatus_code=error_code";

    @Test
    public void testOrderNew() {
        assertParseEquals(
                UID, "billing-order-new", TSKV_LINE,
                new BillingEvent(BillingEventType.ORDER_NEW, EVENT_METADATA, PRODUCT, PAYMENT, false)
        );
    }

    @Test
    public void testBuyNew() {
        assertParseEquals(
                UID, "billing-buy-new", SUCCESS_TSKV_LINE,
                new BillingEvent(BillingEventType.BUY_NEW, EVENT_METADATA, PRODUCT, SUCCESS_PAYMENT, false),
                EventType.BILLING_BUY_NEW
        );
    }

    @Test
    public void testBuyNewError() {
        assertParseEquals(
                UID, "billing-buy-new", ERROR_TSKV_LINE,
                new BillingEvent(BillingEventType.BUY_NEW, EVENT_METADATA, PRODUCT, ERROR_PAYMENT, false),
                EventType.SKIP
        );
    }

    @Test
    public void testProlong() {
        assertParseEquals(
                UID, "billing-prolong", SUCCESS_TSKV_LINE,
                new BillingEvent(BillingEventType.PROLONG, EVENT_METADATA, PRODUCT, SUCCESS_PAYMENT, false),
                EventType.BILLING_PROLONG
        );
    }

    @Test
    public void testProlongError() {
        assertParseEquals(
                UID, "billing-prolong", ERROR_TSKV_LINE,
                new BillingEvent(BillingEventType.PROLONG, EVENT_METADATA, PRODUCT, ERROR_PAYMENT, false),
                EventType.BILLING_PROLONG_ERROR
        );
    }

    @Test
    public void testDelete() {
        assertParseEquals(
                UID, "billing-delete", TSKV_LINE,
                new BillingEvent(BillingEventType.DELETE, EVENT_METADATA, PRODUCT, PAYMENT, false)
        );
    }

    @Test
    public void testUnsubscribe() {
        assertParseEquals(
                UID, "billing-unsubscribe", TSKV_LINE,
                new BillingEvent(BillingEventType.UNSUBSCRIBE, EVENT_METADATA, PRODUCT, PAYMENT, false)
        );
    }
}
