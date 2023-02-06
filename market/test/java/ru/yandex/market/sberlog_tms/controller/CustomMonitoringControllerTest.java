package ru.yandex.market.sberlog_tms.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.market.sberlog_tms.SberlogtmsConfig;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 18.11.19
 */
@Disabled
@SpringJUnitConfig(SberlogtmsConfig.class)
class CustomMonitoringControllerTest {

    private CustomMonitoringController customMonitoringController;

    @BeforeEach
    public void CustomMonitoringControllerTestInitial() {
        this.customMonitoringController = new CustomMonitoringController();
    }

    @Test
    void checkRefreshReportTime() {
        ResponseEntity<?> responce = customMonitoringController.checkRefreshReportTime();
        Assertions.assertEquals(200, responce.getStatusCode().value());

        String answer = Objects.requireNonNull(responce.getBody()).toString();
        //2;UpdateMonitoringDataReportThread exception
        //2;sberlog users report is too old in YT
        //1;lock file
        //0;OK
        final Pattern answer_pattern = Pattern.compile("[0-2];\\S+.*");

        Assertions.assertTrue(answer_pattern.matcher(answer).matches());
    }

    @Test
    void checkStalledUser() {
        ResponseEntity<?> responce = customMonitoringController.checkStalledUser();
        Assertions.assertEquals(200, responce.getStatusCode().value());

        String answer = Objects.requireNonNull(responce.getBody()).toString();
        //2;UpdateMonitoringMigrateUserTimeThread
        //2;This users:
        //0;OK
        final Pattern answer_pattern = Pattern.compile("[0-2];\\S+.*");

        Assertions.assertTrue(answer_pattern.matcher(answer).matches());
    }


}
