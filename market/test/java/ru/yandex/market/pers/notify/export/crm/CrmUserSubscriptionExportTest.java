package ru.yandex.market.pers.notify.export.crm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.export.TestCrmUserSubscriptionItemWriter;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CrmUserSubscriptionExportTest extends MarketMailerMockedDbTest {

    @Autowired
    private CrmEmailSubscriptionExportService exportService;
    @Autowired
    private TestCrmUserSubscriptionItemWriter exportedItems;
    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;

    @BeforeEach
    public void _setUp() {
        exportedItems.clear();
    }

    @Test
    public void emailSubscriptionIsUpdated_export_waitUpdatedSubscriptionIsExported() {
        String email = "test@example.com";
        Uid identity = new Uid(123l);
        saveSubscription(getSampleEmailSubscription(x -> {
                x.setParameters(new HashMap<>());
                x.setEmail(email);
            }),
            identity);
        exportService.exportChanged();
        assertEquals(1, exportedItems.size());
        exportedItems.clear();

        List<EmailSubscription> subscriptions = getSavedSubscriptionByIdentity(identity);
        EmailSubscription savedSubscription = subscriptions.get(0);

        savedSubscription.setSubscriptionStatus(EmailSubscriptionStatus.UNSUBSCRIBED);

        subscriptionAndIdentityService.updateSubscription(savedSubscription);

        exportService.exportChanged();
        assertEquals(1, exportedItems.size());
        CrmEmailSubscriptionItem exported = exportedItems.getItem(0);
        assertEquals(EmailSubscriptionStatus.UNSUBSCRIBED.getId(), exported.getSubscriptionStatus());
    }

    @Test
    public void emailSubscriptionIsUpdatedForNotAuthorizedUser_export_waitUpdatedSubscriptionIsExported() {
        String email = "test@example.com";
        Uuid identity = new Uuid("012345678901234567890123456789012");
        saveSubscription(getSampleEmailSubscription(x -> {
                x.setParameters(new HashMap<>());
                x.setEmail(email);
            }),
            identity);
        exportService.exportChanged();
        assertEquals(1, exportedItems.size());
    }

    @Test
    public void exportAll_and_exportChanged_are_independent() {
        String email = "test@example.com";
        long uid = 123l;

        saveSubscription(getSampleEmailSubscription(x -> {
                x.setSubscriptionType(NotificationType.ADVERTISING);
                x.setParameters(parameters());

                x.setEmail(email);
            }),
            new Uid(uid));

        exportService.exportChanged();
        assertEquals(1, exportedItems.size());

        exportedItems.clear();
        exportService.exportChanged();
        assertEquals(0, exportedItems.size());

        exportedItems.clear();
        exportService.exportAll();
        assertEquals(1, exportedItems.size());

        exportedItems.clear();
        exportService.exportChanged();
        assertEquals(0, exportedItems.size());
    }

    @Test
    public void exportChangedSubscriptionWithoutParameters() {
        String email = "test@example.com";
        long uid = 123l;

        saveSubscription(getSampleEmailSubscription(x -> {
                x.setSubscriptionType(NotificationType.ADVERTISING);
                x.setParameters(parameters());

                x.setEmail(email);
            }),
            new Uid(uid));

        exportService.exportChanged();

        assertEquals(1, exportedItems.size());
        CrmEmailSubscriptionItem item = exportedItems.getItem(0);

        assertTrue(item.getSubscriptionParameters().isEmpty());
    }

    @Test
    public void exportChangedSubscription_waitExportedFields() {
        Long uid = 123l;
        String email = "test@example.com";

        saveSubscription(getSampleEmailSubscription(x -> {
                x.setSubscriptionType(NotificationType.ADVERTISING);
                x.setParameters(parameters(
                    param("regionId", "213"),
                    param("userName", "SomeUser")));
                x.setEmail(email);
            }),
            new Uid(uid));

        exportService.exportChanged();

        assertEquals(1, exportedItems.size());
        CrmEmailSubscriptionItem item = exportedItems.getItem(0);

        assertEquals(email, item.getEmail());
        assertEquals(uid, item.getUid());
        assertNull(item.getUuid());
        assertNull(item.getYandexUid());
        assertEquals(NotificationType.ADVERTISING.getId(), item.getSubscriptionType());
        assertEquals(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION.getId(), item.getSubscriptionStatus());

        assertCurrentTime(item.getModificationDate().toLocalDateTime());

        List<CrmEmailSubscriptionExportService.Parameter> subscriptionParameters = item.getSubscriptionParameters();
        assertEquals(2, subscriptionParameters.size());
        CrmEmailSubscriptionExportService.Parameter regionId = getParameterOrNull(subscriptionParameters, "regionId");
        assertEquals("213", regionId.getValue());

        CrmEmailSubscriptionExportService.Parameter userName = getParameterOrNull(subscriptionParameters, "userName");
        assertEquals("SomeUser", userName.getValue());
    }

    @Test
    public void initialExportNoSubscriptions_waitEmptyExport() {
        exportService.exportChanged();
        assertEquals(0, exportedItems.size());

        exportedItems.clear();

        exportService.exportAll();
        assertEquals(0, exportedItems.size());
    }

    @Test
    public void newEmailSubscriptionCreated_exportChanged_waitNewSubscriptionIsExported() {
        saveSubscription(getSampleEmailSubscription(x -> {
                x.setParameters(new HashMap<>());
                x.setParameters(parameters(
                    param("modelId", "m1"),
                    param("regionId", "r1"),
                    param("userName", "User1")));
                x.setEmail("test1@example.com");
            }),
            new Uid(123L));
        saveSubscription(getSampleEmailSubscription(x -> {
                x.setParameters(parameters(
                    param("modelId", "m2"),
                    param("regionId", "r2"),
                    param("userName", "User2")));
                x.setEmail("test2@example.com");
            }),
            new Uid(124L));
        exportService.exportChanged();
        assertEquals(2, exportedItems.size());

        exportedItems.clear();
        saveSubscription(getSampleEmailSubscription(x -> {
                x.setParameters(parameters(
                    param("modelId", "m3"),
                    param("regionId", "r3"),
                    param("userName", "User3")));
                x.setEmail("test3@example.com");
            }),
            new Uid(125L));
        exportService.exportChanged();
        assertEquals(1, exportedItems.size());
    }

    private void assertCurrentTime(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();
        assertTrue(time.until(now, ChronoUnit.MINUTES) < 2);
    }

    private Date getNow() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private CrmEmailSubscriptionExportService.Parameter getParameterOrNull(
        List<CrmEmailSubscriptionExportService.Parameter> subscriptionParameters,
        String paramName) {
        return subscriptionParameters.stream()
            .filter(x -> x.getName().equalsIgnoreCase(paramName))
            .findFirst()
            .orElse(null);
    }

    private EmailSubscription getSampleEmailSubscription(Consumer<EmailSubscription> modificator) {
        EmailSubscription subscription = new EmailSubscription();
        subscription.setEmail("test@example.com");
        subscription.setSubscriptionType(NotificationType.ADVERTISING);
        subscription.setSubscriptionStatus(EmailSubscriptionStatus.CONFIRMED);
        subscription.setCreationDate(getNow());
        if (null != modificator) {
            modificator.accept(subscription);
        }
        return subscription;
    }

    private List<EmailSubscription> getSavedSubscriptionByIdentity(Identity identity) {
        return subscriptionAndIdentityService.getEmailSubscriptions(identity, 1, 10, NotificationType.ADVERTISING);
    }

    private Map.Entry<String, String> param(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    private Map<String, String> parameters(Map.Entry<String, String>... entries) {
        Map<String, String> m = new HashMap<>();
        for (Map.Entry<String, String> e : entries) {
            m.put(e.getKey(), e.getValue());
        }
        return m;
    }

    private void saveSubscription(EmailSubscription subscription, Identity identity) {
        String email = subscription.getEmail();
        subscriptionAndIdentityService.createSubscriptions(email, subscriptions(subscription), identity);
    }

    private List<EmailSubscription> subscriptions(EmailSubscription... subscriptions) {
        return Arrays.stream(subscriptions).collect(Collectors.toList());
    }
}
