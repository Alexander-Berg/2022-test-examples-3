package ru.yandex.market.pers.notify.ems.filter;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import ru.yandex.market.pers.notify.ems.NotificationEventConsumer;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.filter.config.NotificationSpamFilterConfig;
import ru.yandex.market.pers.notify.ems.persistence.EventSourceDAO;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.external.sender.SenderClient;
import ru.yandex.market.pers.notify.mail.consumer.CarterConsumer_1;
import ru.yandex.market.pers.notify.mock.MarketMailerMockFactory;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.YandexUid;
import ru.yandex.market.pers.notify.model.event.EventAddressType;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.templates.EmailSnippetGenerator;
import ru.yandex.market.pers.notify.test.MailProcessorInvoker;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;
import ru.yandex.market.pers.notify.util.SubscriptionUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 17.02.16
 */
public class NotificationSpamFilterTest extends MarketMailerMockedDbTest {
    private static final String EMAIL = "foobar@example.com";
    private static final YandexUid YANDEXUID = new YandexUid("abcdef12345");

    private static final Clock CLOCK_INITIAL = Clock.systemDefaultZone();
    private static final Clock CLOCK_AFTER_2_DAYS = Clock.offset(CLOCK_INITIAL, Duration.ofDays(2));
    private static final Clock CLOCK_AFTER_1_MIN = Clock.offset(CLOCK_INITIAL, Duration.ofMinutes(1));
    private static final Clock CLOCK_AFTER_3_MIN = Clock.offset(CLOCK_INITIAL, Duration.ofMinutes(3));


    @Autowired
    private NotificationSpamFilter notificationSpamFilter;
    @Autowired
    private EmailSnippetGenerator emailSnippetGenerator;
    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private SubscriptionUtil subscriptionUtil;
    @Autowired
    private SubscriptionAndIdentityService subscriptionService;
    @Autowired
    private MailProcessorInvoker mailProcessorInvoker;
    @Autowired
    private EventSourceDAO eventSourceDAO;
    @Autowired
    @Qualifier("senderClient")
    private SenderClient senderClient;
    @Autowired
    MarketMailerMockFactory mockFactory;

    private Clock originalClock;

    @BeforeEach
    void saveOriginalClock() {
        originalClock = notificationSpamFilter.getClock();
    }

    @AfterEach
    void restoreClock() {
        notificationSpamFilter.setClock(originalClock);
    }


    @Test
    void testAll() {
        NotificationEventConsumer testConsumer = new CarterConsumer_1(emailSnippetGenerator, notificationEventService
        );
        NotificationEventConsumer consumer = notificationSpamFilter.filter(
            new NotificationEvent(1L, 3L, null, null, "valetr@yandex.ru", EventAddressType.MAIL,
                NotificationSubtype.WELCOME_1, NotificationEventStatus.NEW,
                new HashMap<>(), new Date(), new Date(), new Date(), false),
            NotificationSpamFilterConfig.empty(),
            testConsumer);
        assertEquals(testConsumer, consumer);

        consumer = notificationSpamFilter.filter(
            new NotificationEvent(1L, 3L, null, null, "valetr@yandex.ru", EventAddressType.MAIL,
                NotificationSubtype.WELCOME_1, NotificationEventStatus.NEW,
                new HashMap<>(), new Date(), new Date(), new Date(), false),
            NotificationSpamFilterConfig.empty(),
            testConsumer);
        assertFalse(Objects.equals(testConsumer, consumer));
    }

    @Test
    void duplicatedEventMustBeFilteredBeforeSpamDuration() {
        testTwoEvents(CLOCK_AFTER_3_MIN, NotificationEventStatus.REJECTED_AS_SPAM);
    }

    @Test
    void duplicatedEventMustNotBeFilteredAfterSpamDuration() {
        testTwoEvents(CLOCK_AFTER_2_DAYS, NotificationEventStatus.SENT);
    }

    @Test
    void duplicatedEventMustBeDelayedBeforeThreshold() {
        notificationSpamFilter.setClock(CLOCK_INITIAL);
        NotificationEvent originalEvent = createSubscriptionAndGetConfirmationEvent();
        NotificationEvent duplicateEvent = createDuplicateEvent(originalEvent);
        mailProcessorInvoker.processAllMail();
        assertEventStatus(NotificationEventStatus.SENT, originalEvent);
        assertEventStatus(NotificationEventStatus.NEW, duplicateEvent);
    }

    @Test
    void failedEventMustBeDelayedBeforeThreshold() {
        testFailedEvent(CLOCK_AFTER_1_MIN, NotificationEventStatus.IOERROR);
    }

    @Test
    void failedEventMustNotBeDelayedAfterThreshold() {
        testFailedEvent(CLOCK_AFTER_3_MIN, NotificationEventStatus.SENT);
    }

    private void testFailedEvent(Clock secondRunClock, NotificationEventStatus expectedStatus) {
        notificationSpamFilter.setClock(CLOCK_INITIAL);
        MarketMailerMockFactory.initSenderClientMock(senderClient, SenderClient.SendTransactionalResponse.serverError());

        NotificationEvent event = createSubscriptionAndGetConfirmationEvent();
        mailProcessorInvoker.processAllMail();
        assertEventStatus(NotificationEventStatus.IOERROR, event);

        notificationSpamFilter.setClock(secondRunClock);
        MarketMailerMockFactory.initSenderClientMock(senderClient);
        mailProcessorInvoker.processAllMail();
        assertEventStatus(expectedStatus, event);
    }

    private void testTwoEvents(Clock duplicatedEventClock, NotificationEventStatus expectedDuplicatedEventStatus) {
        notificationSpamFilter.setClock(CLOCK_INITIAL);
        NotificationEvent originalEvent = createSubscriptionAndGetConfirmationEvent();
        mailProcessorInvoker.processAllMail();
        assertEventStatus(NotificationEventStatus.SENT, originalEvent);

        notificationSpamFilter.setClock(duplicatedEventClock);
        NotificationEvent duplicateEvent = createDuplicateEvent(originalEvent);
        mailProcessorInvoker.processAllMail();
        assertEventStatus(expectedDuplicatedEventStatus, duplicateEvent);
    }

    private void assertEventStatus(NotificationEventStatus expectedStatus, NotificationEvent event) {
        assertEquals(expectedStatus, eventSourceDAO.getMailEvent(event.getId()).getStatus());
    }

    private NotificationEvent createDuplicateEvent(NotificationEvent originalEvent) {
        return notificationEventService.addEvent(NotificationEventSource
                .fromEmail(EMAIL, NotificationSubtype.CONFIRM_SUBSCRIPTION)
                .setSourceId(originalEvent.getSourceId())
                .addDataParam(NotificationEventDataName.SUBSCRIPTION_ID,
                        originalEvent.getData().get(NotificationEventDataName.SUBSCRIPTION_ID))
                .build());
    }

    @NotNull
    private NotificationEvent createSubscriptionAndGetConfirmationEvent() {
        subscriptionService.createSubscriptions(EMAIL, Collections.singletonList(new EmailSubscription(EMAIL, NotificationType.ADVERTISING,
                EmailSubscriptionStatus.NEED_SEND_CONFIRMATION)), YANDEXUID);
        NotificationEvent originalEvent = eventSourceDAO.getLastEventByEmail(EMAIL);
        assertEquals(NotificationSubtype.CONFIRM_SUBSCRIPTION, originalEvent.getNotificationSubtype());
        return originalEvent;
    }


}
