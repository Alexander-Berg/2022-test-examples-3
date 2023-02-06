package ru.yandex.market.pers.notify.api.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.EventSourceDAO;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.Email;
import ru.yandex.market.pers.notify.model.EmailOwnership;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.passport.PassportService;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityDAO;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.notify.test.TestUtil.stringFromFile;

class SettingsControllerTest extends MarketUtilsMockedDbTest {
    private static final long UID = 12345L;
    private static final long ANOTHER_UID = 12346L;
    private static final String EMAIL = "foo@bar.buzz";
    private static final String ANOTHER_EMAIL = "another_foo@bar.buzz";
    private static final String DEFAULT_PASSPORT_EMAIL = "default_passport_email@bar.buzz";
    private static final String YANDEX_EMAIL = "ya_yandex_mail@@@yandex.ru";
    private static final String ACTIVE_EMAIL = "active_email@bar.buzz";
    private static final String OLD_AUTHORIZED_EMAIL = "old_moldel_authorized@bar.buzz";
    private static final String PLACE = "SETTINGS";
    private static final String PLATFORM = "DESKTOP";
    static final String SUBSCRIBED_SETTINGS = "/data/settings/subscribed_all.json";
    private static final String CONTEXT_SUBSCRIBED_SETTINGS = "/data/settings/context_subscribed_all.json";
    private static final String UNSUBSCRIBED_SETTINGS = "/data/settings/unsubscribed_all.json";
    private static final String SETTINGS_WITHOUT_ADS = "/data/settings/subscribed_all_except_ads.json";
    static final String SETTINGS_WITHOUT_JOURNAL = "/data/settings/subscribed_all_except_journal.json";
    private static final String SETTINGS_WITHOUT_EXPLICIT = "/data/settings/subscribed_all_except_explicit.json";
    private static final String SETTINGS_WITH_ADS_ONLY = "/data/settings/subscribed_ads_only.json";
    private static final String SUBSCRIBED_SETTINGS_WITH_PARAMS = "/data/settings/subscribed_all_with_place_platform.json";
    private static final String MIXED_SETTINGS = "/data/settings/mixed.json";
    private static final String MIXED_NO_ADS_SETTINGS = "/data/settings/mixed_no_ads.json";
    private static final String SUBSCRIBED_ADV_WISHLIST_SETTINGS = "/data/settings/subscribed_adv_wishlist.json";
    private static final String UNSUBSCRIBED_ADV_WISHLIST_SETTINGS = "/data/settings/unsubscribed_adv_wishlist.json";


    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;
    @Autowired
    private PassportService passportService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SubscriptionAndIdentityDAO subscriptionAndIdentityDAO;
    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;
    @Autowired
    private SubscriptionControllerInvoker controllerInvoker;
    @Autowired
    private EventSourceDAO eventSourceDAO;
    @Autowired
    private SettingsControllerInvoker settingsControllerInvoker;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void getSettingsContextUserSubscriptionConfirmed() throws Exception {
        createSubscriptionsForce(UID, EMAIL, SUBSCRIBED_SETTINGS);
        mockMvc.perform(get("/settings/email/notifications/context")
            .contentType(MediaType.APPLICATION_JSON)
            .param("uid", String.valueOf(UID)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    public void getSettingsContextUserUnsubscriptionConfirmed() throws Exception {
        createSubscriptionsForce(UID, EMAIL, UNSUBSCRIBED_SETTINGS);
        mockMvc.perform(get("/settings/email/notifications/context")
            .contentType(MediaType.APPLICATION_JSON)
            .param("uid", String.valueOf(UID)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    public void getSettingsContextUnauthorizedUser() throws Exception {
        mockMvc.perform(get("/settings/email/notifications/context")
            .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
            .andExpect(content().json(toJson(
                new Error("INVALID_FORMAT", "Required long parameter 'uid' is not present", 400))));
    }

    @Test
    void getSettingsContextNewUser() throws Exception {
        createPassportEmails(Collections.singletonList(EMAIL));
        mockMvc.perform(get("/settings/email/notifications/context")
            .contentType(MediaType.APPLICATION_JSON)
            .param("uid", String.valueOf(UID)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().json(stringFromFile(CONTEXT_SUBSCRIBED_SETTINGS)));
    }

    @Test
    public void getEmailsNoEmails() throws Exception {
        blackBoxPassportService.returnEmptyForUid(UID);

        List<Email> result = Collections.emptyList();

        checkEmails(result);
    }

    @Test
    public void getEmailsOnlyOnePassport() throws Exception {
        createPassportEmails(Collections.singletonList(DEFAULT_PASSPORT_EMAIL));

        List<Email> result = Collections.singletonList(new Email(DEFAULT_PASSPORT_EMAIL, true));

        checkEmails(result);
    }

    @Test
    public void getEmailsHasManyPassport() throws Exception {
        createPassportEmails(new ArrayList<String>() {{
            add(DEFAULT_PASSPORT_EMAIL);
            add(EMAIL);
            add(ANOTHER_EMAIL);
        }});

        List<Email> result = Arrays.asList(
            new Email(DEFAULT_PASSPORT_EMAIL, true),
            new Email(EMAIL, false),
            new Email(ANOTHER_EMAIL, false)
        );

        checkEmails(result);
    }

    @Test
    public void getEmailsHasManyPassportPassportWithDefaultYandex() throws Exception {
        // создаем паспортные email-ы
        createPassportEmails(new ArrayList<String>() {{
            add(DEFAULT_PASSPORT_EMAIL);
            add(YANDEX_EMAIL);
            add(EMAIL);
        }});

        List<Email> result = Arrays.asList(
            new Email(YANDEX_EMAIL, true),
            new Email(DEFAULT_PASSPORT_EMAIL, false),
            new Email(EMAIL, false)
        );

        checkEmails(result);
    }

    @Test
    public void getEmailsHasManyPassportAndActive() throws Exception {
        // создаем паспортные email-ы
        createPassportEmails(new ArrayList<String>() {{
            add(DEFAULT_PASSPORT_EMAIL);
            add(YANDEX_EMAIL);
            add(EMAIL);
        }});

        // создали активный
        createActiveForce(UID, ACTIVE_EMAIL);

        List<Email> result = Arrays.asList(
            new Email(DEFAULT_PASSPORT_EMAIL, false),
            new Email(YANDEX_EMAIL, false),
            new Email(EMAIL, false),
            new Email(ACTIVE_EMAIL, true)
        );

        checkEmails(result);
    }

    @Test
    public void getEmailsShouldFilterUnconfirmed() throws Exception {
        blackBoxPassportService.doReturn(UID, Collections.singletonList("ukchuvrusconfirmed@email.ru"));
        subscriptionAndIdentityService.createEmailOwnershipIfNecessary(new Uid(UID), "ukchuvrus@email.ru", false);
        subscriptionAndIdentityService.createEmailOwnershipIfNecessary(new Uid(UID), "ukchuvrusconfirmed@email.ru", false);
        checkEmails(Collections.singletonList(new Email("ukchuvrusconfirmed@email.ru", true)));
    }

    @Test
    public void duplicatePassportEmailNonActive() throws Exception {
        createPassportEmails(Arrays.asList(OLD_AUTHORIZED_EMAIL, ACTIVE_EMAIL));

        createActiveForce(UID, OLD_AUTHORIZED_EMAIL);
        createActiveForce(UID, ACTIVE_EMAIL);

        List<Email> result = new ArrayList<Email>() {{
            add(new Email(OLD_AUTHORIZED_EMAIL, false));
            add(new Email(ACTIVE_EMAIL, true));
        }};
        checkEmails(result);
    }

    @Test
    public void setActiveEmailTwice() throws Exception {
        blackBoxPassportService.returnEmptyForUid(UID);

        createActiveForce(UID, EMAIL);

        List<Email> result = Collections.singletonList(new Email(EMAIL, true));
        checkEmails(result);

        createActiveForce(UID, EMAIL);

        result = Collections.singletonList(new Email(EMAIL, true));
        checkEmails(result);
    }

    @Test
    public void wrongSetActiveEmailState() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            subscriptionAndIdentityDAO.setActiveEmail(new Uid(UID), EMAIL);
        });
    }

    @Test
    public void changeActiveEmailTest() throws Exception {
        blackBoxPassportService.returnEmptyForUid(UID);

        createActiveForce(UID, EMAIL);
        assertEquals(1, subscriptionAndIdentityDAO.getEmailsByIdentity(new Uid(UID)).size());

        List<Email> result = Collections.singletonList(new Email(EMAIL, true));
        checkEmails(result);

        createActiveForce(UID, ACTIVE_EMAIL);
        assertEquals(2, subscriptionAndIdentityDAO.getEmailsByIdentity(new Uid(UID)).size());

        result = Arrays.asList(
            new Email(EMAIL, false),
            new Email(ACTIVE_EMAIL, true)
        );
        checkEmails(result);
    }

    @Test
    public void setActiveEmail() throws Exception {
        blackBoxPassportService.doReturn(UID, ANOTHER_EMAIL);

        settingsControllerInvoker.createActive(UID, ANOTHER_EMAIL);

        blackBoxPassportService.returnEmptyForUid(UID);

        checkEmails(Collections.singletonList(new Email(ANOTHER_EMAIL, true)));
    }

    @Test
    public void setActiveEmailWithSubscriptions() throws Exception {
        createSubscriptionsForce(UID, ANOTHER_EMAIL, MIXED_SETTINGS);

        settingsControllerInvoker.createActive(UID, ANOTHER_EMAIL);

        List<EmailSubscription> subscriptions = new ArrayList<>(subscriptionAndIdentityDAO.getSubscriptions(ANOTHER_EMAIL));

        // должно быть сохранено только 2 подписки в статусе CONFIRMED
        assertEquals(2, subscriptions.stream()
            .filter(e -> e.getSubscriptionStatus().equals(EmailSubscriptionStatus.CONFIRMED)).count());

        assertEquals(4, subscriptions.size());

        checkSubscriptions(UID, ANOTHER_EMAIL, MIXED_SETTINGS);
        blackBoxPassportService.returnEmptyForUid(UID);
        checkEmails(Collections.singletonList(new Email(ANOTHER_EMAIL, true)));
    }

    @Test
    public void setActiveEmailContainsInPassport() throws Exception {
        createPassportEmails(Collections.singletonList(DEFAULT_PASSPORT_EMAIL));

        createActiveForce(UID, DEFAULT_PASSPORT_EMAIL);
        checkEmails(Collections.singletonList(new Email(DEFAULT_PASSPORT_EMAIL, true)));
    }

    @Test
    void setActiveEmailContainsInEmailOwnership() throws Exception {
        blackBoxPassportService.returnEmptyForUid(UID);

        createActiveForce(UID, OLD_AUTHORIZED_EMAIL);
        createActiveForce(UID, EMAIL);

        jdbcTemplate.update("DELETE FROM CRM_CHANGED_EMAIL_OWNERSHIP");

        createActiveForce(UID, OLD_AUTHORIZED_EMAIL);
        checkEmails(Arrays.asList(new Email(OLD_AUTHORIZED_EMAIL, true), new Email(EMAIL, false)));

        EmailOwnership oldEmail = getOwnership(OLD_AUTHORIZED_EMAIL);
        assertTrue(isOwnershipInExportQueue(oldEmail.getId()));

        EmailOwnership newEmail = getOwnership(EMAIL);
        assertTrue(isOwnershipInExportQueue(newEmail.getId()));
    }

    @NotNull
    private EmailOwnership getOwnership(String email) {
        return subscriptionAndIdentityDAO.getEmailsByIdentity(new Uid(UID)).stream()
            .filter(x -> email.equals(x.getEmail()))
            .findFirst().orElseThrow(() -> new IllegalStateException("No ownership for " + email));
    }

    @Test
    public void setIllegalActiveEmail() throws Exception {
        settingsControllerInvoker.createActiveIllegal(EMAIL, UID);
    }

    @Test
    public void firstSetActiveShouldCreateSubscriptionsAndReturnHonestSettingsForOthersEmails() throws Exception {
        List<String> emails = new ArrayList<String>() {{
            add(EMAIL);
            add(ANOTHER_EMAIL);
            add(OLD_AUTHORIZED_EMAIL);
        }};
        createPassportEmails(emails);

        createActiveForce(UID, ACTIVE_EMAIL);

        checkSubscriptions(UID, ACTIVE_EMAIL, SUBSCRIBED_SETTINGS);

        for (String email : emails) {
            settingsControllerInvoker.checkSubscriptions(UID, email, SETTINGS_WITHOUT_EXPLICIT);
        }
    }

    @Test
    public void firstSetSubscriptionsShouldCreateSubscriptionsAndReturnHonestSettingsForOthersEmails() throws Exception {
        List<String> emails = new ArrayList<String>() {{
            add(ACTIVE_EMAIL);
            add(ANOTHER_EMAIL);
            add(OLD_AUTHORIZED_EMAIL);
        }};
        createPassportEmails(emails);

        createSubscriptionsForce(UID, EMAIL, SUBSCRIBED_SETTINGS);

        checkSubscriptions(UID, EMAIL, SUBSCRIBED_SETTINGS);


        for (String email : emails) {
            settingsControllerInvoker.checkSubscriptions(UID, email, SETTINGS_WITHOUT_EXPLICIT);
        }
    }

    @Test
    public void shouldBeDefaultSettingsForAllOnFirstUsage() throws Exception {
        List<String> emails;
        createPassportEmails(emails = new ArrayList<String>() {{
            add(EMAIL);
            add(ANOTHER_EMAIL);
            add(ACTIVE_EMAIL);
            add(OLD_AUTHORIZED_EMAIL);
        }});

        for (String email : emails) {
            settingsControllerInvoker.checkSubscriptions(UID, email, SUBSCRIBED_SETTINGS);
            assertEquals(0, subscriptionAndIdentityDAO.getSubscriptions(email).size());
        }
    }

    @Test
    public void unsubscribeAdsOnFirstUsage() throws Exception {
        createSubscriptionsForce(UID, EMAIL, SETTINGS_WITHOUT_ADS);

        checkSubscriptions(UID, EMAIL, SETTINGS_WITHOUT_ADS);

        List<EmailSubscription> subscriptions = subscriptionAndIdentityDAO.getSubscriptions(EMAIL, EmailSubscriptionStatus.UNSUBSCRIBED);
        assertEquals(1, subscriptions.size());
        assertEquals(NotificationType.ADVERTISING, subscriptions.iterator().next().getSubscriptionType());
        assertEquals(EmailSubscriptionStatus.UNSUBSCRIBED, subscriptions.iterator().next().getSubscriptionStatus());
    }

    @Test
    public void setSubscriptionsAndSetActive() throws Exception {
        createSubscriptionsForce(UID, EMAIL, SUBSCRIBED_SETTINGS);

        createActiveForce(UID, ACTIVE_EMAIL);

        settingsControllerInvoker.checkSubscriptions(UID, EMAIL, SUBSCRIBED_SETTINGS);
        settingsControllerInvoker.checkSubscriptions(UID, ACTIVE_EMAIL, SETTINGS_WITHOUT_EXPLICIT);

        createSubscriptionsForce(UID, ACTIVE_EMAIL, MIXED_NO_ADS_SETTINGS);
        checkSubscriptions(UID, ACTIVE_EMAIL, MIXED_NO_ADS_SETTINGS);
    }

    @Test
    public void setActiveAndSetSubscriptions() throws Exception {
        createActiveForce(UID, ACTIVE_EMAIL);

        createSubscriptionsForce(UID, EMAIL, SUBSCRIBED_SETTINGS);

        checkSubscriptions(UID, ACTIVE_EMAIL, SUBSCRIBED_SETTINGS);
        checkSubscriptions(UID, EMAIL, SUBSCRIBED_SETTINGS);
    }

    @Test
    public void getSubscriptions() throws Exception {
        // реклама не подавляется в личном кабинете
        settingsControllerInvoker.checkSubscriptions(UID, EMAIL, SUBSCRIBED_SETTINGS);
        // реклама подавляется при рассылках
        Set<NotificationType> types = subscriptionAndIdentityService.getSubscribedNotificationTypes(EMAIL);
        assertFalse(types.contains(NotificationType.ADVERTISING));
        assertFalse(types.contains(NotificationType.JOURNAL));
    }

    @Test
    public void getSubscriptions2() throws Exception {
        createSubscriptionsForce(UID, EMAIL, SETTINGS_WITH_ADS_ONLY);

        checkSubscriptions(UID, EMAIL, SETTINGS_WITH_ADS_ONLY);
    }


    @Test
    public void changeSubscriptions() throws Exception {
        createSubscriptionsForce(UID, EMAIL, MIXED_SETTINGS);

        checkSubscriptions(UID, EMAIL, MIXED_SETTINGS);

        List<EmailSubscription> confirmed = subscriptionAndIdentityDAO.getSubscriptions(EMAIL, EmailSubscriptionStatus.CONFIRMED);
        assertEquals(2, confirmed.size());

        List<EmailSubscription> unsubscribed = subscriptionAndIdentityDAO.getSubscriptions(EMAIL, EmailSubscriptionStatus.UNSUBSCRIBED);
        assertEquals(2, unsubscribed.size());


        createSubscriptionsForce(UID, EMAIL, MIXED_NO_ADS_SETTINGS);

        checkSubscriptions(UID, EMAIL, MIXED_NO_ADS_SETTINGS);

        confirmed = subscriptionAndIdentityDAO.getSubscriptions(EMAIL, EmailSubscriptionStatus.CONFIRMED);
        assertEquals(2, confirmed.size());

        unsubscribed = subscriptionAndIdentityDAO.getSubscriptions(EMAIL, EmailSubscriptionStatus.UNSUBSCRIBED);
        assertEquals(4, unsubscribed.size());


        createSubscriptionsForce(UID, EMAIL, SUBSCRIBED_SETTINGS);

        checkSubscriptions(UID, EMAIL, SUBSCRIBED_SETTINGS);

        confirmed = subscriptionAndIdentityDAO.getSubscriptions(EMAIL, EmailSubscriptionStatus.CONFIRMED);
        assertEquals(5, confirmed.size());

        unsubscribed = subscriptionAndIdentityDAO.getSubscriptions(EMAIL, EmailSubscriptionStatus.UNSUBSCRIBED);
        assertEquals(4, unsubscribed.size());


        createSubscriptionsForce(UID, EMAIL, SETTINGS_WITHOUT_ADS);

        checkSubscriptions(UID, EMAIL, SETTINGS_WITHOUT_ADS);
        confirmed = subscriptionAndIdentityDAO.getSubscriptions(EMAIL, EmailSubscriptionStatus.CONFIRMED);
        assertEquals(4, confirmed.size());

        unsubscribed = subscriptionAndIdentityDAO.getSubscriptions(EMAIL, EmailSubscriptionStatus.UNSUBSCRIBED);
        assertEquals(5, unsubscribed.size());
    }

    @Test
    void savePlaceAndPlatform() throws Exception {
        testPlaceAndPlatform(NotificationType.ADVERTISING);
    }

    @Test
    void savePlaceAndPlatformForJournal() throws Exception {
        testPlaceAndPlatform(NotificationType.JOURNAL);
    }

    @Test
    void addNewOwnershipToExportQueue() throws Exception {
        createSubscriptionsForce(UID, EMAIL, SUBSCRIBED_SETTINGS_WITH_PARAMS);

        List<EmailOwnership> ownerships = subscriptionAndIdentityDAO.getEmailsByIdentity(new Uid(UID));
        assertEquals(1, ownerships.size());

        assertTrue(isOwnershipInExportQueue(ownerships.get(0).getId()));
    }

    @Test
    void subscribeOnUnknownEmailMustCreateUnconfirmedSubscriptions() throws Exception {
        createPassportEmails(Collections.singletonList(EMAIL));
        settingsControllerInvoker.createSubscriptions(UID, ANOTHER_EMAIL, SUBSCRIBED_ADV_WISHLIST_SETTINGS);
        List<EmailSubscription> subscriptions = controllerInvoker.getSubscriptions(new Uid(UID));
        assertEquals(2, subscriptions.size());
        assertEquals(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION, subscriptions.get(0).getSubscriptionStatus());
        assertEquals(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION, subscriptions.get(1).getSubscriptionStatus());
    }

    @Test
    void subscribeOnUnknownEmailMustSendConfirmationForAdvOnly() throws Exception {
        createPassportEmails(Collections.singletonList(EMAIL));
        settingsControllerInvoker.createSubscriptions(UID, ANOTHER_EMAIL, SUBSCRIBED_ADV_WISHLIST_SETTINGS);
        assertEquals(1, eventSourceDAO.getEventsCount(ANOTHER_EMAIL, NotificationSubtype.CONFIRM_SUBSCRIPTION,
                NotificationEventStatus.NEW).intValue());
        NotificationEvent event = eventSourceDAO.getLastEventByEmail(ANOTHER_EMAIL);
        assertNotNull(event);
        assertEquals(NotificationSubtype.CONFIRM_SUBSCRIPTION, event.getNotificationSubtype());
        EmailSubscription subscriptionToConfirm = subscriptionAndIdentityDAO.getSubscription(event.getSourceId());
        assertEquals(NotificationType.ADVERTISING, subscriptionToConfirm.getSubscriptionType());
    }

    @Test
    void unsubscribeOnUnknownEmailMustMustBeForbidden() throws Exception {
        blackBoxPassportService.doReturn(ANOTHER_UID, ANOTHER_EMAIL);
        settingsControllerInvoker.createSubscriptions(ANOTHER_UID, ANOTHER_EMAIL, SUBSCRIBED_ADV_WISHLIST_SETTINGS);
        blackBoxPassportService.doReturn(UID, EMAIL);

        settingsControllerInvoker.createSubscriptionsIllegal(UID, ANOTHER_EMAIL, UNSUBSCRIBED_ADV_WISHLIST_SETTINGS);

        settingsControllerInvoker.checkSubscriptions(ANOTHER_UID, ANOTHER_EMAIL, SUBSCRIBED_ADV_WISHLIST_SETTINGS);
    }

    @Test
    void unsubscribeOnUnknownEmailMustNotSendConfirmation() throws Exception {
        createPassportEmails(Collections.singletonList(EMAIL));
        settingsControllerInvoker.createSubscriptionsIllegal(UID, ANOTHER_EMAIL, UNSUBSCRIBED_ADV_WISHLIST_SETTINGS);
        assertNull(eventSourceDAO.getLastEventByEmail(ANOTHER_EMAIL));
    }

    @Test
    void journalMustNotBeAffectedIfNotPassedInSettings() throws Exception {
        createSubscriptionsForce(UID, EMAIL, MIXED_SETTINGS);

        assertEquals(0, subscriptionAndIdentityDAO.getSubscriptions(EMAIL).stream()
                .filter(s -> NotificationType.JOURNAL ==s.getSubscriptionType())
                .count());
    }

    private void testPlaceAndPlatform(NotificationType notificationType) throws Exception {
        createSubscriptionsForce(UID, EMAIL, SUBSCRIBED_SETTINGS_WITH_PARAMS);

        List<EmailSubscription> subscriptions = subscriptionAndIdentityService.getEmailSubscriptions(EMAIL,
                notificationType);
        assertEquals(1, subscriptions.size());
        EmailSubscription subscription = subscriptions.get(0);
        assertEquals(PLATFORM, subscription.getParameters().get(EmailSubscriptionParam.PARAM_PLATFORM));
        assertEquals(PLACE, subscription.getParameters().get(EmailSubscriptionParam.PARAM_PLACE));
    }

    private boolean isOwnershipInExportQueue(long ownershipId) {
        Boolean exists = jdbcTemplate.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM CRM_CHANGED_EMAIL_OWNERSHIP\n" +
                "WHERE EMAIL_OWNERSHIP_ID = ?)",
            Boolean.class,
            ownershipId
        );
        // To make idea code analyzer happy
        return Boolean.TRUE.equals(exists);
    }

    private void checkEmails(List<Email> result) throws Exception {
        mockMvc.perform(get("/settings/UID/" + UID + "/emails")
            .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(content().json(toJson(result)));
//         проверяем что в ходе работы с пользователем, другим пользователям ничего не добавляется
        blackBoxPassportService.returnEmptyForUid(ANOTHER_UID);
        mockMvc.perform(get("/settings/UID/" + ANOTHER_UID + "/emails")
            .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(content().json(toJson(Collections.emptyList())));
    }

    private void createActiveForce(Long uid, String email) throws Exception {
        // для того, чтобы можно было создать активный email, он должен изначально попасть в возможные email-ы
        List<String> saved = passportService.getEmails(uid);
        blackBoxPassportService.doReturn(uid, Collections.singletonList(email));

        settingsControllerInvoker.createActive(uid, email);

        // возвращаем обратно, что было
        blackBoxPassportService.doReturn(uid, saved);
    }

    private void checkSubscriptions(Long uid, String email, String settingsFileName)
            throws Exception {
        settingsControllerInvoker.checkSubscriptions(uid, email, settingsFileName);
    }

    private void createSubscriptionsForce(Long uid, String email, String jsonFileName) throws Exception {
        // для того, чтобы можно было создать подписки по email, он должен изначально попасть в возможные email-ы
        List<String> saved = passportService.getEmails(uid);
        blackBoxPassportService.doReturn(uid, Collections.singletonList(email));

        settingsControllerInvoker.createSubscriptions(uid, email, jsonFileName);

        // возвращаем обратно, что было
        blackBoxPassportService.doReturn(uid, saved);
    }

    private void createPassportEmails(List<String> emails) {
        blackBoxPassportService.doReturn(UID, emails);
    }

}
