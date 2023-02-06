package ru.yandex.market.pers.pay.tms.state;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.pay.MockUtils;
import ru.yandex.market.pers.pay.PersPayTest;
import ru.yandex.market.pers.pay.model.ContentPrice;
import ru.yandex.market.pers.pay.model.PersPayEntityState;
import ru.yandex.market.pers.pay.model.PersPayEntityType;
import ru.yandex.market.pers.pay.model.PersPayState;
import ru.yandex.market.pers.pay.model.PersPayUser;
import ru.yandex.market.pers.pay.model.PersPayer;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.model.PersPaymentFilter;
import ru.yandex.market.pers.pay.service.PaymentService;
import ru.yandex.market.pers.pay.service.TmsPaymentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.pay.model.PersPayState.NEW;
import static ru.yandex.market.pers.pay.model.PersPayUserType.UID;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.03.2021
 */
public class AbstractPaymentStatesExecutorTest extends PersPayTest {
    protected static final long MODEL_ID = 234234;
    protected static final long USER_ID = 562462;
    protected static final long DATASOURCE_ID = 23252452;
    protected static final String DATASOURCE_ID_STR = String.valueOf(DATASOURCE_ID);
    public static final PersPayUser USER = new PersPayUser(UID, USER_ID);
    public static final int UNLIMITED_LIFE = 10000;

    @Autowired
    protected PaymentService paymentService;
    @Autowired
    protected TmsPaymentService tmsPaymentService;

    protected void assertState(long payId, PersPayState state) {
        PersPayment payment = paymentService.getPayment(new PersPaymentFilter().id(payId)).orElse(null);
        assertNotNull(payment, "payId = " + payId);
        assertEquals(state, payment.getState());

        Optional<PersPayState> lastHstState = getLastHstState(payId);

        if (state == NEW) {
            assertTrue(lastHstState.isEmpty());
        } else {
            assertTrue(lastHstState.isPresent());
            assertEquals(state, lastHstState.get());
        }
    }

    @NotNull
    protected List<PersPayState> getHstStates(long payId) {
        return jdbcTemplate.query(
            "select STATE\n" +
                "from pay.payment_hst\n" +
                "where PAY_ID = ?" +
                "order by id",
            (rs, rowNum) -> PersPayState.valueOf(rs.getInt("state")),
            payId
        );
    }

    @NotNull
    protected Optional<PersPayState> getLastHstState(long payId) {
        List<PersPayState> result = jdbcTemplate.query(
            "select STATE\n" +
                "from pay.payment_hst\n" +
                "where PAY_ID = ?" +
                "order by id desc\n" +
                "limit 1",
            (rs, rowNum) -> PersPayState.valueOf(rs.getInt("state")),
            payId
        );
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @NotNull
    protected Optional<String> getLastHstReason(long payId) {
        List<String> result = jdbcTemplate.query(
            "select reason\n" +
                "from pay.payment_hst\n" +
                "where PAY_ID = ?" +
                "order by id desc\n" +
                "limit 1",
            (rs, rowNum) -> rs.getString("reason"),
            payId
        );
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    protected long createTestPayment(long modelId, long userId) {
        return paymentService.savePaymentForTests(PersPayment.builderModelGradeUid(modelId, userId)
            .payer(PersPayerType.MARKET, "test")
            .amount(10));
    }

    protected long createTestPhotoPayment(long modelId, long userId) {
        return paymentService.savePaymentForTests(PersPayment.builder()
            .uid(userId)
            .entity(PersPayEntityType.MODEL_GRADE_PHOTO, String.valueOf(modelId))
            .payer(PersPayerType.MARKET, "test")
            .amount(10));
    }

    protected long createTestVideoPayment(long modelId, long userId) {
        return paymentService.savePaymentForTests(PersPayment.builder()
            .uid(userId)
            .entity(PersPayEntityType.MODEL_VIDEO, String.valueOf(modelId))
            .payer(PersPayerType.MARKET, "test")
            .amount(10));
    }

    protected void createTestPaymentOffer(long modelId, long userId, int amount) {
        paymentService.saveShownOffers(List.of(MockUtils.testPay(modelId, userId).amount(amount)));
    }

    protected void createTestPhotoPaymentOffer(long modelId, long userId, int amount) {
        paymentService.saveShownOffers(List.of(MockUtils.testPhotoPay(modelId, userId).amount(amount)));
    }

    protected long createTestVendorPayment(long modelId, long userId, String datasourceId) {
        return paymentService.savePaymentForTests(PersPayment.builderModelGradeUid(modelId, userId)
            .payer(PersPayerType.VENDOR, datasourceId)
            .amount(10));
    }

    public String createModelGrade(long modelId, long userId, PersPayEntityState state) {
        return createModelGrade(modelId, userId, state, "" + userId + "-" + modelId, System.currentTimeMillis());
    }

    public String createModelGrade(long modelId,
                                   long userId,
                                   PersPayEntityState state,
                                   String contentId,
                                   long timestamp) {
        tmsPaymentService.saveContentStateChanges(List.of(
            MockUtils.createModelGradeDto(modelId, userId, state, contentId, timestamp)
        ));

        return contentId;
    }

    public String createModelGradePhoto(long modelId, long userId, PersPayEntityState state) {
        return createModelGradePhoto(modelId, userId, state, "" + userId + "-" + modelId, System.currentTimeMillis());
    }

    public String createModelGradePhoto(long modelId,
                                        long userId,
                                        PersPayEntityState state,
                                        String contentId,
                                        long timestamp) {
        tmsPaymentService.saveContentStateChanges(List.of(
            MockUtils.createModelGradePhotoDto(modelId, userId, state, contentId, timestamp)
        ));

        return contentId;
    }

    public String createModelVideo(long modelId, long userId, PersPayEntityState state) {
        return createModelVideo(modelId, userId, state, "" + userId + "-" + modelId, System.currentTimeMillis());
    }

    public String createModelVideo(long modelId,
                                   long userId,
                                   PersPayEntityState state,
                                   String contentId,
                                   long timestamp) {
        tmsPaymentService.saveContentStateChanges(List.of(
            MockUtils.createModelVideoDto(modelId, userId, state, contentId, timestamp)
        ));

        return contentId;
    }

    protected void updateToBeRejected(List<Long> payIds) {
        List<PersPayment> paysToReject = paymentService.getPayments(new PersPaymentFilter().ids(payIds));
        for (PersPayment payment : paysToReject) {
            payment.putData(PersPayment.DATA_MOD_ATTEMPT, String.valueOf(TmsPaymentService.MAX_MOD_ATTEMPTS + 1));
        }

        tmsPaymentService.updateData(paysToReject);
    }

    protected void savePrice(long modelId, int amount) {
        savePrice(modelId, amount, Map.of());
    }

    protected void savePrice(long modelId, int amount, String payerId) {
        paymentService.savePrice(MockUtils.testPrice(modelId, amount, payerId), TimeUnit.DAYS.toSeconds(1));
    }

    protected void savePrice(long modelId, int amount, Map<String, String> data) {
        ContentPrice price = MockUtils.testPrice(modelId, amount);
        price.setData(data);
        paymentService.savePrice(price, TimeUnit.DAYS.toSeconds(1));
    }

    protected void savePrice(long modelId, int amount, PersPayerType payerType, String payerId) {
        ContentPrice price = MockUtils.testPrice(modelId, amount);
        price.setPayer(new PersPayer(payerType, payerId));
        paymentService.savePrice(price, TimeUnit.DAYS.toSeconds(1));
    }

    public void mockBalance(PersPayerType payerType, String payerId, int balance) {
        paymentService.saveBalance(payerType, payerId, BigDecimal.valueOf(balance), UNLIMITED_LIFE);
    }
}
