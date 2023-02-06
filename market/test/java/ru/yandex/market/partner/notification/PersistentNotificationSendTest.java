package ru.yandex.market.partner.notification;


import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.AliasToUidsDTO;
import ru.yandex.market.mbi.open.api.client.model.DestinationWithAliasesDTO;
import ru.yandex.market.mbi.open.api.client.model.EmailAddressDTO;
import ru.yandex.market.mbi.open.api.client.model.EmailAddressTypeDTO;
import ru.yandex.market.mbi.open.api.client.model.GetBusinessIdsForPartnersResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerIdToBusinessIdDTO;
import ru.yandex.market.mbi.open.api.client.model.ProvideEmailAddressesByUidsRequest;
import ru.yandex.market.mbi.open.api.client.model.ProvideEmailAddressesByUidsResponse;
import ru.yandex.market.mbi.open.api.client.model.ProvideEmailAddressesResponse;
import ru.yandex.market.mbi.open.api.client.model.ProvidePushAddressesRequest;
import ru.yandex.market.mbi.open.api.client.model.ProvidePushAddressesResponse;
import ru.yandex.market.mbi.open.api.client.model.ProvideTelegramAddressesByUidsRequest;
import ru.yandex.market.mbi.open.api.client.model.ProvideTelegramAddressesByUidsResponse;
import ru.yandex.market.mbi.open.api.client.model.ProvideTelegramAddressesResponse;
import ru.yandex.market.mbi.open.api.client.model.ProvideUidsByAliasesResponse;
import ru.yandex.market.mbi.open.api.client.model.PushAddressDTO;
import ru.yandex.market.mbi.open.api.client.model.TelegramAddressDTO;
import ru.yandex.market.mbi.open.api.client.model.UidToEmailAddressesDTO;
import ru.yandex.market.mbi.open.api.client.model.UidToTelegramAddressesDTO;
import ru.yandex.market.notification.mail.model.address.EmailAddress;
import ru.yandex.market.notification.mail.model.result.EmailSuccess;
import ru.yandex.market.notification.simple.model.result.NotificationResultImpl;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.notification.telegram.bot.model.address.TelegramIdAddress;
import ru.yandex.market.notification.telegram.bot.model.status.TelegramBotSuccess;
import ru.yandex.market.partner.notification.environment.EnvironmentService;
import ru.yandex.market.partner.notification.task.PreparePersistentNotificationTask;
import ru.yandex.market.partner.notification.task.SendPersistentNotificationTask;
import ru.yandex.market.partner.notification.user_sending_settings.model.UserBusinessSendingSettings;
import ru.yandex.market.partner.notification.user_sending_settings.model.UserPartnerSendingSettings;
import ru.yandex.market.partner.notification.user_sending_settings.model.UserThemeSettings;
import ru.yandex.market.partner.notification.user_sending_settings.model.UserTransportSettings;
import ru.yandex.market.partner.notification.user_sending_settings.service.UserBusinessSendingSettingsService;
import ru.yandex.market.partner.notification.user_sending_settings.service.UserPartnerSendingSettingsService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"
        ))
public class PersistentNotificationSendTest extends AbstractFunctionalTest {

    @Autowired
    PreparePersistentNotificationTask preparePersistentNotificationTask;

    @Autowired
    SendPersistentNotificationTask sendPersistentNotificationTask;

    @Autowired
    MbiOpenApiClient mbiOpenApiClient;

    @Autowired
    EnvironmentService environmentService;

    @Autowired
    TestableClock clock;

    @Autowired
    UserPartnerSendingSettingsService partnerSendingSettingsService;

    @Autowired
    UserBusinessSendingSettingsService businessSendingSettingsService;

    @Test
    @DbUnitDataSet(
            before = "shouldPrepareValidMessage.email.before.csv",
            after = "shouldPrepareValidMessage.email.after.csv"
    )
    public void shouldPrepareValidEmailMessage() throws Exception {
        when(mbiOpenApiClient.provideEmailAddresses(any()))
                .thenReturn(new ProvideEmailAddressesResponse()
                        .addresses(List.of(
                                new EmailAddressDTO()
                                        .email("noreply-seller@market.yandex.ru")
                                        .type(EmailAddressTypeDTO.FROM),
                                new EmailAddressDTO()
                                        .email("test@yandex-team.ru")
                                        .type(EmailAddressTypeDTO.TO)
                        )));

        preparePersistentNotificationTask.execute();
    }


    @Test
    @DbUnitDataSet(
            before = "shouldPrepareValidMessage.email.before.csv",
            after = "shouldPrepareValidMessage.email.after.csv"
    )
    public void shouldPrepareValidEmailMessageSendingSettingsExperiment() throws Exception {
        partnerSendingSettingsService.setSettings(2L, 100200300L,
                UserPartnerSendingSettings.builder().setEnabled(false).build());
        businessSendingSettingsService.setSettings(3L, 66600200300L,
                UserBusinessSendingSettings.builder()
                        .setThemes(Map.of(0L, UserThemeSettings.builder()
                                .setEnabled(true)
                                .setTransports(Map.of(
                                        NotificationTransport.EMAIL,
                                        UserTransportSettings.builder()
                                                .setEnabled(false)
                                                .build()))
                                .build()))
                        .build());
        when(isSendingSettingsExperimentFlag.get(any())).thenReturn(true);
        when(mbiOpenApiClient.provideUidsByAliases(any()))
                .thenReturn(new ProvideUidsByAliasesResponse().aliasesToUids(List.of(
                        new AliasToUidsDTO()
                                .alias("ShopAdmins")
                                .uids(List.of(1L, 2L)),
                        new AliasToUidsDTO()
                                .alias("YaManagerOnly")
                                .uids(List.of(3L, 2L))
                )));
        when(mbiOpenApiClient.getBusinessIdsForPartners(eq(List.of(100200300L))))
                .thenReturn(new GetBusinessIdsForPartnersResponse()
                        .addPartnerBusinessListItem(new PartnerIdToBusinessIdDTO()
                                .partnerId(100200300L)
                                .businessId(66600200300L)));
        when(mbiOpenApiClient.provideEmailAddressesByUids(any()))
                .thenReturn(new ProvideEmailAddressesByUidsResponse().uidToEmailAddressesList(List.of(
                        new UidToEmailAddressesDTO()
                                .uid(1L)
                                .addresses(List.of("test@yandex-team.ru")),
                        new UidToEmailAddressesDTO()
                                .uid(2L)
                                .addresses(List.of())
                )));

        preparePersistentNotificationTask.execute();

        verify(mbiOpenApiClient).provideEmailAddressesByUids(eq(
                new ProvideEmailAddressesByUidsRequest().uids(List.of(1L))
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "shouldPrepareValidMessage.telegram.before.csv",
            after = "shouldPrepareValidMessage.telegram.after.csv"
    )
    public void shouldPrepareValidTelegramMessage() throws Exception {
        when(mbiOpenApiClient.provideTelegramAddresses(any()))
                .thenReturn(new ProvideTelegramAddressesResponse()
                        .addresses(List.of(
                                new TelegramAddressDTO()
                                        .botId("MarketPartnerTest2Bot")
                                        .telegramId(1234567L)
                        )));

        preparePersistentNotificationTask.execute();
    }

    @Test
    @DbUnitDataSet(
            before = "shouldPrepareValidMessage.telegram.before.csv",
            after = "shouldPrepareValidMessage.telegram.experiment.after.csv"
    )
    public void shouldPrepareValidTelegramMessageSendingSettingsExperiment() throws Exception {
        partnerSendingSettingsService.setSettings(2L, 100200300L,
                UserPartnerSendingSettings.builder().setEnabled(false).build());
        businessSendingSettingsService.setSettings(3L, 66600200300L,
                UserBusinessSendingSettings.builder()
                        .setThemes(Map.of(0L, UserThemeSettings.builder()
                                .setEnabled(true)
                                .setTransports(Map.of(
                                        NotificationTransport.TELEGRAM_BOT,
                                        UserTransportSettings.builder()
                                                .setEnabled(false)
                                                .build()))
                                .build()))
                        .build());
        when(isSendingSettingsExperimentFlag.get(any())).thenReturn(true);
        when(mbiOpenApiClient.provideUidsByAliases(any()))
                .thenReturn(new ProvideUidsByAliasesResponse().aliasesToUids(List.of(
                        new AliasToUidsDTO()
                                .alias("theFirstAlias")
                                .uids(List.of(1L, 2L)),
                        new AliasToUidsDTO()
                                .alias("theSecondAlias")
                                .uids(List.of(3L, 2L))
                )));
        when(mbiOpenApiClient.getBusinessIdsForPartners(eq(List.of(100200300L))))
                .thenReturn(new GetBusinessIdsForPartnersResponse()
                        .addPartnerBusinessListItem(new PartnerIdToBusinessIdDTO()
                                .partnerId(100200300L)
                                .businessId(66600200300L)));
        when(mbiOpenApiClient.provideTelegramAddressesByUids(any()))
                .thenReturn(new ProvideTelegramAddressesByUidsResponse().uidToTelegramAddressesList(List.of(
                        new UidToTelegramAddressesDTO()
                                .uid(1L)
                                .addresses(List.of(
                                        new TelegramAddressDTO()
                                                .botId("MarketPartnerTest2Bot")
                                                .telegramId(1114777L),
                                        new TelegramAddressDTO()
                                                .botId("MarketPartnerTest2Bot")
                                                .telegramId(1234567L)
                                )),
                        new UidToTelegramAddressesDTO()
                                .uid(2L)
                                .addresses(List.of(
                                        new TelegramAddressDTO()
                                                .botId("MarketPartnerTest2Bot")
                                                .telegramId(1234567L)
                                )),
                        new UidToTelegramAddressesDTO()
                                .uid(3L)
                                .addresses(List.of())
                )));

        preparePersistentNotificationTask.execute();

        verify(mbiOpenApiClient).provideTelegramAddressesByUids(eq(
                new ProvideTelegramAddressesByUidsRequest().uids(List.of(1L))
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "shouldPrepareValidMessage.web.before.csv",
            after = "shouldPrepareValidMessage.web.after.csv"
    )
    public void shouldPrepareValidWebMessage() throws Exception {
        when(mbiOpenApiClient.providePushAddresses(new ProvidePushAddressesRequest()
                .notificationType(2L)
                .destinationWithAliases(new DestinationWithAliasesDTO()
                        .shopId(100200300L)
                        .aliases(new ArrayList<>())
                )
        ))
                .thenReturn(new ProvidePushAddressesResponse()
                        .addresses(List.of(
                                new PushAddressDTO()
                                        .uid(10001L),
                                new PushAddressDTO()
                                        .uid(10002L)
                        )));
        preparePersistentNotificationTask.execute();
    }

    @Test
    @DbUnitDataSet(
            before = "shouldSendValidMessage.email.before.csv",
            after = "shouldSendValidMessage.email.after.csv"
    )
    public void shouldSendValidEmailMessage() throws Exception {
        when(emailService.send(any())).thenReturn(
                new NotificationResultImpl(
                        Map.of(EmailAddress.create("user@yandex.ru", EmailAddress.Type.TO),
                                new EmailSuccess()),
                        Map.of()
                )
        );
        sendPersistentNotificationTask.execute();
    }

    @Test
    @DbUnitDataSet(
            before = "shouldSendValidMessage.telegram.before.csv",
            after = "shouldSendValidMessage.telegram.after.csv"
    )
    public void shouldSendValidTelegramMessage() throws Exception {
        when(telegramBotTransportService.send(any())).thenReturn(
                new NotificationResultImpl(
                        Map.of(TelegramIdAddress.create("MarketPartnerTest2Bot", 1234567L),
                                new TelegramBotSuccess()),
                        Map.of()
                )
        );
        sendPersistentNotificationTask.execute();
    }

    @Test
    @DbUnitDataSet(
            before = "shouldSendValidMessage.web.before.csv",
            after = "shouldSendValidMessage.web.byMbi.after.csv"
    )
    public void shouldSendValidWebMessageByMbi() throws Exception {
        sendPersistentNotificationTask.execute();
    }


    @Test
    @DbUnitDataSet(
            before = "shouldSendValidMessage.web.before.csv",
            after = "shouldSendValidMessage.web.byMessageStorage.after.csv"
    )
    public void shouldSendValidWebMessageByMessageStorage() throws Exception {
        shouldSendValidWebMessageByMessageStorageCommon();
    }

    @Test
    @DbUnitDataSet(
            before = "shouldSendValidMessageOldUidFormat.web.before.csv",
            after = "shouldSendValidMessage.web.byMessageStorageOldUidFormat.after.csv"
    )
    public void shouldSendValidWebMessageByMessageStorageOldUidFormat() throws Exception {
        shouldSendValidWebMessageByMessageStorageCommon();
    }

    @Test
    @DbUnitDataSet(
            before = "shouldNotSendValidMessage.email.before.csv",
            after = "shouldNotSendValidMessage.email.after.csv"
    )
    public void shouldNotSendValidEmailMessage() throws Exception {
        when(emailService.send(any())).thenReturn(
                new NotificationResultImpl(
                        Map.of(EmailAddress.create("user@yandex.ru", EmailAddress.Type.TO),
                                new EmailSuccess()),
                        Map.of()
                )
        );
        sendPersistentNotificationTask.execute();
    }

    private void shouldSendValidWebMessageByMessageStorageCommon() throws Exception {
        clock.setFixed(Instant.parse("2022-03-03T16:16:16Z"), ZoneId.of("UTC"));
        environmentService.setValue("web_transport.write_to_mbi.enable", "false");
        environmentService.setValue("web_transport.message_storage.enable", "true");

        when(mbiOpenApiClient.providePushAddresses(new ProvidePushAddressesRequest()
                .notificationType(2L)
                .destinationWithAliases(new DestinationWithAliasesDTO()
                        .shopId(123456L)
                        .aliases(new ArrayList<>())
                )
        ))
                .thenReturn(new ProvidePushAddressesResponse()
                        .addresses(List.of(
                                new PushAddressDTO()
                                        .uid(777L)
                                        .uid(778L)
                        )));

        sendPersistentNotificationTask.execute();
    }
}
