package ru.yandex.market.pers.pay.tms;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.pay.MockUtils;
import ru.yandex.market.pers.pay.model.PersPayEntityState;
import ru.yandex.market.pers.pay.model.PersPayEntityType;
import ru.yandex.market.pers.pay.model.PersPayState;
import ru.yandex.market.pers.pay.model.PersPayUserType;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.model.PersPaymentBuilder;
import ru.yandex.market.pers.pay.tms.state.AbstractPaymentStatesExecutorTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 25.03.2021
 */
public class AutoCreatePaymentJobTest extends AbstractPaymentStatesExecutorTest {
    public static final int LIMIT = 1000;

    @Autowired
    private AutoCreatePaymentJob job;

    @Test
    public void testSimpleCreation() {
        // has offer and content - just create when can
        createTestPaymentOffer(MODEL_ID, USER_ID, 1);
        createTestPaymentOffer(MODEL_ID, USER_ID + 1, 2);
        createTestPaymentOffer(MODEL_ID, USER_ID + 2, 3);
        createTestPaymentOffer(MODEL_ID, USER_ID + 3, 4);

        // create content
        // cover all states
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.CREATED);
        createModelGrade(MODEL_ID, USER_ID + 1, PersPayEntityState.READY);
        createModelGrade(MODEL_ID, USER_ID + 2, PersPayEntityState.APPROVED);
        createModelGrade(MODEL_ID, USER_ID + 3, PersPayEntityState.REJECTED);

        job.autoCreatePayments();

        List<PersPayment> payments = tmsPaymentService.getPaymentsToProcess(PersPayState.NEW, LIMIT);
        payments.sort(Comparator.comparing(PersPayment::getAmount));

        assertEquals(2, payments.size());
        assertEquals(USER_ID + 1, payments.get(0).getUser().getIdLong());
        assertEquals(USER_ID + 2, payments.get(1).getUser().getIdLong());

        // common checks
        for (PersPayment payment : payments) {
            assertEquals(PersPayUserType.UID, payment.getUser().getType());
            assertEquals(PersPayEntityType.MODEL_GRADE, payment.getEntity().getType());
            assertEquals(MODEL_ID, payment.getEntity().getIdLong());
            assertEquals(PersPayerType.MARKET, payment.getPayer().getType());
            assertEquals("test", payment.getPayer().getId());
        }
    }

    @Test
    public void testSimpleCreationPhoto() {
        // has offer and content - just create when can
        createTestPhotoPaymentOffer(MODEL_ID, USER_ID, 1);
        createTestPhotoPaymentOffer(MODEL_ID, USER_ID + 1, 2);
        createTestPhotoPaymentOffer(MODEL_ID, USER_ID + 2, 3);
        createTestPhotoPaymentOffer(MODEL_ID, USER_ID + 3, 4);
        createTestPaymentOffer(MODEL_ID, USER_ID + 4, 5);

        // create content
        // cover all states
        createModelGradePhoto(MODEL_ID, USER_ID, PersPayEntityState.CREATED);
        createModelGradePhoto(MODEL_ID, USER_ID + 1, PersPayEntityState.READY);
        createModelGradePhoto(MODEL_ID, USER_ID + 2, PersPayEntityState.APPROVED);
        createModelGradePhoto(MODEL_ID, USER_ID + 3, PersPayEntityState.REJECTED);
        //also some grade states to check only photo
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);
        createModelGrade(MODEL_ID, USER_ID + 1, PersPayEntityState.APPROVED);
        createModelGrade(MODEL_ID, USER_ID + 4, PersPayEntityState.APPROVED);

        job.autoCreatePayments();

        List<PersPayment> payments = tmsPaymentService.getPaymentsToProcess(PersPayState.NEW, LIMIT);
        payments.sort(Comparator.comparing(PersPayment::getAmount));

        assertEquals(3, payments.size());
        assertEquals(USER_ID + 1, payments.get(0).getUser().getIdLong());
        assertEquals(USER_ID + 2, payments.get(1).getUser().getIdLong());
        assertEquals(USER_ID + 4, payments.get(2).getUser().getIdLong());

        // common checks
        for (PersPayment payment : payments) {
            assertEquals(PersPayUserType.UID, payment.getUser().getType());
            if (payment.getUser().getIdLong() < USER_ID + 4) {
                assertEquals(PersPayEntityType.MODEL_GRADE_PHOTO, payment.getEntity().getType());
            } else {
                assertEquals(PersPayEntityType.MODEL_GRADE, payment.getEntity().getType());
            }
            assertEquals(MODEL_ID, payment.getEntity().getIdLong());
            assertEquals(PersPayerType.MARKET, payment.getPayer().getType());
            assertEquals("test", payment.getPayer().getId());
        }
    }

    private void assertPayment(PersPayment payment, long modelId, long userId, int amount) {
        assertEquals(modelId, payment.getEntity().getIdLong());
        assertEquals(userId, payment.getUser().getIdLong());
        assertEquals(amount, payment.getAmount());
    }

    @Test
    public void testDuplicateCreation() {
        // has offer and content - just create when can
        createTestPaymentOffer(MODEL_ID, USER_ID, 1);
        createTestPaymentOffer(MODEL_ID, USER_ID, 2);

        // create content
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.READY);

        job.autoCreatePayments();

        // check only one created
        List<PersPayment> payments = tmsPaymentService.getPaymentsToProcess(PersPayState.NEW, LIMIT);
        payments.sort(Comparator.comparing(PersPayment::getAmount));

        assertEquals(1, payments.size());
        assertEquals(2, payments.get(0).getAmount());

        assertEquals(2, jdbcTemplate.queryForObject("select count(*) from pay.payment_offer", Long.class));
    }

    @Test
    public void testNotExpiredCreation() {
        // two identical offers. One is fresh, another expired
        createTestPaymentOffer(MODEL_ID, USER_ID, 1);
        createTestPaymentOffer(MODEL_ID, USER_ID + 1, 2);

        jdbcTemplate.update("update pay.payment_offer " +
            "set cr_time = now() - interval '23' hour " +
            "where user_id = ?::text", USER_ID);

        // create content
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.READY);
        createModelGrade(MODEL_ID, USER_ID + 1, PersPayEntityState.READY);

        job.autoCreatePayments();

        // check only one created
        List<PersPayment> payments = tmsPaymentService.getPaymentsToProcess(PersPayState.NEW, LIMIT);
        payments.sort(Comparator.comparing(PersPayment::getAmount));

        assertEquals(2, payments.size());
        assertPayment(payments.get(0), MODEL_ID, USER_ID, 1);
        assertPayment(payments.get(1), MODEL_ID, USER_ID + 1, 2);
    }

    @Test
    public void testExpiredCreation() {
        // two identical offers. One is fresh, another expired
        createTestPaymentOffer(MODEL_ID, USER_ID, 1);
        createTestPaymentOffer(MODEL_ID, USER_ID + 1, 2);

        jdbcTemplate.update("update pay.payment_offer " +
            "set cr_time = now() - interval '2' day " +
            "where user_id = ?::text", USER_ID);

        // create content
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.READY);
        createModelGrade(MODEL_ID, USER_ID + 1, PersPayEntityState.READY);

        job.autoCreatePayments();

        // check only one created
        List<PersPayment> payments = tmsPaymentService.getPaymentsToProcess(PersPayState.NEW, LIMIT);
        payments.sort(Comparator.comparing(PersPayment::getAmount));

        assertEquals(1, payments.size());
        assertEquals(USER_ID + 1, payments.get(0).getUser().getIdLong());
    }

    @Test
    public void testBatchedCreation() {
        // set batch to 2, try to save 3 payments
        createTestPaymentOffer(MODEL_ID, USER_ID, 1);
        createTestPaymentOffer(MODEL_ID, USER_ID + 1, 2);
        createTestPaymentOffer(MODEL_ID, USER_ID + 2, 3);

        job.changeBatchSize(2);

        // create content
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.READY);
        createModelGrade(MODEL_ID, USER_ID + 1, PersPayEntityState.READY);
        createModelGrade(MODEL_ID, USER_ID + 2, PersPayEntityState.APPROVED);

        job.autoCreatePayments();

        List<PersPayment> payments = tmsPaymentService.getPaymentsToProcess(PersPayState.NEW, LIMIT);
        payments.sort(Comparator.comparing(PersPayment::getAmount));

        assertEquals(2, payments.size());
        assertEquals(USER_ID + 1, payments.get(0).getUser().getIdLong());
        assertEquals(USER_ID + 2, payments.get(1).getUser().getIdLong());

        // process second batch
        job.autoCreatePayments();

        payments = tmsPaymentService.getPaymentsToProcess(PersPayState.NEW, LIMIT);
        payments.sort(Comparator.comparing(PersPayment::getAmount));

        assertEquals(3, payments.size());
        assertEquals(USER_ID, payments.get(0).getUser().getIdLong());
        assertEquals(USER_ID + 1, payments.get(1).getUser().getIdLong());
        assertEquals(USER_ID + 2, payments.get(2).getUser().getIdLong());
    }

    @Test
    public void testAllFieldsSavedProperly() {
        BigDecimal amountCharge = BigDecimal.valueOf(314).movePointLeft(2);

        paymentService.saveShownOffers(List.of(MockUtils.testPay(MODEL_ID, USER_ID)
            .payer(PersPayerType.VENDOR, "1234")
            .data(Map.of("testKey", "testValue"))
            .amount(12, amountCharge)));

        // create content
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.READY);

        job.autoCreatePayments();

        // check only one created
        List<PersPayment> payments = tmsPaymentService.getPaymentsToProcess(PersPayState.NEW, LIMIT);
        PersPayment payment = payments.get(0);

        assertEquals(USER_ID, payment.getUser().getIdLong());
        assertEquals(12, payment.getAmount());
        assertEquals(amountCharge, payment.getAmountCharge());
        assertEquals("3.14", payment.getAmountCharge().toString());
        assertEquals("testValue", payment.getDataField("testKey", x -> x));
        assertEquals(PersPayerType.VENDOR, payment.getPayer().getType());
        assertEquals("1234", payment.getPayer().getId());
    }

    @Test
    public void testOfferCleanup() {
        // amount > 100 is expired in this test

        // fresh
        createTestPaymentOffer(MODEL_ID, USER_ID, 1);
        // fresh with fresh duplicate
        createTestPaymentOffer(MODEL_ID, USER_ID + 1, 2);
        createTestPaymentOffer(MODEL_ID, USER_ID + 1, 3);
        // fresh with expired duplicate
        createTestPaymentOffer(MODEL_ID, USER_ID + 2, 110);
        createTestPaymentOffer(MODEL_ID, USER_ID + 2, 4);
        // expired
        createTestPaymentOffer(MODEL_ID, USER_ID + 3, 120);
        // fresh payed
        createTestPaymentOffer(MODEL_ID, USER_ID + 4, 5);

        // create content
        createModelGrade(MODEL_ID, USER_ID + 4, PersPayEntityState.READY);

        jdbcTemplate.update("update pay.payment_offer " +
            "set cr_time = now() - interval '2' day " +
            "where amount > 100");

        // create payment for some offers
        job.autoCreatePayments();

        // check before cleanup
        List<String> expectedKeys = List.of(
            MockUtils.testPayKey(MODEL_ID, USER_ID),
            MockUtils.testPayKey(MODEL_ID, USER_ID + 1),
            MockUtils.testPayKey(MODEL_ID, USER_ID + 2),
            MockUtils.testPayKey(MODEL_ID, USER_ID + 3),
            MockUtils.testPayKey(MODEL_ID, USER_ID + 4)
        );

        assertOffers(List.of(1, 2, 3, 4, 5, 110, 120), paymentService.getOffers(expectedKeys, 100));
        assertOffers(List.of(1, 2, 3, 4, 5), paymentService.getFreshOffers(expectedKeys));
        assertOffers(List.of(1, 3, 4, 5),
            new ArrayList<>(paymentService.getCurrentPayAndFreshOffers(expectedKeys)));

        // check after cleanup
        job.autoCleanOffers();

        assertOffers(List.of(1, 3, 4), paymentService.getOffers(expectedKeys, 100));
        assertOffers(List.of(1, 3, 4), paymentService.getFreshOffers(expectedKeys));
        assertOffers(List.of(1, 3, 4, 5),
            new ArrayList<>(paymentService.getCurrentPayAndFreshOffers(expectedKeys)));
    }

    private void assertOffers(List<Integer> expectedAmts, List<PersPaymentBuilder> offers) {
        offers.sort(Comparator.comparing(PersPaymentBuilder::getAmount));

        assertEquals(expectedAmts.size(), offers.size());
        assertEquals(expectedAmts,
            offers.stream().map(PersPaymentBuilder::getAmount).collect(Collectors.toList()));
    }


}
