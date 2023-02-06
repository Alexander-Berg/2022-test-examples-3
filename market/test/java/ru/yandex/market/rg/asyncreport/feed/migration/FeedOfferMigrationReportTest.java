package ru.yandex.market.rg.asyncreport.feed.migration;

import java.io.UncheckedIOException;
import java.util.Map;
import java.util.stream.Stream;

import Market.DataCamp.SyncAPI.MigrateFeedOffers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.util.ParamsUtils;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Тесты для {@link FeedOfferMigrationReport}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class FeedOfferMigrationReportTest extends FunctionalTest {

    private static final String REPORT_ID = "report_id_123";
    private static final long BUSINESS_ID = 4001L;
    private static final long PARTNER_ID = 1001L;
    private static final long FEED_ID = 2001L;
    private static final long TARGET_FEED_ID = 2003L;

    private static final String EXCEPTION_HOLDER = "###exception###";

    @Autowired
    private FeedOfferMigrationReport feedOfferMigrationReport;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private ReportsService<ReportsType> reportsService;

    @ParameterizedTest
    @MethodSource("invalidParamData")
    @DisplayName("Невалидные параметры для миграции. Будем выброшено исключение")
    @DbUnitDataSet(before = "csv/FeedOfferMigrationReportTest.invalidParam.before.csv")
    void reportParams_invalidParam_throwsException(String name, String paramFile, String expectedError) {
        FeedOfferMigrationParams params = getParams(paramFile);
        IllegalStateException actualError = Assertions.assertThrows(
                IllegalStateException.class,
                () -> feedOfferMigrationReport.generate(REPORT_ID, params)
        );
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    private static Stream<Arguments> invalidParamData() {
        return Stream.of(
                Arguments.of(
                        "Фид, с которого происходит миграция, принадлежит другому магазину",
                        "json/feedFromOtherPartner.json",
                        "Feed 2002 belongs to another partner. Expected partner: 1001, actual partner: 1002"
                ),
                Arguments.of(
                        "У партнера 1 фид. Пытаемся смигрировать офферы с него. Не можем выбрать целевой фид",
                        "json/unknownTargetFeed.json",
                        "Partner doesn't have any feed for migration. Partner: 1001"
                ),
                Arguments.of(
                        "У партнера 1 выключенный фид. Не можем его выбрать в качестве целевого",
                        "json/disabledTargetFeed.json",
                        "Partner doesn't have any feed for migration. Partner: 1003"
                ),
                Arguments.of(
                        "У партнера вообще нет фидов. Не можем найти целевой",
                        "json/withoutFeed.json",
                        "Partner doesn't have any feed for migration. Partner: 1004"
                ),
                Arguments.of(
                        "У партнера нет бизнеса",
                        "json/withoutBusiness.json",
                        "Partner without business: 1005"
                )
        );
    }

    @Test
    @DisplayName("Из строллера не вернулся следующий токен. Сразу закончили работу")
    @DbUnitDataSet(before = {
            "csv/FeedOfferMigrationReportTest.correct.before.csv",
            "csv/FeedOfferMigrationReportTest.singleFeed.before.csv"
    })
    void migrationMode_emptyResponse_success() {
        mockStrollerHidings("proto/stroller.migration.empty.response.json");

        FeedOfferMigrationParams params = getParams("json/correctMigration.json");
        ReportResult result = feedOfferMigrationReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();
        verifyStrollerHidings(new String[]{null});
        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    @Test
    @DisplayName("Кастомный размер батча")
    @DbUnitDataSet(before = {
            "csv/FeedOfferMigrationReportTest.batch.before.csv",
            "csv/FeedOfferMigrationReportTest.correct.before.csv",
            "csv/FeedOfferMigrationReportTest.singleFeed.before.csv"
    })
    void migrationMode_customBatch_success() {
        mockStrollerHidings("proto/stroller.migration.empty.response.json");

        FeedOfferMigrationParams params = getParams("json/correctMigration.json");
        ReportResult result = feedOfferMigrationReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();
        Mockito.verify(dataCampShopClient, Mockito.times(1))
                .migrateOffersByFeed(eq(BUSINESS_ID), eq(PARTNER_ID), eq(FEED_ID), eq(TARGET_FEED_ID), eq(500), eq(null));
        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    @Test
    @DisplayName("Несколько страниц в строллере. Прошли по всем")
    @DbUnitDataSet(before = {
            "csv/FeedOfferMigrationReportTest.correct.before.csv",
            "csv/FeedOfferMigrationReportTest.singleFeed.before.csv"
    })
    void migrationMode_severalPagesResponse_success() {
        mockStrollerHidings(
                "proto/stroller.migration.page1.response.json",
                "proto/stroller.migration.page2.response.json",
                "proto/stroller.migration.empty.response.json"
        );

        FeedOfferMigrationParams params = getParams("json/correctMigration.json");
        ReportResult result = feedOfferMigrationReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();
        verifyStrollerHidings(null, "page_2", "page_3");
        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    @Test
    @DisplayName("Несколько страниц в строллере. Во время второй получили исключение. Сработал ретрай. Прошли по всем")
    @DbUnitDataSet(before = {
            "csv/FeedOfferMigrationReportTest.correct.before.csv",
            "csv/FeedOfferMigrationReportTest.singleFeed.before.csv"
    })
    void migrationMode_retryOnException_success() {
        mockStrollerHidings(
                "proto/stroller.migration.page1.response.json",
                EXCEPTION_HOLDER,
                "proto/stroller.migration.page2.response.json",
                "proto/stroller.migration.empty.response.json"
        );

        FeedOfferMigrationParams params = getParams("json/correctMigration.json");
        ReportResult result = feedOfferMigrationReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();
        verifyStrollerHidings(null, "page_2", "page_2", "page_3");
        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    @Test
    @DisplayName("Магазин мультифидовый. Целевой фид - первый по id")
    @DbUnitDataSet(before = {
            "csv/FeedOfferMigrationReportTest.correct.before.csv",
            "csv/FeedOfferMigrationReportTest.multiFeed.before.csv"
    })
    void migrationMode_multiFeed_success() {
        mockStrollerHidings("proto/stroller.migration.empty.response.json");

        FeedOfferMigrationParams params = getParams("json/correctMigration.json");
        ReportResult result = feedOfferMigrationReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();
        verifyStrollerHidings(new String[]{null});
        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    @Test
    @DisplayName("Магазин мультифидовый. Фид-источник - первый фид. Поэтому в качестве целевого берем второй")
    @DbUnitDataSet(before = {
            "csv/FeedOfferMigrationReportTest.correct.before.csv",
            "csv/FeedOfferMigrationReportTest.multiFeedSecond.before.csv"
    })
    void migrationMode_multiFeedSecond_success() {
        mockStrollerHidings("proto/stroller.migration.empty.response.json");

        FeedOfferMigrationParams params = getParams("json/correctMigration.json");
        ReportResult result = feedOfferMigrationReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();
        verifyStrollerHidings(new String[]{null});
        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    @Test
    @DisplayName("Магазин мультифидовый. Первый фид выключен. Поэтому в качестве целевого берем второй")
    @DbUnitDataSet(before = {
            "csv/FeedOfferMigrationReportTest.correct.before.csv",
            "csv/FeedOfferMigrationReportTest.disabledFeed.before.csv"
    })
    void migrationMode_disabledFeed_success() {
        mockStrollerHidings("proto/stroller.migration.empty.response.json");

        FeedOfferMigrationParams params = getParams("json/correctMigration.json");
        ReportResult result = feedOfferMigrationReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();
        verifyStrollerHidings(new String[]{null});
        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    // первый фид выключен. Берем следующий

    private void mockStrollerHidings(String... responses) {
        String prevToken = null;
        for (String responseFile : responses) {
            if (responseFile == EXCEPTION_HOLDER) {
                Mockito.doThrow(UncheckedIOException.class)
                        .when(dataCampShopClient)
                        .migrateOffersByFeed(eq(BUSINESS_ID), eq(PARTNER_ID), eq(FEED_ID), eq(TARGET_FEED_ID), any(), eq(prevToken));
                continue;
            }

            MigrateFeedOffers.MigrateFeedOffersResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                    MigrateFeedOffers.MigrateFeedOffersResponse.class,
                    responseFile,
                    getClass()
            );

            Mockito.doReturn(mockedResponse)
                    .when(dataCampShopClient)
                    .migrateOffersByFeed(eq(BUSINESS_ID), eq(PARTNER_ID), eq(FEED_ID), eq(TARGET_FEED_ID), any(), eq(prevToken));
            prevToken = mockedResponse.getNextPageToken();
        }
    }

    private void verifyStrollerHidings(String... tokens) {
        for (String token : tokens) {
            Mockito.verify(dataCampShopClient, Mockito.times(1))
                    .migrateOffersByFeed(eq(BUSINESS_ID), eq(PARTNER_ID), eq(FEED_ID), eq(TARGET_FEED_ID), any(), eq(token));
        }
    }

    private void verifyTouchAtTimestamp() {
        ReportInfo<ReportsType> reportInfo = reportsService.getReportInfo(REPORT_ID);
        Assertions.assertNotNull(reportInfo.getTouchedAt());
    }

    private FeedOfferMigrationParams getParams(String paramFile) {
        String json = StringTestUtil.getString(getClass(), paramFile);
        Map<String, Object> paramsMap = ParamsUtils.convertParamsToMap(json);
        return ParamsUtils.convertToParams(
                paramsMap,
                feedOfferMigrationReport.getParamsType()
        );
    }
}
