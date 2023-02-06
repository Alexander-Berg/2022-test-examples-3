package ru.yandex.market.pers.notify.api.service.subscription;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.notify.EmailUtil;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionUnsubscribeReason;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author semin-serg
 */
class EmailSubscriptionDAOTest extends MockedDbTest {

    private static final long MODEL_ID_DESIRED = 12542L;
    private static final long MODEL_ID_ANOTHER = 6458L;
    private static final String EMAIL = "semin-serg@yandex.ru";

    @Autowired
    EmailSubscriptionDAO emailSubscriptionDAO;

    @Test
    void testGetSubscriptions() {
        //ADVERTISING
        createEmailSubscription(NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED, null);
        //PRICE_DROP на ту же модель с нужным статусом
        createEmailSubscription(NotificationType.PRICE_DROP, EmailSubscriptionStatus.CONFIRMED, MODEL_ID_DESIRED);
        //PA_ON_SALE на нужную модель с нужным статусом (1)
        EmailSubscription emailSubscription1 = createEmailSubscription(NotificationType.PA_ON_SALE,
            EmailSubscriptionStatus.CONFIRMED, MODEL_ID_DESIRED);
        //PA_ON_SALE на другую модель с нужным статусом
        createEmailSubscription(NotificationType.PA_ON_SALE, EmailSubscriptionStatus.CONFIRMED, MODEL_ID_ANOTHER);
        //PA_ON_SALE на ту модель с другим статуом
        createEmailSubscription(NotificationType.PA_ON_SALE, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION,
            MODEL_ID_DESIRED);
        //PA_ON_SALE на нужную модель с нужным статусом (2)
        EmailSubscription emailSubscription2 = createEmailSubscription(NotificationType.PA_ON_SALE,
            EmailSubscriptionStatus.CONFIRMED, MODEL_ID_DESIRED);

        List<EmailSubscription> actualSubscriptions = emailSubscriptionDAO.getSubscriptions(NotificationType.PA_ON_SALE,
            EmailSubscriptionStatus.CONFIRMED, EmailSubscriptionParam.PARAM_MODEL_ID, String.valueOf(MODEL_ID_DESIRED));

        assertSubscriptionsEquals(Arrays.asList(emailSubscription1, emailSubscription2), actualSubscriptions);
    }

    @Nested
    class UnsubscribeReasonPersistanceTests extends MockedDbTest {

        @Test
        void testParametricSubscription() {
            test(createAndSaveSubscription(NotificationType.PA_ON_SALE, MODEL_ID_DESIRED));
        }

        @Test
        void testDefaultSubscription() {
            EmailSubscription subscription = EmailSubscription.builder()
                .setSubscriptionType(NotificationType.MODEL_GRADE)
                .build();
            test(subscription);
        }

        @Test
        void testNonParametricSubscription() {
            test(createAndSaveSubscription(NotificationType.ADVERTISING, null));
        }

        EmailSubscription createAndSaveSubscription(NotificationType type, Long modelId) {
            EmailSubscription subscription = createEmailSubscription(type, EmailSubscriptionStatus.CONFIRMED, modelId);
            assertNotEquals(EmailSubscriptionStatus.UNSUBSCRIBED, subscription.getSubscriptionStatus());
            assertNull(subscription.getParameters().get(EmailSubscriptionParam.PARAM_UNSUBSCRIBE_REASON));
            return subscription;
        }

        void test(EmailSubscription subscription) {
            subscription.addParameter(EmailSubscriptionParam.PARAM_UNSUBSCRIBE_REASON,
                EmailSubscriptionUnsubscribeReason.EUROPEAN_USER);
            subscription.setSubscriptionStatus(EmailSubscriptionStatus.UNSUBSCRIBED);
            emailSubscriptionDAO.saveSubscriptions(EMAIL, Collections.singletonList(subscription));

            subscription = emailSubscriptionDAO.getSubscription(subscription.getId());
            assertEquals(EmailSubscriptionStatus.UNSUBSCRIBED, subscription.getSubscriptionStatus());
            assertEquals(EmailSubscriptionUnsubscribeReason.EUROPEAN_USER, subscription.getParameters().get(
                EmailSubscriptionParam.PARAM_UNSUBSCRIBE_REASON));
        }


    }

    private EmailSubscription createEmailSubscription(String email,
                                                      NotificationType type,
                                                      EmailSubscriptionStatus status) {
        EmailSubscription emailSubscription = new EmailSubscription();
        emailSubscription.setSubscriptionType(type);
        emailSubscription.setSubscriptionStatus(status);
        boolean inserted = emailSubscriptionDAO.saveSubscriptions(email, Collections.singletonList(emailSubscription));
        assertTrue(inserted);
        return emailSubscriptionDAO.getSubscription(emailSubscription.getId());
    }

    EmailSubscription createEmailSubscription(NotificationType type, EmailSubscriptionStatus status, Long modelId) {
        EmailSubscription emailSubscription = new EmailSubscription();
        emailSubscription.setSubscriptionType(type);
        emailSubscription.setSubscriptionStatus(status);
        if (modelId != null) {
            emailSubscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, String.valueOf(modelId));
        }
        boolean inserted = emailSubscriptionDAO.saveSubscriptions(EMAIL, Collections.singletonList(emailSubscription));
        assertTrue(inserted);
        return emailSubscriptionDAO.getSubscription(emailSubscription.getId());
    }

    void assertSubscriptionsEquals(List<EmailSubscription> expectedList, List<EmailSubscription> actualList) {
        assertEquals(expectedList.size(), actualList.size());
        expectedList.sort(Comparator.comparingLong(EmailSubscription::getId));
        actualList.sort(Comparator.comparingLong(EmailSubscription::getId));
        for (int i = 0; i < expectedList.size(); i++) {
            EmailSubscription expected = expectedList.get(i);
            EmailSubscription actual = actualList.get(i);
            assertEquals(expected.getSubscriptionType(), actual.getSubscriptionType());
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getSubscriptionStatus(), actual.getSubscriptionStatus());
            assertEquals(expected.getParameters(), actual.getParameters());
        }
    }

    @Test
    public void testDeleteAllOtherYandexDefaultSubscriptions() {
        EmailSubscription subscriptionToDelete = createEmailSubscription(EMAIL,
                NotificationType.STORE_ADVERTISING, EmailSubscriptionStatus.CONFIRMED);
        subscriptionToDelete.setSubscriptionStatus(EmailSubscriptionStatus.UNSUBSCRIBED);
        assertTrue(emailSubscriptionDAO.saveSubscriptions(EMAIL, Collections.singletonList(subscriptionToDelete)));

        List<String> yandexEmails = EmailUtil.distributeIfYandexEmail(EMAIL);
        List<EmailSubscription> subscriptions =
                emailSubscriptionDAO.getSubscriptions(yandexEmails);

        assertEquals(subscriptions.size(), yandexEmails.size());
        for (String yandexEmail : yandexEmails) {
            Optional<EmailSubscription> subscription = subscriptions.stream()
                    .filter(x -> yandexEmail.equalsIgnoreCase(x.getEmail()))
                    .findFirst();
            assertTrue(subscription.isPresent());
            assertEquals(subscription.get().getSubscriptionStatus(), EmailSubscriptionStatus.UNSUBSCRIBED);
        }
    }

    @Test
    public void testDeleteOtherExistingYandexSubscriptions() {
        EmailSubscription subscriptionToDelete = createEmailSubscription(EMAIL,
                NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED);
        createEmailSubscription("semin-serg@yandex.ua",
                NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED);

        subscriptionToDelete.setSubscriptionStatus(EmailSubscriptionStatus.UNSUBSCRIBED);
        assertTrue(emailSubscriptionDAO.saveSubscriptions(EMAIL, Collections.singletonList(subscriptionToDelete)));

        List<String> yandexEmails = EmailUtil.distributeIfYandexEmail(EMAIL);
        List<EmailSubscription> subscriptions = emailSubscriptionDAO.getSubscriptions(yandexEmails);

        assertEquals(2, subscriptions.size());
        for (String email : Arrays.asList(EMAIL, "semin-serg@yandex.ua")) {
            Optional<EmailSubscription> subscription = subscriptions.stream()
                    .filter(x -> email.equalsIgnoreCase(x.getEmail()))
                    .findFirst();
            assertTrue(subscription.isPresent());
            assertEquals(subscription.get().getSubscriptionStatus(), EmailSubscriptionStatus.UNSUBSCRIBED);
        }
    }

    @Test
    public void testSaveSubscriptionsConfirmedShouldNotBeUpdatedToNeedSendConfirmation() {
        EmailSubscription savedSubscription = createEmailSubscription(EMAIL,
                NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED);
        savedSubscription.setSubscriptionStatus(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        assertFalse(emailSubscriptionDAO.saveSubscriptions(EMAIL, Collections.singletonList(savedSubscription)));

        List<EmailSubscription> subscriptions = emailSubscriptionDAO.getSubscriptions(EMAIL);

        assertEquals(1, subscriptions.size());
        assertEquals(savedSubscription, subscriptions.get(0));
        assertEquals(EmailSubscriptionStatus.CONFIRMED, subscriptions.get(0).getSubscriptionStatus());
    }

    @Test
    public void testSaveSubscriptionsNotDuplicateConfirmedSubscriptions() {
        EmailSubscription savedSubscription = createEmailSubscription(EMAIL, NotificationType.ADVERTISING,
                EmailSubscriptionStatus.CONFIRMED);
        savedSubscription.setSubscriptionStatus(EmailSubscriptionStatus.CONFIRMED);
        assertFalse(emailSubscriptionDAO.saveSubscriptions(EMAIL, Collections.singletonList(savedSubscription)));

        List<EmailSubscription> subscriptions = emailSubscriptionDAO.getSubscriptions(EMAIL);

        assertEquals(1, subscriptions.size());
        assertEquals(savedSubscription, subscriptions.get(0));
        assertEquals(EmailSubscriptionStatus.CONFIRMED, subscriptions.get(0).getSubscriptionStatus());
    }
  
    @Test
    public void testGetParametrizedSubscriptionsByEmailAndParam() {
        EmailSubscription emailSubscription = new EmailSubscription();
        emailSubscription.setSubscriptionType(NotificationType.PA_ON_SALE);
        emailSubscription.setSubscriptionStatus(EmailSubscriptionStatus.CONFIRMED);
        final String modelId = String.valueOf(123456);
        emailSubscription.setParameters(new HashMap<String, String>() {{
            put(EmailSubscriptionParam.PARAM_MODEL_ID, modelId);
        }});
        boolean inserted = emailSubscriptionDAO.saveSubscriptions(EMAIL, Collections.singletonList(emailSubscription));
        assertTrue(inserted);
        emailSubscriptionDAO.getParametrizedSubscriptions(Collections.singletonList(EMAIL), EmailSubscriptionParam.PARAM_MODEL_ID, modelId);
    }
}
