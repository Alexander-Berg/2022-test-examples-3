package ru.yandex.market.pers.pay.tms;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.pay.MockUtils;
import ru.yandex.market.pers.pay.model.PersPayEntityState;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.tms.state.AbstractPaymentStatesExecutorTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersPayCleanerJobTest extends AbstractPaymentStatesExecutorTest {

    @Autowired
    private JdbcTemplate tmsJdbcTemplate;

    @Autowired
    private PersPayCleanerJob job;

    @Test
    public void testCleanOldOffersWithoutPayment() {
        // create offers
        createTestPaymentOffer(MODEL_ID, USER_ID, 100);
        createTestPaymentOffer(MODEL_ID + 1, USER_ID + 1, 100);
        createTestPaymentOffer(MODEL_ID + 2, USER_ID + 2, 100);
        createTestPaymentOffer(MODEL_ID + 3, USER_ID + 3,  100);

        // create payments for two offer
        createTestPayment(MODEL_ID, USER_ID);
        createTestPayment(MODEL_ID + 1, USER_ID + 1);

        // shift cr_time for two payment offer
        shiftPaymentOfferCrTimeIntoThePast(MODEL_ID + 1, USER_ID + 1, 180);
        shiftPaymentOfferCrTimeIntoThePast(MODEL_ID + 3, USER_ID + 3, 180);

        List<String> before = getPaymentOfferPayKeys();

        job.cleanOldPayData();

        List<String> after = getPaymentOfferPayKeys();

        // deleted payment offer
        before.remove(3);

        assertEquals(before, after);
    }

    @Test
    public void testCleanVeryOldOffers() {
        // create offers
        createTestPaymentOffer(MODEL_ID, USER_ID, 100);

        // shift cr_time for more than 60 days
        shiftPaymentOfferCrTimeIntoThePast(MODEL_ID, USER_ID, 210);

        List<String> before = getPaymentOfferPayKeys();

        job.cleanOldPayData();

        List<String> after = getPaymentOfferPayKeys();

        assertEquals(before, after);
    }

    @Test
    public void testCleanOldContentStatesWithoutPayment() {
        // create content
        List<String> contentIds = List.of(
            createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.CREATED),
            createModelGrade(MODEL_ID, USER_ID + 1, PersPayEntityState.READY),
            createModelGrade(MODEL_ID, USER_ID + 2, PersPayEntityState.APPROVED),
            createModelGrade(MODEL_ID, USER_ID + 3, PersPayEntityState.REJECTED)
        );


        // create payments for two contents
        createTestPayment(MODEL_ID, USER_ID, contentIds.get(0));
        createTestPayment(MODEL_ID, USER_ID + 2,contentIds.get(2));

        // shift upd_time for two contents
        shiftContentStateUpdTimeIntoThePast(MODEL_ID, USER_ID, 30);
        shiftContentStateUpdTimeIntoThePast(MODEL_ID, USER_ID + 3, 30);

        List<String> before = getContentStatePayKeys();

        job.cleanOldPayData();

        List<String> after = getContentStatePayKeys();

        // deleted content state
        before.remove(3);

        assertEquals(before, after);
    }

    @Test
    public void testCleanVeryOldContentStates() {
        // create content
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.CREATED);

        // shift upd_time for more than 60 days
        shiftContentStateUpdTimeIntoThePast(MODEL_ID, USER_ID, 60);

        List<String> before = getContentStatePayKeys();

        job.cleanOldPayData();

        List<String> after = getContentStatePayKeys();

        assertEquals(before, after);
    }

    @Test
    public void testCleanOldContentStatesWithModelTransition() {
        // create content
        String contentId = createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);

        // create payments for two contents
        createTestPayment(MODEL_ID + 1, USER_ID, contentId);

        // shift upd_time for two contents
        shiftContentStateUpdTimeIntoThePast(MODEL_ID, USER_ID, 30);

        List<String> before = getContentStatePayKeys();

        job.cleanOldPayData();

        List<String> after = getContentStatePayKeys();

        assertEquals(before, after);
    }

    @Test
    public void testCleanOldContentStatesWithoutCorrectContentId() {
        // create content
        createModelGrade(MODEL_ID, USER_ID, PersPayEntityState.APPROVED);

        // create payments for content
        createTestPayment(MODEL_ID, USER_ID, "content_id");

        // shift upd_time for two contents
        shiftContentStateUpdTimeIntoThePast(MODEL_ID, USER_ID, 30);

        List<String> before = getContentStatePayKeys();

        job.cleanOldPayData();

        List<String> after = getContentStatePayKeys();

        assertEquals(before, after);
    }

    @Test
    public void testCleanOldContentPriceWithExpiredEndTime() {
        savePrice(MODEL_ID, 1, PersPayerType.MARKET, MockUtils.TEST_PAYER);
        savePrice(MODEL_ID + 1, 1, PersPayerType.MARKET, MockUtils.TEST_PAYER);

        // shift end_time
        tmsJdbcTemplate.update(
            "update pay.content_price " +
                "set end_time = end_time - interval '91' day " +
                "where entity_id = ?",
            String.valueOf(MODEL_ID + 1)
        );

        List<String> before = getContentPriceEntityKeys();

        job.cleanOldPayData();

        List<String> after = getContentPriceEntityKeys();

        // deleted content price
        before.remove(1);

        assertEquals(before, after);
    }

    protected long createTestPayment(long modelId, long userId, String contentId) {
        return paymentService.savePaymentForTests(PersPayment.builderModelGradeUid(modelId, userId)
            .payer(PersPayerType.MARKET, "test")
            .amount(10)
            .contentId(contentId));
    }

    private void shiftPaymentOfferCrTimeIntoThePast(long modelId, long userId, int dayCount) {
        tmsJdbcTemplate.update(
            "update pay.payment_offer " +
                "set cr_time = cr_time - ? * interval '1' day " +
                "where entity_id = ? and user_id = ?",
            dayCount,
            String.valueOf(modelId),
            String.valueOf(userId)
        );
    }

    private List<String> getPaymentOfferPayKeys() {
        return jdbcTemplate.queryForList("select pay_key from pay.payment_offer order by id asc", String.class);
    }

    private void shiftContentStateUpdTimeIntoThePast(long modelId, long userId, int dayCount) {
        tmsJdbcTemplate.update(
            "update pay.content_state " +
                "set upd_time = upd_time - ? * interval '1' day " +
                "where entity_id = ? and user_id = ?",
            dayCount,
            String.valueOf(modelId),
            String.valueOf(userId)
        );
    }

    private List<String> getContentStatePayKeys() {
        return jdbcTemplate.queryForList("select pay_key from pay.content_state order by id asc", String.class);
    }

    private List<String> getContentPriceEntityKeys() {
        return jdbcTemplate.queryForList("select entity_key from pay.content_price order by id asc", String.class);
    }
}
