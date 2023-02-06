package ru.yandex.market.pers.notify.api.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.DeletedUserInfo;
import ru.yandex.market.pers.notify.model.Market;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.model.subscription.Subscription;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus;
import ru.yandex.market.pers.notify.push.MobileAppInfoDAO;
import ru.yandex.market.pers.notify.service.BlackListService;
import ru.yandex.market.pers.notify.service.UsersService;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityDAO;
import ru.yandex.market.pers.notify.subscription.SubscriptionService;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.notify.api.controller.Util.SubscriptionsUtil.compareSubscriptions;
import static ru.yandex.market.pers.notify.subscription.MobileAppSubscriptionsMigrator.DEFAULT_NON_TRANSACTIONAL_PUSH_TYPES;

public class UserControllerTest extends MarketUtilsMockedDbTest {
    private static final Uid UID = new Uid(12345L);
    private static final String EMAIL = "foo@bar.buzz";
    private static final String ANOTHER_EMAIL = "another_foo@bar.buzz";
    private static final String REASON = "reason";
    private static final String UUID = "1234345678967";
    private static final String PUSH_TOKEN = "shdfkjsfljknfl";
    private static final String UUID_NOT_BLUE = "sdfsdfskm";

    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;
    @Autowired
    private SubscriptionAndIdentityDAO subscriptionAndIdentityDAO;
    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;
    @Autowired
    private UserControllerInvoker controllerInvoker;
    @Autowired
    private BlackListService blackListService;
    @Autowired
    private UsersService usersService;
    @Autowired
    private SubscriptionService subscriptionService;

    @BeforeEach
    public void setUp() {
        blackBoxPassportService.doReturn(UID.getValue(), Collections.emptyList());
    }

    @Test
    public void unsubscribeMobileAppsTest() throws Exception {
        mockPassport(Collections.singletonList(EMAIL));

        mobileAppInfoDAO.add(new MobileAppInfo(
                UID.getValue(),
                UUID,
                "ru.beru.android",
                PUSH_TOKEN,
                MobilePlatform.ANDROID,
                false
        ));
        mobileAppInfoDAO.add(new MobileAppInfo(
                UID.getValue(),
                UUID_NOT_BLUE,
                "ru.yandex.red.market",
                PUSH_TOKEN,
                MobilePlatform.ANDROID,
                false
        ));

        controllerInvoker.deleteUser(
                UID,
                Market.STORE,
                REASON,
                status().is2xxSuccessful()
        );

        assertTrue(mobileAppInfoDAO.getByUuid(UUID).isUnregistered());
        assertFalse(mobileAppInfoDAO.getByUuid(UUID_NOT_BLUE).isUnregistered());

        assertSubscriptions(UUID, SubscriptionStatus.UNSUBSCRIBED);
        assertNoSubscriptions(UUID_NOT_BLUE);
    }

    @Test
    public void deleteUserTest() throws Exception {
        mockPassport(Collections.singletonList(EMAIL));
        List<EmailSubscription> subscriptions =  new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.STORE_CART, EmailSubscriptionStatus.CONFIRMED));
            add(new EmailSubscription(EMAIL, NotificationType.STORE_PUSH, EmailSubscriptionStatus.CONFIRMED));
            add(new EmailSubscription(EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED));
        }};
        subscriptionAndIdentityDAO.saveSubscriptions(EMAIL, subscriptions);

        DeletedUserInfo deletedUserInfo = controllerInvoker.deleteUser(
                UID,
                Market.STORE,
                REASON,
                status().is2xxSuccessful()
        );

        compareEmails(deletedUserInfo, Collections.singletonList(EMAIL));
        assertTrue(blackListService.inRemovals(EMAIL));


        List<EmailSubscription> result = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.STORE_CART, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.STORE_ADVERTISING, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.STORE_WISHLIST, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.STORE_PUSH, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED));
            add(new EmailSubscription(EMAIL, NotificationType.GRADE_AFTER_ORDER, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.STORE_GENERAL_INFORMATION, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.STORE_PUSH_ORDER_STATUS, EmailSubscriptionStatus.UNSUBSCRIBED));
        }};

        compareSubscriptions(
                result,
                subscriptionAndIdentityDAO.getSubscriptions(EMAIL),
                false
        );
    }

    @Test
    public void deleteUserTestDifferentEmails() throws Exception {
        List<String> emails = Arrays.asList(EMAIL, ANOTHER_EMAIL);
        mockPassport(emails);

        DeletedUserInfo deletedUserInfo = controllerInvoker.deleteUser(UID, Market.STORE, REASON, status().is2xxSuccessful());
        compareEmails(deletedUserInfo, emails);

        assertTrue(blackListService.inRemovals(EMAIL));
        assertTrue(blackListService.inRemovals(ANOTHER_EMAIL));

        List<EmailSubscription> resultByEmail = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(EMAIL, NotificationType.STORE_CART, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.STORE_ADVERTISING, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.STORE_WISHLIST, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.GRADE_AFTER_ORDER, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.STORE_GENERAL_INFORMATION, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(EMAIL, NotificationType.STORE_PUSH_ORDER_STATUS, EmailSubscriptionStatus.UNSUBSCRIBED));
        }};

        List<EmailSubscription> resultByAnotherEmail = new ArrayList<EmailSubscription>() {{
            add(new EmailSubscription(ANOTHER_EMAIL, NotificationType.STORE_CART, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(ANOTHER_EMAIL, NotificationType.STORE_ADVERTISING, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(ANOTHER_EMAIL, NotificationType.STORE_WISHLIST, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(ANOTHER_EMAIL, NotificationType.GRADE_AFTER_ORDER, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(ANOTHER_EMAIL, NotificationType.STORE_GENERAL_INFORMATION, EmailSubscriptionStatus.UNSUBSCRIBED));
            add(new EmailSubscription(ANOTHER_EMAIL, NotificationType.STORE_PUSH_ORDER_STATUS, EmailSubscriptionStatus.UNSUBSCRIBED));
        }};

        compareSubscriptions(
                resultByEmail,
                subscriptionAndIdentityDAO.getSubscriptions(EMAIL),
                false
        );

        compareSubscriptions(
                resultByAnotherEmail,
                subscriptionAndIdentityDAO.getSubscriptions(ANOTHER_EMAIL),
                false
        );
    }

    private void compareEmails(DeletedUserInfo deletedUserInfo, List<String> expectedEmails) {

        List<String> unsubscribedEmails = deletedUserInfo.getSubscriptions().stream()
                .map(EmailSubscription::getEmail)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        expectedEmails.sort(Comparator.naturalOrder());

        assertIterableEquals(expectedEmails, unsubscribedEmails);
    }

    private void mockPassport(List<String> emails) {
        blackBoxPassportService.doReturn(UserControllerTest.UID.getValue(), emails);
    }

    private void assertNoSubscriptions(String uuid) {
        Uuid identity = new Uuid(uuid);
        List<Subscription> subscriptions = subscriptionService.get(identity, DEFAULT_NON_TRANSACTIONAL_PUSH_TYPES);
        assertTrue(subscriptions.isEmpty());
    }

    private void assertSubscriptions(String uuid, SubscriptionStatus status) {
        Set<NotificationType> pushTypes = new HashSet<>(DEFAULT_NON_TRANSACTIONAL_PUSH_TYPES);

        Uuid identity = new Uuid(uuid);
        List<Subscription> subscriptions = subscriptionService.get(identity, DEFAULT_NON_TRANSACTIONAL_PUSH_TYPES);
        assertEquals(DEFAULT_NON_TRANSACTIONAL_PUSH_TYPES.size(), subscriptions.size());
        subscriptions.forEach(subscription -> {
            assertEquals(identity, subscription.getIdentity());
            assertEquals(NotificationTransportType.PUSH, subscription.getChannel());
            assertEquals(status, subscription.getStatus());
            assertTrue(pushTypes.remove(subscription.getType()));
        });
    }
}
