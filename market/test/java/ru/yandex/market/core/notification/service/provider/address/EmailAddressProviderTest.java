package ru.yandex.market.core.notification.service.provider.address;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.notification.dao.NotificationSubscriptionDao;
import ru.yandex.market.core.notification.dao.NotificationTemplateDao;
import ru.yandex.market.core.notification.service.NotificationThemeService;
import ru.yandex.market.core.notification.service.resolver.CampaignsResolver;
import ru.yandex.market.core.notification.service.resolver.EmailAddressByAliasResolver;
import ru.yandex.market.core.notification.service.resolver.MailingListWatchersResolver;
import ru.yandex.market.core.notification.service.subscription.NotificationSubscriptionServiceImpl;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.notification.common.model.destination.MbiDestination;
import ru.yandex.market.notification.mail.model.address.ComposedEmailAddress;
import ru.yandex.market.notification.mail.model.address.EmailAddress;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.service.provider.context.NotificationAddressProviderContextImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link EmailAddressProvider}.
 *
 * @author avetokhin 27/07/16.
 */
class EmailAddressProviderTest {

    private static final Long NOTIFICATION_TYPE = 1L;
    private static final Long SHOP_ID = 13L;
    private static final Long USER_ID = 20L;

    private static final List<Long> CAMPAIGNS = Arrays.asList(55L, 56L);

    private static final EmailAddress ADDRESS_FROM = EmailAddress.create("from@yandex.ru", EmailAddress.Type.FROM);
    private static final EmailAddress ADDRESS_TO_1 = EmailAddress.create("to1@yandex.ru", EmailAddress.Type.TO);
    private static final EmailAddress ADDRESS_TO_2 = EmailAddress.create("to2@yandex.ru", EmailAddress.Type.TO);
    private static final EmailAddress ADDRESS_TO_3 = EmailAddress.create("to3@yandex.ru", EmailAddress.Type.TO);
    private static final EmailAddress ADDRESS_CC_1 = EmailAddress.create("cc1@yandex.ru", EmailAddress.Type.CC);
    private static final EmailAddress ADDRESS_BCC_1 = EmailAddress.create("bcc1@yandex.ru", EmailAddress.Type.BCC);
    private static final EmailAddress ADDRESS_BCC_2 = EmailAddress.create("bcc2@yandex.ru", EmailAddress.Type.BCC);

    @Test
    void testProvide() {
        // Создать мок для резолвера кампаний.
        var campaignsResolver = mock(CampaignsResolver.class);
        when(campaignsResolver.resolve(SHOP_ID, USER_ID))
                .thenReturn(CAMPAIGNS);

        // Создать мок для резолвера по алиасам.
        var aliasResolver = mock(EmailAddressByAliasResolver.class);
        when(aliasResolver.resolve(eq(NOTIFICATION_TYPE), any()))
                .thenReturn(Set.of(ADDRESS_TO_1, ADDRESS_TO_2));

        // Создать мок для резолвера шпионов.
        var watchersResolver = mock(MailingListWatchersResolver.class);
        when(watchersResolver.resolve(CAMPAIGNS))
                .thenReturn(Set.of(ADDRESS_BCC_1, ADDRESS_BCC_2));

        // Создать мок для резолвера отписок.
        var notificationSubscriptionDao = mock(NotificationSubscriptionDao.class);
        var notificationSubscriptionService = new NotificationSubscriptionServiceImpl(
                mock(ContactService.class),
                notificationSubscriptionDao,
                mock(ProtocolService.class),
                mock(HistoryService.class),
                mock(NotificationThemeService.class),
                mock(CampaignService.class),
                mock(NotificationTemplateDao.class)
        );
        when(notificationSubscriptionDao.getUnsubscribedEmails(
                eq(NOTIFICATION_TYPE),
                anyCollection(),
                anyCollection())
        ).thenReturn(Set.of(ADDRESS_TO_2.getEmail()));

        // Подготовить явно указанные адреса.
        var explicitRecipients = ComposedEmailAddress.create(
                Set.of(ADDRESS_FROM.getEmail()),
                Set.of(ADDRESS_TO_3.getEmail()),
                Set.of(ADDRESS_CC_1.getEmail()),
                Set.of(),
                Set.of()
        );

        // Подготовить пунк назначения.
        var destination = MbiDestination.create(SHOP_ID, USER_ID, explicitRecipients);

        // Подготовить контекст.
        var context = new NotificationAddressProviderContextImpl(
                new CodeNotificationType(NOTIFICATION_TYPE),
                destination
        );

        // Создать провайер.
        var provider = new EmailAddressProvider(campaignsResolver, aliasResolver,
                watchersResolver, notificationSubscriptionService);

        // Ожидаемый результат.

        var addresses = provider.provide(context);
        assertThat(addresses).containsExactlyInAnyOrder(
                ADDRESS_FROM,
                ADDRESS_TO_1,
                ADDRESS_TO_3,
                ADDRESS_CC_1,
                ADDRESS_BCC_1,
                ADDRESS_BCC_2
        );
    }
}
