package ru.yandex.direct.api.v5.entity.agencyclients.delegate;

import com.yandex.direct.api.v5.agencyclients.AgencyClientUpdateItem;
import com.yandex.direct.api.v5.general.LangEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.generalclients.ClientSettingUpdateEnum;
import com.yandex.direct.api.v5.generalclients.ClientSettingUpdateItem;
import com.yandex.direct.api.v5.generalclients.EmailSubscriptionEnum;
import com.yandex.direct.api.v5.generalclients.EmailSubscriptionItem;
import com.yandex.direct.api.v5.generalclients.GrantItem;
import com.yandex.direct.api.v5.generalclients.NotificationUpdate;
import com.yandex.direct.api.v5.generalclients.PrivilegeEnum;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.agencyclients.service.UpdateAgencyClientConverter;
import ru.yandex.direct.api.v5.entity.agencyclients.service.UserAgencyClientChanges;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.grants.model.Grants;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.i18n.Language;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class UpdateAgencyClientConverterTest {
    private static final long UID = 11L;
    private static final long CLIENT_ID = 22L;
    private static final String EMAIL = "aaa@bbb.ru";
    private static final String PHONE = "+72733729765";
    private static final String CLIENT_INFO = "xxx";

    @Test
    public void convertToChanges_checkUid() throws Exception {
        UserAgencyClientChanges changes =
                UpdateAgencyClientConverter.convertToChanges(new AgencyClientUpdateItem(), UID);
        assertThat(changes.getUserChanges().getId(), equalTo(UID));
    }

    @Test
    public void convertToChanges_checkIds() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withClientId(CLIENT_ID), UID);
        assertThat(changes.getClientChanges().getId(), equalTo(CLIENT_ID));
    }

    @Test
    public void convertToChanges_emptyChanges() throws Exception {
        UserAgencyClientChanges changes =
                UpdateAgencyClientConverter.convertToChanges(new AgencyClientUpdateItem(), UID);
        assertThat(changes.getUserChanges().isAnyPropChanged(), equalTo(false));
    }

    @Test
    public void convertToChanges_clientInfo() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withClientInfo(CLIENT_INFO), UID);
        assertThat(changes.getUserChanges().getChangedProp(User.FIO), equalTo(CLIENT_INFO));
    }

    @Test
    public void convertToChanges_phone() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withPhone(PHONE), UID);
        assertThat(changes.getUserChanges().getChangedProp(User.PHONE), equalTo(PHONE));
    }

    @Test
    public void convertToChanges_notificationEmail() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withNotification(new NotificationUpdate().withEmail(EMAIL)), UID);
        assertThat(changes.getUserChanges().getChangedProp(User.EMAIL), equalTo(EMAIL));
    }

    @Test
    public void convertToChanges_notificationEmailSubscriptionsReceiveRecommendationsYes() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withNotification(
                        new NotificationUpdate().withEmailSubscriptions(
                                new EmailSubscriptionItem()
                                        .withOption(EmailSubscriptionEnum.RECEIVE_RECOMMENDATIONS)
                                        .withValue(YesNoEnum.YES)
                        )),
                UID);
        assertThat(changes.getUserChanges().getChangedProp(User.SEND_NEWS), equalTo(true));
    }

    @Test
    public void convertToChanges_notificationEmailSubscriptionsReceiveRecommendationsNo() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withNotification(
                        new NotificationUpdate().withEmailSubscriptions(
                                new EmailSubscriptionItem()
                                        .withOption(EmailSubscriptionEnum.RECEIVE_RECOMMENDATIONS)
                                        .withValue(YesNoEnum.NO)
                        )),
                UID);
        assertThat(changes.getUserChanges().getChangedProp(User.SEND_NEWS), equalTo(false));
    }

    @Test
    public void convertToChanges_notificationEmailSubscriptionsTrackManagedCampaignsYes() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withNotification(
                        new NotificationUpdate().withEmailSubscriptions(
                                new EmailSubscriptionItem()
                                        .withOption(EmailSubscriptionEnum.TRACK_MANAGED_CAMPAIGNS)
                                        .withValue(YesNoEnum.YES)
                        )),
                UID);
        assertThat(changes.getUserChanges().getChangedProp(User.SEND_ACC_NEWS), equalTo(true));
    }

    @Test
    public void convertToChanges_notificationEmailSubscriptionsTrackPositionChangesYes() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withNotification(
                        new NotificationUpdate().withEmailSubscriptions(
                                new EmailSubscriptionItem()
                                        .withOption(EmailSubscriptionEnum.TRACK_POSITION_CHANGES)
                                        .withValue(YesNoEnum.YES)
                        )),
                UID);
        assertThat(changes.getUserChanges().getChangedProp(User.SEND_WARN), equalTo(true));
    }

    @Test
    public void convertToChanges_notificationLang() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withNotification(new NotificationUpdate().withLang(LangEnum.TR)),
                UID);
        assertThat(changes.getUserChanges().getChangedProp(User.LANG), equalTo(Language.TR));
    }

    @Test
    public void convertToChanges_settingsDisplayStoreRatingYes() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withSettings(
                        new ClientSettingUpdateItem()
                                .withOption(ClientSettingUpdateEnum.DISPLAY_STORE_RATING)
                                .withValue(YesNoEnum.YES)),
                UID);
        assertThat(changes.getClientChanges().getChangedProp(Client.HIDE_MARKET_RATING), equalTo(false));
    }

    @Test
    public void convertToChanges_settingsDisplayStoreRatingNo() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withSettings(
                        new ClientSettingUpdateItem()
                                .withOption(ClientSettingUpdateEnum.DISPLAY_STORE_RATING)
                                .withValue(YesNoEnum.NO)),
                UID);
        assertThat(changes.getClientChanges().getChangedProp(Client.HIDE_MARKET_RATING), equalTo(true));
    }

    @Test
    public void convertToChanges_settingsCorrectTyposAutomatically() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withSettings(
                        new ClientSettingUpdateItem()
                                .withOption(ClientSettingUpdateEnum.CORRECT_TYPOS_AUTOMATICALLY)
                                .withValue(YesNoEnum.YES)),
                UID);
        assertThat(changes.getClientChanges().getChangedProp(Client.NO_TEXT_AUTOCORRECTION), equalTo(false));
    }

    @Test
    public void convertToChanges_grantsEditCampaignYes() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withGrants(
                        new GrantItem()
                                .withPrivilege(PrivilegeEnum.EDIT_CAMPAIGNS)
                                .withValue(YesNoEnum.YES)),
                UID);
        assertThat(changes.getGrantsChanges().getChangedProp(Grants.ALLOW_EDIT_CAMPAIGN), equalTo(true));
    }

    @Test
    public void convertToChanges_grantsEditCampaignNo() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withGrants(
                        new GrantItem()
                                .withPrivilege(PrivilegeEnum.EDIT_CAMPAIGNS)
                                .withValue(YesNoEnum.NO)),
                UID);
        assertThat(changes.getGrantsChanges().getChangedProp(Grants.ALLOW_EDIT_CAMPAIGN), equalTo(false));
    }

    @Test
    public void convertToChanges_grantsImportXlsYes() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withGrants(
                        new GrantItem()
                                .withPrivilege(PrivilegeEnum.IMPORT_XLS)
                                .withValue(YesNoEnum.YES)),
                UID);
        assertThat(changes.getGrantsChanges().getChangedProp(Grants.ALLOW_IMPORT_XLS), equalTo(true));
    }

    @Test
    public void convertToChanges_grantsImportXlsNo() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withGrants(
                        new GrantItem()
                                .withPrivilege(PrivilegeEnum.IMPORT_XLS)
                                .withValue(YesNoEnum.NO)),
                UID);
        assertThat(changes.getGrantsChanges().getChangedProp(Grants.ALLOW_IMPORT_XLS), equalTo(false));
    }

    @Test
    public void convertToChanges_grantsTransferMoneyYes() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withGrants(
                        new GrantItem()
                                .withPrivilege(PrivilegeEnum.TRANSFER_MONEY)
                                .withValue(YesNoEnum.YES)),
                UID);
        assertThat(changes.getGrantsChanges().getChangedProp(Grants.ALLOW_TRANSFER_MONEY), equalTo(true));
    }

    @Test
    public void convertToChanges_grantsTransferMoneyNo() throws Exception {
        UserAgencyClientChanges changes = UpdateAgencyClientConverter.convertToChanges(
                new AgencyClientUpdateItem().withGrants(
                        new GrantItem()
                                .withPrivilege(PrivilegeEnum.TRANSFER_MONEY)
                                .withValue(YesNoEnum.NO)),
                UID);
        assertThat(changes.getGrantsChanges().getChangedProp(Grants.ALLOW_TRANSFER_MONEY), equalTo(false));
    }
}
