package ru.yandex.market.partner.notification.user_sending_settings.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.user_sending_settings.model.dao.VersionedUserBusinessSendingSettings;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@DbUnitDataSet(before = "UserBusinessSendingSettingsDaoTest.before.csv")
class UserBusinessSendingSettingsDaoTest extends AbstractFunctionalTest {
    @Autowired
    private UserBusinessSendingSettingsDao userBusinessSendingSettingsDao;

    @Test
    void get() {
        var result = userBusinessSendingSettingsDao.getSettings(100L, 200L);
        var expected = VersionedUserBusinessSendingSettings.builder()
                .setUserId(100L)
                .setBusinessId(200L)
                .setFormatVersion(1)
                .setSettings("{\"settings\": \"send a quarter\"}")
                .build();

        assertThat(result.get(), equalTo(expected));
    }

    @Test
    void getSettingsByUids() {
        var result = userBusinessSendingSettingsDao.getSettingsByUids(List.of(100L, 110L, 111L), 200L);
        var expected = List.of(
                VersionedUserBusinessSendingSettings.builder()
                        .setUserId(100L)
                        .setBusinessId(200L)
                        .setFormatVersion(1)
                        .setSettings("{\"settings\": \"send a quarter\"}")
                        .build(),
                VersionedUserBusinessSendingSettings.builder()
                        .setUserId(111L)
                        .setBusinessId(200L)
                        .setFormatVersion(1)
                        .setSettings("{\"settings\": \"don't send at all\"}")
                        .build()
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    @DbUnitDataSet(after = "UserBusinessSendingSettingsDaoTest.insert.after.csv")
    void insert() {
        var settings = VersionedUserBusinessSendingSettings.builder()
                .setUserId(111L)
                .setBusinessId(222L)
                .setFormatVersion(1)
                .setSettings("{\"settings\": \"send'em all\"}")
                .build();
        userBusinessSendingSettingsDao.upsertSettings(settings);
    }

    @Test
    @DbUnitDataSet(after = "UserBusinessSendingSettingsDaoTest.update.after.csv")
    void update() {
        var settings = VersionedUserBusinessSendingSettings.builder()
                .setUserId(100L)
                .setBusinessId(200L)
                .setFormatVersion(1)
                .setSettings("{\"settings\": \"send'em all to devnull\"}")
                .build();
        userBusinessSendingSettingsDao.upsertSettings(settings);
    }
}
