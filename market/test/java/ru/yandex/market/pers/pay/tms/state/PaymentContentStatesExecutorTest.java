package ru.yandex.market.pers.pay.tms.state;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.pay.model.PersPayEntityState;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.service.TmsPaymentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.pers.pay.model.PersPayState.CANCELED;
import static ru.yandex.market.pers.pay.model.PersPayState.EXPIRED;
import static ru.yandex.market.pers.pay.model.PersPayState.NEED_PAY;
import static ru.yandex.market.pers.pay.model.PersPayState.NEW;
import static ru.yandex.market.pers.pay.model.PersPayState.WAIT_CONTENT;
import static ru.yandex.market.pers.pay.model.PersPayState.WAIT_MODERATION;
import static ru.yandex.market.pers.pay.model.PersPayState.WAIT_MODERATION_RETRY;
import static ru.yandex.market.pers.pay.model.PersPayment.DATA_MOD_ATTEMPT;
import static ru.yandex.market.pers.pay.service.TmsPaymentService.MAX_MOD_ATTEMPTS;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.03.2021
 */
public class PaymentContentStatesExecutorTest extends AbstractPaymentStatesExecutorTest {
    @Autowired
    private PaymentContentStatesExecutor executor;

    @Test
    public void testDuplicatePayment() {
        long payId = createTestPayment(MODEL_ID, USER_ID);
        String gradeId = createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);
        long payIdDup = createTestPayment(MODEL_ID, USER_ID);

        assertEquals(payId, payIdDup);

        processPayments();

        List<PersPayment> userPayments = tmsPaymentService.getUserPayments(USER);
        assertEquals(1, userPayments.size());
        assertEquals(payId, userPayments.get(0).getId().longValue());
        assertEquals(gradeId, userPayments.get(0).getContentId());
        assertState(payId, NEED_PAY);

        assertEquals(List.of(WAIT_CONTENT, NEED_PAY), getHstStates(payId));
    }

    @Test
    public void testDuplicatePaymentAfterPayed() {
        long payId = createTestPayment(MODEL_ID, USER_ID);
        String gradeId = createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);

        processPayments();
        assertState(payId, NEED_PAY);

        long payIdDup = createTestPayment(MODEL_ID, USER_ID);
        assertEquals(payId, payIdDup);
        assertState(payIdDup, NEED_PAY);

        List<PersPayment> userPayments = tmsPaymentService.getUserPayments(USER);
        assertEquals(1, userPayments.size());
        assertEquals(payId, userPayments.get(0).getId().longValue());
        assertEquals(gradeId, userPayments.get(0).getContentId());

        assertEquals(List.of(WAIT_CONTENT, NEED_PAY), getHstStates(payId));
    }

    @Test
    public void testPositiveStepByStepModelGrade() {
        // grade without text
        long payId = createTestPayment(MODEL_ID, USER_ID);
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.CREATED);
        assertState(payId, NEW);

        processPayments();
        assertState(payId, WAIT_CONTENT);

        // add text
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.READY);

        processPayments();
        assertState(payId, WAIT_MODERATION);

        // approve
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);

        processPayments();
        assertState(payId, NEED_PAY);

        assertEquals(List.of(WAIT_CONTENT, WAIT_MODERATION, NEED_PAY), getHstStates(payId));
    }

    @Test
    public void testPositiveStepByStepModelGradePhoto() {
        // grade photo payment without proper content (but with grade)
        long payId = createTestPhotoPayment(MODEL_ID, USER_ID);
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);
        assertState(payId, NEW);

        // should wait content, since no photo was found yet
        processPayments();
        assertState(payId, WAIT_CONTENT);

        // add photo
        createModelGradePhoto(MODEL_ID, USER_ID, PersPayEntityState.READY);

        processPayments();
        assertState(payId, WAIT_MODERATION);

        // approve
        createModelGradePhoto(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);

        processPayments();
        assertState(payId, NEED_PAY);

        assertEquals(List.of(WAIT_CONTENT, WAIT_MODERATION, NEED_PAY), getHstStates(payId));
    }

    @Test
    public void testPositiveStepByStepModelGradeVide() {
        // grade photo payment without proper content (but with grade)
        long payId = createTestVideoPayment(MODEL_ID, USER_ID);
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);
        assertState(payId, NEW);

        // should wait content, since no photo was found yet
        processPayments();
        assertState(payId, WAIT_CONTENT);

        // add photo
        createModelVideo(MODEL_ID, USER_ID, PersPayEntityState.READY);

        processPayments();
        assertState(payId, WAIT_MODERATION);

        // approve
        createModelVideo(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);

        processPayments();
        assertState(payId, NEED_PAY);

        assertEquals(List.of(WAIT_CONTENT, WAIT_MODERATION, NEED_PAY), getHstStates(payId));
    }

    @Test
    public void testALotOfPayments() {
        // small batch size for tests
        int batchSize = 2;
        executor.changeBatchSize(batchSize);

        int paySize = 8;

        long[] payIds = IntStream.range(0, paySize)
            .mapToLong(x -> createTestPayment(MODEL_ID, USER_ID + x))
            .toArray();

        for (int idx = 0; idx < paySize; idx++) {
            createModelGrade(MODEL_ID, USER_ID + idx, PersPayEntityState.CREATED);
        }

        processPayments();

        // only 2 are created
        for (int idx = batchSize; idx < paySize; idx++) {
            assertState(payIds[idx], NEW);
        }
        assertState(payIds[0], WAIT_CONTENT);
        assertState(payIds[1], WAIT_CONTENT);

        executor.changeBatchSize(1000);

        processPayments();

        // all are wait_content now
        for (int idx = 0; idx < paySize; idx++) {
            assertState(payIds[idx], WAIT_CONTENT);
        }


        // limit batch again
        executor.changeBatchSize(batchSize);

        // expire all payments and content
        jdbcTemplate.update("update pay.payment set cr_time = cr_time - interval '2' day where 1=1");
        jdbcTemplate.update("update pay.content_state set upd_time = upd_time - interval '2' day where 1=1");

        // add content
        for (int idx = 0; idx < batchSize + 1; idx++) {
            createModelGrade(MODEL_ID, USER_ID + idx, PersPayEntityState.APPROVED);
        }

        // should approve 2 payments in first iteration, approve 1 +expire all other on second
        processPayments();

        // only 2 are approved
        assertState(payIds[0], NEED_PAY);
        assertState(payIds[1], NEED_PAY);
        for (int idx = batchSize; idx < paySize; idx++) {
            assertState(payIds[idx], WAIT_CONTENT);
        }

        executor.changeBatchSize(1000);
        processPayments();

        assertState(payIds[0], NEED_PAY);
        assertState(payIds[1], NEED_PAY);
        assertState(payIds[2], NEED_PAY);

        for (int idx = batchSize + 2; idx < paySize; idx++) {
            assertState(payIds[idx], EXPIRED);
        }
    }

    @Test
    public void testCancelFullCase() {
        // create - fail - ready - fail - ready - cancel

        // ready grade
        long payId = createTestPayment(MODEL_ID, USER_ID);
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.READY);
        assertState(payId, NEW);

        // N retries = N+1 attempts.
        // First attempt has null in mod_attempt.
        // After fail - write attempt+1 to data
        for (int idx = 0; idx <= MAX_MOD_ATTEMPTS; idx++) {
            PersPayment payment = paymentService.getPayment(payId);

            if (idx == 0) {
                assertNull(payment.getDataField(DATA_MOD_ATTEMPT, x -> x));
            } else {
                assertEquals(idx + 1, payment.getDataField(DATA_MOD_ATTEMPT, Integer::parseInt).intValue());
            }

            processPayments();
            assertState(payId, WAIT_MODERATION);

            // add text
            createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.REJECTED);

            processPayments();

            payment = paymentService.getPayment(payId);
            if (idx == MAX_MOD_ATTEMPTS) {
                assertState(payId, CANCELED);
                assertEquals(MAX_MOD_ATTEMPTS + 1,
                    payment.getDataField(DATA_MOD_ATTEMPT, Integer::parseInt).intValue());
            } else {
                assertState(payId, WAIT_MODERATION_RETRY);
                assertEquals(idx + 2, payment.getDataField(DATA_MOD_ATTEMPT, Integer::parseInt).intValue());

                // pretend new version of grade added
                createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.READY);
                processPayments();
            }
        }

        assertEquals(
            List.of(
                WAIT_CONTENT,
                WAIT_MODERATION, WAIT_MODERATION_RETRY,
                WAIT_MODERATION, WAIT_MODERATION_RETRY,
                WAIT_MODERATION, CANCELED
            ),
            getHstStates(payId));
    }

    @Test
    public void testCancelCaseInstantReject() {
        // new - reject
        long payId = createTestPayment(MODEL_ID, USER_ID);
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.REJECTED);
        assertState(payId, NEW);

        processPayments();
        assertState(payId, WAIT_MODERATION_RETRY);

        assertEquals(List.of(WAIT_CONTENT, WAIT_MODERATION_RETRY), getHstStates(payId));
    }

    @Test
    public void testCancelDoubleRejectOk() {
        // ready - reject - reject - ok
        // new - reject
        long payId = createTestPayment(MODEL_ID, USER_ID);
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.REJECTED);
        assertState(payId, NEW);

        processPayments();
        assertState(payId, WAIT_MODERATION_RETRY);

        PersPayment payment = paymentService.getPayment(payId);
        assertEquals(2, payment.getDataField(DATA_MOD_ATTEMPT, Integer::parseInt).intValue());

        // reject again - nothing changes
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.REJECTED);
        processPayments();

        payment = paymentService.getPayment(payId);
        assertEquals(2, payment.getDataField(DATA_MOD_ATTEMPT, Integer::parseInt).intValue());

        // accept
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);
        processPayments();
        assertState(payId, NEED_PAY);

        assertEquals(List.of(WAIT_CONTENT, WAIT_MODERATION_RETRY, NEED_PAY), getHstStates(payId));
    }

    @Test
    public void testCancelRetryExpired() {
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.REJECTED);
        long payId = createTestPayment(MODEL_ID, USER_ID);

        processPayments();
        assertState(payId, WAIT_MODERATION_RETRY);

        // move to expired
        jdbcTemplate.update("update pay.payment set cr_time = now() - make_interval(hours := ?) where id = ?",
            TmsPaymentService.PAYMENT_EXPIRE_HOURS + 1, payId);

        processPayments();
        assertState(payId, WAIT_MODERATION_RETRY);

        // move more
        jdbcTemplate.update("update pay.payment set cr_time = now() - make_interval(hours := ?) where id = ?",
            TmsPaymentService.PAYMENT_RETRY_EXPIRE_HOURS + 1, payId);

        processPayments();
        assertState(payId, EXPIRED);
    }

    @Test
    public void testExpired() {
        // wait_content+expired+notext -> expired
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.CREATED);
        long payId = createTestPayment(MODEL_ID, USER_ID);

        jdbcTemplate.update("update pay.payment set cr_time = cr_time - interval '2' day where id = ?", payId);

        processPayments();

        assertState(payId, EXPIRED);
    }

    @Test
    public void testExpiredWhenBrokenEntityStates() {
        // when there are no fresh states - do not expire
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.CREATED);
        long payId = createTestPayment(MODEL_ID, USER_ID);

        jdbcTemplate.update("update pay.payment set cr_time = cr_time - interval '2' day where id = ?", payId);
        jdbcTemplate.update("update pay.content_state set upd_time = upd_time - interval '2' day where 1=1");

        processPayments();

        assertState(payId, WAIT_CONTENT);
    }

    @Test
    public void testExpiredHasText() {
        // wait_content+content -> ok even if actually expired
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.READY);
        long payId = createTestPayment(MODEL_ID, USER_ID);

        jdbcTemplate.update("update pay.payment set cr_time = cr_time - interval '2' day where id = ?", payId);

        processPayments();

        assertState(payId, WAIT_MODERATION);
    }

    @Test
    public void testGradeExpired() {
        // grade old, payment new - wait for content
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.CREATED);
        long payId = createTestPayment(MODEL_ID, USER_ID);

        jdbcTemplate.update("update pay.content_state set upd_time = upd_time - interval '2' day where 1=1");

        processPayments();

        assertState(payId, WAIT_CONTENT);
    }

    @Test
    public void testPaymentsAfterModelIdChange() {
        final String contentId = "123123";

        //create model grade
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.READY, contentId, 0);
        long payId = createTestPayment(MODEL_ID, USER_ID);
        processPayments();

        assertState(payId, WAIT_MODERATION);

        //change model's id, create model grade in new status with new modelId
        long changedModelId = MODEL_ID + 1;
        createModelGrade(changedModelId, USER_ID, PersPayEntityState.APPROVED, contentId, 1000L);
        processPayments();

        //check there are 2 rows in pay.content_state with contentId
        Integer countOfContentIdRows = jdbcTemplate.queryForObject(
            "select count(*) from pay.content_state where content_id = ?",
            Integer.class,
            contentId
        );
        assertEquals(2, countOfContentIdRows);

        assertState(payId, NEED_PAY);
    }

    private void processPayments() {
        executor.processPaymentContentStates();
    }
}
