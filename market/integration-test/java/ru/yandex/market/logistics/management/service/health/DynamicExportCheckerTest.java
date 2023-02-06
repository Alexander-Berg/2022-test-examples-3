package ru.yandex.market.logistics.management.service.health;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.health.jobs.DynamicExportChecker;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@SuppressWarnings("checkstyle:MagicNumber")
class DynamicExportCheckerTest extends AbstractContextualTest {

    @Autowired
    private DynamicExportChecker service;

    private static final LocalDateTime TIME_TO_RETURN = LocalDateTime.parse("2018-07-03T15:01:00");

    @Test
    @Sql("/data/controller/health/export_last_not_ok.sql")
    void timeCheckOk() {
        String timeStatus = service.getTimeStatus(TIME_TO_RETURN);
        softly.assertThat(timeStatus).as("Should return ok")
            .isEqualTo("0;OK");
    }

    @Test
    @Sql("/data/controller/health/export_last_not_ok.sql")
    void timeCheckFail() {
        String timeStatus = service.getTimeStatus(TIME_TO_RETURN.plusMinutes(30));
        softly.assertThat(timeStatus).as("Should return fail")
            .isEqualTo("2;Latest try to export dynamic for report was at 2018-07-03T15:11:00.056");
    }

    @Test
    void timeCheckWhenReturnsEmptyList() {
        String timeStatus = service.getTimeStatus(TIME_TO_RETURN.plusMinutes(30));
        softly.assertThat(timeStatus).as("Should return fail")
            .isEqualTo("2;Dynamic was never exported");
    }

    @Test
    @Sql("/data/controller/health/export_last_ok.sql")
    void lastStatusOk() {
        String resultStatus = service.getLastStatus();
        softly.assertThat(resultStatus).as("Should return fail")
            .isEqualTo("0;OK");
    }

    @Test
    @Sql("/data/controller/health/export_last_not_ok.sql")
    void lastStatusFail() {
        String resultStatus = service.getLastStatus();
        softly.assertThat(resultStatus).as("Should return fail")
            .isEqualTo("2;Latest try to export dynamic failed 2018-07-03T15:11:00.056");
    }

    @Test
    void lastStatusCheckWhenReturnEmptyList() {
        String resultStatus = service.getLastStatus();
        softly.assertThat(resultStatus).as("Should return fail")
            .isEqualTo("2;Dynamic was never exported");
    }
}
