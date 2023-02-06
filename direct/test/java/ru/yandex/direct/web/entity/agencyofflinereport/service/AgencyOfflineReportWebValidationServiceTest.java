package ru.yandex.direct.web.entity.agencyofflinereport.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportParametersService;
import ru.yandex.direct.validation.defect.DateDefects;
import ru.yandex.direct.validation.defect.ids.DateDefectIds;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.agencyofflinereport.model.EnqueueReportRequestParams;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportParametersService.MAX_REPORT_DATE_PROP_NAME;
import static ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportParametersService.MIN_REPORT_DATE;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.entity.agencyofflinereport.model.EnqueueReportRequestParams.DATE_FROM_FIELD_NAME;
import static ru.yandex.direct.web.entity.agencyofflinereport.model.EnqueueReportRequestParams.DATE_TO_FIELD_NAME;

@DirectWebTest
public class AgencyOfflineReportWebValidationServiceTest {
    private static final LocalDate START_DATE = LocalDate.of(2018, 4, 12);
    private static final LocalDate END_DATE = LocalDate.of(2018, 5, 4);

    private final AgencyOfflineReportWebValidationService validationService;
    private final PpcPropertiesSupport ppcPropertiesSupport;

    public AgencyOfflineReportWebValidationServiceTest() {
        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        AgencyOfflineReportParametersService reportParametersService = new AgencyOfflineReportParametersService(
                ppcPropertiesSupport);
        this.validationService = new AgencyOfflineReportWebValidationService(reportParametersService);
    }

    @Before
    public void beforeTest() {
        initMock(null);
    }

    private void initMock(String date) {
        when(ppcPropertiesSupport.find(MAX_REPORT_DATE_PROP_NAME)).thenReturn(Optional.ofNullable(date));
    }

    @Test
    public void validateEnqueueRequest_goodRequest() {
        EnqueueReportRequestParams request = new EnqueueReportRequestParams()
                .withDateFrom(START_DATE)
                .withDateTo(END_DATE);
        assertFalse(validationService.validateEnqueueRequest(request).hasAnyErrors());
    }

    @Test
    public void validateEnqueueRequest_goodRequest_endDateEqualsDateFromProp() {
        initMock(END_DATE.format(ISO_LOCAL_DATE));
        EnqueueReportRequestParams request = new EnqueueReportRequestParams()
                .withDateFrom(START_DATE)
                .withDateTo(END_DATE);
        assertFalse(validationService.validateEnqueueRequest(request).hasAnyErrors());
    }

    @Test
    public void validateEnqueueRequest_goodRequest_endDateInFutureEqualsDateFromProp() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        initMock(futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        EnqueueReportRequestParams request = new EnqueueReportRequestParams()
                .withDateFrom(START_DATE)
                .withDateTo(futureDate);
        assertFalse(validationService.validateEnqueueRequest(request).hasAnyErrors());
    }

    @Test
    public void validateEnqueueRequest_goodRequest_startDateEqualsEndDate() {
        EnqueueReportRequestParams request = new EnqueueReportRequestParams()
                .withDateFrom(START_DATE)
                .withDateTo(START_DATE);
        assertFalse(validationService.validateEnqueueRequest(request).hasAnyErrors());
    }

    @Test
    public void validateEnqueueRequest_badRequest_startDateBeforeAllowed() {
        EnqueueReportRequestParams request = new EnqueueReportRequestParams()
                .withDateFrom(LocalDate.of(2001, 1, 1))
                .withDateTo(END_DATE);
        assertThat(validationService.validateEnqueueRequest(request).flattenErrors(), contains(
                validationError(path(field(DATE_FROM_FIELD_NAME)), DateDefects.greaterThanOrEqualTo(MIN_REPORT_DATE))));
    }

    @Test
    public void validateEnqueueRequest_badRequest_startDateInFuture() {
        EnqueueReportRequestParams request = new EnqueueReportRequestParams()
                .withDateFrom(LocalDate.now().plusDays(1))
                .withDateTo(END_DATE);
        assertThat(validationService.validateEnqueueRequest(request).flattenErrors(), contains(
                validationError(path(field(DATE_FROM_FIELD_NAME)), DateDefectIds.MUST_BE_LESS_THAN_OR_EQUAL_TO_MAX)));
    }

    @Test
    public void validateEnqueueRequest_badRequest_endDateInFuture() {
        EnqueueReportRequestParams request = new EnqueueReportRequestParams()
                .withDateFrom(START_DATE)
                .withDateTo(LocalDate.now().plusDays(1));
        assertThat(validationService.validateEnqueueRequest(request).flattenErrors(), contains(
                validationError(path(field(DATE_TO_FIELD_NAME)), DateDefectIds.MUST_BE_LESS_THAN_OR_EQUAL_TO_MAX)));
    }

    @Test
    public void validateEnqueueRequest_badRequest_endDateIsAfterDateFromProp() {
        initMock(END_DATE.minusDays(1).format(ISO_LOCAL_DATE));
        EnqueueReportRequestParams request = new EnqueueReportRequestParams()
                .withDateFrom(START_DATE)
                .withDateTo(LocalDate.now().plusDays(1));
        assertThat(validationService.validateEnqueueRequest(request).flattenErrors(), contains(
                validationError(path(field(DATE_TO_FIELD_NAME)), DateDefectIds.MUST_BE_LESS_THAN_OR_EQUAL_TO_MAX)));
    }

    @Test
    public void validateEnqueueRequest_badRequest_endDateBeforeStartDate() {
        EnqueueReportRequestParams request = new EnqueueReportRequestParams()
                .withDateFrom(START_DATE)
                .withDateTo(START_DATE.minusDays(1));
        assertThat(validationService.validateEnqueueRequest(request).flattenErrors(), contains(
                validationError(DefectIds.INCONSISTENT_STATE)));
    }
}
