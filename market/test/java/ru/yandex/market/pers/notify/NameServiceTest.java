package ru.yandex.market.pers.notify;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.event.NotificationEventPayload;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.event.EventAddressType;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pers.notify.mock.MockFactory.SBER_ID;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 22.02.2018
 */
class NameServiceTest extends MockedDbTest {
    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;
    @Autowired
    private NameService nameService;
    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;

    @Test
    void generateFullNameNameInSubscription() {
        String email = "myemail@email.em";
        String name = "Myname";
        EmailSubscription subscription = createSubscription(email,
            Collections.singletonMap(EmailSubscriptionParam.PARAM_USER_NAME, name));
        NotificationEventPayload<?> eventPayload = eventPayload(email);
        eventPayload.getData().put(NotificationEventDataName.SUBSCRIPTION_ID, String.valueOf(subscription.getId()));
        assertEquals(name, nameService.generateFullName(eventPayload));
    }

    @Test
    void generateFullNameUidInSubscription() {
        String email = "myemail@email.em";
        String name = "Myname";
        long uid = 1L;
        EmailSubscription subscription = createSubscription(email,
            Collections.singletonMap(EmailSubscriptionParam.PARAM_UID, String.valueOf(uid)));
        NotificationEventPayload<?> eventPayload = eventPayload(email);
        eventPayload.getData().put(NotificationEventDataName.SUBSCRIPTION_ID, String.valueOf(subscription.getId()));
        blackBoxPassportService.doReturn(uid, email, name);
        assertEquals(name, nameService.generateFullName(eventPayload));
    }

    @Test
    void generateFullNameNameInEvent() {
        String email = "myemail@email.em";
        String name = "Myname";
        EmailSubscription subscription = createSubscription(email, Collections.emptyMap());
        NotificationEventPayload<?> eventPayload = eventPayload(email);
        eventPayload.getData().put(NotificationEventDataName.SUBSCRIPTION_ID, String.valueOf(subscription.getId()));
        eventPayload.getData().put(NotificationEventDataName.USER_NAME, name);
        assertEquals(name, nameService.generateFullName(eventPayload));
    }

    @Test
    void generateFullNameUidInEvent() {
        String email = "myemail@email.em";
        String name = "Myname";
        long uid = 1L;
        EmailSubscription subscription = createSubscription(email, Collections.emptyMap());
        NotificationEventPayload<?> eventPayload = eventPayload(email);
        eventPayload.getData().put(NotificationEventDataName.SUBSCRIPTION_ID, String.valueOf(subscription.getId()));
        eventPayload.getData().put(NotificationEventDataName.USER_NAME, name);
        blackBoxPassportService.doReturn(uid, email, name);
        assertEquals(name, nameService.generateFullName(eventPayload));
    }

    @Test
    void getFullNameForPassportUid() {
        assertEquals("PassportFirstName PassportLastName", nameService.getFullName(Uid.of(1L)));
    }

    @Test
    void getFullNameForSberId() {
        assertEquals("First_Name_Sber_Id Last_Name_Sber_Id", nameService.getFullName(Uid.of(SBER_ID)));
    }

    private EmailSubscription createSubscription(String email, Map<String, String> params) {
        NotificationType type = NotificationType.PA_ON_SALE;
        EmailSubscription subscription = subscription(email, type);
        subscription.setParameters(params);
        subscriptionAndIdentityService.createSubscriptions(email, Collections.singletonList(
            subscription
        ), new Uid(1L), true);
        return subscriptionAndIdentityService.getEmailSubscriptions(email, type).get(0);
    }

    private EmailSubscription subscription(String email, NotificationType type) {
        return new EmailSubscription(email, type, EmailSubscriptionStatus.UNSUBSCRIBED);
    }

    private NotificationEventPayload<?> eventPayload(String email) {
        NotificationEvent event = new NotificationEvent(0L, email, EventAddressType.MAIL, NotificationSubtype.PA_WELCOME,
            NotificationEventStatus.NEW, Collections.emptyMap(), new Date(), new Date(), new Date(), false);
        return NotificationEventPayload.from(event);
    }
}
