package ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync;

import lombok.experimental.UtilityClass;

import java.time.OffsetDateTime;

@UtilityClass
public class Constants {
    public static final String MIGRATIONS = "migrations";
    public static final String DB_NAME_PROPERTY = "test.database.name";

    public static final OffsetDateTime TODAY = OffsetDateTime.now();
    public static final OffsetDateTime YESTERDAY = TODAY.minusDays(1);
    public static final OffsetDateTime TOMORROW = TODAY.plusDays(1);
    public static final OffsetDateTime BC = TODAY.minusYears(2100);
}
