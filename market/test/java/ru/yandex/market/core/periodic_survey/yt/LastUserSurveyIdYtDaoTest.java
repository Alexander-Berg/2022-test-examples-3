package ru.yandex.market.core.periodic_survey.yt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.periodic_survey.model.SurveyId;
import ru.yandex.market.core.periodic_survey.model.SurveyType;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.client.YtClientProxySource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LastUserSurveyIdYtDaoTest {

    private static final BindingTable<YtLastUserSurveyIdRecord> BINDING_TABLE =
            new BindingTable<>("T", YtLastUserSurveyIdRecord.class);
    private final YtClientProxy ytClient = mock(YtClientProxy.class);

    private LastUserSurveyIdYtDao ytDao;

    @BeforeEach
    void setUp() {
        ytDao = new LastUserSurveyIdYtDao(ytClient, YtClientProxySource.singleSource(ytClient), BINDING_TABLE);
    }

    @Test
    void getLastSurveyIds() {
        ytDao.getLastSurveyIds(List.of(123L, 234L), SurveyType.NPS_DBS);

        verify(ytClient).lookupRows(
                BINDING_TABLE.getTable(),
                LastUserSurveyIdYtDao.KEY_BINDER,
                Set.of(createKey(123L, SurveyType.NPS_DBS), createKey(234L, SurveyType.NPS_DBS)),
                BINDING_TABLE.getBinder()
        );
    }

    @Test
    void saveLastSurveyIds() {
        List<SurveyId> surveyIds = List.of(
                createSurveyId(101L, 1001L),
                createSurveyId(102L, 1002L));

        ytDao.saveLastSurveyIds(surveyIds);

        verify(ytClient).insertRows(
                "T",
                BINDING_TABLE.getBinder(),
                surveyIds.stream()
                        .map(YtLastUserSurveyIdRecord::of)
                        .collect(Collectors.toList()));
    }

    @Test
    void deleteSurveyIds() {
        List<SurveyId> surveyIds = List.of(
                SurveyId.of(101L, 1001L, SurveyType.NPS_DBS, Instant.parse("2020-07-01T13:02:32Z")),
                SurveyId.of(102L, 1002L, SurveyType.NPS_DBS, Instant.parse("2020-07-01T13:02:32Z")),
                SurveyId.of(103L, 1003L, SurveyType.NPS_DBS, Instant.parse("2020-07-01T13:02:32Z"))
        );
        when(ytClient.lookupRows(
                BINDING_TABLE.getTable(),
                LastUserSurveyIdYtDao.KEY_BINDER,
                Set.of(
                        createKey(1001L, SurveyType.NPS_DBS),
                        createKey(1002L, SurveyType.NPS_DBS),
                        createKey(1003L, SurveyType.NPS_DBS)
                ),
                BINDING_TABLE.getBinder()))
                .thenReturn(List.of(
                        YtLastUserSurveyIdRecord.of(
                                10111L,
                                1001L,
                                SurveyType.NPS_DBS,
                                Instant.parse("2020-07-01T13:02:32Z")),
                        YtLastUserSurveyIdRecord.of(
                                102L,
                                1002L,
                                SurveyType.NPS_DBS,
                                Instant.parse("2020-07-01T13:02:32Z")),
                        YtLastUserSurveyIdRecord.of(
                                103L,
                                1003L,
                                SurveyType.NPS_DBS,
                                Instant.parse("2021-11-11T11:11:11Z"))
                ));

        ytDao.deleteSurveyIds(surveyIds);

        verify(ytClient).deleteRows("T", LastUserSurveyIdYtDao.KEY_BINDER, List.of(
                createKey(1002, SurveyType.NPS_DBS)
        ));
    }

    private static YtLastUserSurveyIdRecord.Key createKey(long userId, SurveyType surveyType) {
        return new YtLastUserSurveyIdRecord.Key(userId, surveyType);
    }

    private static SurveyId createSurveyId(long partnerId, long userId) {
        return SurveyId.of(partnerId, userId, SurveyType.NPS_DROPSHIP, Instant.parse("2020-07-01T13:02:32Z"));
    }

}
