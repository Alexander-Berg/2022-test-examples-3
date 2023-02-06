package ru.yandex.market.rg.agency;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.rg.asyncreport.agencychecker.AgencyCheckerParams;
import ru.yandex.market.rg.asyncreport.agencychecker.AgencyCheckerReportGenerator;
import ru.yandex.market.rg.config.ClickhouseFunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Тесты для {@link AgencyCheckerReportGeneratorTest}.
 */
public class AgencyCheckerReportGeneratorTest extends ClickhouseFunctionalTest {
    private static final String REPORT_URL = "http://mds.yandex.net/test.xlsx";
    @Autowired
    AgencyCheckerReportGenerator agencyCheckerReportGenerator;
    @Autowired
    MdsS3Client mdsS3Client;

    @BeforeEach
    void init() throws MalformedURLException {
        doReturn(new URL(REPORT_URL)).when(mdsS3Client).getUrl(any());
    }

    @Test
    @DisplayName("смоук тест, все замокано, проверят, что нет всяких npe и прочего")
    void smokeTest() {
        AgencyCheckerParams reportParams = new AgencyCheckerParams(11, DateTimes.toInstant(2020, 1, 1));
        ReportResult report = agencyCheckerReportGenerator.generate(UUID.randomUUID().toString(), reportParams);
        Assert.assertEquals(report.getNewState(), ReportState.DONE);
        //пустой отчет
        Assert.assertNull(report.getReportGenerationInfo().getUrlToDownload());
    }
}
