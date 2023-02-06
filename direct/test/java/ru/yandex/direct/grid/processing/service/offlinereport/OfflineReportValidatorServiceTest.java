package ru.yandex.direct.grid.processing.service.offlinereport;

import java.time.DateTimeException;
import java.time.LocalDate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.offlinereport.model.OfflineReportType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.offlinereport.GdOfflineReportResultRequest;
import ru.yandex.direct.grid.processing.model.offlinereport.GdOfflineReportWithDailyIntervalRequest;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OfflineReportValidatorServiceTest {
    @Autowired
    OfflineReportValidationService service;

    @Test
    public void testDomainsRequestNoException() {
        var domainsRequest = getDefaultDomainsRequest();
        service.validateOfflineReportWithMonthlyIntervalRequest(domainsRequest);
    }

    @Test(expected = GridValidationException.class)
    public void testDomainsRequestInvalidReportType() {
        var domainsRequest = getDefaultDomainsRequest()
                .withReportType(OfflineReportType.AGENCY_KPI);
        service.validateOfflineReportWithMonthlyIntervalRequest(domainsRequest);
    }

    @Test(expected = GridValidationException.class)
    public void testDomainsRequestInvalidDate() {
        var domainsRequest = getDefaultDomainsRequest()
                .withMonthFrom("201914");
        service.validateOfflineReportWithMonthlyIntervalRequest(domainsRequest);
    }

    @Test(expected = GridValidationException.class)
    public void testDomainsRequestMaxReportPeriod() {
        var domainsRequest = getDefaultDomainsRequest()
                .withMonthTo("202305");
        service.validateOfflineReportWithMonthlyIntervalRequest(domainsRequest);
    }

    @Test(expected = GridValidationException.class)
    public void testDomainsRequestMonthFromMinValue() {
        var domainsRequest = getDefaultDomainsRequest()
                .withMonthFrom("201902");
        service.validateOfflineReportWithMonthlyIntervalRequest(domainsRequest);
    }

    @Test(expected = GridValidationException.class)
    public void testDomainsRequestMonthFromAfterMonthTo() {
        var domainsRequest = getDefaultDomainsRequest()
                .withMonthFrom("201904");
        service.validateOfflineReportWithMonthlyIntervalRequest(domainsRequest);
    }

    private GdOfflineReportResultRequest getDefaultDomainsRequest() {
        return new GdOfflineReportResultRequest()
                .withMonthFrom("201903")
                .withMonthTo("201903")
                .withReportType(OfflineReportType.DOMAINS);
    }

    @Test
    public void testAgencyKpiRequestNoException() {
        var agencyKpiRequest = getDefaultAgencyKpiRequest();
        service.validateOfflineReportWithDailyIntervalRequest(agencyKpiRequest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAgencyKpiRequestInvalidReportType() {
        var agencyKpiRequest = getDefaultAgencyKpiRequest()
                .withReportType(OfflineReportType.DOMAINS);
        service.validateOfflineReportWithDailyIntervalRequest(agencyKpiRequest);
    }

    @Test(expected = DateTimeException.class)
    public void testAgencyKpiRequestInvalidDate() {
        var agencyKpiRequest = getDefaultAgencyKpiRequest()
                .withDateFrom(LocalDate.of(2020, 14, 1));
        service.validateOfflineReportWithDailyIntervalRequest(agencyKpiRequest);
    }

    @Test(expected = GridValidationException.class)
    public void testAgencyKpiRequestExceedingMaxReportPeriod() {
        var agencyKpiRequest = getDefaultAgencyKpiRequest()
                .withDateTo(LocalDate.of(2021, 3, 1));
        service.validateOfflineReportWithDailyIntervalRequest(agencyKpiRequest);
    }

    @Test(expected = GridValidationException.class)
    public void testAgencyKpiRequestDateFromEarlierThanMinValue() {
        var agencyKpiRequest = getDefaultAgencyKpiRequest()
                .withDateFrom(LocalDate.of(2020, 11, 30));
        service.validateOfflineReportWithDailyIntervalRequest(agencyKpiRequest);
    }

    @Test(expected = GridValidationException.class)
    public void testAgencyKpiRequestDateFromAfterDateTo() {
        var agencyKpiRequest = getDefaultAgencyKpiRequest()
                .withDateFrom(LocalDate.of(2021, 3, 1));
        service.validateOfflineReportWithDailyIntervalRequest(agencyKpiRequest);
    }

    private GdOfflineReportWithDailyIntervalRequest getDefaultAgencyKpiRequest() {
        return new GdOfflineReportWithDailyIntervalRequest()
                .withDateFrom(LocalDate.of(2020, 12, 1))
                .withDateTo(LocalDate.of(2021, 2, 28))
                .withReportType(OfflineReportType.AGENCY_KPI);
    }
}
