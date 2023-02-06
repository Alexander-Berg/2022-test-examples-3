package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;


public class CalendarWorkFor58DaysLoaderTest extends FunctionalTest {
    @Autowired
    CalendarWorkFor58DaysLoader calendarWorkFor58DaysLoader;

    @Test
    @DbUnitDataSet(
        before = "CalendarWorkFor58DaysLoaderTest.before.csv",
        after = "CalendarWorkFor58DaysLoaderTest.after.csv"
    )
    public void load_isOk() {
        setTestTime(LocalDateTime.of(2022, 5, 20, 0, 0));
        calendarWorkFor58DaysLoader.load();
    }
}
