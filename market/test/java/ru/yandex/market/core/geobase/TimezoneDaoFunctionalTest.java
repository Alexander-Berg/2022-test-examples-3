package ru.yandex.market.core.geobase;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.geobase.model.Timezone;

/**
 * Тесты для {@link TimezoneDao}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class TimezoneDaoFunctionalTest extends FunctionalTest {

    @Autowired
    private TimezoneDao timezoneDao;

    @Test
    @DisplayName("Создание таймзоны")
    @DbUnitDataSet(after = "csv/TimezoneDao.createTimezone.after.csv")
    void testCreateTimezone() {
        timezoneDao.createTimezone("test_tz", 123);
    }

    @Test
    @DisplayName("Обновление таймзоны")
    @DbUnitDataSet(before = "csv/TimezoneDao.updateTimezone.before.csv", after = "csv/TimezoneDao.updateTimezone.after.csv")
    void testUpdateTimezone() {
        final Timezone timezone = new Timezone(1, "test_tz1_upd", 456);
        timezoneDao.updateTimezone(timezone);
    }

    @Test
    @DisplayName("Получение списка таймзон")
    @DbUnitDataSet(before = "csv/TimezoneDao.getTimezone.before.csv")
    void testGetTimezone() {
        final List<Timezone> timezones = timezoneDao.getAllTimezones();
        final List<Timezone> expected = Arrays.asList(
                new Timezone(1, "test_tz1", 123),
                new Timezone(2, "test_tz2", 1234),
                new Timezone(3, "test_tz3", 12345)
        );
        Assert.assertEquals(expected, timezones);

        final Timezone byName = timezoneDao.getTimezone("test_tz2");
        Assert.assertEquals(expected.get(1), byName);

        final Timezone byId = timezoneDao.getTimezone(3);
        Assert.assertEquals(expected.get(2), byId);
    }
}
