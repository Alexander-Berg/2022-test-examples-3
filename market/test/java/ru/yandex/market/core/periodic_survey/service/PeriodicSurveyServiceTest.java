package ru.yandex.market.core.periodic_survey.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.periodic_survey.model.SurveyId;
import ru.yandex.market.core.periodic_survey.model.SurveyRecord;
import ru.yandex.market.core.periodic_survey.model.SurveyStatus;
import ru.yandex.market.core.periodic_survey.model.SurveyType;
import ru.yandex.market.core.periodic_survey.yt.PeriodicSurveyYtDao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "PeriodicSurveyServiceTest.before.csv")
public class PeriodicSurveyServiceTest extends FunctionalTest {

    private PeriodicSurveyService periodicSurveyService;
    private PeriodicSurveyYtDao daoMock;
    private TestableClock clock;

    @BeforeEach
    void setUp() {
        daoMock = mock(PeriodicSurveyYtDao.class);
        clock = new TestableClock();
        periodicSurveyService = new PeriodicSurveyService(daoMock, Duration.of(1800, ChronoUnit.SECONDS), clock);
    }

    @Test
    void writeFailOffTimeout() {
        SurveyId surveyId = SurveyId.of(1L, 2L, SurveyType.NPS_DBS, Instant.parse("2021-08-21T20:00:00Z"));
        SurveyRecord survey = SurveyRecord.newBuilder().withSurveyId(surveyId).withStatus(SurveyStatus.ACTIVE).build();
        clock.setFixed(Instant.parse("2021-08-23T20:00:00Z"), ZoneOffset.UTC);
        doThrow(new RuntimeException("write error")).when(daoMock).upsertRecords(any());
        when(daoMock.getSurvey(any())).thenReturn(Optional.of(survey));
        when(daoMock.getOpenedSurveysForPartnerUser(anyLong(), anyLong())).thenReturn(List.of(survey));

        assertThrows(RuntimeException.class, () -> periodicSurveyService.completeSurvey(surveyId, "{}", "{}"));

        assertThat(periodicSurveyService.getSurvey(surveyId), equalTo(Optional.empty()));
        assertThat(periodicSurveyService.getVisibleSurveys(1L, 2L), equalTo(Collections.emptyList()));

        clock.setFixed(Instant.parse("2021-08-23T20:29:00Z"), ZoneOffset.UTC);
        assertThat(periodicSurveyService.getSurvey(surveyId), equalTo(Optional.empty()));
        assertThat(periodicSurveyService.getVisibleSurveys(1L, 2L), equalTo(Collections.emptyList()));

        clock.setFixed(Instant.parse("2021-08-23T20:31:00Z"), ZoneOffset.UTC);
        assertThat(periodicSurveyService.getSurvey(surveyId), equalTo(Optional.of(survey)));
        assertThat(periodicSurveyService.getVisibleSurveys(1L, 2L), equalTo(List.of(survey)));
    }
}
