package ru.yandex.market.tpl.billing.service;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tms.quartz2.model.JobMonitoringResult;
import ru.yandex.market.tms.quartz2.model.MonitoringStatus;
import ru.yandex.market.tms.quartz2.model.TmsMonitoringResult;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;

public class JugglerResponseHelperTest extends AbstractFunctionalTest {

    @Autowired
    private JugglerResponseHelper jugglerResponseHelper;

    @Test
    void testJugglerResponse() {
        var result = jugglerResponseHelper.getJugglerResponse(getTmsMonitoringResult());
        var expected = "2;<CRIT> Job1 : NullPointerException\n" +
                "<CRIT> Job2 : Job Exception: class org.springframework.web.client." +
                "HttpClientErrorException$Unauthorized: 401 Unauthorized\n" +
                "<CRIT> Job3 : RuntimeException";

        Assertions.assertEquals(expected, result);
    }

    private TmsMonitoringResult getTmsMonitoringResult() {
        return new TmsMonitoringResult(
                MonitoringStatus.CRIT,
                "MONITORING_RESULT",
                List.of(
                        new JobMonitoringResult(
                                "Job1", MonitoringStatus.CRIT, List.of("NullPointerException")
                        ),
                        new JobMonitoringResult(
                                "Job2",
                                MonitoringStatus.CRIT,
                                List.of(
                                        "Job Exception: class org.springframework.web.client." +
                                                "HttpClientErrorException$Unauthorized: 401 Unauthorized"
                                )
                        ),
                        new JobMonitoringResult(
                                "Job3", MonitoringStatus.CRIT, List.of("RuntimeException")
                        )
                        )
        );
    }
}
