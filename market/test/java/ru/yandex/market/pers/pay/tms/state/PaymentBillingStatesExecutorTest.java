package ru.yandex.market.pers.pay.tms.state;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.market.pers.pay.MockUtils;
import ru.yandex.market.pers.pay.model.PaymentBillingEvent;
import ru.yandex.market.pers.pay.model.PersPayEntityState;
import ru.yandex.market.pers.pay.model.PersPaymentBillingEventType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pers.pay.model.PersPayState.ERROR;
import static ru.yandex.market.pers.pay.model.PersPayState.NEED_CHARGE;
import static ru.yandex.market.pers.pay.model.PersPayState.NEED_PAY;
import static ru.yandex.market.pers.pay.model.PersPayState.NEW;
import static ru.yandex.market.pers.pay.model.PersPayState.WAIT_CONTENT;
import static ru.yandex.market.pers.pay.model.PersPayerType.MARKET;
import static ru.yandex.market.pers.pay.model.PersPayerType.VENDOR;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 23.03.2021
 */
public class PaymentBillingStatesExecutorTest extends AbstractPaymentStatesExecutorTest {

    public static final int START_LAST_ID = -1;
    public static final int BATCH_SIZE = 100;
    public static final int INVALID_PAYER = -9999;
    @Autowired
    private PaymentContentStatesExecutor executorState;

    @Autowired
    private PaymentBillingStatesExecutor executorBilling;

    @Autowired
    private LogbrokerClientFactory clientFactory;

    @Test
    public void testEventQueueFillOk() {
        long[] payIds = {
            createTestPayment(MODEL_ID, USER_ID),
            createTestVendorPayment(MODEL_ID, USER_ID + 1, DATASOURCE_ID_STR),
            createTestVendorPayment(MODEL_ID, USER_ID + 2, DATASOURCE_ID + "wrong"),
            createTestVendorPayment(MODEL_ID, USER_ID + 3, DATASOURCE_ID_STR),
            createTestVendorPayment(MODEL_ID, USER_ID + 4, DATASOURCE_ID_STR), // would be expired
        };

        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);
        createModelGrade(MODEL_ID, USER_ID + 2, PersPayEntityState.APPROVED);
        createModelGrade(MODEL_ID, USER_ID + 3, PersPayEntityState.REJECTED);

        // make payment to instant reject
        updateToBeRejected(List.of(payIds[3]));

        executorState.processPaymentContentStates();

        // for correct ordering
        mockEventsOrdering();

        // check events are only in vendor queue
        assertEquals(0, tmsPaymentService.getBillingEvents(MARKET, START_LAST_ID, BATCH_SIZE).size());

        List<PaymentBillingEvent> billingEvents = getBillingEvents(START_LAST_ID, BATCH_SIZE);
        assertEquals(6, billingEvents.size());
        assertEvent(billingEvents.get(0), payIds[1], PersPaymentBillingEventType.HOLD, DATASOURCE_ID);
        assertEvent(billingEvents.get(1), payIds[2], PersPaymentBillingEventType.HOLD, INVALID_PAYER);
        assertEvent(billingEvents.get(2), payIds[2], PersPaymentBillingEventType.CHARGE, INVALID_PAYER);
        assertEvent(billingEvents.get(3), payIds[3], PersPaymentBillingEventType.HOLD, DATASOURCE_ID);
        assertEvent(billingEvents.get(4), payIds[3], PersPaymentBillingEventType.CANCEL, DATASOURCE_ID);
        assertEvent(billingEvents.get(5), payIds[4], PersPaymentBillingEventType.HOLD, DATASOURCE_ID);

        // add expired model
        jdbcTemplate.update("update pay.payment set cr_time = now() - interval '2' day where id = ?", payIds[4]);

        executorState.processPaymentContentStates();

        billingEvents = getBillingEvents(START_LAST_ID, BATCH_SIZE);
        assertEquals(7, billingEvents.size());
        assertEvent(billingEvents.get(5), payIds[4], PersPaymentBillingEventType.HOLD, DATASOURCE_ID);
        assertEvent(billingEvents.get(6), payIds[4], PersPaymentBillingEventType.CANCEL, DATASOURCE_ID);
    }


    @Test
    public void testEventQueuePaging() {
        long[] payIds = {
            createTestPayment(MODEL_ID, USER_ID),
            createTestVendorPayment(MODEL_ID, USER_ID + 1, DATASOURCE_ID_STR),
            createTestVendorPayment(MODEL_ID, USER_ID + 2, DATASOURCE_ID + "wrong"),
            createTestVendorPayment(MODEL_ID, USER_ID + 3, DATASOURCE_ID_STR),
        };

        executorState.processPaymentContentStates();

        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);
        createModelGrade(MODEL_ID, USER_ID + 2, PersPayEntityState.APPROVED);
        createModelGrade(MODEL_ID, USER_ID + 3, PersPayEntityState.REJECTED);

        // make payment to instant reject
        updateToBeRejected(List.of(payIds[3]));

        executorState.processPaymentContentStates();

        // for correct ordering
        mockEventsOrdering();

        int batchSize = 3;
        assertEquals(0, tmsPaymentService.getBillingEvents(MARKET, START_LAST_ID, batchSize).size());

        List<PaymentBillingEvent> billingEvents = getBillingEvents(START_LAST_ID, batchSize);
        assertEquals(3, billingEvents.size());
        assertEvent(billingEvents.get(0), payIds[1], PersPaymentBillingEventType.HOLD, DATASOURCE_ID);
        assertEvent(billingEvents.get(1), payIds[2], PersPaymentBillingEventType.HOLD, INVALID_PAYER);
        assertEvent(billingEvents.get(2), payIds[3], PersPaymentBillingEventType.HOLD, DATASOURCE_ID);

        // second page
        long lastId = billingEvents.stream().mapToLong(PaymentBillingEvent::getId).max().orElse(0L);
        billingEvents = getBillingEvents((int) lastId, BATCH_SIZE);
        assertEquals(2, billingEvents.size());
        assertEvent(billingEvents.get(0), payIds[2], PersPaymentBillingEventType.CHARGE, INVALID_PAYER);
        assertEvent(billingEvents.get(1), payIds[3], PersPaymentBillingEventType.CANCEL, DATASOURCE_ID);
    }

    @Test
    public void testStatesProcessing() {
        long payId = createTestVendorPayment(MODEL_ID, USER_ID + 1, DATASOURCE_ID_STR);
        createModelGrade(MODEL_ID, USER_ID + 1, PersPayEntityState.APPROVED);

        executorState.processPaymentContentStates();
        executorBilling.processPaymentCsBillingStates();

        // should have unbalanced record
        assertState(payId, NEED_CHARGE);
        assertEquals(List.of(WAIT_CONTENT, NEED_CHARGE),getHstStates(payId));

        // clean unbalanced record
        jdbcTemplate.update("delete from pay.unbalanced_pays where 1=1");

        executorBilling.processPaymentCsBillingStates();

        // now ready to pay
        assertState(payId, NEED_PAY);
        assertEquals(List.of(WAIT_CONTENT, NEED_CHARGE, NEED_PAY), getHstStates(payId));
    }

    @Test
    public void testSendEvents() throws Exception {
        long[] payIds = {
            createTestVendorPayment(MODEL_ID, USER_ID, DATASOURCE_ID_STR),
            createTestVendorPayment(MODEL_ID, USER_ID + 2, DATASOURCE_ID_STR + "wrong"),
            createTestVendorPayment(MODEL_ID, USER_ID + 3, DATASOURCE_ID_STR),
        };

        tmsPaymentService.savePayerEvents(List.of(payIds[0], payIds[1], payIds[2]), PersPaymentBillingEventType.HOLD);
        tmsPaymentService.savePayerEvents(List.of(payIds[2]), PersPaymentBillingEventType.CANCEL);
        tmsPaymentService.savePayerEvents(List.of(-1L), PersPaymentBillingEventType.HOLD); //unknown payment, ignore

        // for correct ordering
        mockEventsOrdering();

        ArgumentCaptor<byte[]> messageCaptor = MockUtils.mockLogbrokerWrite(clientFactory);

        assertEquals(-1, executorBilling.getEventsLastId());

        executorBilling.sendCsBillingEvents();

        List<String> events = MockUtils.parseCapturedMessagesRaw(messageCaptor);

        List<String> expected = List.of(
            "{\n" +
                "  \"refId\": perspay-" + payIds[0] + ",\n" +
                "  \"event\": HOLD,\n" +
                "  \"amount\": 10,\n" +
                "  \"serviceId\": 132,\n" +
                "  \"datasourceId\": " + DATASOURCE_ID_STR + ",\n" +
                "  \"timestampMs\": 0\n" +
                "}",
            "{\n" +
                "  \"refId\": perspay-" + payIds[2] + ",\n" +
                "  \"event\": HOLD,\n" +
                "  \"amount\": 10,\n" +
                "  \"serviceId\": 132,\n" +
                "  \"datasourceId\": " + DATASOURCE_ID_STR + ",\n" +
                "  \"timestampMs\": 0\n" +
                "}",
            "{\n" +
                "  \"refId\": perspay-" + payIds[2] + ",\n" +
                "  \"event\": CANCEL,\n" +
                "  \"amount\": 10,\n" +
                "  \"serviceId\": 132,\n" +
                "  \"datasourceId\": " + DATASOURCE_ID_STR + ",\n" +
                "  \"timestampMs\": 0\n" +
                "}"
        );

        assertEquals(expected.size(), events.size());
        for (int idx = 0; idx < events.size(); idx++) {
            JSONAssert.assertEquals(expected.get(idx), events.get(idx),
                new CustomComparator(JSONCompareMode.NON_EXTENSIBLE, // ony order, strict fields
                    new Customization("timestampMs", (o1, o2) -> true)));
        }

        long lastIdExpected = tmsPaymentService.getBillingEvents(VENDOR, START_LAST_ID, BATCH_SIZE).stream()
            .mapToLong(PaymentBillingEvent::getId)
            .max().orElse(-2);
        assertEquals(lastIdExpected, executorBilling.getEventsLastId());

        assertState(payIds[0], NEW);
        assertState(payIds[1], ERROR);
        assertState(payIds[2], NEW);
    }

    private List<PaymentBillingEvent> getBillingEvents(int startId, int batchSize) {
        // resort results to simplify checks
        Comparator<PaymentBillingEvent> cmp = Comparator.comparing(x -> x.getPayId());
        return tmsPaymentService.getBillingEvents(VENDOR, startId, batchSize).stream()
            .sorted(cmp.thenComparing(PaymentBillingEvent::getId))
            .collect(Collectors.toList());
    }

    private void mockEventsOrdering() {
        jdbcTemplate.update("update pay.payer_event_queue p set cr_time = now() + pp.ord * interval '1' minute\n" +
            "from (select id, row_number() over (order by pay_id, id) ord from pay.payer_event_queue) pp\n" +
            "where p.id = pp.id");
    }

    private void assertEvent(PaymentBillingEvent event,
                             long payId,
                             PersPaymentBillingEventType eventType,
                             long payerId) {
        assertEquals(payId, event.getPayId());
        assertEquals(eventType, event.getEvent());
        assertEquals(payerId, event.getPayer().tryGetIdLong(INVALID_PAYER));
    }

}
