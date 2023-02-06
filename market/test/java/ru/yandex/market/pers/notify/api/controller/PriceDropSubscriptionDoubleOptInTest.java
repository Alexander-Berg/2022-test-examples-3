package ru.yandex.market.pers.notify.api.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.EventSourceDAO;
import ru.yandex.market.pers.notify.ems.persistence.EventSourceDAOTestHelper;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.YandexUid;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;
import ru.yandex.market.pers.notify.test.VerificationUtil;

import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author semin-serg
 */
public class PriceDropSubscriptionDoubleOptInTest extends MarketUtilsMockedDbTest {

    private static final String MAIL_ADDRESS = "some-mail@mail.ru";
    private static final long UID = 2143L;
    private static final String YANDEX_UID = "rhfjbg";
    private static final String MODEL_ID_1 = "5623623";
    private static final String REGION_ID_1 = "162"; //Алматы
    private static final String PRICE_1 = "100";
    private static final String CURRENCY_1 = "KZT";
    private static final String MODEL_ID_2 = "4398425";
    private static final String REGION_ID_2 = "213"; //Москва
    private static final String PRICE_2 = "200";
    private static final String CURRENCY_2 = "RUR";
    private static final int GET_EVENTS_FOR_PROCESSING_LIMIT = 1000;

    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;

    @Autowired
    EventSourceDAO eventSourceDAO;

    @Autowired
    SubscriptionAndIdentityService subscriptionAndIdentityService;

    @Autowired
    private VerificationUtil verificationUtil;

    @Autowired
    SubscriptionControllerInvoker subscriptionControllerInvoker;

    @Test
    public void authorizedUserUsesPassportEmailDefaultCurrency() {
        authorizedUserUsesPassportEmail();
    }

    @Test
    public void authorizedUserUsesPassportEmailSpecialCurrency() {
        //отличие данного теста от предыдущего состоит в том, что имитируется ситуация, что currency по-прежнему
        //передаётся с фронта. Делается проверка того факта, что это значение безболезненно игнорируется
        authorizedUserUsesPassportEmail("RUR");
    }

    private void authorizedUserUsesPassportEmail() {
        authorizedUserUsesPassportEmail(null);
    }

    @Test
    public void authorizedUserMakesTwoPriceDropSubscriptionsSequentiallyNewEmail() {
        userMakesTwoPriceDropSubscriptionsSequentiallyNewEmail(new Uid(UID));
    }

    @Test
    public void notAuthorizedUserMakesTwoPriceDropSubscriptionsSequentiallyNewEmail() {
        userMakesTwoPriceDropSubscriptionsSequentiallyNewEmail(new YandexUid(YANDEX_UID));
    }

    /**
     * Авторизованный пользователь делает подписку PRICE_DROP на 1 товар на новый email, неизвестный паспорту.
     * Получает confirmation-письмо. (Создаётся неподтвержденная подписка). Подтверждает подписку - получает
     * welcome-письмо по первому товару. Первая подписка подтвердилась. Делает вторую подписку на другой товар на
     * тот же новый email - сразу присылается welcome-письмо, подписка является подтвержденной.
     */
    @Test
    public void authorizedUserMakesTwoPriceDropSubscriptionsNewEmail() {
        Uid identity = new Uid(UID);
        ArrayList<Long> alreadyProcessedEvents = new ArrayList<>();
        EmailSubscription emailSubscription1 = validateSubscription(makeSubscription(identity, MODEL_ID_1, REGION_ID_1,
            PRICE_1), identity, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION, MODEL_ID_1, REGION_ID_1, PRICE_1,
            CURRENCY_1);
        checkConfirmationLetterScheduled(emailSubscription1.getId(), identity, alreadyProcessedEvents);

        verificationUtil.confirmSubscription(emailSubscription1);
        checkWelcomeLetterScheduled(emailSubscription1.getId(), identity, alreadyProcessedEvents);
        assertEquals(EmailSubscriptionStatus.CONFIRMED,
            subscriptionAndIdentityService.getEmailSubscription(emailSubscription1.getId()).getSubscriptionStatus());

        EmailSubscription emailSubscription2 = validateSubscription(makeSubscription(identity, MODEL_ID_2, REGION_ID_2,
            PRICE_2), identity, EmailSubscriptionStatus.CONFIRMED, MODEL_ID_2, REGION_ID_2, PRICE_2,
            CURRENCY_2);
        checkWelcomeLetterScheduled(emailSubscription2.getId(), identity, alreadyProcessedEvents);
    }

    /**
     * Неавторизованный пользователь делает подписку PRICE_DROP на 1 товар на новый email, неизвестный паспорту.
     * Получает confirmation-письмо. (Создаётся неподтвержденная подписка). Подтверждает подписку - получает
     * welcome-письмо по первому товару. Первая подписка подтвердилась. Делает вторую подписку на другой товар на
     * тот же новый email - получает confirmation-письмо, подписка является неподтвержденной.
     */
    @Test
    public void notAuthorizedUserMakesTwoPriceDropSubscriptionsNewEmail() {
        YandexUid identity = new YandexUid(YANDEX_UID);
        ArrayList<Long> alreadyProcessedEvents = new ArrayList<>();
        EmailSubscription emailSubscription1 = validateSubscription(makeSubscription(identity, MODEL_ID_1, REGION_ID_1,
            PRICE_1), identity, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION, MODEL_ID_1, REGION_ID_1, PRICE_1,
            CURRENCY_1);
        checkConfirmationLetterScheduled(emailSubscription1.getId(), identity, alreadyProcessedEvents);

        verificationUtil.confirmSubscription(emailSubscription1);
        checkWelcomeLetterScheduled(emailSubscription1.getId(), identity, alreadyProcessedEvents);
        assertEquals(EmailSubscriptionStatus.CONFIRMED,
            subscriptionAndIdentityService.getEmailSubscription(emailSubscription1.getId()).getSubscriptionStatus());

        EmailSubscription emailSubscription2 = validateSubscription(makeSubscription(identity, MODEL_ID_2, REGION_ID_2,
            PRICE_2), identity, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION, MODEL_ID_2, REGION_ID_2, PRICE_2,
            CURRENCY_2);
        checkConfirmationLetterScheduled(emailSubscription2.getId(), identity, alreadyProcessedEvents);
    }

    /**
     * Авторизованный пользователь делает подписку на PRICE_DROP на свой паспортный email - сразу получает
     * welcome-письмо
     */
    private void authorizedUserUsesPassportEmail(String sentCurrency) {
        Uid identity = new Uid(UID);
        setupPassportService();
        List<EmailSubscription> emailSubscriptions = makeSubscription(identity, MODEL_ID_1, REGION_ID_1, PRICE_1,
            sentCurrency);
        EmailSubscription emailSubscription = validateSubscription(emailSubscriptions, identity,
            EmailSubscriptionStatus.CONFIRMED, MODEL_ID_1, REGION_ID_1, PRICE_1, CURRENCY_1);
        checkWelcomeLetterScheduled(emailSubscription.getId(), identity);
    }

    /**
     * пользователь делает подписку PRICE_DROP на 2 товара на новый email, неизвестный паспорту. Получает 2 confirmation
     * письма. (Создаются две неподтвержденные подписки). Подтверждает первую подписку - получает welcome-письмо по
     * первому товару. Первая подписка подтвердилась. Подтверждает вторую подписку - получает welcome-письмо по второму
     * товару. Вторая подписка подтвердилась.
     * @param identity
     */
    private void userMakesTwoPriceDropSubscriptionsSequentiallyNewEmail(Identity<?> identity) {
        ArrayList<Long> alreadyProcessedEvents = new ArrayList<>();
        EmailSubscription emailSubscription1 = validateSubscription(makeSubscription(identity, MODEL_ID_1, REGION_ID_1,
            PRICE_1), identity, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION, MODEL_ID_1, REGION_ID_1, PRICE_1,
            CURRENCY_1);
        checkConfirmationLetterScheduled(emailSubscription1.getId(), identity, alreadyProcessedEvents);
        EmailSubscription emailSubscription2 = validateSubscription(makeSubscription(identity, MODEL_ID_2, REGION_ID_2,
            PRICE_2), identity, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION, MODEL_ID_2, REGION_ID_2, PRICE_2,
            CURRENCY_2);
        checkConfirmationLetterScheduled(emailSubscription2.getId(), identity,
            alreadyProcessedEvents);

        verificationUtil.confirmSubscription(emailSubscription1);
        checkWelcomeLetterScheduled(emailSubscription1.getId(), identity, alreadyProcessedEvents);
        assertEquals(EmailSubscriptionStatus.CONFIRMED,
            subscriptionAndIdentityService.getEmailSubscription(emailSubscription1.getId()).getSubscriptionStatus());

        verificationUtil.confirmSubscription(emailSubscription2);
        checkWelcomeLetterScheduled(emailSubscription2.getId(), identity, alreadyProcessedEvents);
        assertEquals(EmailSubscriptionStatus.CONFIRMED,
            subscriptionAndIdentityService.getEmailSubscription(emailSubscription2.getId()).getSubscriptionStatus());

    }

    @Test
    public void notAuthorizedUserMakesPriceDropSubscriptionWithAdvertising() {
        notAuthorizedUserMakesPriceSubscriptionWithAdvertising(NotificationType.PRICE_DROP, PRICE_1,
            NotificationSubtype.PRICE_DROP_WELCOME);
    }

    @Test
    public void notAuthorizedUserMakesPaOnSaleSubscriptionWithAdvertising() {
        notAuthorizedUserMakesPriceSubscriptionWithAdvertising(NotificationType.PA_ON_SALE, "0",
            NotificationSubtype.PA_WELCOME);
    }

    private void notAuthorizedUserMakesPriceSubscriptionWithAdvertising(NotificationType priceSubscriptionType,
                                                                        String price,
                                                                        NotificationSubtype welcomeLetterSubtype) {
        YandexUid identity = new YandexUid(YANDEX_UID);
        List<EmailSubscription> expectedSubscriptions = Arrays.asList(
            prepareExpectedPriceSubscription(priceSubscriptionType, price), prepareExpectedAdvertisingSubscription());

        List<EmailSubscription> actualSubscriptions = createSubscriptions(identity, expectedSubscriptions);

        validateSubscriptions(identity, expectedSubscriptions, actualSubscriptions);
        EmailSubscription priceSubscription = actualSubscriptions.get(0);
        EmailSubscription advertisingSubscription = actualSubscriptions.get(1);
        ArrayList<Long> alreadyProcessedEvents = new ArrayList<>();
        checkConfirmationLetterScheduled(priceSubscription.getId(), identity, alreadyProcessedEvents);

        verificationUtil.confirmSubscription(priceSubscription);

        checkWelcomeLetterScheduled(priceSubscription.getId(), identity, alreadyProcessedEvents, welcomeLetterSubtype);
        assertEquals(EmailSubscriptionStatus.CONFIRMED, subscriptionAndIdentityService.getEmailSubscription(
            priceSubscription.getId()).getSubscriptionStatus());
        assertEquals(EmailSubscriptionStatus.CONFIRMED, subscriptionAndIdentityService.getEmailSubscription(
            advertisingSubscription.getId()).getSubscriptionStatus());
    }

    @Test
    public void notAuthorizedUserSimultaneouslyMakesTwoPriceSubscriptions() {
        YandexUid identity = new YandexUid(YANDEX_UID);
        List<EmailSubscription> expectedSubscriptions = Arrays.asList(
            prepareExpectedPriceSubscription(NotificationType.PRICE_DROP, PRICE_1, MODEL_ID_1, REGION_ID_1),
            prepareExpectedPriceSubscription(NotificationType.PA_ON_SALE, "0", MODEL_ID_2, REGION_ID_2));

        List<EmailSubscription> actualSubscriptions = createSubscriptions(identity, expectedSubscriptions);

        validateSubscriptions(identity, expectedSubscriptions, actualSubscriptions);
        List<NotificationEvent> events = getNewEventsForProcessing(new ArrayList<>());
        assertEquals(2, events.size());
        assertEquals(Collections.singleton(NotificationSubtype.CONFIRM_SUBSCRIPTION),
            events.stream().map(NotificationEvent::getNotificationSubtype).collect(Collectors.toSet()));
        for (NotificationEvent event : events) {
            assertEquals((long) event.getSourceId(),
                Long.parseLong(event.getData().get(NotificationEventDataName.SUBSCRIPTION_ID)));
        }
        assertEquals(actualSubscriptions.stream().map(EmailSubscription::getId).collect(Collectors.toSet()),
            events.stream().map(s -> Long.parseLong(s.getData().get(NotificationEventDataName.SUBSCRIPTION_ID)))
                .collect(Collectors.toSet()));
    }

    private EmailSubscription prepareExpectedPriceSubscription(NotificationType type, String price) {
        return prepareExpectedPriceSubscription(type, price, MODEL_ID_1, REGION_ID_1);
    }

    private EmailSubscription prepareExpectedPriceSubscription(NotificationType type, String price, String modelId,
                                                               String regionId) {
        EmailSubscription emailSubscription = new EmailSubscription(null, type, null);
        emailSubscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, modelId);
        emailSubscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, regionId);
        emailSubscription.addParameter(EmailSubscriptionParam.PARAM_PRICE, price);
        return emailSubscription;
    }

    private EmailSubscription prepareExpectedAdvertisingSubscription() {
        EmailSubscription emailSubscription = new EmailSubscription(null, NotificationType.ADVERTISING, null);
        emailSubscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, REGION_ID_1);
        return emailSubscription;
    }

    private List<EmailSubscription> createSubscriptions(Identity<?> identity, List<EmailSubscription> subscriptions) {
        try {
            return subscriptionControllerInvoker.createSubscriptions(identity, MAIL_ADDRESS, subscriptions);
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    private void validateSubscriptions(Identity<?> expectedIdentity, List<EmailSubscription> expectedSubscriptions,
                                       List<EmailSubscription> actualSubscriptions) {
        assertEquals(expectedSubscriptions.size(), actualSubscriptions.size());
        for (int i = 0; i < expectedSubscriptions.size(); i++) {
            validateSubscription(actualSubscriptions.get(i), expectedIdentity,
                EmailSubscriptionStatus.NEED_SEND_CONFIRMATION, expectedSubscriptions.get(i));
        }
    }

    private void setupPassportService() {
        blackBoxPassportService.doReturn(UID, MAIL_ADDRESS);
    }

    private List<EmailSubscription> makeSubscription(Identity<?> identity, String modelId, String regionId,
                                                     String price) {
        return makeSubscription(identity, modelId, regionId, price, null);
    }

    private List<EmailSubscription> makeSubscription(Identity<?> identity, String modelId, String regionId,
                                                     String price, String currency) {
        EmailSubscription emailSubscription = EmailSubscription.builder()
            .setSubscriptionType(NotificationType.PRICE_DROP)
            .addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, modelId)
            .addParameter(EmailSubscriptionParam.PARAM_REGION_ID, regionId)
            .addParameter(EmailSubscriptionParam.PARAM_PRICE, price)
            .build();
        if (currency != null) {
            emailSubscription.addParameter(EmailSubscriptionParam.PARAM_CURRENCY, currency);
        }
        return createSubscriptions(identity, Collections.singletonList(emailSubscription));
    }

    private EmailSubscription validateSubscription(List<EmailSubscription> emailSubscriptions,
                                                   Identity<?> expectedIdentity, EmailSubscriptionStatus expectedStatus,
                                                   String expectedModelId, String expectedRegionId,
                                                   String expectedPrice, String expectedCurrency) {
        assertEquals(1, emailSubscriptions.size());
        EmailSubscription emailSubscription = emailSubscriptions.get(0);
        validateSubscription(emailSubscription, expectedIdentity, expectedStatus, expectedModelId, expectedRegionId,
            expectedPrice, expectedCurrency);
        return emailSubscription;
    }

    private void validateSubscription(EmailSubscription actualEmailSubscription, Identity<?> identity,
                                      EmailSubscriptionStatus expectedStatus, String expectedModelId,
                                      String expectedRegionId, String expectedPrice, String expectedCurrency) {
        EmailSubscription expectedEmailSubscription = new EmailSubscription(null, NotificationType.PRICE_DROP, null);
        expectedEmailSubscription.addParameter(EmailSubscriptionParam.PARAM_PRICE, expectedPrice);
        expectedEmailSubscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, expectedRegionId);
        expectedEmailSubscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, expectedModelId);
        expectedEmailSubscription.addParameter(EmailSubscriptionParam.PARAM_CURRENCY, expectedCurrency);
        validateSubscription(actualEmailSubscription, identity, expectedStatus, expectedEmailSubscription);
    }

    private void validateSubscription(EmailSubscription actualEmailSubscription, Identity<?> identity,
                                      EmailSubscriptionStatus expectedStatus,
                                      EmailSubscription expectedEmailSubscription) {
        assertEquals(expectedEmailSubscription.getSubscriptionType(), actualEmailSubscription.getSubscriptionType());
        assertEquals(expectedStatus, actualEmailSubscription.getSubscriptionStatus());
        if (identity instanceof Uid) {
            assertEquals((long) ((Uid) identity).getValue(), (long) actualEmailSubscription.getUid());
        } else if (identity instanceof YandexUid) {
            assertEquals(((YandexUid) identity).getValue(), actualEmailSubscription.getYandexUid());
        } else if (identity instanceof Uuid) {
            assertEquals(((Uuid) identity).getValue(), actualEmailSubscription.getUuid());
        } else {
            throw new IllegalArgumentException("Unsupported identity type: " + identity.getClass());
        }
        assertEquals(MAIL_ADDRESS, actualEmailSubscription.getEmail());
        actualEmailSubscription.getParameters().entrySet().containsAll(
            expectedEmailSubscription.getParameters().entrySet());
    }

    private void checkWelcomeLetterScheduled(long subscriptionId, Identity<?> identity) {
        checkWelcomeLetterScheduled(subscriptionId, identity, new ArrayList<>());
    }

    private List<NotificationEvent> getNewEventsForProcessing(List<Long> alreadyProcessedEvents) {
        subscriptionAndIdentityService.reloadNotificationPool();
        List<NotificationEvent> eventsForProcessing = eventSourceDAO.getMailEventsForProcessing(
            Arrays.asList(NotificationSubtype.values()), GET_EVENTS_FOR_PROCESSING_LIMIT,
            NotificationEventStatus.values());
        return eventsForProcessing.stream().filter(e -> !alreadyProcessedEvents.contains(e.getId()))
            .collect(Collectors.toList());
    }

    private NotificationEvent getNewEventForProcessing(Identity<?> identity, List<Long> alreadyProcessedEvents) {
        List<NotificationEvent> eventsForProcessing = getNewEventsForProcessing(alreadyProcessedEvents);
        assertEquals(1, eventsForProcessing.size());
        NotificationEvent event = eventsForProcessing.get(0);
        assertEquals(NotificationEventStatus.NEW, event.getStatus());
        if (identity instanceof Uid) {
            assertEquals((long) ((Uid) identity).getValue(), (long) event.getUid());
        }
        assertEquals(MAIL_ADDRESS, event.getAddress());
        return event;
    }

    private void checkWelcomeLetterScheduled(long subscriptionId, Identity<?> identity,
                                             List<Long> alreadyProcessedEvents) {
        checkWelcomeLetterScheduled(subscriptionId, identity, alreadyProcessedEvents,
            NotificationSubtype.PRICE_DROP_WELCOME);
    }

    private void checkWelcomeLetterScheduled(long subscriptionId, Identity<?> identity,
                                             List<Long> alreadyProcessedEvents,
                                             NotificationSubtype welcomeLetterSubtype) {
        NotificationEvent event = getNewEventForProcessing(identity, alreadyProcessedEvents);
        assertEquals(welcomeLetterSubtype, event.getNotificationSubtype());
        Map<String, String> data = event.getData();
        assertEquals(subscriptionId, Long.parseLong(data.get(NotificationEventDataName.SUBSCRIPTION_ID)));
        alreadyProcessedEvents.add(event.getId());
    }

    private NotificationEvent checkConfirmationLetterScheduled(long subscriptionId, Identity<?> identity,
                                                               List<Long> alreadyProcessedEvents) {
        NotificationEvent event = getNewEventForProcessing(identity, alreadyProcessedEvents);
        assertEquals(NotificationSubtype.CONFIRM_SUBSCRIPTION, event.getNotificationSubtype());
        Map<String, String> data = event.getData();
        assertEquals(subscriptionId, Long.parseLong(data.get(NotificationEventDataName.SUBSCRIPTION_ID)));
        assertEquals(subscriptionId, (long) event.getSourceId());
        alreadyProcessedEvents.add(event.getId());
        return event;
    }

    @Nested
    class PriceDropPromoLettersScheduleTests extends MarketUtilsMockedDbTest {

        private Instant fixedTime = Instant.from(Year.of(2038).atDay(10).atStartOfDay(ZoneId.systemDefault()));
        private Instant expectedSendTime = Instant.from(Year.of(2038).atDay(11).atStartOfDay(ZoneId.systemDefault()));
        private Clock fixedClock = Clock.fixed(fixedTime, ZoneId.systemDefault());

        private Clock originalClock;

        @Autowired
        EventSourceDAOTestHelper eventSourceDAOTestHelper;

        @BeforeEach
        void beforeEach() {
            originalClock = subscriptionAndIdentityService.getClock();
            subscriptionAndIdentityService.setClock(fixedClock);
        }

        @AfterEach
        void afterEach() {
            subscriptionAndIdentityService.setClock(originalClock);
        }

        @Test
        void authorizedUserMakesSubscriptionOnPassportEmail() throws Exception {
            blackBoxPassportService.doReturn(UID, MAIL_ADDRESS);

            EmailSubscription emailSubscription = createSubscription();

            checkPriceDropPromoScheduledWithUid(emailSubscription.getId());
        }

        @Test
        void authorizedUserMakesSubscriptionOnOtherEmail() throws Exception {
            createSubscription();
            checkPriceDropPromoNotScheduled();
        }

        @Test
        void authorizedUserConfirmSubscriptionOnOtherEmail() throws Exception {
            EmailSubscription subscription = createSubscription();

            verificationUtil.confirmSubscription(subscription);

            checkPriceDropPromoScheduledWithUid(subscription.getId());
        }

        @Test
        void unauthorizedUserMakesSubscription() throws Exception {
            createSubscription(new YandexUid(YANDEX_UID));
            checkPriceDropPromoNotScheduled();
        }

        @Test
        void unauthorizedUserConfirmSubscription() throws Exception {
            EmailSubscription subscription = createSubscription(new YandexUid(YANDEX_UID));

            verificationUtil.confirmSubscription(subscription);

            checkPriceDropPromoScheduled(subscription.getId());
        }

        EmailSubscription createSubscription() throws Exception {
            return createSubscription(new Uid(UID));
        }

        EmailSubscription createSubscription(Identity<?> identity) throws Exception {
            EmailSubscription subscription = EmailSubscription.builder()
                .setSubscriptionType(NotificationType.PRICE_DROP)
                .addParameter(EmailSubscriptionParam.PARAM_REGION_ID, REGION_ID_1)
                .addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, MODEL_ID_1)
                .addParameter(EmailSubscriptionParam.PARAM_PRICE, PRICE_1)
                .build();
            List<EmailSubscription> subscriptions = subscriptionControllerInvoker.createSubscriptions(identity,
                MAIL_ADDRESS, Collections.singletonList(subscription));
            assertEquals(1, subscriptions.size());
            return subscriptions.get(0);
        }

        void checkPriceDropPromoScheduledWithUid(long subscriptionId) {
            NotificationEvent event = checkPriceDropPromoScheduled(subscriptionId);
            assertEquals(UID, (long) event.getUid());
        }

        NotificationEvent checkPriceDropPromoScheduled(long subscriptionId) {
            subscriptionAndIdentityService.reloadNotificationPool();
            List<NotificationEvent> events = eventSourceDAOTestHelper.getEventsByType(
                NotificationSubtype.PRICE_DROP_PROMO);
            assertEquals(1, events.size());
            NotificationEvent event = events.get(0);
            assertEquals(MAIL_ADDRESS, event.getAddress());
            assertEquals(NotificationEventStatus.NEW, event.getStatus());
            Map<String, String> eventData = event.getData();
            assertEquals(MODEL_ID_1, eventData.get(NotificationEventDataName.MODEL_ID));
            assertEquals(String.valueOf(subscriptionId), eventData.get(NotificationEventDataName.SUBSCRIPTION_ID));
            assertEquals(expectedSendTime, event.getSendTime().toInstant());
            return event;
        }

        void checkPriceDropPromoNotScheduled() {
            subscriptionAndIdentityService.reloadNotificationPool();
            List<NotificationEvent> events = eventSourceDAOTestHelper.getEventsByType(
                NotificationSubtype.PRICE_DROP_PROMO);
            assertEquals(Collections.emptyList(), events);
        }

    }

}
