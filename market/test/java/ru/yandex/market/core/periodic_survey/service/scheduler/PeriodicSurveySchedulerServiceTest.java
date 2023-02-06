package ru.yandex.market.core.periodic_survey.service.scheduler;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.core.periodic_survey.model.SurveyId;
import ru.yandex.market.core.periodic_survey.model.SurveyRecord;
import ru.yandex.market.core.periodic_survey.model.SurveyStatus;
import ru.yandex.market.core.periodic_survey.model.SurveyType;
import ru.yandex.market.core.periodic_survey.service.NetPromoterScoreNotificationService;
import ru.yandex.market.core.periodic_survey.service.provider.PeriodicSurveyPartnersProvider;
import ru.yandex.market.core.periodic_survey.yt.LastUserSurveyIdYtDao;
import ru.yandex.market.core.periodic_survey.yt.PeriodicSurveyYtDao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PeriodicSurveySchedulerServiceTest {
    private PeriodicSurveySchedulerService schedulerService;

    private PeriodicSurveyPartnersDayFilter partnersDayFilterMock;
    private PeriodicSurveyYtDao periodicSurveyDaoMock;
    private LastUserSurveyIdYtDao lastUserSurveyIdDaoMock;
    private NetPromoterScoreNotificationService npsNotificationServiceMock;
    private NetPromoterScorePartnerUsersProvider partnerUsersProviderMock;
    private TestableClock clock;

    private PeriodicSurveyPartnersProvider partnersProviderMock;

    @BeforeEach
    void setUp() {
        clock = new TestableClock();
        clock.setFixed(Instant.parse("2021-08-17T18:10:12.345678Z"), ZoneOffset.UTC);

        partnersDayFilterMock = mock(PeriodicSurveyPartnersDayFilter.class);
        periodicSurveyDaoMock = mock(PeriodicSurveyYtDao.class);
        lastUserSurveyIdDaoMock = mock(LastUserSurveyIdYtDao.class);
        npsNotificationServiceMock = mock(NetPromoterScoreNotificationService.class);
        partnerUsersProviderMock = mock(NetPromoterScorePartnerUsersProvider.class);

        partnersProviderMock = mock(PeriodicSurveyPartnersProvider.class);

        int surveyPeriod = 180;
        int batchSize = 2;
        schedulerService = new PeriodicSurveySchedulerService(
                partnersDayFilterMock,
                periodicSurveyDaoMock,
                lastUserSurveyIdDaoMock,
                npsNotificationServiceMock,
                partnerUsersProviderMock,
                clock,
                surveyPeriod,
                batchSize);

        when(partnersDayFilterMock.shouldBeSurveyedTodayPredicate()).thenReturn((partnerId) -> partnerId % 2 == 0);
    }

    @Test
    void filteredByPartnersDayFilter() {
        when(partnersProviderMock.getPartners())
                .thenReturn(List.of(1001L, 1003L).stream());

        schedulerService.createSurveysForToday(SurveyType.NPS_DBS, partnersProviderMock);

        verify(partnerUsersProviderMock, never()).getPartnerToUsersMap(any());
        verify(lastUserSurveyIdDaoMock, never()).getLastSurveyIds(any(), any());
        verify(lastUserSurveyIdDaoMock, never()).saveLastSurveyIds(any());
        verify(periodicSurveyDaoMock, never()).getSurveys(any());
        verify(periodicSurveyDaoMock, never()).upsertRecords(any());
        verify(npsNotificationServiceMock, never()).notifyUsers(any());
    }

    @Test
    void noUsersForPartners() {
        when(partnersProviderMock.getPartners())
                .thenReturn(LongStream.range(1L, 6L).boxed());
        when(partnerUsersProviderMock.getPartnerToUsersMap(eq(List.of(2L, 4L))))
                .thenReturn(Map.of());

        schedulerService.createSurveysForToday(SurveyType.NPS_DBS, partnersProviderMock);

        verify(lastUserSurveyIdDaoMock, never()).getLastSurveyIds(any(), any());
        verify(lastUserSurveyIdDaoMock, never()).saveLastSurveyIds(any());
        verify(periodicSurveyDaoMock, never()).getSurveys(any());
        verify(periodicSurveyDaoMock, never()).upsertRecords(any());
        verify(npsNotificationServiceMock, never()).notifyUsers(any());
    }

    @Test
    void uniqueUserForEachPartner() {
        when(partnersProviderMock.getPartners())
                .thenReturn(LongStream.range(1L, 6L).boxed());
        when(partnerUsersProviderMock.getPartnerToUsersMap(eq(List.of(2L, 4L))))
                .thenReturn(Map.of(2L, Set.of(22L), 4L, Set.of(44L)));
        when(lastUserSurveyIdDaoMock.getLastSurveyIds(eq(Set.of(22L, 44L)), eq(SurveyType.NPS_DBS)))
                .thenReturn(List.of());

        schedulerService.createSurveysForToday(SurveyType.NPS_DBS, partnersProviderMock);

        ArgumentCaptor<Set<SurveyId>> surveyIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(lastUserSurveyIdDaoMock).saveLastSurveyIds(surveyIdsCaptor.capture());
        assertThat(surveyIdsCaptor.getValue(), containsInAnyOrder(
                SurveyId.of(2L, 22L, SurveyType.NPS_DBS, Instant.parse("2021-08-17T18:10:12Z")),
                SurveyId.of(4L, 44L, SurveyType.NPS_DBS, Instant.parse("2021-08-17T18:10:12Z"))
        ));

        verify(periodicSurveyDaoMock).getSurveys(eq(List.of()));

        ArgumentCaptor<List<SurveyRecord>> surveysCaptor = ArgumentCaptor.forClass(List.class);

        verify(periodicSurveyDaoMock).upsertRecords(surveysCaptor.capture());
        assertThat(surveysCaptor.getValue(), containsInAnyOrder(
                getActiveSurvey(2L, 22L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z"),
                getActiveSurvey(4L, 44L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z")
        ));

        verify(npsNotificationServiceMock).notifyUsers(surveysCaptor.capture());
        assertThat(surveysCaptor.getValue(), containsInAnyOrder(
                getActiveSurvey(2L, 22L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z"),
                getActiveSurvey(4L, 44L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z")
        ));
    }

    @Test
    void doNotCreateSurveyWhenYoungerThan180DaysForUserExists() {
        when(partnersProviderMock.getPartners())
                .thenReturn(LongStream.range(1L, 6L).boxed());
        when(partnerUsersProviderMock.getPartnerToUsersMap(eq(List.of(2L, 4L))))
                .thenReturn(Map.of(2L, Set.of(22L), 4L, Set.of(44L)));
        when(lastUserSurveyIdDaoMock.getLastSurveyIds(eq(Set.of(22L, 44L)), eq(SurveyType.NPS_DBS)))
                .thenReturn(List.of(
                        SurveyId.of(6L, 22L, SurveyType.NPS_DBS, Instant.parse("2021-03-17T18:10:12Z")),
                        SurveyId.of(6L, 44L, SurveyType.NPS_DBS, Instant.parse("2021-01-17T18:10:12Z"))
                ));

        schedulerService.createSurveysForToday(SurveyType.NPS_DBS, partnersProviderMock);

        ArgumentCaptor<Set<SurveyId>> surveyIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(lastUserSurveyIdDaoMock).saveLastSurveyIds(surveyIdsCaptor.capture());
        assertThat(surveyIdsCaptor.getValue(), containsInAnyOrder(
                SurveyId.of(4L, 44L, SurveyType.NPS_DBS, Instant.parse("2021-08-17T18:10:12Z"))
        ));

        verify(periodicSurveyDaoMock).getSurveys(eq(List.of(
                SurveyId.of(6L, 22L, SurveyType.NPS_DBS, Instant.parse("2021-03-17T18:10:12Z")),
                SurveyId.of(6L, 44L, SurveyType.NPS_DBS, Instant.parse("2021-01-17T18:10:12Z"))
        )));

        ArgumentCaptor<List<SurveyRecord>> surveysCaptor = ArgumentCaptor.forClass(List.class);

        verify(periodicSurveyDaoMock).upsertRecords(surveysCaptor.capture());
        assertThat(surveysCaptor.getValue(), containsInAnyOrder(
                getActiveSurvey(4L, 44L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z")
        ));

        verify(npsNotificationServiceMock).notifyUsers(surveysCaptor.capture());
        assertThat(surveysCaptor.getValue(), containsInAnyOrder(
                getActiveSurvey(4L, 44L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z")
        ));
    }

    @Test
    void doNotCreateSurveyWhenOpenExists() {
        when(partnersProviderMock.getPartners())
                .thenReturn(LongStream.range(1L, 8L).boxed());
        when(partnerUsersProviderMock.getPartnerToUsersMap(eq(List.of(2L, 4L))))
                .thenReturn(Map.of(2L, Set.of(22L), 4L, Set.of(44L), 6L, Set.of(66L)));
        when(lastUserSurveyIdDaoMock.getLastSurveyIds(eq(Set.of(22L, 44L)), eq(SurveyType.NPS_DBS)))
                .thenReturn(List.of(
                        SurveyId.of(2L, 22L, SurveyType.NPS_DBS, Instant.parse("2021-01-17T18:10:12Z")),
                        SurveyId.of(4L, 44L, SurveyType.NPS_DBS, Instant.parse("2021-01-17T18:10:12Z")),
                        SurveyId.of(6L, 66L, SurveyType.NPS_DBS, Instant.parse("2021-01-17T18:10:12Z"))
                ));
        when(periodicSurveyDaoMock.getSurveys(any()))
                .thenReturn(List.of(
                        getSurvey(2L, 22L, SurveyType.NPS_DBS, "2021-01-17T18:10:12Z", SurveyStatus.ACTIVE),
                        getSurvey(4L, 44L, SurveyType.NPS_DBS, "2021-01-17T18:10:12Z", SurveyStatus.POSTPONED),
                        getSurvey(6L, 66L, SurveyType.NPS_DBS, "2021-01-17T18:10:12Z", SurveyStatus.COMPLETED)
                ));

        schedulerService.createSurveysForToday(SurveyType.NPS_DBS, partnersProviderMock);

        ArgumentCaptor<Set<SurveyId>> surveyIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(lastUserSurveyIdDaoMock).saveLastSurveyIds(surveyIdsCaptor.capture());
        assertThat(surveyIdsCaptor.getValue(), containsInAnyOrder(
                SurveyId.of(6L, 66L, SurveyType.NPS_DBS, Instant.parse("2021-08-17T18:10:12Z"))
        ));

        ArgumentCaptor<List<SurveyRecord>> surveysCaptor = ArgumentCaptor.forClass(List.class);

        verify(periodicSurveyDaoMock).upsertRecords(surveysCaptor.capture());
        assertThat(surveysCaptor.getValue(), containsInAnyOrder(
                getActiveSurvey(6L, 66L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z")
        ));

        verify(npsNotificationServiceMock).notifyUsers(surveysCaptor.capture());
        assertThat(surveysCaptor.getValue(), containsInAnyOrder(
                getActiveSurvey(6L, 66L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z")
        ));
    }

    @Test
    void twoPartnersForUser() {
        when(partnersProviderMock.getPartners())
                .thenReturn(LongStream.range(1L, 6L).boxed());
        when(partnerUsersProviderMock.getPartnerToUsersMap(eq(List.of(2L, 4L))))
                .thenReturn(Map.of(2L, Set.of(22L, 44L), 4L, Set.of(22L, 44L)));
        when(lastUserSurveyIdDaoMock.getLastSurveyIds(eq(Set.of(22L, 44L)), eq(SurveyType.NPS_DBS)))
                .thenReturn(List.of());

        schedulerService.createSurveysForToday(SurveyType.NPS_DBS, partnersProviderMock);

        ArgumentCaptor<Set<SurveyId>> surveyIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(lastUserSurveyIdDaoMock).saveLastSurveyIds(surveyIdsCaptor.capture());
        assertThat(surveyIdsCaptor.getValue(), containsInAnyOrder(
                SurveyId.of(2L, 22L, SurveyType.NPS_DBS, Instant.parse("2021-08-17T18:10:12Z")),
                SurveyId.of(2L, 44L, SurveyType.NPS_DBS, Instant.parse("2021-08-17T18:10:12Z"))
        ));

        verify(periodicSurveyDaoMock).getSurveys(eq(List.of()));

        ArgumentCaptor<List<SurveyRecord>> surveysCaptor = ArgumentCaptor.forClass(List.class);

        verify(periodicSurveyDaoMock).upsertRecords(surveysCaptor.capture());
        assertThat(surveysCaptor.getValue(), containsInAnyOrder(
                getActiveSurvey(2L, 22L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z"),
                getActiveSurvey(2L, 44L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z")
        ));

        verify(npsNotificationServiceMock).notifyUsers(surveysCaptor.capture());
        assertThat(surveysCaptor.getValue(), containsInAnyOrder(
                getActiveSurvey(2L, 22L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z"),
                getActiveSurvey(2L, 44L, SurveyType.NPS_DBS, "2021-08-17T18:10:12Z")
        ));
    }

    static SurveyRecord getActiveSurvey(long partnerId, long userId, SurveyType surveyType, String timestamp) {
        return SurveyRecord.newBuilder()
                .withSurveyId(SurveyId.of(partnerId, userId, surveyType, Instant.parse(timestamp)))
                .withStatus(SurveyStatus.ACTIVE)
                .build();
    }

    static SurveyRecord getSurvey(long partnerId, long userId, SurveyType surveyType, String timestamp,
                                  SurveyStatus status) {
        return SurveyRecord.newBuilder()
                .withSurveyId(SurveyId.of(partnerId, userId, surveyType, Instant.parse(timestamp)))
                .withStatus(status)
                .build();
    }
}
