package ru.yandex.market.core.notification.service.provider.address;

import java.util.Collection;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.notification.exception.AliasResolvingException;
import ru.yandex.market.core.notification.service.resolver.CampaignsResolver;
import ru.yandex.market.core.notification.service.resolver.UidByAliasResolver;
import ru.yandex.market.core.notification.service.subscription.NotificationSubscriptionService;
import ru.yandex.market.core.telegram.service.TelegramAccountService;
import ru.yandex.market.notification.common.model.destination.MbiDestination;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.telegram.bot.model.address.TelegramIdAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DbUnitDataSet(before = "TelegramIdByEmailAliasAddressProviderFunctionalTest.before.csv")
class TelegramIdByEmailAliasAddressProviderFunctionalTest extends FunctionalTest {
    private static final long NOTIFICATION_TYPE = 1612872409L;

    @Autowired
    private CampaignsResolver campaignsResolver;
    @Autowired
    private UidByAliasResolver uidByAliasResolver;
    @Autowired
    private TelegramAccountService telegramAccountService;
    @Autowired
    private NotificationSubscriptionService notificationSubscriptionService;

    private TelegramIdByEmailAliasAddressProvider telegramIdByEmailAliasAddressProvider;

    @BeforeEach
    void init() {
        telegramIdByEmailAliasAddressProvider = new TelegramIdByEmailAliasAddressProvider(
                campaignsResolver,
                uidByAliasResolver,
                telegramAccountService,
                notificationSubscriptionService
        );
    }

    @Test
    @DisplayName("????????????????????, ???????? ???????????? ???????????? ???? ??????????")
    void testEmptyData() {
        assertThatExceptionOfType(AliasResolvingException.class)
                .isThrownBy(() -> telegramIdByEmailAliasAddressProvider.getAddresses(
                        new MbiDestination(),
                        new CodeNotificationType(NOTIFICATION_TYPE)
                ));
    }

    @ParameterizedTest
    @DisplayName("???????????? id ?????????????????? ???? ????????????????")
    @MethodSource("testAliasProviderData")
    void testAliasProvider(String name, Long shopId, Long expectedTgId) {
        var mbiDestination = new MbiDestination();
        mbiDestination.setShopId(shopId);
        var adressesWithShopDestination = telegramIdByEmailAliasAddressProvider.getAddresses(
                mbiDestination,
                new CodeNotificationType(NOTIFICATION_TYPE)
        );
        checkTelegramId(adressesWithShopDestination, expectedTgId);
    }

    private static Stream<Arguments> testAliasProviderData() {
        return Stream.of(
                Arguments.of(
                        "???????????????? ???????????????? ???? ???????????? ???? ??????????????????",
                        10L,
                        100L
                ),
                Arguments.of(
                        "?????? ???????????????? ??????????????????",
                        20L,
                        null
                ),
                Arguments.of(
                        "???????? ???????? ???? ???????????????? 30",
                        30L,
                        110L
                ),
                Arguments.of(
                        "???????? ???????? ???? ???????????????? 50",
                        50L,
                        120L
                ),
                Arguments.of(
                        "???????? ???????????????? ???? ???????? ????????????",
                        80L,
                        130L
                )
        );
    }

    private static void checkTelegramId(Collection<NotificationAddress> addresses, @Nullable Long telegramId) {
        if (telegramId == null) {
            assertThat(addresses).isEmpty();
        } else {
            assertThat(addresses)
                    .singleElement()
                    .extracting(a -> a.cast(TelegramIdAddress.class).getTelegramId())
                    .isEqualTo(telegramId);
        }
    }
}
