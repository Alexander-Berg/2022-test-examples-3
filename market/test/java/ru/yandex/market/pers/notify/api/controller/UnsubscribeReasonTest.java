package ru.yandex.market.pers.notify.api.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;
import ru.yandex.market.pers.notify.test.VerificationUtil;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.notify.test.TestUtil.stringFromFile;

@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
class UnsubscribeReasonTest extends MarketUtilsMockedDbTest {
    private static final long UID = 12345;
    private static final long UNSUBSCRIBE_UID = 67890;
    private static final String EMAIL = "foo@bar.com";
    private static final String MODEL_ID = "5555";
    private static final String REGION_ID = "213";
    private static final long UNSUBSCRIBE_REGION_ID = 350;
    private static final String UNSUBSCRIBE_YANDEXUID = "abracadabra";
    private static final String PRICE = "100";
    private static final String DEFAULT_REASONS = "/data/unsubscribe-reason/reasons_default.json";
    private static final String PRODUCT_REASONS = "/data/unsubscribe-reason/reasons_product.json";
    private static final String SINGLE_REASON = "/data/unsubscribe-reason/single_reason.json";
    private static final String NO_REASONS = "/data/unsubscribe-reason/no_reasons.json";
    private static final String TWO_REASONS = "/data/unsubscribe-reason/two_reasons.json";
    private static final String REASONS_WITH_CUSTOM = "/data/unsubscribe-reason/reasons_with_custom.json";
    private static final String DID_NOT_SUBSCRIBE = "DID_NOT_SUBSCRIBE";
    private static final String TOO_MANY_MAILS = "TOO_MANY_MAILS";
    private static final String CUSTOM_TEXT = "Спам-спам-спам!";

    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;
    @Autowired
    private SubscriptionControllerInvoker subscriptionControllerInvoker;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private VerificationUtil verificationUtil;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setupPassport() {
        blackBoxPassportService.doReturn(UID, EMAIL);
    }

    @Test
    void unsubscribeMustReturnDefaultReasonsForAdvertising() throws Exception {
        createAdvertisingSubscription();
        checkReasons(NotificationType.ADVERTISING, null, DEFAULT_REASONS);
    }

    @Test
    void unsubscribeMustReturnSpecificReasonsForPriceDrop() throws Exception {
        long subscriptionId = createProductSubscription(NotificationType.PRICE_DROP);
        checkReasons(NotificationType.PRICE_DROP, subscriptionId, PRODUCT_REASONS);
    }

    @Test
    void unsubscribeMustReturnSpecificReasonsForPriceAlertOnSale() throws Exception {
        long subscriptionId = createProductSubscription(NotificationType.PA_ON_SALE);
        checkReasons(NotificationType.PA_ON_SALE, subscriptionId, PRODUCT_REASONS);
    }

    @Test
    void unsubscribeMustReturnDefaultReasonsForGradeAfterCpa() throws Exception {
        checkReasons(NotificationType.GRADE_AFTER_CPA, null, DEFAULT_REASONS);
    }

    @Test
    void unsubscribeMustReturnDefaultReasonsForAll() throws Exception {
        checkReasons(NotificationType.ALL, null, DEFAULT_REASONS);
    }

    @Test
    void eventMustBeSavedForAdvertising() throws Exception {
        createAdvertisingSubscription();
        invokeSaveReasons(NotificationType.ADVERTISING, null, SINGLE_REASON);
        checkEventSaved(NotificationType.ADVERTISING);
    }

    @Test
    void eventMustBeSavedForPriceDrop() throws Exception {
        long subscriptionId = createProductSubscription(NotificationType.PRICE_DROP);
        invokeSaveReasons(NotificationType.PRICE_DROP, subscriptionId, SINGLE_REASON);
        checkEventSavedWithSubscriptionId(NotificationType.PRICE_DROP, subscriptionId);
    }

    @Test
    void eventMustBeSavedForAll() throws Exception {
        invokeSaveReasons(NotificationType.ALL, null, SINGLE_REASON);
        checkEventSaved(NotificationType.ALL);
    }

    @Test
    void singleReasonMustBeSaved() throws Exception {
        createAdvertisingSubscription();
        invokeSaveReasons(NotificationType.ADVERTISING, null, SINGLE_REASON);
        checkReasonsCount(1);
        checkReasonSaved(DID_NOT_SUBSCRIBE);
    }

    @Test
    void severalReasonsMustBeSaved() throws Exception {
        createAdvertisingSubscription();
        invokeSaveReasons(NotificationType.ADVERTISING, null, TWO_REASONS);
        checkReasonsCount(2);
        checkReasonSaved(DID_NOT_SUBSCRIBE);
        checkReasonSaved(TOO_MANY_MAILS);
    }

    @Test
    void zeroReasonsMustBeSaved() throws Exception {
        createAdvertisingSubscription();
        invokeSaveReasons(NotificationType.ADVERTISING, null, NO_REASONS);
        checkReasonsCount(0);
    }

    @Test
    void customReasonMustBeSaved() throws Exception {
        createAdvertisingSubscription();
        invokeSaveReasons(NotificationType.ADVERTISING, null, REASONS_WITH_CUSTOM);
        checkReasonsCount(2);
        checkReasonSaved(DID_NOT_SUBSCRIBE);
        checkReasonSaved("OTHER", CUSTOM_TEXT);
    }

    @Test
    void saveEventWithoutUid() throws Exception {
        createAdvertisingSubscription();
        invokeSaveReasons(SINGLE_REASON, null, verificationUtil.generateActionAndSk(NotificationType.ADVERTISING, null, EMAIL), status().is2xxSuccessful());
        checkEventSaved(NotificationType.ADVERTISING, null);
    }

    @Test
    void saveReasonWithIncorrectSkMustReturn400() throws Exception {
        invokeSaveReasons(SINGLE_REASON, UNSUBSCRIBE_UID, "action=fake&sk=fake", status().isForbidden());
    }

    private void checkReasonSaved(String code) {
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from UNSUBSCRIBE_EVENT_REASON where REASON_TYPE =?",
                Integer.class, code).intValue());
    }

    private void checkReasonSaved(String code, String text) {
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from UNSUBSCRIBE_EVENT_REASON where REASON_TYPE =? and USER_TEXT=?",
                Integer.class, code, text).intValue());
    }

    private void checkReasonsCount(int expectedCount) {
        assertEquals(expectedCount, jdbcTemplate.queryForObject("select count(*) from UNSUBSCRIBE_EVENT_REASON",
                Integer.class).intValue());
    }

    private void invokeSaveReasons(NotificationType notificationType, Long subscriptionId, String filename) throws Exception {
        invokeSaveReasons(filename, UNSUBSCRIBE_UID,
                verificationUtil.generateActionAndSk(notificationType, subscriptionId, EMAIL),
                status().is2xxSuccessful());
    }

    private void invokeSaveReasons(String filename, Long uid, String actionAndSk, ResultMatcher resultMatcher) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/unsubscribe-reason/?" +
                actionAndSk)
                .param("regionId", String.valueOf(UNSUBSCRIBE_REGION_ID))
                .param("yandexuid", UNSUBSCRIBE_YANDEXUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(stringFromFile(filename));
        if (uid != null) {
            requestBuilder.param("uid", String.valueOf(uid));
        }
        mockMvc.perform(requestBuilder).andDo(print()).andExpect(resultMatcher);
    }

    private void checkEventSaved(NotificationType type) {
        checkEventSaved(type, UNSUBSCRIBE_UID);
    }

    private void checkEventSaved(NotificationType type, Long uid) {
        assertEquals(1,
                jdbcTemplate.queryForObject("select count(*) from UNSUBSCRIBE_EVENT", Integer.class).intValue());
        jdbcTemplate.query("select * from UNSUBSCRIBE_EVENT", rs -> {
            assertEquals(type.getId(), rs.getInt("SUBSCRIPTION_TYPE_ID"));
            assertEquals(EMAIL, rs.getString("EMAIL"));
            if (uid != null) {
                assertEquals(uid.longValue(), rs.getLong("UID"));
            } else {
                assertNull(rs.getObject("UID"));
            }

            assertEquals(UNSUBSCRIBE_REGION_ID, rs.getLong("REGION_ID"));
            assertEquals(UNSUBSCRIBE_YANDEXUID, rs.getString("YANDEXUID"));
        });

    }

    private void checkEventSavedWithSubscriptionId(NotificationType type, long subscriptionId) {
        checkEventSaved(type);
        assertEquals(subscriptionId, jdbcTemplate.queryForObject("select subscription_id from UNSUBSCRIBE_EVENT limit 1",
                Long.class).longValue());
    }



    private void createAdvertisingSubscription() throws Exception {
        subscriptionControllerInvoker.createSubscriptions(new Uid(UID), EMAIL,
                Collections.singletonList(EmailSubscription.builder()
                        .setSubscriptionType(NotificationType.ADVERTISING).build()));
    }

    private long createProductSubscription(NotificationType type) throws Exception {
        return subscriptionControllerInvoker.createProductSubscription(type, new Uid(UID), EMAIL, MODEL_ID, REGION_ID, PRICE);
    }

    private void checkReasons(NotificationType type, Long subscriptionId, String reasonsFilename) throws Exception {
        String response = verificationUtil.unsubscribe(type, subscriptionId, EMAIL);
        JSONAssert.assertEquals(stringFromFile(reasonsFilename), response, JSONCompareMode.LENIENT);
    }

}
