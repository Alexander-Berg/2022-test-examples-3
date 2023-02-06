package ru.yandex.direct.api.v5.entity.clients.service;

import com.yandex.direct.api.v5.general.LangEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.generalclients.ClientSettingUpdateEnum;
import com.yandex.direct.api.v5.generalclients.ClientSettingUpdateItem;
import com.yandex.direct.api.v5.generalclients.ClientUpdateItem;
import com.yandex.direct.api.v5.generalclients.EmailSubscriptionEnum;
import com.yandex.direct.api.v5.generalclients.EmailSubscriptionItem;
import com.yandex.direct.api.v5.generalclients.NotificationUpdate;
import org.junit.Test;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.i18n.Language;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;


public class UpdateConverterTest {
    public static final long UID = 11L;
    public static final long CLIENT_ID = 22L;
    public static final String EMAIL = "aaa@bbb.ru";
    public static final String PHONE = "+72733729765";
    public static final String CLIENT_INFO = "xxx";

    @Test
    public void convertToChanges_checkIds() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(new ClientUpdateItem(), UID, CLIENT_ID);
        assertThat(changes.getClientChanges().getId(), equalTo(CLIENT_ID));
        assertThat(changes.getUserChanges().getId(), equalTo(UID));
    }

    @Test
    public void convertToChanges_emptyChanges() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(new ClientUpdateItem(), UID, CLIENT_ID);
        assertThat(changes.getUserChanges().isAnyPropChanged(), equalTo(false));
    }

    @Test
    public void convertToChanges_clientInfo() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withClientInfo(CLIENT_INFO), UID, CLIENT_ID);
        assertThat(changes.getUserChanges().getChangedProp(User.FIO), equalTo(CLIENT_INFO));
    }

    @Test
    public void convertToChanges_phone() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withPhone(PHONE), UID, CLIENT_ID);
        assertThat(changes.getUserChanges().getChangedProp(User.PHONE), equalTo(PHONE));
    }

    @Test
    public void convertToChanges_notificationEmail() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withNotification(new NotificationUpdate().withEmail(EMAIL)), UID, CLIENT_ID);
        assertThat(changes.getUserChanges().getChangedProp(User.EMAIL), equalTo(EMAIL));
    }

    @Test
    public void convertToChanges_notificationEmailSubscriptionsReceiveRecommendationsYes() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withNotification(
                        new NotificationUpdate().withEmailSubscriptions(
                                new EmailSubscriptionItem()
                                        .withOption(EmailSubscriptionEnum.RECEIVE_RECOMMENDATIONS)
                                        .withValue(YesNoEnum.YES)
                        )),
                UID, CLIENT_ID);
        assertThat(changes.getUserChanges().getChangedProp(User.SEND_NEWS), equalTo(true));
    }

    @Test
    public void convertToChanges_notificationEmailSubscriptionsReceiveRecommendationsNo() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withNotification(
                        new NotificationUpdate().withEmailSubscriptions(
                                new EmailSubscriptionItem()
                                        .withOption(EmailSubscriptionEnum.RECEIVE_RECOMMENDATIONS)
                                        .withValue(YesNoEnum.NO)
                        )),
                UID, CLIENT_ID);
        assertThat(changes.getUserChanges().getChangedProp(User.SEND_NEWS), equalTo(false));
    }

    @Test
    public void convertToChanges_notificationEmailSubscriptionsTrackManagedCampaignsYes() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withNotification(
                        new NotificationUpdate().withEmailSubscriptions(
                                new EmailSubscriptionItem()
                                        .withOption(EmailSubscriptionEnum.TRACK_MANAGED_CAMPAIGNS)
                                        .withValue(YesNoEnum.YES)
                        )),
                UID, CLIENT_ID);
        assertThat(changes.getUserChanges().getChangedProp(User.SEND_ACC_NEWS), equalTo(true));
    }

    @Test
    public void convertToChanges_notificationEmailSubscriptionsTrackPositionChangesYes() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withNotification(
                        new NotificationUpdate().withEmailSubscriptions(
                                new EmailSubscriptionItem()
                                        .withOption(EmailSubscriptionEnum.TRACK_POSITION_CHANGES)
                                        .withValue(YesNoEnum.YES)
                        )),
                UID, CLIENT_ID);
        assertThat(changes.getUserChanges().getChangedProp(User.SEND_WARN), equalTo(true));
    }

    @Test
    public void convertToChanges_notificationLang() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withNotification(new NotificationUpdate().withLang(LangEnum.TR)),
                UID, CLIENT_ID);
        assertThat(changes.getUserChanges().getChangedProp(User.LANG), equalTo(Language.TR));
    }

    @Test
    public void convertToChanges_settingsDisplayStoreRatingYes() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withSettings(
                        new ClientSettingUpdateItem()
                                .withOption(ClientSettingUpdateEnum.DISPLAY_STORE_RATING)
                                .withValue(YesNoEnum.YES)),
                UID, CLIENT_ID);
        assertThat(changes.getClientChanges().getChangedProp(Client.HIDE_MARKET_RATING), equalTo(false));
    }

    @Test
    public void convertToChanges_settingsDisplayStoreRatingNo() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withSettings(
                        new ClientSettingUpdateItem()
                                .withOption(ClientSettingUpdateEnum.DISPLAY_STORE_RATING)
                                .withValue(YesNoEnum.NO)),
                UID, CLIENT_ID);
        assertThat(changes.getClientChanges().getChangedProp(Client.HIDE_MARKET_RATING), equalTo(true));
    }

    @Test
    public void convertToChanges_settingsCorrectTyposAutomatically() throws Exception {
        UserClientChanges changes = UpdateConverter.convertToChanges(
                new ClientUpdateItem().withSettings(
                        new ClientSettingUpdateItem()
                                .withOption(ClientSettingUpdateEnum.CORRECT_TYPOS_AUTOMATICALLY)
                                .withValue(YesNoEnum.YES)),
                UID, CLIENT_ID);
        assertThat(changes.getClientChanges().getChangedProp(Client.NO_TEXT_AUTOCORRECTION), equalTo(false));
    }
}
