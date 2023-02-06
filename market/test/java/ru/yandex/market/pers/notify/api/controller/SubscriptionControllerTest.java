package ru.yandex.market.pers.notify.api.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.events.SubscriptionEventDto;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.pers.notify.SubscriptionsCacher;
import ru.yandex.market.pers.notify.api.service.sk.UnsubscribeMailManager;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.EventSourceDAO;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.EmailOwnership;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.SubscriptionSettings;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.YandexUid;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatusDto;
import ru.yandex.market.pers.notify.model.subscription.ReturnMode;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityDAO;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;
import ru.yandex.market.pers.notify.test.TestUtil;
import ru.yandex.market.pers.notify.test.VerificationUtil;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.notify.api.controller.Util.SubscriptionsUtil.compareSubscriptions;

class SubscriptionControllerTest extends MarketUtilsMockedDbTest {
    private static final Uid UID = new Uid(12345L);
    private static final Uuid UUID = new Uuid("123456L");
    private static final YandexUid YANDEX_UID = new YandexUid("1234567L");
    private static final String EMAIL = "foo@bar.buzz";
    private static final String ANOTHER_EMAIL = "another_foo@bar.buzz";
    private static final String ANOTHER_EMAIL2 = "another2_foo@bar.buzz";
    private static final String OTHER_SUBSCRIPTION_PLACE = "some_other_place";
    private static final String LOTTERY_PLACE = "birthday19";
    private static final Date STREAM_DATE = DateUtils.addDays(new Date(), 10);
    @Autowired
    SubscriptionAndIdentityService subscriptionAndIdentityService;
    @Autowired
    VerificationUtil verificationUtil;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;
    @Autowired
    private SubscriptionAndIdentityDAO subscriptionAndIdentityDAO;
    @Autowired
    private EventSourceDAO eventSourceDAO;
    @Autowired
    private SubscriptionsCacher subscriptionsCacher;
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;
    @Autowired
    private UnsubscribeMailManager unsubscribeMailManager;
    @Autowired
    private SubscriptionControllerInvoker controllerInvoker;
    @Autowired
    private SettingsControllerInvoker settingsControllerInvoker;

    private EmailSubscription grade() {
        EmailSubscription subscription = new EmailSubscription(null, EMAIL, NotificationType.SHOP_GRADE, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "213");
        subscription.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, null);
        return subscription;
    }

    private EmailSubscription forum() {
        EmailSubscription subscription = new EmailSubscription(null, ANOTHER_EMAIL, NotificationType.FORUM, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, null);
        return subscription;
    }

    private EmailSubscription advertising() {
        EmailSubscription subscription = new EmailSubscription(null, ANOTHER_EMAIL, NotificationType.ADVERTISING, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "213");
        subscription.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, null);
        return subscription;
    }

    @BeforeEach
    public void setUp() {
        blackBoxPassportService.doReturn(UID.getValue(), Collections.emptyList());
    }

    @Test
    void testPlatformAndPlaceParamsPersistance() throws Exception {
        EmailSubscription advertising = advertising();
        final String place = "some place";
        final String platform = "some platform";
        advertising.addParameter(EmailSubscriptionParam.PARAM_PLACE, place);
        advertising.addParameter(EmailSubscriptionParam.PARAM_PLATFORM, platform);
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, asList(advertising), false);
        List<EmailSubscription> actualSubscriptions = subscriptionAndIdentityService.getEmailSubscriptions(EMAIL);
        assertEquals(actualSubscriptions.size(), 1);
        EmailSubscription actualSubscription = actualSubscriptions.get(0);
        Map<String, String> actualParameters = actualSubscription.getParameters();
        assertEquals(place, actualParameters.get(EmailSubscriptionParam.PARAM_PLACE));
        assertEquals(platform, actualParameters.get(EmailSubscriptionParam.PARAM_PLATFORM));
    }

    @Test
    public void validReadTest() throws Exception {
        List<EmailSubscription> result = new ArrayList<EmailSubscription>() {{
            add(grade());
            add(forum());
        }};

        mockPassport(UID, EMAIL);
        settingsControllerInvoker.createSubscriptionsBySettings(UID, EMAIL, new SubscriptionSettings(false, true, false, false, false));

        mockPassport(UID, ANOTHER_EMAIL);
        settingsControllerInvoker.createSubscriptionsBySettings(UID, ANOTHER_EMAIL, new SubscriptionSettings(true, false, false, false, false));

        Set<EmailSubscriptionStatus> acceptedStatuses =
            EnumSet.of(EmailSubscriptionStatus.CONFIRMED, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UID);
        // в ответе все подписки CONFIRMED, NEED_SEND_CONFIRMATION, несмотря на то, что в базе есть и другие
        assertTrue(subscriptions.stream().allMatch(e -> acceptedStatuses.contains(e.getSubscriptionStatus())));
        assertFalse(subscriptionAndIdentityDAO.getSubscriptions(EMAIL)
            .stream().allMatch(e -> acceptedStatuses.contains(e.getSubscriptionStatus())));

        setStatus(EmailSubscriptionStatus.CONFIRMED, result);
        compareSubscriptions(result, subscriptions, false);
    }

    @Test
    public void validReadTestWithDifferentCasesEmail() throws Exception {
        EmailSubscription first = grade();
        first.setEmail("emaillower@email.ru");
        EmailSubscription second = advertising();
        second.setEmail("EMAILLOWER@email.ru");
        controllerInvoker.createSubscriptions(UID, "emaillower@email.ru", Collections.singletonList(first));
        controllerInvoker.createSubscriptions(UID, "EMAILLOWER@email.ru", Collections.singletonList(second));

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UID);
        setStatus(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION, Arrays.asList(first, second));
        compareSubscriptions(Arrays.asList(first, second), subscriptions, false);
    }

    @Test
    public void validPagerReadTest() throws Exception {
        EmailSubscription first = grade();
        EmailSubscription second = forum();
        List<EmailSubscription> result = new ArrayList<EmailSubscription>() {{
            add(first);
            add(second);
        }};
        setStatus(EmailSubscriptionStatus.CONFIRMED, result);

        mockPassport(UID, EMAIL);
        settingsControllerInvoker.createSubscriptionsBySettings(UID, EMAIL, new SubscriptionSettings(false, true, false, false, false));
        mockPassport(UID, ANOTHER_EMAIL
        );
        settingsControllerInvoker.createSubscriptionsBySettings(UID, ANOTHER_EMAIL, new SubscriptionSettings(true, false, false, false, false));

        // помним, что сортировка by id desc
        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UID, 1, 1);
        compareSubscriptions(asList(second), subscriptions, false);

        subscriptions = controllerInvoker.getSubscriptions(UID, 2, 1);
        compareSubscriptions(asList(first), subscriptions, false);

        subscriptions = controllerInvoker.getSubscriptions(UID, 1, 2);
        compareSubscriptions(result, subscriptions, false);

        subscriptions = controllerInvoker.getSubscriptions(UID, 2, 2);
        compareSubscriptions(Collections.emptyList(), subscriptions, false);
    }

    @Test
    public void illegalArgumentsTest() throws Exception {
        mockMvc.perform(get("/subscription/")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().json(toJson(
                new Error("BAD_REQUEST", "One of id, uid, uuid, yandexUid, returnMode should exist", 400))))
            .andDo(print())
            .andReturn();
    }

    @Test
    public void checkIdentityOrderTest() throws Exception {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UUID);
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(YANDEX_UID);

        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED, EmailOwnership.Source.PASSPORT);
        subscriptionAndIdentityDAO.createEmailOwnership(UUID, ANOTHER_EMAIL, EmailOwnership.Status.CONFIRMED, EmailOwnership.Source.PASSPORT);
        subscriptionAndIdentityDAO.createEmailOwnership(YANDEX_UID, ANOTHER_EMAIL2, EmailOwnership.Status.CONFIRMED, EmailOwnership.Source.PASSPORT);

        subscriptionAndIdentityDAO.saveSubscriptions(EMAIL,
            asList(new EmailSubscription(EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED)));
        subscriptionAndIdentityDAO.saveSubscriptions(ANOTHER_EMAIL,
            asList(new EmailSubscription(ANOTHER_EMAIL, NotificationType.FORUM, EmailSubscriptionStatus.CONFIRMED)));
        subscriptionAndIdentityDAO.saveSubscriptions(ANOTHER_EMAIL2,
            asList(new EmailSubscription(ANOTHER_EMAIL2, NotificationType.SHOP_GRADE, EmailSubscriptionStatus.CONFIRMED)));

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UUID, YANDEX_UID, UID);
        EmailSubscription firstResult = new EmailSubscription(null, EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED);
        compareSubscriptions(asList(firstResult), subscriptions, false);

        subscriptions = controllerInvoker.getSubscriptions(YANDEX_UID, UUID);
        EmailSubscription secondResult = new EmailSubscription(null, ANOTHER_EMAIL, NotificationType.FORUM, EmailSubscriptionStatus.CONFIRMED);
        compareSubscriptions(asList(secondResult), subscriptions, false);

        subscriptions = controllerInvoker.getSubscriptions(YANDEX_UID);
        EmailSubscription thirdResult = new EmailSubscription(null, ANOTHER_EMAIL2, NotificationType.SHOP_GRADE, EmailSubscriptionStatus.CONFIRMED);
        compareSubscriptions(asList(thirdResult), subscriptions, false);
    }

    @Test
    public void newModelSubscriptionsSimpleTest() throws Exception {
        EmailSubscription first = grade(); // grade
        controllerInvoker.createSubscriptions(UID, EMAIL, asList(first));
        first.setSubscriptionStatus(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UID);

        compareSubscriptions(asList(first), subscriptions, false);
        // по дефолту все считаются подписанными, кроме рекламы и журнала
        settingsControllerInvoker.checkSubscriptionsBySettings(UID, EMAIL, new SubscriptionSettings(true, true, false, true, false));
    }

    @Test
    public void unsubscribe() throws Exception {
        EmailSubscription first = grade(); // grade
        controllerInvoker.createSubscriptions(UID, EMAIL, asList(first));

        first.setSubscriptionStatus(EmailSubscriptionStatus.CONFIRMED);
        subscriptionAndIdentityDAO.saveSubscriptions(EMAIL, asList(first));

        first.setSubscriptionStatus(EmailSubscriptionStatus.UNSUBSCRIBED);
        controllerInvoker.unsubscribe(EMAIL, first.getSubscriptionType());
        compareSubscriptions(Collections.emptyList(), controllerInvoker.getSubscriptions(UID, first.getSubscriptionType()), false);
    }

    @Test
    public void isSubscribed() throws Exception {
        EmailSubscription first = advertising();
        assertEquals(Collections.singletonList(
            new EmailSubscriptionStatusDto(first.getSubscriptionType(), false)
        ), controllerInvoker.isSubscribed(EMAIL, first.getSubscriptionType()));

        controllerInvoker.createSubscriptions(UID, EMAIL, asList(first), true);
        assertEquals(Collections.singletonList(
            new EmailSubscriptionStatusDto(first.getSubscriptionType(), true)
        ), controllerInvoker.isSubscribed(EMAIL, first.getSubscriptionType()));
    }

    @Test
    public void newModelSubscriptionsDataParamSaved() throws Exception {
        EmailSubscription subscription = new EmailSubscription(EMAIL, NotificationType.PA_ON_SALE, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "212");
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "212");
        subscription.addParameter(EmailSubscriptionParam.PARAM_PRICE, "0");
        subscription.addParameter(EmailSubscriptionParam.PARAM_YANDEX_UID, "123yandex");
        subscription.addParameter(EmailSubscriptionParam.PARAM_UID, "123");
        subscription.addParameter(EmailSubscriptionParam.PARAM_UUID, "uuid");

        controllerInvoker.createSubscriptions(UID, ANOTHER_EMAIL, asList(subscription));

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UID);

        assertEquals(1, subscriptions.size());
        assertEquals("123yandex", subscriptions.get(0).getParameters().get(EmailSubscriptionParam.PARAM_YANDEX_UID));
        assertEquals("123", subscriptions.get(0).getParameters().get(EmailSubscriptionParam.PARAM_UID));
        assertEquals("uuid", subscriptions.get(0).getParameters().get(EmailSubscriptionParam.PARAM_UUID));
    }

    @Test
    public void newModelSubscriptionsSaveSubscriptionsBySettingsAndBySubscriptions() throws Exception {
        SubscriptionSettings subscriptionSettings = new SubscriptionSettings(false, false, false, true, false);
        mockPassport(UID, EMAIL);
        settingsControllerInvoker.createSubscriptionsBySettings(UID, EMAIL, subscriptionSettings);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UID);
        List<EmailSubscription> result = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.WISHLIST, EmailSubscriptionStatus.CONFIRMED));
        }};
        compareSubscriptions(result, subscriptions, false);
        settingsControllerInvoker.checkSubscriptionsBySettings(UID, EMAIL, subscriptionSettings);

        EmailSubscription advertising = advertising();
        controllerInvoker.createSubscriptions(UID, EMAIL, asList(advertising));

        subscriptions = controllerInvoker.getSubscriptions(UID);
        result = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.WISHLIST, EmailSubscriptionStatus.CONFIRMED));
            add(new EmailSubscription(EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED));
        }};

        compareSubscriptions(result, subscriptions, false);
        settingsControllerInvoker.checkSubscriptionsBySettings(UID, EMAIL, new SubscriptionSettings(false, false, true, true, false));

        subscriptionSettings = new SubscriptionSettings(false, false, false, true, false);
        settingsControllerInvoker.createSubscriptionsBySettings(UID, EMAIL, subscriptionSettings);

        subscriptions = controllerInvoker.getSubscriptions(UID);
        result = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.WISHLIST, EmailSubscriptionStatus.CONFIRMED));
        }};

        compareSubscriptions(result, subscriptions, false);
        settingsControllerInvoker.checkSubscriptionsBySettings(UID, EMAIL, new SubscriptionSettings(false, false, false, true, false));
    }

    @Test
    public void adsSubscriptionWithForceConfirmation() throws Exception {
        EmailSubscription advertising = advertising();
        advertising.addParameter(EmailSubscriptionParam.PARAM_ADS_LOCATION, "mall");
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, asList(advertising), true);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(YANDEX_UID);
        List<EmailSubscription> result = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED));
        }};
        compareSubscriptions(result, subscriptions, false);
    }

    @Test
    public void childSubscription() throws Exception {
        EmailSubscription advertising = advertising();
        advertising.addParameter(EmailSubscriptionParam.PARAM_CHILDREN, "[" +
            "{\"gender\":\"male\", \"dateOfBirth\":\"2011-12-03\"}," +
            "{\"gender\":\"female\", \"dateOfBirth\":\"2011-12-04\"}" +
            "]");
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, asList(advertising), false);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(YANDEX_UID);
        List<EmailSubscription> result = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION));
        }};
        compareSubscriptions(result, subscriptions, false);
    }

    @Test
    public void childSubscriptionWithEmptyChildren() throws Exception {
        EmailSubscription advertising = advertising();
        advertising.addParameter(EmailSubscriptionParam.PARAM_CHILDREN, "");
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, asList(advertising), false);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(YANDEX_UID);
        List<EmailSubscription> result = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION));
        }};
        assertEquals("", subscriptions.get(0).getParameters().get(EmailSubscriptionParam.PARAM_CHILDREN));
        compareSubscriptions(result, subscriptions, false);
    }

    @Test
    public void childSubscriptionNotValidDateOfBirth() throws Exception {
        EmailSubscription advertising = advertising();
        advertising.addParameter(EmailSubscriptionParam.PARAM_CHILDREN, "[" +
            "{\"gender\":\"male\", \"dateOfBirth\":\"2011-12-03\"}," +
            "{\"gender\":\"female\", \"dateOfBirth\":\"2011.12.04\"}" +
            "]");
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, asList(advertising), false, 400);
    }

    @Test
    public void childSubscriptionNotValidGender() throws Exception {
        EmailSubscription advertising = advertising();
        advertising.addParameter(EmailSubscriptionParam.PARAM_CHILDREN, "[" +
            "{\"gender\":\"not_valid\", \"dateOfBirth\":\"2011-12-03\"}" +
            "]");
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, asList(advertising), false, 400);
    }

    @Test
    public void newModelSubscriptionsSaveSubscriptionsBySettingsWithAds() throws Exception {
        mockPassport(UID, EMAIL);
        EmailSubscription first = advertising();
        controllerInvoker.createSubscriptions(UID, EMAIL, asList(first));

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UID);
        List<EmailSubscription> result = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED));
        }};

        compareSubscriptions(result, subscriptions, false);
        // по дефолту все, кроме рекламы и журнала и так считаются подписанными
        settingsControllerInvoker.checkSubscriptions(UID.getValue(), EMAIL, SettingsControllerTest.SETTINGS_WITHOUT_JOURNAL);
    }

    @Test
    public void getByTypesTest() throws Exception {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, ANOTHER_EMAIL, EmailOwnership.Status.CONFIRMED, EmailOwnership.Source.PASSPORT);
        subscriptionAndIdentityDAO.saveSubscriptions(ANOTHER_EMAIL, asList(
            new EmailSubscription(ANOTHER_EMAIL, NotificationType.FORUM, EmailSubscriptionStatus.CONFIRMED),
            new EmailSubscription(ANOTHER_EMAIL, NotificationType.CART, EmailSubscriptionStatus.CONFIRMED)));

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UID, new NotificationType[]{NotificationType.SHOP_GRADE, NotificationType.CART});
        assertEquals(1, subscriptions.size());
        compareSubscriptions(
            asList(new EmailSubscription(null, ANOTHER_EMAIL, NotificationType.CART, EmailSubscriptionStatus.CONFIRMED)),
            subscriptions, false);
    }

    @Test
    public void subscribedOnAllJournalDefaultSubscriptions() throws Exception {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
            EmailOwnership.Source.PASSPORT);

        List<EmailSubscriptionStatusDto> actualResult = controllerInvoker.isSubscribed(EMAIL,
            Arrays.asList(
                NotificationType.JOURNAL_COMMENTS_REJECTION,
                NotificationType.JOURNAL_MODERATION_RESULTS,
                NotificationType.JOURNAL_NEW_ANSWERS_ON_COMMENTS,
                NotificationType.JOURNAL_NEW_COMMENTS
            )
        );

        for (EmailSubscriptionStatusDto dto : actualResult) {
            assertTrue(dto.isSubscribed());
        }
    }

    @Test
    public void subscribedOnVideoDefaultSubscriptions() throws Exception {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
            EmailOwnership.Source.PASSPORT);

        List<EmailSubscriptionStatusDto> actualResult = controllerInvoker.isSubscribed(EMAIL,
            Arrays.asList(
                NotificationType.VIDEO_NEW_COMMENTS
            )
        );

        for (EmailSubscriptionStatusDto dto : actualResult) {
            assertTrue(dto.isSubscribed());
        }
    }

    private Long createLiveStreamSubscription(long streamId) throws Exception {
        return createLiveStreamSubscription(streamId, STREAM_DATE);
    }

    private Long createLiveStreamSubscription(long streamId, Date streamTime) throws Exception {
        EmailSubscription emailSubscription = new EmailSubscription(null, NotificationType.LIVE_STREAM,
            EmailSubscriptionStatus.CONFIRMED);
        emailSubscription.addParameter(EmailSubscriptionParam.PARAM_LIVE_STREAM_ID, String.valueOf(streamId));
        emailSubscription.addParameter(EmailSubscriptionParam.PARAM_LIVE_STREAM_START_TIME, String.valueOf(streamTime.getTime()));
        List<EmailSubscription> emailSubscriptions = controllerInvoker.createSubscriptions(UID, EMAIL, Collections.singletonList(emailSubscription));
        return emailSubscriptions.get(0).getId();
    }

    @Test
    public void filterByStreamIdTest() throws Exception {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
            EmailOwnership.Source.PASSPORT);

        final long desiredStreamId = 100L;
        createLiveStreamSubscription(desiredStreamId + 1);
        createLiveStreamSubscription(desiredStreamId + 2);
        final long expectedSubscriptionId = createLiveStreamSubscription(desiredStreamId);
        createLiveStreamSubscription(desiredStreamId - 1);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptionsByStreamId(UID, NotificationType.LIVE_STREAM, desiredStreamId);
        assertEquals(1, subscriptions.size());
        assertEquals(expectedSubscriptionId, (long) subscriptions.get(0).getId());
    }


    public void prepareLiveStreamingSubscription(int addMinutes, NotificationSubtype expectedSubtype) throws Exception {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
            EmailOwnership.Source.PASSPORT);

        final long desiredStreamId = 100L;
        createLiveStreamSubscription(desiredStreamId, DateUtils.addMinutes(new Date(), addMinutes));
        subscriptionAndIdentityService.reloadNotificationPool();
        List<NotificationEvent> eventsByEmail = eventSourceDAO.getEventsByEmail(EMAIL);
        Assertions.assertEquals(1, eventsByEmail.size());
        Assertions.assertEquals(expectedSubtype, eventsByEmail.get(0).getNotificationSubtype());
    }

    @Test
    public void checkLiveStreamingLetterSchedulingTwoDaysBefore() throws Exception {
        prepareLiveStreamingSubscription(2 * 60 * 24, NotificationSubtype.LIVE_STREAM_LONG_BEFORE); // 2 day
    }

    @Test
    public void checkLiveStreamingLetterSchedulingHalfHourBefore() throws Exception {
        prepareLiveStreamingSubscription(30, NotificationSubtype.LIVE_STREAM_SHORT_BEFORE); // half hour
    }

    @Test
    public void checkLiveStreamingLetterSchedulingHourAfter() throws Exception {
        prepareLiveStreamingSubscription(-30, NotificationSubtype.LIVE_STREAM_AFTER); // half hour
    }

    @Test
    public void checkLiveStreamNotificationScheduled() throws Exception {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
            EmailOwnership.Source.PASSPORT);
        final long desiredStreamId = 100L;
        List<NotificationEvent> events = eventSourceDAO.getEventsByEmail(EMAIL);
        Assertions.assertEquals(0, events.size());

        createLiveStreamSubscription(desiredStreamId);

        subscriptionAndIdentityService.reloadNotificationPool();
        events = eventSourceDAO.getEventsByEmail(EMAIL);
        Assertions.assertEquals(1, events.size());
        NotificationEvent event = events.get(0);
        Assertions.assertEquals(NotificationSubtype.LIVE_STREAM_LONG_BEFORE, event.getNotificationSubtype());
        Assertions.assertEquals(DateUtils.addHours(STREAM_DATE, -24).getTime() / 1000,
            event.getSendTime().getTime() / 1000);
    }

    @Test
    public void filterByQuestionIdTest() {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
            EmailOwnership.Source.PASSPORT);
        Function<Long, Long> subscriptionFactory =
            questionId -> {
                EmailSubscription emailSubscription = new EmailSubscription(null, NotificationType.QA_NEW_ANSWERS,
                    EmailSubscriptionStatus.CONFIRMED);
                emailSubscription.addParameter(EmailSubscriptionParam.PARAM_QUESTION_ID, String.valueOf(questionId));
                subscriptionAndIdentityDAO.saveSubscriptions(EMAIL, asList(emailSubscription));
                return emailSubscription.getId();
            };
        final long desiredQuestionId = 100L;
        subscriptionFactory.apply(desiredQuestionId + 1);
        subscriptionFactory.apply(desiredQuestionId + 2);
        final long expectedSubscriptionId = subscriptionFactory.apply(desiredQuestionId);
        subscriptionFactory.apply(desiredQuestionId - 1);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptionsByQuestionId(UID, NotificationType.QA_NEW_ANSWERS, desiredQuestionId);
        assertEquals(1, subscriptions.size());
        assertEquals(expectedSubscriptionId, (long) subscriptions.get(0).getId());
    }

    /**
     * Даже если подписчиков на вопрос несколько, при указании идентификатора вернётся только тот, чей идентификатор
     * мы предоставили.
     */
    @Test public void findOnlyOneQuestionSubscriber() {
        createQuestionWithTwoSubscribers();

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptionsByQuestionId(UID, NotificationType.QA_NEW_ANSWERS, 100L);
        assertEquals(1, subscriptions.size());
        assertEquals(EMAIL, subscriptions.get(0).getEmail());
    }

    /**
     * Если не указать идентификатор пользователя, то вернётся сообщение об ошибке
     */
    @Test
    public void cannotFindQuestionSubscriberWithoutIdentity() throws IOException {
        createQuestionWithTwoSubscribers();

        final String[] answer = new String[1];
        controllerInvoker.getSubscriptionsByQuestionId(NotificationType.QA_NEW_ANSWERS, 100L, ReturnMode.SINGLE,
                (m) -> {
                    try {
                        answer[0] = m.getResponse().getContentAsString();
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                });

        JSONAssert.assertEquals(TestUtil.stringFromFile("/data/subscription/wrong_request.json"), answer[0], true);
    }

    /**
     * Если подписчиков на вопрос несколько, при указании {@code returnMode=ALL} вернутся они все
     */
    @Test
    public void findAllQuestionSubscribers() {
        createQuestionWithTwoSubscribers();

        List<EmailSubscription> subscriptions = new ArrayList<>();
        controllerInvoker.getSubscriptionsByQuestionId(NotificationType.QA_NEW_ANSWERS, 100L, ReturnMode.ALL,
                (m) -> subscriptions.addAll(controllerInvoker.getSubscriptions(m)));
        assertEquals(2, subscriptions.size());
        Set<String> expectedEmails = Sets.newHashSet(EMAIL, ANOTHER_EMAIL);
        assertEquals(expectedEmails, subscriptions.stream().map(EmailSubscription::getEmail).collect(Collectors.toSet()));
    }

    /**
     * Если параметр не указан, то даже с returnMode=ALL вернётся пустой результат
     */
    @Test
    public void emptyResultWhenQuestionIsNotDefined() {
        createQuestionWithTwoSubscribers();

        List<EmailSubscription> subscriptions = new ArrayList<>();
        controllerInvoker.getSubscriptionsWithoutQuestionId(NotificationType.QA_NEW_ANSWERS, ReturnMode.ALL,
                (m) -> subscriptions.addAll(controllerInvoker.getSubscriptions(m)));
        assertEquals(0, subscriptions.size());
    }

    private void createQuestionWithTwoSubscribers() {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(YANDEX_UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
                EmailOwnership.Source.PASSPORT);
        subscriptionAndIdentityDAO.createEmailOwnership(YANDEX_UID, ANOTHER_EMAIL, EmailOwnership.Status.CONFIRMED,
                EmailOwnership.Source.PASSPORT);

        EmailSubscription emailSubscription = new EmailSubscription(null, NotificationType.QA_NEW_ANSWERS,
                EmailSubscriptionStatus.CONFIRMED);
        emailSubscription.addParameter(EmailSubscriptionParam.PARAM_QUESTION_ID, String.valueOf(100L));
        subscriptionAndIdentityDAO.saveSubscriptions(EMAIL, asList(emailSubscription));

        emailSubscription = new EmailSubscription(null, NotificationType.QA_NEW_ANSWERS,
                EmailSubscriptionStatus.CONFIRMED);
        emailSubscription.addParameter(EmailSubscriptionParam.PARAM_QUESTION_ID, String.valueOf(100L));
        subscriptionAndIdentityDAO.saveSubscriptions(ANOTHER_EMAIL, asList(emailSubscription));
    }

    @Test
    public void filterByAnswerIdTest() {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
            EmailOwnership.Source.PASSPORT);
        Function<Long, Long> subscriptionFactory =
            answerId -> {
                EmailSubscription emailSubscription = new EmailSubscription(null, NotificationType.QA_NEW_COMMENTS,
                    EmailSubscriptionStatus.CONFIRMED);
                emailSubscription.addParameter(EmailSubscriptionParam.PARAM_ANSWER_ID, String.valueOf(answerId));
                subscriptionAndIdentityDAO.saveSubscriptions(EMAIL, asList(emailSubscription));
                return emailSubscription.getId();
            };
        final long desiredAnswerId = 100L;
        subscriptionFactory.apply(desiredAnswerId + 1);
        subscriptionFactory.apply(desiredAnswerId + 2);
        final long expectedSubscriptionId = subscriptionFactory.apply(desiredAnswerId);
        subscriptionFactory.apply(desiredAnswerId - 1);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptionsByAnswerId(UID, NotificationType.QA_NEW_COMMENTS, desiredAnswerId);
        assertEquals(1, subscriptions.size());
        assertEquals(expectedSubscriptionId, (long) subscriptions.get(0).getId());
    }

    @Test
    public void filterByModelIdTest() {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
            EmailOwnership.Source.PASSPORT);
        Function<Long, Long> subscriptionFactory =
            modelId -> {
                EmailSubscription emailSubscription = new EmailSubscription(null, NotificationType.PRICE_DROP,
                    EmailSubscriptionStatus.CONFIRMED);
                emailSubscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, String.valueOf(modelId));
                subscriptionAndIdentityDAO.saveSubscriptions(EMAIL, asList(emailSubscription));
                return emailSubscription.getId();
            };
        final long desiredModelId = 100L;
        subscriptionFactory.apply(desiredModelId + 1);
        subscriptionFactory.apply(desiredModelId + 2);
        final long expectedSubscriptionId = subscriptionFactory.apply(desiredModelId);
        subscriptionFactory.apply(desiredModelId - 1);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UID, NotificationType.PRICE_DROP, desiredModelId);
        assertEquals(1, subscriptions.size());
        assertEquals(expectedSubscriptionId, (long) subscriptions.get(0).getId());
    }

    @Test
    public void filterByStatusTest() {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
            EmailOwnership.Source.PASSPORT);
        Function<EmailSubscriptionStatus, Long> subscriptionFactory =
            subscriptionStatus -> {
                EmailSubscription emailSubscription = new EmailSubscription(null, NotificationType.PRICE_DROP,
                    subscriptionStatus);
                subscriptionAndIdentityDAO.saveSubscriptions(EMAIL, asList(emailSubscription));
                return emailSubscription.getId();
            };
        subscriptionFactory.apply(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        subscriptionFactory.apply(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        final long expectedSubscriptionId = subscriptionFactory.apply(EmailSubscriptionStatus.CONFIRMED);

        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(UID, NotificationType.PRICE_DROP,
            EmailSubscriptionStatus.CONFIRMED);
        assertEquals(1, subscriptions.size());
        assertEquals(expectedSubscriptionId, (long) subscriptions.get(0).getId());
    }

    @Test
    public void defaultFilterByStatusTest() {
        defaultFilterByStatusTest(() -> controllerInvoker.getSubscriptions(UID, NotificationType.PRICE_DROP));
    }

    @Test
    public void emptyFilterByStatusTest() {
        defaultFilterByStatusTest(() -> controllerInvoker.getSubscriptions(UID, NotificationType.PRICE_DROP, new String[]{""}));
    }

    private void defaultFilterByStatusTest(Supplier<List<EmailSubscription>> subscriptionsGetter) {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED,
            EmailOwnership.Source.PASSPORT);
        Function<EmailSubscriptionStatus, Long> subscriptionFactory =
            subscriptionStatus -> {
                EmailSubscription emailSubscription = new EmailSubscription(null, NotificationType.PRICE_DROP,
                    subscriptionStatus);
                subscriptionAndIdentityDAO.saveSubscriptions(EMAIL, asList(emailSubscription));
                return emailSubscription.getId();
            };
        Set<Long> expectedSubscriptionsIds = new HashSet<>();
        expectedSubscriptionsIds.add(subscriptionFactory.apply(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION));
        subscriptionFactory.apply(EmailSubscriptionStatus.UNSUBSCRIBED);
        expectedSubscriptionsIds.add(subscriptionFactory.apply(EmailSubscriptionStatus.CONFIRMED));

        Set<Long> actualSubscriptionsIds = subscriptionsGetter.get().stream().map(EmailSubscription::getId)
            .collect(Collectors.toSet());
        assertEquals(expectedSubscriptionsIds, actualSubscriptionsIds);
    }


    @Test
    public void postAndGetTestOnDifferentEmails() throws Exception {
        //UUID
        List<EmailSubscription> subscriptions = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.CART, null));
        }};
        controllerInvoker.createSubscriptions(UUID, EMAIL, subscriptions);

        EmailSubscription result = new EmailSubscription(EMAIL, NotificationType.CART, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        compareSubscriptions(Collections.singletonList(result), controllerInvoker.getSubscriptions(UUID), false);

        //YANDEX_UID
        subscriptions = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(ANOTHER_EMAIL, NotificationType.FORUM, null));
        }};
        controllerInvoker.createSubscriptions(YANDEX_UID, ANOTHER_EMAIL, subscriptions);

        result = new EmailSubscription(ANOTHER_EMAIL, NotificationType.FORUM, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        compareSubscriptions(Collections.singletonList(result), controllerInvoker.getSubscriptions(YANDEX_UID), false);

        //UID
        subscriptions = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(ANOTHER_EMAIL2, NotificationType.SHOP_GRADE, null));
        }};
        controllerInvoker.createSubscriptions(UID, ANOTHER_EMAIL2, subscriptions);

        result = new EmailSubscription(ANOTHER_EMAIL2, NotificationType.SHOP_GRADE, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        compareSubscriptions(Collections.singletonList(result), controllerInvoker.getSubscriptions(UID), false);
    }

    @Test
    public void postAndGetTestOnOneEmail() throws Exception {
        //UUID
        List<EmailSubscription> subscriptions = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.CART, null));
        }};
        controllerInvoker.createSubscriptions(UUID, EMAIL, subscriptions);

        List<EmailSubscription> result = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.CART, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION));
        }};
        compareSubscriptions(result, controllerInvoker.getSubscriptions(UUID), false);

        //YANDEX_UID
        subscriptions = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.FORUM, null));
        }};
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, subscriptions);

        result.add(new EmailSubscription(EMAIL, NotificationType.FORUM, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION));
        compareSubscriptions(result, controllerInvoker.getSubscriptions(YANDEX_UID), false);

        //UID
        subscriptions = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.SHOP_GRADE, null));
        }};
        controllerInvoker.createSubscriptions(UID, EMAIL, subscriptions);

        result.add(new EmailSubscription(EMAIL, NotificationType.SHOP_GRADE, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION));
        compareSubscriptions(result, controllerInvoker.getSubscriptions(UID), false);
    }

    @Test
    public void putTestForNewModel() throws Exception {
        EmailSubscription subscription = new EmailSubscription(EMAIL, NotificationType.PA_ON_SALE, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "212");
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "212");
        subscription.addParameter(EmailSubscriptionParam.PARAM_PRICE, "0");
        controllerInvoker.createSubscriptions(UID, EMAIL, Collections.singletonList(subscription));

        Long id = controllerInvoker.getSubscriptions(UID).get(0).getId();

        EmailSubscription result = new EmailSubscription(id, EMAIL, NotificationType.PA_ON_SALE, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        result.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "212");
        result.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "225"); //default toJson
        result.addParameter(EmailSubscriptionParam.PARAM_PRICE, "0");
        result.addParameter(EmailSubscriptionParam.PARAM_RATING, null);
        result.addParameter(EmailSubscriptionParam.PARAM_CURRENCY, "RUR");
        result.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, null);
        result.addParameter(EmailSubscriptionParam.PARAM_UID, String.valueOf(UID.getValue()));
        result.addParameter(EmailSubscriptionParam.PARAM_UUID, null);
        compareSubscriptions(Collections.singletonList(result), controllerInvoker.getSubscriptions(UID), true);

        subscription.setSubscriptionType(NotificationType.PA_ON_SALE);
        subscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "213");
        subscription.addParameter(EmailSubscriptionParam.PARAM_RATING, "10");
        subscription.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, "name");
        subscription.setId(result.getId());
        subscription.setSubscriptionStatus(null);
        controllerInvoker.updateSubscription(UID, subscription);

        result = new EmailSubscription(result.getId(), EMAIL, NotificationType.PA_ON_SALE, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        result.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "213");
        result.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "225");
        result.addParameter(EmailSubscriptionParam.PARAM_PRICE, "0");
        result.addParameter(EmailSubscriptionParam.PARAM_RATING, "10");
        result.addParameter(EmailSubscriptionParam.PARAM_CURRENCY, "RUR");
        result.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, "name");
        result.addParameter(EmailSubscriptionParam.PARAM_UID, String.valueOf(UID.getValue()));
        result.addParameter(EmailSubscriptionParam.PARAM_UUID, null);

        compareSubscriptions(Collections.singletonList(result), controllerInvoker.getSubscriptions(UID), true);
    }

    @Test
    public void putTestForNewModelNeedSendConfirmation() throws Exception {
        EmailSubscription subscription = new EmailSubscription(EMAIL, NotificationType.ADVERTISING, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, "");
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "213");
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, Collections.singletonList(subscription));

        Long id = controllerInvoker.getSubscriptions(YANDEX_UID).get(0).getId();

        EmailSubscription result = new EmailSubscription(id, EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        result.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, "");
        result.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "213");
        compareSubscriptions(Collections.singletonList(result), controllerInvoker.getSubscriptions(YANDEX_UID), true);

        subscription.setSubscriptionType(NotificationType.ADVERTISING);
        subscription.setId(id);
        subscription.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, "userName2");
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "214");
        controllerInvoker.updateSubscription(YANDEX_UID, subscription);

        result = new EmailSubscription(result.getId(), EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        result.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, "userName2");
        result.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "214");

        compareSubscriptions(Collections.singletonList(result), controllerInvoker.getSubscriptions(YANDEX_UID), true);
    }

    @Test
    public void sendConfirmationNotificationWhenEmailNotConfirmedAdvertising() throws Exception {
        sendConfirmationNotificationWhenEmailNotConfirmed(NotificationType.ADVERTISING,
            NotificationSubtype.CONFIRM_SUBSCRIPTION);
    }

    @Test
    public void sendConfirmationNotificationWhenEmailNotConfirmedStoreAdvertising() throws Exception {
        sendConfirmationNotificationWhenEmailNotConfirmed(NotificationType.STORE_ADVERTISING,
            NotificationSubtype.CONFIRM_STORE_SUBSCRIPTION);
    }


    public void sendConfirmationNotificationWhenEmailNotConfirmed(NotificationType notificationType,
                                                                  NotificationSubtype notificationSubtype)
        throws Exception {
        EmailSubscription subscription = new EmailSubscription(null, notificationType, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, "userName");
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "213");
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, Collections.singletonList(subscription));

        Long id = controllerInvoker.getSubscriptions(YANDEX_UID).get(0).getId();

        NotificationEvent lastEvent = eventSourceDAO.getLastEventByEmail(EMAIL);
        assertNotNull(lastEvent);
        assertEquals(notificationSubtype, lastEvent.getNotificationSubtype());
        assertEquals(id.toString(), lastEvent.getNotificationData().get(NotificationEventDataName.SUBSCRIPTION_ID));
    }

    List<NotificationEvent> getScheduledLetters(NotificationSubtype subtype) {
        subscriptionAndIdentityService.reloadNotificationPool();
        return eventSourceDAO.getMailEventsForProcessing(
            Collections.singletonList(subtype), 100, NotificationEventStatus.values());
    }

    @Test
    @Disabled
    public void sendEventToLoyaltyWhenEmailAlreadyConfirmed() throws Exception {
        subscriptionsCacher.createOrGetUserIdentity(UID, EMAIL);
        subscriptionsCacher.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED, EmailOwnership.Source.PASSPORT);

        long region = 213L;
        EmailSubscription subscription = new EmailSubscription(null, NotificationType.STORE_ADVERTISING, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, String.valueOf(region));
        controllerInvoker.createSubscriptions(UID, EMAIL, Collections.singletonList(subscription), false, 201);

        assertNull(eventSourceDAO.getLastEventByEmail(EMAIL));

        verify(marketLoyaltyClient).processEvent(argThat(allOf(
            isA(SubscriptionEventDto.class),
            hasProperty("notificationType", equalTo(NotificationType.STORE_ADVERTISING)),
            hasProperty("region", equalTo(region)),
            hasProperty("email", equalTo(EMAIL)),
            hasProperty("platform", equalTo(MarketPlatform.BLUE)),
            hasProperty("uid", equalTo(UID.getValue()))
        )));
    }

    @Test
    @Disabled
    public void sendEventToLoyaltyWhenForceConfirm() throws Exception {
        long region = 213L;
        EmailSubscription subscription = new EmailSubscription(null, NotificationType.STORE_ADVERTISING, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, String.valueOf(region));
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, Collections.singletonList(subscription), true, 201);

        assertNull(eventSourceDAO.getLastEventByEmail(EMAIL));

        verify(marketLoyaltyClient).processEvent(argThat(allOf(
            isA(SubscriptionEventDto.class),
            hasProperty("notificationType", equalTo(NotificationType.STORE_ADVERTISING)),
            hasProperty("region", equalTo(region)),
            hasProperty("email", equalTo(EMAIL)),
            hasProperty("platform", equalTo(MarketPlatform.BLUE)),
            hasProperty("lastUnsubscribeDate", nullValue())
        )));
    }

    @Test
    public void notSendEventToLoyaltyIfSubscriptionAlreadyExists() throws Exception {
        long region = 213L;
        EmailSubscription subscription = new EmailSubscription(null, NotificationType.STORE_ADVERTISING, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, String.valueOf(region));
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, Collections.singletonList(subscription), true, 201);
        reset(marketLoyaltyClient);


        //will not grand coupon for already subscribed email
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, Collections.singletonList(subscription), false, 201);
        verifyZeroInteractions(marketLoyaltyClient);
    }

    @Test
    @Disabled
    public void sendLastUnsubscribeDateToLoyalty() throws Exception {
        long region = 213L;
        //create subscription
        EmailSubscription subscription = new EmailSubscription(null, NotificationType.STORE_ADVERTISING, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, String.valueOf(region));
        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, Collections.singletonList(subscription), true, 201);

        unsubscribeMailManager.unsubscribe(EMAIL, NotificationType.STORE_ADVERTISING);

        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, Collections.singletonList(subscription), true, 201);
        verify(marketLoyaltyClient).processEvent(argThat(allOf(
            isA(SubscriptionEventDto.class),
            hasProperty("email", equalTo(EMAIL)),
            hasProperty("lastUnsubscribeDate", notNullValue())
        )));
    }

    @Test
    public void sendConfirmationNotificationWhenNotConfirmedEmailAdvertising() throws Exception {
        testSendConfirmationNotificationWhenNotConfirmedEmail(NotificationType.ADVERTISING,
            NotificationSubtype.CONFIRM_SUBSCRIPTION);
    }

    @Test
    public void sendConfirmationNotificationWhenNotConfirmedEmailStoreAdvertising() throws Exception {
        testSendConfirmationNotificationWhenNotConfirmedEmail(NotificationType.STORE_ADVERTISING,
            NotificationSubtype.CONFIRM_STORE_SUBSCRIPTION);
    }

    private void testSendConfirmationNotificationWhenNotConfirmedEmail(NotificationType notificationType,
                                                                       NotificationSubtype notificationSubtype
    ) throws Exception {
        long region = 213L;
        EmailSubscription subscription = new EmailSubscription(null, notificationType, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, String.valueOf(region));
        controllerInvoker.createSubscriptions(UID, EMAIL, Collections.singletonList(subscription), false, 201);

        EmailSubscription emailSubscription = controllerInvoker.getSubscriptions(UID).get(0);
        assertEquals(UID.getValue(), emailSubscription.getUid());

        checkDoubleOptInLetterScheduledForAuthorized(notificationSubtype, emailSubscription.getId());
    }

    private NotificationEvent checkDoubleOptInLetterScheduled(NotificationSubtype notificationSubtype,
                                                              long subscriptionId) {
        List<NotificationEvent> letters = getScheduledLetters(notificationSubtype);
        assertEquals(1, letters.size());
        NotificationEvent letter = letters.get(0);
        assertNotNull(letter);
        assertEquals(notificationSubtype, letter.getNotificationSubtype());
        assertEquals(subscriptionId, Long.parseLong(letter.getNotificationData().get(
            NotificationEventDataName.SUBSCRIPTION_ID)));
        return letter;
    }

    private NotificationEvent checkDoubleOptInLetterScheduledForAuthorized(NotificationSubtype notificationSubtype,
                                                                           long subscriptionId) {
        NotificationEvent letter = checkDoubleOptInLetterScheduled(notificationSubtype, subscriptionId);
        assertEquals(UID.getValue(), letter.getUid());
        return letter;
    }

    private NotificationEvent checkDoubleOptInLetterScheduledForUnAuthorized(NotificationSubtype notificationSubtype,
                                                                             long subscriptionId) {
        NotificationEvent letter = checkDoubleOptInLetterScheduled(notificationSubtype, subscriptionId);
        assertNull(letter.getUid());
        return letter;
    }

    @Test
    public void createSubscription() throws Exception {
        EmailSubscription subscription = new EmailSubscription(EMAIL, NotificationType.PA_ON_SALE, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "212");
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "212");
        subscription.addParameter(EmailSubscriptionParam.PARAM_PRICE, "0");
        controllerInvoker.createSubscriptions(UID, EMAIL, Collections.singletonList(subscription));

        Long id = controllerInvoker.getSubscriptions(UID).get(0).getId();

        EmailSubscription result = new EmailSubscription(id, EMAIL, NotificationType.PA_ON_SALE,
            EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        result.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "212");
        result.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "225"); //default toJson
        result.addParameter(EmailSubscriptionParam.PARAM_PRICE, "0");
        result.addParameter(EmailSubscriptionParam.PARAM_RATING, null);
        result.addParameter(EmailSubscriptionParam.PARAM_CURRENCY, "RUR");
        result.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, null);
        result.addParameter(EmailSubscriptionParam.PARAM_UID, String.valueOf(UID.getValue()));
        result.addParameter(EmailSubscriptionParam.PARAM_UUID, null);

        compareSubscriptions(Collections.singletonList(result), controllerInvoker.getSubscriptions(UID), true);
    }

    @Test
    public void deleteTestForNewModelUid() throws Exception {
        deleteTestForNewModel(UID);
    }

    @Test
    public void deleteTestForNewModelYandexUid() throws Exception {
        deleteTestForNewModel(YANDEX_UID);
    }

    public void deleteTestForNewModel(Identity<?> identity) throws Exception {
        EmailSubscription subscription = new EmailSubscription(EMAIL, NotificationType.PA_ON_SALE, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "212");
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "2");
        subscription.addParameter(EmailSubscriptionParam.PARAM_PRICE, "0");
        controllerInvoker.createSubscriptions(identity, EMAIL, Collections.singletonList(subscription));

        Long id = controllerInvoker.getSubscriptions(identity).get(0).getId();

        controllerInvoker.deleteSubscription(identity, id);

        compareSubscriptions(Collections.emptyList(), controllerInvoker.getSubscriptions(identity), false);
    }

    @Test
    void subscriptionCreatedByYandexuidShouldBeReturnedForUidWithSameEmail() throws Exception {
        mockPassport(UID, EMAIL);
        EmailSubscription subscription = new EmailSubscription(EMAIL, NotificationType.STORE_ADVERTISING, null);
        subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "213");

        controllerInvoker.createSubscriptions(YANDEX_UID, EMAIL, Collections.singletonList(subscription));

        List<EmailSubscription> actualSubscriptions = controllerInvoker.getSubscriptions(UID);
        assertEquals(1, actualSubscriptions.size());
        assertEquals(NotificationType.STORE_ADVERTISING, actualSubscriptions.get(0).getSubscriptionType());
    }

    @Test
    public void testDeleteOldSubscription() {
        // создали подписку без email
        EmailSubscription subscription =
            new EmailSubscription(null, null, NotificationType.CART, EmailSubscriptionStatus.CONFIRMED);
        subscriptionAndIdentityDAO.saveSubscriptions(null, Collections.singletonList(subscription));

        // удалили подписку
        EmailSubscription deleted =
            new EmailSubscription(subscription.getId(), null, NotificationType.CART, EmailSubscriptionStatus.UNSUBSCRIBED);
        assertTrue(subscriptionAndIdentityDAO.saveSubscriptions(null, Collections.singletonList(deleted)));
    }

    @Test
    public void testUpperLowerEmailIdentical() {
        String email = "UPPER@mail.ru";
        EmailSubscription subscription =
            new EmailSubscription(null, email, NotificationType.CART, EmailSubscriptionStatus.CONFIRMED);
        subscriptionAndIdentityDAO.saveSubscriptions(email, Collections.singletonList(subscription));

        String inverse = "upper@MAIL.RU";
        EmailSubscription deleted =
            new EmailSubscription(subscription.getId(), inverse, NotificationType.CART, EmailSubscriptionStatus.UNSUBSCRIBED);
        assertTrue(subscriptionAndIdentityDAO.saveSubscriptions(inverse, Collections.singletonList(deleted)));
    }

    @Test
    void transboundaryDefaultSubscriptions() throws Exception {
        EnumSet<NotificationType> defaultSubscribed = EnumSet.of(
            NotificationType.TRANSBOUNDARY_TRADING_TRANSACTION, NotificationType.TRANSBOUNDARY_TRADING_TRANSACTION_PUSH
        );
        EnumSet<NotificationType> defaultUnsubscribed = EnumSet.of(
            NotificationType.TRANSBOUNDARY_TRADING_ADVERTISING,
            NotificationType.TRANSBOUNDARY_TRADING_ADVERTISING_NEW,
            NotificationType.TRANSBOUNDARY_TRADING_ADVERTISING_PUSH,
            NotificationType.TRANSBOUNDARY_TRADING_TRIGGER,
            NotificationType.TRANSBOUNDARY_TRADING_TRIGGER_PUSH
        );
        Set<NotificationType> allTypes = Stream.concat(
            defaultSubscribed.stream(), defaultUnsubscribed.stream()).collect(Collectors.toSet());

        // default
        checkSubscribed(defaultSubscribed, defaultUnsubscribed);

        // subscribe
        List<EmailSubscription> subscriptions = defaultUnsubscribed.stream()
            .map(type -> new EmailSubscription(null, EMAIL, type, null))
            .collect(Collectors.toList());
        controllerInvoker.createSubscriptions(UID, EMAIL, subscriptions, true);
        checkSubscribed(allTypes, Collections.emptySet());

        // unsubscribe
        for (NotificationType type : allTypes) {
            controllerInvoker.unsubscribe(EMAIL, type);
        }
        checkSubscribed(Collections.emptySet(), allTypes);
    }

    /**
     * В случае если для некоторого email и identity уже существует ownership при сохранении
     * новой подписки для того же email и identity новый ownership не создается даже если
     * email в новой подписке содержит заглавные буквы.
     */
    @Test
    void testDoNotCreateNewOwnershipForEmailWIthUppercaseLetterInIt() throws Exception {
        subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(UID);
        subscriptionAndIdentityDAO.createEmailOwnership(UID, EMAIL, EmailOwnership.Status.CONFIRMED, EmailOwnership.Source.PASSPORT);

        String alteredEmail = StringUtils.capitalize(EMAIL);

        EmailSubscription subscription2 = new EmailSubscription(alteredEmail, NotificationType.QA_NEW_COMMENTS, null);
        subscription2.addParameter(EmailSubscriptionParam.PARAM_ANSWER_ID, "321");
        controllerInvoker.createSubscriptions(UID, alteredEmail, Collections.singletonList(subscription2));

        List<?> ownerships = subscriptionAndIdentityDAO.getEmailsByIdentity(UID);
        assertEquals(1, ownerships.size());
    }

    private void checkSubscribed(Set<NotificationType> subscribedTypes,
                                 Set<NotificationType> unsubscribedTypes) throws Exception {
        List<NotificationType> allTypes = Stream.concat(
            subscribedTypes.stream(), unsubscribedTypes.stream()).collect(Collectors.toList());

        List<EmailSubscriptionStatusDto> subscribed = controllerInvoker.isSubscribed(EMAIL, allTypes);
        for (EmailSubscriptionStatusDto emailSubscriptionStatusDto : subscribed) {
            NotificationType type = emailSubscriptionStatusDto.getSubscriptionType();
            if (subscribedTypes.contains(type)) {
                assertTrue(emailSubscriptionStatusDto.isSubscribed());
            } else if (unsubscribedTypes.contains(type)) {
                assertFalse(emailSubscriptionStatusDto.isSubscribed());
            }
        }
    }

    private void setStatus(EmailSubscriptionStatus subscriptionStatus, List<EmailSubscription> subscriptions) {
        for (EmailSubscription subscription : subscriptions) {
            subscription.setSubscriptionStatus(subscriptionStatus);
        }
    }

    private List<EmailSubscription> asList(EmailSubscription... subscriptions) {
        return Arrays.asList(subscriptions);
    }

    private void mockPassport(Uid uid, String email) {
        blackBoxPassportService.doReturn(uid.getValue(), Collections.singletonList(email));
    }

    @Nested
    class PriceNotificationsWhenEmailAlreadyConfirmedTest extends MarketUtilsMockedDbTest {

        static final String REGION_ID = "162";
        static final String CURRENCY = "KZT";
        //CHECKSTYLE:ON
        static final String MODEL_ID = "39123";
        static final String YANDEX_STATION_MODEL_ID_IN_PROD = "1971204201";
        static final String YANDEX_PHONE_MODEL_ID_IN_PROD = "177547282";
        //CHECKSTYLE:OFF
        final Clock fixedClock = Clock.fixed(Instant.from(Year.of(2038).atDay(1).atStartOfDay(ZoneId.systemDefault())),
            ZoneId.systemDefault());
        final Instant reminderLetterSendTime = Instant.from(Year.of(2038).atDay(8).atStartOfDay(ZoneId.systemDefault()));
        Clock originalClock;

        @BeforeEach
        void setup() {
            originalClock = subscriptionAndIdentityService.getClock();
            subscriptionAndIdentityService.setClock(fixedClock);
            blackBoxPassportService.doReturn(UID.getValue(), EMAIL);
        }

        @AfterEach
        void restoreClock() {
            subscriptionAndIdentityService.setClock(originalClock);
        }

        void testWelcomeLetterForPaOnSaleScheduled(String modelId) throws Exception {
            String subscriptionId = createSubscription(NotificationType.PA_ON_SALE, modelId, "0");
            checkWelcomeLetterScheduled(modelId, subscriptionId);
        }

        @Test
        void testWelcomeLetterForPaOnSaleScheduledOtherModel() throws Exception {
            testWelcomeLetterForPaOnSaleScheduled(MODEL_ID);
        }

        @Test
        void testWelcomeLetterForPaOnSaleScheduledYandexStation() throws Exception {
            testWelcomeLetterForPaOnSaleScheduled(YANDEX_STATION_MODEL_ID_IN_PROD);
        }

        @Test
        void testWelcomeLetterForPaOnSaleScheduledYandexPhone() throws Exception {
            testWelcomeLetterForPaOnSaleScheduled(YANDEX_PHONE_MODEL_ID_IN_PROD);
        }

        void testNoReminderLetterScheduled(NotificationType type, String price, String modelId,
                                           NotificationSubtype checkedSubtype) throws Exception {
            createSubscription(type, modelId, price);
            checkReminderLetterNotScheduled(checkedSubtype);
        }

        @Test
        void testNoReminderLetterScheduledYandexStationPaOnSale() throws Exception {
            testNoReminderLetterScheduled(NotificationType.PA_ON_SALE, "0", YANDEX_STATION_MODEL_ID_IN_PROD,
                NotificationSubtype.PA_SIMILAR);
        }

        @Test
        void testNoReminderLetterScheduledYandexStationPriceDrop() throws Exception {
            testNoReminderLetterScheduled(NotificationType.PRICE_DROP, "123", YANDEX_STATION_MODEL_ID_IN_PROD,
                NotificationSubtype.PRICE_DROP_REMINDER);
        }

        @Test
        void testNoReminderLetterScheduledYandexPhonePaOnSale() throws Exception {
            testNoReminderLetterScheduled(NotificationType.PA_ON_SALE, "0", YANDEX_PHONE_MODEL_ID_IN_PROD,
                NotificationSubtype.PA_SIMILAR);
        }

        @Test
        void testNoReminderLetterScheduledYandexPhonePriceDrop() throws Exception {
            testNoReminderLetterScheduled(NotificationType.PRICE_DROP, "4576", YANDEX_PHONE_MODEL_ID_IN_PROD,
                NotificationSubtype.PRICE_DROP_REMINDER);
        }

        void testReminderLetterScheduledOtherModel(NotificationType type, String price,
                                                   NotificationSubtype expectedSubtype) throws Exception {
            String subscriptionId = createSubscription(type, MODEL_ID, price);
            checkReminderLetterScheduled(MODEL_ID, subscriptionId, expectedSubtype);
        }

        @Test
        void testReminderLetterScheduledOtherModelPaOnSale() throws Exception {
            testReminderLetterScheduledOtherModel(NotificationType.PA_ON_SALE, "0", NotificationSubtype.PA_SIMILAR);
        }

        @Test
        void testReminderLetterScheduledOtherModelPriceDrop() throws Exception {
            testReminderLetterScheduledOtherModel(NotificationType.PRICE_DROP, "23786",
                NotificationSubtype.PRICE_DROP_REMINDER);
        }


        String createSubscription(NotificationType type, String modelId, String price) throws Exception {
            EmailSubscription subscription = new EmailSubscription(null, type, null);
            subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, REGION_ID);
            subscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, modelId);
            subscription.addParameter(EmailSubscriptionParam.PARAM_PRICE, price);
            controllerInvoker.createSubscriptions(UID, EMAIL, Collections.singletonList(subscription));

            return controllerInvoker.getSubscriptions(UID).get(0).getId().toString();
        }

        private void checkWelcomeLetterScheduled(String modelId, String subscriptionId) {
            List<NotificationEvent> eventList = eventSourceDAO.getMailEventsForProcessing(
                Arrays.asList(NotificationSubtype.values()), 100, NotificationEventStatus.values());
            assertEquals(1, eventList.size());
            checkWelcomeLetter(modelId, subscriptionId, eventList.get(0));
        }

        private void checkWelcomeLetter(String modelId, String subscriptionId, NotificationEvent welcomeLetter) {
            assertNotNull(welcomeLetter);
            assertEquals(NotificationSubtype.PA_WELCOME, welcomeLetter.getNotificationSubtype());
            assertEquals(EMAIL, welcomeLetter.getAddress());
            Map<String, String> data = welcomeLetter.getData();
            assertEquals(modelId, data.get(NotificationEventDataName.MODEL_ID));
            assertEquals(REGION_ID, data.get(NotificationEventDataName.REGION_ID));
            assertEquals(CURRENCY, data.get(NotificationEventDataName.CURRENCY));
            assertEquals(subscriptionId, data.get(NotificationEventDataName.SUBSCRIPTION_ID));
        }

        private void checkReminderLetterScheduled(String modelId, String subscriptionId,
                                                  NotificationSubtype expectedSubtype) {
            NotificationEvent reminderLetter = eventSourceDAO.getLastEventByEmail(EMAIL);
            assertNotNull(reminderLetter);
            assertEquals(expectedSubtype, reminderLetter.getNotificationSubtype());
            assertEquals(EMAIL, reminderLetter.getAddress());
            Map<String, String> data = reminderLetter.getData();
            assertEquals(subscriptionId, data.get(NotificationEventDataName.SUBSCRIPTION_ID));
            assertEquals(modelId, data.get(NotificationEventDataName.MODEL_ID));
            assertEquals(reminderLetterSendTime, reminderLetter.getSendTime().toInstant());
        }

        private void checkReminderLetterNotScheduled(NotificationSubtype subtype) {
            assertEquals(0, (int) eventSourceDAO.getEventsCount(EMAIL, subtype, NotificationEventStatus.values()));
        }

    }
    //todo tests for non uids

    @Nested
    class LotteryLettersScheduleTests extends AdvertisingLettersScheduleTests {

        @Override
        String getSubscriptionPlace() {
            return "birthday19";
        }

        @Override
        NotificationSubtype getWelcomeLettersSubtype() {
            return NotificationSubtype.LOTTERY_WELCOME;
        }

        @Override
        NotificationSubtype getDoubleOptInLettersSubtype() {
            return NotificationSubtype.CONFIRM_LOTTERY_SUBSCRIPTION;
        }

        @Test
        void authorizedUserMakesOtherAdvertisingSubscriptionOnPassportEmail() throws Exception {
            mockPassport(UID, EMAIL);
            createOtherAdvertisingSubscriptionForAuthorizedUser();
            checkLetterNotScheduled(getWelcomeLettersSubtype());
            checkLetterNotScheduled(getDoubleOptInLettersSubtype());
        }

        @Test
        void authorizedUserMakesOtherSubscriptionOnEmailWithConfirmation() throws Exception {
            userMakesOtherAdvertisingSubscriptionOnEmailWithConfirmation(
                createOtherAdvertisingSubscriptionForAuthorizedUser());
        }

        @Test
        void notAuthorizedUserMakesOtherSubscription() throws Exception {
            EmailSubscription emailSubscription = prepareAdvertisingSubscription(OTHER_SUBSCRIPTION_PLACE);
            emailSubscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "2");
            long subscriptionId = createSubscription(YANDEX_UID, emailSubscription);
            userMakesOtherAdvertisingSubscriptionOnEmailWithConfirmation(subscriptionId);
        }
    }

    @Nested
    class SubscriptionWithBonusLettersScheduleTests extends AdvertisingLettersScheduleTests {

        @Override
        String getSubscriptionPlace() {
            return "footer_berubonus_exp";
        }

        @Override
        NotificationSubtype getWelcomeLettersSubtype() {
            return NotificationSubtype.SUBSCRIPTIONS_WITH_BONUS_WELCOME;
        }

        @Override
        NotificationSubtype getDoubleOptInLettersSubtype() {
            return NotificationSubtype.CONFIRM_SUBSCRIPTION_WITH_BONUS;
        }

        @Test
        void authorizedUserMakesOtherAdvertisingSubscriptionOnPassportEmail() throws Exception {
            mockPassport(UID, EMAIL);
            createOtherAdvertisingSubscriptionForAuthorizedUser();
            checkLetterNotScheduled(getWelcomeLettersSubtype());
            checkLetterNotScheduled(getDoubleOptInLettersSubtype());
        }

        @Test
        void authorizedUserMakesOtherSubscriptionOnEmailWithConfirmation() throws Exception {
            userMakesOtherAdvertisingSubscriptionOnEmailWithConfirmation(
                createOtherAdvertisingSubscriptionForAuthorizedUser());
        }

        @Test
        void notAuthorizedUserMakesOtherSubscription() throws Exception {
            EmailSubscription emailSubscription = prepareAdvertisingSubscription(OTHER_SUBSCRIPTION_PLACE);
            emailSubscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "2");
            long subscriptionId = createSubscription(YANDEX_UID, emailSubscription);
            userMakesOtherAdvertisingSubscriptionOnEmailWithConfirmation(subscriptionId);
        }
    }

    @Nested
    class GenericAdvertisingLettersScheduleTests extends AdvertisingLettersScheduleTests {

        @Override
        String getSubscriptionPlace() {
            return OTHER_SUBSCRIPTION_PLACE;
        }

        @Override
        NotificationSubtype getWelcomeLettersSubtype() {
            return NotificationSubtype.ADVERTISING_WELCOME;
        }

        @Override
        NotificationSubtype getDoubleOptInLettersSubtype() {
            return NotificationSubtype.CONFIRM_SUBSCRIPTION;
        }
    }

    abstract class AdvertisingLettersScheduleTests extends MarketUtilsMockedDbTest {

        abstract String getSubscriptionPlace();

        abstract NotificationSubtype getWelcomeLettersSubtype();

        abstract NotificationSubtype getDoubleOptInLettersSubtype();

        @Test
        void authorizedUserMakesSubscriptionOnPassportEmail() throws Exception {
            mockPassport(UID, EMAIL);
            long subscriptionId = createAdvertisingSubscriptionForAuthorizedUser();
            checkAdvertisingSubscriptionWelcomeLetterScheduledForAuthorized(subscriptionId);
            checkLetterNotScheduled(getDoubleOptInLettersSubtype());
        }

        @Test
        void authorizedUserMakesSubscriptionOnEmailWithConfirmation() throws Exception {
            long subscriptionId = createAdvertisingSubscriptionForAuthorizedUser();
            checkDoubleOptInLetterScheduledForAuthorized(getDoubleOptInLettersSubtype(), subscriptionId);
            checkLetterNotScheduled(getWelcomeLettersSubtype());
            verificationUtil.confirmSubscription(subscriptionId);
            checkAdvertisingSubscriptionWelcomeLetterScheduledForAuthorized(subscriptionId);
        }

        @Test
        void notAuthorizedUserMakesSubscription() throws Exception {
            EmailSubscription emailSubscription = prepareAdvertisingSubscription(getSubscriptionPlace());
            emailSubscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "2");
            long subscriptionId = createSubscription(YANDEX_UID, emailSubscription);
            checkLetterNotScheduled(getWelcomeLettersSubtype());
            checkDoubleOptInLetterScheduledForUnAuthorized(getDoubleOptInLettersSubtype(), subscriptionId);
            verificationUtil.confirmSubscription(subscriptionId);
            checkAdvertisingSubscriptionWelcomeLetterScheduledForUnAuthorized(subscriptionId);
        }

        void userMakesOtherAdvertisingSubscriptionOnEmailWithConfirmation(long subscriptionId) {
            checkLetterNotScheduled(getWelcomeLettersSubtype());
            checkLetterNotScheduled(getDoubleOptInLettersSubtype());
            verificationUtil.confirmSubscription(subscriptionId);
            checkLetterNotScheduled(getWelcomeLettersSubtype());
        }

        EmailSubscription prepareAdvertisingSubscription(String place) {
            return EmailSubscription.builder()
                .setSubscriptionType(NotificationType.ADVERTISING)
                .addParameter(EmailSubscriptionParam.PARAM_PLACE, place)
                .build();
        }

        long createOtherAdvertisingSubscriptionForAuthorizedUser() throws Exception {
            return createSubscription(UID, prepareAdvertisingSubscription(OTHER_SUBSCRIPTION_PLACE));
        }

        long createAdvertisingSubscriptionForAuthorizedUser() throws Exception {
            return createSubscription(UID, prepareAdvertisingSubscription(getSubscriptionPlace()));
        }

        long createSubscription(Identity<?> identity, EmailSubscription emailSubscription) throws Exception {
            List<EmailSubscription> subscriptions = controllerInvoker.createSubscriptions(identity, EMAIL,
                Collections.singletonList(emailSubscription));
            assertEquals(1, subscriptions.size());
            return subscriptions.get(0).getId();
        }

        void checkLetterNotScheduled(NotificationSubtype subtype) {
            assertEquals(Collections.emptyList(), getScheduledLetters(subtype));
        }

        void checkAdvertisingSubscriptionWelcomeLetterScheduledForUnAuthorized(long subscriptionId) {
            NotificationEvent event = checkAdvertisingSubscriptionWelcomeLetterScheduled(subscriptionId);
            assertNull(event.getUid());
        }

        NotificationEvent checkAdvertisingSubscriptionWelcomeLetterScheduled(long subscriptionId) {
            List<NotificationEvent> eventList = getScheduledLetters(getWelcomeLettersSubtype());
            assertEquals(1, eventList.size());
            NotificationEvent event = eventList.get(0);
            assertEquals(EMAIL, event.getAddress());
            assertEquals(NotificationEventStatus.NEW, event.getStatus());
            assertEquals(getWelcomeLettersSubtype(), event.getNotificationSubtype());
            assertEquals(subscriptionId, (long) event.getSourceId());
            assertEquals(subscriptionId, Long.parseLong(event.getData().get(NotificationEventDataName.SUBSCRIPTION_ID)));
            return event;
        }

        void checkAdvertisingSubscriptionWelcomeLetterScheduledForAuthorized(long subscriptionId) {
            NotificationEvent event = checkAdvertisingSubscriptionWelcomeLetterScheduled(subscriptionId);
            assertEquals(UID.getValue(), event.getUid());
        }

    }

    @Nested
    class DeleteSubscriptionsOwnershipTests extends MarketUtilsMockedDbTest {

        @Test
        void deleteForeignSubscription1() throws Exception {
            deleteForeignSubscription(new Uid(1L), new Uid(2L));
        }

        @Test
        void deleteForeignSubscription2() throws Exception {
            deleteForeignSubscription(YANDEX_UID, UID);
        }

        @Test
        void deleteForeignSubscription3() throws Exception {
            deleteForeignSubscription(UID, YANDEX_UID);
        }

        @Test
        void deleteForeignSubscription4() throws Exception {
            deleteForeignSubscription(new YandexUid("1"), new YandexUid("2"));
        }

        @Test
        void deleteOwnSubscriptionUsingUnsubscribeNotParametricWithIdentity() throws Exception {
            createSubscription(YANDEX_UID);
            long subscriptionId = controllerInvoker.getSubscriptions(YANDEX_UID).get(0).getId();

            controllerInvoker.unsubscribe(EMAIL, NotificationType.STORE_ADVERTISING, YANDEX_UID);

            EmailSubscription emailSubscription = controllerInvoker.getSubscription(subscriptionId);
            assertEquals(EmailSubscriptionStatus.UNSUBSCRIBED, emailSubscription.getSubscriptionStatus());
        }

        @Test
        void deleteForeignSubscriptionUsingUnsubscribeNotParametricWithIdentity() throws Exception {
            createSubscription(YANDEX_UID);
            EmailSubscription emailSubscription = controllerInvoker.getSubscriptions(YANDEX_UID).get(0);
            EmailSubscriptionStatus initialStatus = emailSubscription.getSubscriptionStatus();

            controllerInvoker.unsubscribeForbidden(EMAIL, NotificationType.STORE_ADVERTISING, UID);

            emailSubscription = controllerInvoker.getSubscription(emailSubscription.getId());
            assertEquals(initialStatus, emailSubscription.getSubscriptionStatus());
        }

        void deleteForeignSubscription(Identity<?> whoMade, Identity<?> whoWantsDelete) throws Exception {
            createSubscription(whoMade);
            EmailSubscription emailSubscription = controllerInvoker.getSubscriptions(whoMade).get(0);
            EmailSubscriptionStatus initialStatus = emailSubscription.getSubscriptionStatus();

            controllerInvoker.deleteSubscriptionForbidden(whoWantsDelete, emailSubscription.getId());
            emailSubscription = controllerInvoker.getSubscription(emailSubscription.getId());
            assertEquals(initialStatus, emailSubscription.getSubscriptionStatus());
        }

        @Test
        void deleteSubscriptionMadeInUnauthorizedModeOnPassportEmail() throws Exception {
            createSubscription(YANDEX_UID);
            long subscriptionId = controllerInvoker.getSubscriptions(YANDEX_UID).get(0).getId();
            blackBoxPassportService.doReturn(UID.getValue(), EMAIL);

            controllerInvoker.deleteSubscription(UID, subscriptionId);

            EmailSubscription subscription = controllerInvoker.getSubscription(subscriptionId);
            assertEquals(EmailSubscriptionStatus.UNSUBSCRIBED, subscription.getSubscriptionStatus());
        }

        private void createSubscription(Identity<?> identity) throws Exception {
            controllerInvoker.createSubscriptions(identity, EMAIL, Collections.singletonList(EmailSubscription.builder()
                .setEmail(EMAIL).setSubscriptionType(NotificationType.STORE_ADVERTISING)
                .addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "2").build()));
        }

    }

    @Nested
    class LotteryFrodTests extends MarketUtilsMockedDbTest {

        @Test
        void checkDotsRemovedFromGmailAddresses() throws Exception {
            checkEmailNormalization("john.doe@gmail.com", "johndoe@gmail.com");
        }

        @Test
        void checkTagsRemoved() throws Exception {
            checkEmailNormalization("petya+1214@mail.ru", "petya@mail.ru");
        }

        @Test
        void checkCreationOfSecondSubscriptionForbidden() throws Exception {
            checkMultipleSubscriptionsOnEffectivelySameEmailForbidden("vasyaivanov@gmail.com",
                "vasya.ivanov+test@gmail.com");
        }

        void checkMultipleSubscriptionsOnEffectivelySameEmailForbidden(String email1, String email2) throws Exception {
            createLotterySubscription(email1);
            assertEquals(1, subscriptionsCacher.getSubscriptions(email1).size());
            createLotterySubscriptionForbidden(email2);
            assertEquals(Collections.emptyList(), subscriptionsCacher.getSubscriptions(email2));
        }

        void checkEmailNormalization(String srcEmail, String expectedEmail) throws Exception {
            long subscriptionId = createLotterySubscription(srcEmail);
            String actualEmail = subscriptionAndIdentityDAO.getSubscription(subscriptionId).getEmail();
            assertEquals(expectedEmail, actualEmail);
        }

        Long createLotterySubscription(String email) throws Exception {
            return createLotterySubscription(email, HttpStatus.CREATED.value());
        }

        void createLotterySubscriptionForbidden(String email) throws Exception {
            createLotterySubscription(email, HttpStatus.FORBIDDEN.value());
        }

        Long createLotterySubscription(String email, int expectedStatus) throws Exception {
            List<EmailSubscription> resultSubscriptions = controllerInvoker.createSubscriptions(
                new YandexUid("dsbfkjbf"), email, Collections.singletonList(EmailSubscription.builder()
                    .setSubscriptionType(NotificationType.ADVERTISING)
                    .addParameter("place", LOTTERY_PLACE)
                    .addParameter("regionId", "2")
                    .build()), false, expectedStatus);
            if (resultSubscriptions != null) {
                return resultSubscriptions.get(0).getId();
            } else {
                return null;
            }
        }

    }
}
