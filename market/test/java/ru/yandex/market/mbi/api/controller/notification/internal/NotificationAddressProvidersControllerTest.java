package ru.yandex.market.mbi.api.controller.notification.internal;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.AliasToUidsDTO;
import ru.yandex.market.mbi.open.api.client.model.DestinationWithAliasesDTO;
import ru.yandex.market.mbi.open.api.client.model.EmailAddressDTO;
import ru.yandex.market.mbi.open.api.client.model.EmailAddressTypeDTO;
import ru.yandex.market.mbi.open.api.client.model.NotificationEmailAliasDTO;
import ru.yandex.market.mbi.open.api.client.model.ProvideEmailAddressesByUidsRequest;
import ru.yandex.market.mbi.open.api.client.model.ProvideEmailAddressesByUidsResponse;
import ru.yandex.market.mbi.open.api.client.model.ProvideEmailAddressesRequest;
import ru.yandex.market.mbi.open.api.client.model.ProvideEmailAddressesResponse;
import ru.yandex.market.mbi.open.api.client.model.ProvidePushAddressesRequest;
import ru.yandex.market.mbi.open.api.client.model.ProvidePushAddressesResponse;
import ru.yandex.market.mbi.open.api.client.model.ProvideTelegramAddressesByUidsRequest;
import ru.yandex.market.mbi.open.api.client.model.ProvideTelegramAddressesByUidsResponse;
import ru.yandex.market.mbi.open.api.client.model.ProvideTelegramAddressesRequest;
import ru.yandex.market.mbi.open.api.client.model.ProvideTelegramAddressesResponse;
import ru.yandex.market.mbi.open.api.client.model.ProvideUidsByAliasesRequest;
import ru.yandex.market.mbi.open.api.client.model.ProvideUidsByAliasesResponse;
import ru.yandex.market.mbi.open.api.client.model.PushAddressDTO;
import ru.yandex.market.mbi.open.api.client.model.TelegramAddressDTO;
import ru.yandex.market.mbi.open.api.client.model.UidToEmailAddressesDTO;
import ru.yandex.market.mbi.open.api.client.model.UidToTelegramAddressesDTO;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@DbUnitDataSet(before = "NotificationAddressProvidersControllerTest.before.csv")
class NotificationAddressProvidersControllerTest extends FunctionalTest {

    @Test
    void provideEmailAddresses() {
        var request = new ProvideEmailAddressesRequest()
                .notificationType(1612872409L)
                .destinationWithAliases(new DestinationWithAliasesDTO()
                        .shopId(50L)
                        .aliases(List.of(new NotificationEmailAliasDTO()
                                .notificationType(1612872409L)
                                .emailType(EmailAddressTypeDTO.CC)
                                .alias("ShopAdmins")
                        ))
                );

        var response = getMbiOpenApiClient().provideEmailAddresses(request);

        var expectedResponse = new ProvideEmailAddressesResponse()
                .addresses(List.of(new EmailAddressDTO()
                        .email("admin4egPassive@yandex.ru")
                        .type(EmailAddressTypeDTO.CC)
                ));

        assertThat(response, equalTo(expectedResponse));
    }

    @Test
    void providePushAddresses() {
        var request = new ProvidePushAddressesRequest()
                .notificationType(1612872409L)
                .destinationWithAliases(new DestinationWithAliasesDTO()
                        .shopId(50L)
                        .aliases(List.of(new NotificationEmailAliasDTO()
                                .notificationType(1612872409L)
                                .emailType(EmailAddressTypeDTO.CC)
                                .alias("ShopAdmins")
                        ))
                );

        var response = getMbiOpenApiClient().providePushAddresses(request);

        var expectedResponse = new ProvidePushAddressesResponse()
                .addresses(List.of(new PushAddressDTO()
                        .uid(100503L)
                ));

        assertThat(response, equalTo(expectedResponse));
    }

    @Test
    void provideTelegramAddresses() {
        var request = new ProvideTelegramAddressesRequest()
                .notificationType(1612872409L)
                .destinationWithAliases(new DestinationWithAliasesDTO()
                        .shopId(50L)
                        .aliases(List.of(new NotificationEmailAliasDTO()
                                .notificationType(1612872409L)
                                .emailType(EmailAddressTypeDTO.CC)
                                .alias("ShopAdmins")
                        ))
                );

        var response = getMbiOpenApiClient().provideTelegramAddresses(request);

        var expectedResponse = new ProvideTelegramAddressesResponse()
                .addresses(List.of(new TelegramAddressDTO()
                        .botId("bot120")
                        .telegramId(120L)
                ));

        assertThat(response, equalTo(expectedResponse));
    }

    @Test
    void provideUidsByAliases() {
        var request = new ProvideUidsByAliasesRequest()
                .partnerId(50L)
                .aliases(List.of("ShopAdmins", "SupplierAll", "YaManager"));

        var response = getMbiOpenApiClient().provideUidsByAliases(request);

        var expectedResponse = new ProvideUidsByAliasesResponse()
                .aliasesToUids(List.of(
                        new AliasToUidsDTO()
                                .alias("ShopAdmins")
                                .uids(List.of(100503L)),
                        new AliasToUidsDTO()
                                .alias("SupplierAll")
                                .uids(List.of(100503L)),
                        new AliasToUidsDTO()
                                .alias("YaManager")
                                .uids(List.of(-2L))
                ));

        assertThat(response, equalTo(expectedResponse));
    }

    @Test
    void provideTelegramAddressesByUids() {
        var request = new ProvideTelegramAddressesByUidsRequest().uids(List.of(100500L, 100501L, 100502L));

        var response = getMbiOpenApiClient().provideTelegramAddressesByUids(request);

        var expectedResponse = new ProvideTelegramAddressesByUidsResponse()
                .uidToTelegramAddressesList(List.of(
                        new UidToTelegramAddressesDTO()
                                .uid(100500L)
                                .addresses(List.of(
                                        new TelegramAddressDTO()
                                                .telegramId(100L)
                                                .botId("bot100")
                                )),
                        new UidToTelegramAddressesDTO()
                                .uid(100501L)
                                .addresses(List.of(
                                        new TelegramAddressDTO()
                                                .telegramId(100L)
                                                .botId("bot100")
                                )),
                        new UidToTelegramAddressesDTO()
                                .uid(100502L)
                                .addresses(List.of(
                                        new TelegramAddressDTO()
                                                .telegramId(110L)
                                                .botId("bot110")
                                ))
                ));

        assertThat(response, equalTo(expectedResponse));
    }

    @Test
    void provideEmailAddressesByUids() {
        var request = new ProvideEmailAddressesByUidsRequest().uids(List.of(100500L, 100501L, 100502L));

        var response = getMbiOpenApiClient().provideEmailAddressesByUids(request);

        var expectedResponse = new ProvideEmailAddressesByUidsResponse()
                .uidToEmailAddressesList(List.of(
                        new UidToEmailAddressesDTO()
                                .uid(100500L)
                                .addresses(List.of("admin4eg@yandex.ru")),
                        new UidToEmailAddressesDTO()
                                .uid(100501L)
                                .addresses(List.of()),
                        new UidToEmailAddressesDTO()
                                .uid(100502L)
                                .addresses(List.of("admin4egPassive@yandex.ru"))
                ));

        assertThat(response, equalTo(expectedResponse));
    }
}
