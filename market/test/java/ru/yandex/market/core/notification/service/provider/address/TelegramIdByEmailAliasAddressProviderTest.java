package ru.yandex.market.core.notification.service.provider.address;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.notification.dao.NotificationSubscriptionDao;
import ru.yandex.market.core.notification.dao.NotificationTemplateDao;
import ru.yandex.market.core.notification.service.NotificationThemeService;
import ru.yandex.market.core.notification.service.resolver.CampaignsResolver;
import ru.yandex.market.core.notification.service.resolver.UidByAliasResolver;
import ru.yandex.market.core.notification.service.subscription.NotificationSubscriptionServiceImpl;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.telegram.model.TelegramAccount;
import ru.yandex.market.core.telegram.service.TelegramAccountService;
import ru.yandex.market.notification.common.model.destination.MbiDestination;
import ru.yandex.market.notification.mail.model.address.ComposedEmailAddress;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.service.provider.context.NotificationAddressProviderContextImpl;
import ru.yandex.market.notification.telegram.bot.model.address.TelegramIdAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelegramIdByEmailAliasAddressProviderTest {
    private static final Long NOTIFICATION_TYPE = 100500L;
    private static final Long SHOP_ID = 13L;
    private static final Long UID_1 = 35L;
    private static final Long UID_2 = 36L;
    private static final Long UID_3 = 37L;
    private static final Long UID_UNSUBSCRIBED = 38L;
    private static final Long TELEGRAM_ID_1 = 55L;
    private static final Long TELEGRAM_ID_2 = 56L;
    private static final Long TELEGRAM_ID_3 = 57L;
    private static final Long TELEGRAM_ID_UNSUBSCRIBED = 58L;
    private static final String BOT_ID = "IAmTgRobot";
    private static final TelegramIdAddress ADDERESS_1 = TelegramIdAddress.create(BOT_ID, TELEGRAM_ID_1);
    private static final TelegramIdAddress ADDERESS_2 = TelegramIdAddress.create(BOT_ID, TELEGRAM_ID_2);
    private static final TelegramIdAddress ADDERESS_3 = TelegramIdAddress.create(BOT_ID, TELEGRAM_ID_3);
    private static final Set<Long> SHOP_USERS = new HashSet<>(Arrays.asList(UID_1, UID_2));
    private static final Set<Long> INDIVIDUAL_USER = Collections.singleton(UID_3);
    private static final Set<Long> ALL_USERS = new HashSet<>(Arrays.asList(UID_1, UID_2, UID_3, UID_UNSUBSCRIBED));

    private static final String ADDRESS_FROM = "from@yandex.ru";
    private static final String ADDRESS_TO = "to@yandex.ru";
    private static final String ADDRESS_CC = "cc@yandex.ru";


    private final TelegramIdByEmailAliasAddressProvider provider;
    private final UidByAliasResolver usersUidByAliasResolver;

    TelegramIdByEmailAliasAddressProviderTest() {
        this.usersUidByAliasResolver = mock(UidByAliasResolver.class);

        var telegramAccountService = mock(TelegramAccountService.class);
        var accounts = Set.of(
                createTelegramAccount(UID_1, TELEGRAM_ID_1, BOT_ID),
                createTelegramAccount(UID_2, TELEGRAM_ID_2, BOT_ID),
                createTelegramAccount(UID_3, TELEGRAM_ID_3, BOT_ID),
                createTelegramAccount(UID_UNSUBSCRIBED, TELEGRAM_ID_UNSUBSCRIBED, BOT_ID)
        );
        when(telegramAccountService.findAccounts(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> uids = invocation.getArgument(0);
            return accounts.stream()
                    .filter(a -> uids.contains(a.getUserId()))
                    .collect(Collectors.toUnmodifiableSet());
        });


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
        when(notificationSubscriptionDao.getUnsubscribedTelegramIds(
                eq(NOTIFICATION_TYPE),
                anyCollection()
        )).thenReturn(Set.of(TELEGRAM_ID_UNSUBSCRIBED));

        var campaignsResolver = mock(CampaignsResolver.class);
        this.provider = new TelegramIdByEmailAliasAddressProvider(
                campaignsResolver,
                usersUidByAliasResolver,
                telegramAccountService,
                notificationSubscriptionService
        );
    }

    @Test
    void testProvideUsersFromShopId() {
        when(usersUidByAliasResolver.resolve(
                eq(NOTIFICATION_TYPE),
                ArgumentMatchers.any()
        )).thenReturn(SHOP_USERS);

        var destination = MbiDestination.create(SHOP_ID, null, null);
        var context = new NotificationAddressProviderContextImpl(
                new CodeNotificationType(NOTIFICATION_TYPE),
                destination
        );

        var actualAddresses = provider.provide(context);
        assertThat(actualAddresses).containsExactlyInAnyOrder(ADDERESS_1, ADDERESS_2);
    }

    @Test
    void testProvideUsersFromUserId() {
        when(usersUidByAliasResolver.resolve(
                eq(NOTIFICATION_TYPE),
                ArgumentMatchers.any()
        )).thenReturn(INDIVIDUAL_USER);

        var destination = MbiDestination.create(null, TELEGRAM_ID_3, null);
        var context = new NotificationAddressProviderContextImpl(
                new CodeNotificationType(NOTIFICATION_TYPE),
                destination
        );

        var actualAddresses = provider.provide(context);
        assertThat(actualAddresses).containsExactlyInAnyOrder(ADDERESS_3);
    }

    @Test
    void testProvideUsersFromUserIdAndShopId() {
        when(usersUidByAliasResolver.resolve(
                eq(NOTIFICATION_TYPE),
                ArgumentMatchers.any()
        )).thenReturn(ALL_USERS);

        var destination = MbiDestination.create(SHOP_ID, TELEGRAM_ID_3, null);
        var context = new NotificationAddressProviderContextImpl(
                new CodeNotificationType(NOTIFICATION_TYPE),
                destination
        );

        var actualAddresses = provider.provide(context);
        assertThat(actualAddresses).containsExactlyInAnyOrder(ADDERESS_1, ADDERESS_2, ADDERESS_3);
    }

    @Test
    void testProvideEmptyListOnExplicitEmailRecipients() {
        when(usersUidByAliasResolver.resolve(
                eq(NOTIFICATION_TYPE),
                ArgumentMatchers.any()
        )).thenReturn(Collections.emptySet());

        var explicitRecipients = ComposedEmailAddress.create(
                Collections.singleton(ADDRESS_FROM),
                Collections.singleton(ADDRESS_TO),
                Collections.singleton(ADDRESS_CC),
                Collections.emptySet(),
                Collections.emptySet()
        );

        var destination = MbiDestination.create(null, null, explicitRecipients);
        var context = new NotificationAddressProviderContextImpl(
                new CodeNotificationType(NOTIFICATION_TYPE),
                destination
        );

        var actualAddresses = provider.provide(context);
        assertThat(actualAddresses).isEmpty();
    }

    @Test
    void testProvideUsersFromShopIdWithNullAddresses() {
        when(usersUidByAliasResolver.resolve(
                eq(NOTIFICATION_TYPE),
                ArgumentMatchers.any()
        )).thenReturn(new HashSet<>(Arrays.asList(UID_1, UID_2, null)));

        var destination = MbiDestination.create(SHOP_ID, null, null);
        var context = new NotificationAddressProviderContextImpl(
                new CodeNotificationType(NOTIFICATION_TYPE),
                destination
        );

        var actualAddresses = provider.provide(context);
        assertThat(actualAddresses).containsExactlyInAnyOrder(ADDERESS_1, ADDERESS_2);
    }

    private static TelegramAccount createTelegramAccount(Long uid, Long telegramId, String botId) {
        var telegramAccount1 = new TelegramAccount();
        telegramAccount1.setUserId(uid);
        telegramAccount1.setTgId(telegramId);
        telegramAccount1.setBotId(botId);
        return telegramAccount1;
    }

}
