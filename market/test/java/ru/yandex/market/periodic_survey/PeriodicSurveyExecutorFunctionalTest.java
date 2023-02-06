package ru.yandex.market.periodic_survey;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.periodic_survey.model.SurveyId;
import ru.yandex.market.core.periodic_survey.model.SurveyRecord;
import ru.yandex.market.core.periodic_survey.model.SurveyStatus;
import ru.yandex.market.core.periodic_survey.model.SurveyType;
import ru.yandex.market.core.periodic_survey.service.NetPromoterScoreNotificationService;
import ru.yandex.market.core.periodic_survey.yt.LastUserSurveyIdYtDao;
import ru.yandex.market.core.periodic_survey.yt.PeriodicSurveyYtDao;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PeriodicSurveyExecutorFunctionalTest extends FunctionalTest {
    @Autowired
    private PeriodicSurveyExecutor periodicSurveyExecutor;
    @Autowired
    private PeriodicSurveyYtDao periodicSurveyYtDao;
    @Autowired
    private LastUserSurveyIdYtDao lastUserSurveyIdYtDao;
    @Autowired
    private Clock clock;

    @Autowired
    private NetPromoterScoreNotificationService npsNotificationService;

    @Test
    @DbUnitDataSet(before = "PeriodicSurveyExecutorFunctionalTest.before.csv")
    @DisplayName("Проверяем, что при запуске джобы создаются опросы и рассылается нотификация")
    public void testJobRun() {
        when(clock.instant()).thenReturn(Instant.parse("1970-04-25T00:00:00Z"));

        periodicSurveyExecutor.doJob(null);

        SurveyId expectedSurveyId = SurveyId.of(
                114L, 1140L, SurveyType.NPS_DROPSHIP, Instant.parse("1970-04-25T00:00:00Z"));
        SurveyRecord expectedSurvey = SurveyRecord.newBuilder()
                .withSurveyId(expectedSurveyId)
                .withStatus(SurveyStatus.ACTIVE)
                .build();

        verify(periodicSurveyYtDao).upsertRecords(eq(List.of(expectedSurvey)));
        verify(lastUserSurveyIdYtDao).saveLastSurveyIds(eq(Set.of(expectedSurveyId)));
        verify(npsNotificationService).notifyUsers(eq(List.of(expectedSurvey)));
    }

    @Test
    @DbUnitDataSet(before = "PeriodicSurveyExecutorFunctionalTest.before.csv")
    @DisplayName("Проверяем, что партнер с ever_activated=false не попадает в выборку")
    public void testEverActivated() {
        when(clock.instant()).thenReturn(Instant.parse("1970-04-18T00:00:00Z"));

        periodicSurveyExecutor.doJob(null);

        verifyNoInteractions(periodicSurveyYtDao);
    }

    @Test
    @DbUnitDataSet(before = {"PeriodicSurveyExecutorFunctionalTest.before.csv",
            "PeriodicSurveyExecutorFunctionalTest.cc.before.csv"})
    @DisplayName("Проверяем, что при запуске джобы создаются опросы и рассылается нотификация " +
            "click-and-collect партнерам вместе с dbs партнерами")
    public void jobRun() {
        when(clock.instant()).thenReturn(Instant.parse("1970-04-25T00:00:00Z"));

        periodicSurveyExecutor.doJob(null);

        SurveyId expectedSurveyId = SurveyId.of(
                114L, 1140L, SurveyType.NPS_DBS, Instant.parse("1970-04-25T00:00:00Z"));
        SurveyRecord expectedSurvey = SurveyRecord.newBuilder()
                .withSurveyId(expectedSurveyId)
                .withStatus(SurveyStatus.ACTIVE)
                .build();

        verify(periodicSurveyYtDao).upsertRecords(eq(List.of(expectedSurvey)));
        verify(lastUserSurveyIdYtDao).saveLastSurveyIds(eq(Set.of(expectedSurveyId)));
        verify(npsNotificationService).notifyUsers(eq(List.of(expectedSurvey)));
    }

    @Test
    @DbUnitDataSet(before = "PeriodicSurveyExecutorFunctionalTest.express.before.csv")
    @DisplayName("Проверяем, что при запуске джобы корректно создаются опросы для FBS-express")
    public void testFbsAndExpress() {
        when(clock.instant()).thenReturn(Instant.parse("1970-04-25T00:00:00Z"));

        periodicSurveyExecutor.doJob(null);

        SurveyId expectedSurveyIdExpress = SurveyId.of(
                114L, 1142L, SurveyType.NPS_EXPRESS, Instant.parse("1970-04-25T00:00:00Z"));
        SurveyRecord expectedSurveyExpress = SurveyRecord.newBuilder()
                .withSurveyId(expectedSurveyIdExpress)
                .withStatus(SurveyStatus.ACTIVE)
                .build();

        verify(periodicSurveyYtDao).upsertRecords(eq(List.of(expectedSurveyExpress)));
        verify(lastUserSurveyIdYtDao).saveLastSurveyIds(eq(Set.of(expectedSurveyIdExpress)));
        verify(npsNotificationService).notifyUsers(eq(List.of(expectedSurveyExpress)));
    }
}
