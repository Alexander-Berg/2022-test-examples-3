package ru.yandex.market.core.periodic_survey.yt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.periodic_survey.model.SurveyId;
import ru.yandex.market.core.periodic_survey.model.SurveyRecord;
import ru.yandex.market.core.periodic_survey.model.SurveyStatus;
import ru.yandex.market.core.periodic_survey.model.SurveyType;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.client.YtClientProxySource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PeriodicSurveyYtDaoTest {
    private static final BindingTable<YtSurveyRecord> BINDING_TABLE = new BindingTable<>("T", YtSurveyRecord.class);
    private final YtClientProxy ytClient = mock(YtClientProxy.class);

    private PeriodicSurveyYtDao periodicSurveyYtDao;

    @BeforeEach
    void setUp() {
        periodicSurveyYtDao = new PeriodicSurveyYtDao(ytClient, YtClientProxySource.singleSource(ytClient),
                BINDING_TABLE);
    }

    @Test
    void getSurvey() {
        SurveyId surveyId = createSurveyId(101L, 1001L);
        periodicSurveyYtDao.getSurvey(surveyId);
        verify(ytClient).lookupRows(
                BINDING_TABLE.getTable(),
                PeriodicSurveyYtDao.NPS_KEYS_BINDER,
                List.of(new YtSurveyRecord.Id(surveyId)),
                BINDING_TABLE.getBinder()
        );
    }

    @Test
    void getSurveys() {
        var id1 = createSurveyId(101L, 1001L);
        var id2 = createSurveyId(102L, 1002L);
        var surveyIds = List.of(id1, id2);

        periodicSurveyYtDao.getSurveys(surveyIds);
        verify(ytClient).lookupRows(
                BINDING_TABLE.getTable(),
                PeriodicSurveyYtDao.NPS_KEYS_BINDER,
                Set.of(new YtSurveyRecord.Id(id1), new YtSurveyRecord.Id(id2)),
                BINDING_TABLE.getBinder()
        );
    }

    @Test
    void getPartnersSurveys() {
        periodicSurveyYtDao.getPartnersSurveys(List.of(101L, 102L), SurveyStatus.OPEN_SURVEY_STATUSES);
        verify(ytClient).selectRows("partnerId, userId, surveyType, createdAt, status, answeredAt, " +
                        "answer, viewDetails, meta from [T] where partnerId in  (101,102) and status in  (0,1)",
                BINDING_TABLE.getBinder());
    }

    @Test
    void getSurveyForPartnerUser() {
        periodicSurveyYtDao.getOpenedSurveysForPartnerUser(101L, 1001L);
        verify(ytClient).selectRows("partnerId, userId, surveyType, createdAt, status, answeredAt, " +
                        "answer, viewDetails, meta from [T] where partnerId = 101 and userId = 1001 and status in  " +
                        "(0,1)",
                BINDING_TABLE.getBinder());
    }

    @Test
    void upsertRecords() {
        List<SurveyRecord> records = List.of(
                createSurveyRecord(101L, 1001L),
                createSurveyRecord(102L, 1002L));
        periodicSurveyYtDao.upsertRecords(records);
        verify(ytClient).insertRows("T", BINDING_TABLE.getBinder(), records.stream()
                .map(YtSurveyRecord::new)
                .collect(Collectors.toList()));
    }

    @Test
    void deleteRecords() {
        List<SurveyId> ids = List.of(
                createSurveyId(101L, 1001L),
                createSurveyId(102L, 1002L));
        periodicSurveyYtDao.deleteSurveys(ids);
        verify(ytClient).deleteRows("T", PeriodicSurveyYtDao.NPS_KEYS_BINDER, ids.stream()
                .map(YtSurveyRecord.Id::new)
                .collect(Collectors.toList()));
    }


    //prepare mocks
    static SurveyId createSurveyId(long partnerId, long userId) {
        return SurveyId.of(partnerId, userId, SurveyType.NPS_DROPSHIP, Instant.parse("2020-07-01T13:02:32Z"));
    }

    static SurveyRecord createSurveyRecord(long partnerId, long userId) {
        return SurveyRecord.newBuilder()
                .withSurveyId(createSurveyId(partnerId, userId))
                .withStatus(SurveyStatus.ACTIVE)
                .build();
    }
}
