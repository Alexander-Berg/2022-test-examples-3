package ru.yandex.market.partner.notification.user_sending_settings.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.user_sending_settings.model.dao.VersionedUserPartnerSendingSettings;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@DbUnitDataSet(before = "UserPartnerSendingSettingsDaoTest.before.csv")
class UserPartnerSendingSettingsDaoTest extends AbstractFunctionalTest {
    @Autowired
    private UserPartnerSendingSettingsDao userPartnerSendingSettingsDao;

    @Test
    void getByPartnerIds() {
        var result = userPartnerSendingSettingsDao.getSettingsByPartnerIds(100L, List.of(200L, 201L, 202L));
        var expected = List.of(
                VersionedUserPartnerSendingSettings.builder()
                        .setUserId(100L)
                        .setPartnerId(200L)
                        .setFormatVersion(1)
                        .setSettings("{\"enabled 200\": false}")
                        .build(),
                VersionedUserPartnerSendingSettings.builder()
                        .setUserId(100L)
                        .setPartnerId(201L)
                        .setFormatVersion(1)
                        .setSettings("{\"enabled 201\": false}")
                        .build()
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    void getByUids() {
        var result = userPartnerSendingSettingsDao.getSettingsByUids(List.of(100L, 101L, 102L), 201L);
        var expected = List.of(
                VersionedUserPartnerSendingSettings.builder()
                        .setUserId(100L)
                        .setPartnerId(201L)
                        .setFormatVersion(1)
                        .setSettings("{\"enabled 201\": false}")
                        .build(),
                VersionedUserPartnerSendingSettings.builder()
                        .setUserId(101L)
                        .setPartnerId(201L)
                        .setFormatVersion(1)
                        .setSettings("{\"enabled 101-201\": true}")
                        .build()
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    @DbUnitDataSet(after = "UserPartnerSendingSettingsDaoTest.insert.after.csv")
    void insert() {
        var settings = VersionedUserPartnerSendingSettings.builder()
                .setUserId(100L)
                .setPartnerId(202L)
                .setFormatVersion(1)
                .setSettings("{\"enabled 202\": false}")
                .build();
        userPartnerSendingSettingsDao.upsertSettings(settings);
    }

    @Test
    @DbUnitDataSet(after = "UserPartnerSendingSettingsDaoTest.update.after.csv")
    void update() {
        var settings = VersionedUserPartnerSendingSettings.builder()
                .setUserId(100L)
                .setPartnerId(201L)
                .setFormatVersion(1)
                .setSettings("{\"enabled 201\": true}")
                .build();
        userPartnerSendingSettingsDao.upsertSettings(settings);
    }
}
