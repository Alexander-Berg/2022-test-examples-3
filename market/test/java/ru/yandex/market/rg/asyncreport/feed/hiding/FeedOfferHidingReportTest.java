package ru.yandex.market.rg.asyncreport.feed.hiding;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import Market.DataCamp.SyncAPI.SyncHideFeed;
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
 * Тесты для {@link FeedOfferHidingReport}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class FeedOfferHidingReportTest extends FunctionalTest {

    private static final String REPORT_ID = "report_id_123";
    private static final long BUSINESS_ID = 4001L;
    private static final long PARTNER_ID = 1001L;
    private static final long FEED_ID = 2001L;

    private static final String EXCEPTION_HOLDER = "###exception###";

    @Autowired
    private FeedOfferHidingReport feedOfferHidingReport;

    @Autowired
    private ReportsService<ReportsType> reportsService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @ParameterizedTest
    @MethodSource("invalidParamData")
    @DisplayName("Невалидные параметры для скрытия. Будем выброшено исключение")
    @DbUnitDataSet(before = "csv/FeedOfferHidingReportTest.invalidParam.before.csv")
    void reportParams_invalidParam_throwsException(String name, String paramFile, String expectedError) {
        FeedOfferHidingParams params = getParams(paramFile);
        IllegalStateException actualError = Assertions.assertThrows(
                IllegalStateException.class,
                () -> feedOfferHidingReport.generate(REPORT_ID, params)
        );
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    private static Stream<Arguments> invalidParamData() {
        return Stream.of(
                Arguments.of(
                        "Фид, для которого происходит скрытие, принадлежит другому магазину",
                        "json/feedFromOtherPartner.json",
                        "Feed 2002 belongs to another partner. Expected partner: 1001, actual partner: 1002"
                ),
                Arguments.of(
                        "Не задан таймстемп",
                        "json/missedTimestamp.json",
                        "Timestamp is required"
                ),
                Arguments.of(
                        "У партнера нет бизнеса",
                        "json/correctHide.json",
                        "Partner without business: 1001"
                )
        );
    }

    @Test
    @DisplayName("Из строллера не вернулся следующий токен. Сразу закончили работу")
    @DbUnitDataSet(before = "csv/FeedOfferHidingReportTest.hidingMode.before.csv")
    void hidingMode_emptyResponse_success() {
        mockStrollerHidings("proto/stroller.hidings.empty.response.json");

        FeedOfferHidingParams params = getParams("json/correctHide.json");
        ReportResult result = feedOfferHidingReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();
        verifyStrollerHidings(new String[]{null});
        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    @Test
    @DisplayName("Кастомный размер батча")
    @DbUnitDataSet(before = {
            "csv/FeedOfferHidingReportTest.batch.before.csv",
            "csv/FeedOfferHidingReportTest.hidingMode.before.csv"
    })
    void hidingMode_customBatch_success() {
        mockStrollerHidings("proto/stroller.hidings.empty.response.json");

        FeedOfferHidingParams params = getParams("json/correctHide.json");
        ReportResult result = feedOfferHidingReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();

        Mockito.verify(dataCampShopClient, Mockito.times(1))
                .hideOffersByFeed(eq(BUSINESS_ID), eq(PARTNER_ID), eq(FEED_ID), any(Instant.class), eq(500), eq(null));

        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    @Test
    @DisplayName("Несколько страниц в строллере. Прошли по всем")
    @DbUnitDataSet(before = "csv/FeedOfferHidingReportTest.hidingMode.before.csv")
    void hidingMode_severalPagesResponse_success() {
        mockStrollerHidings(
                "proto/stroller.hidings.page1.response.json",
                "proto/stroller.hidings.page2.response.json",
                "proto/stroller.hidings.empty.response.json"
        );

        FeedOfferHidingParams params = getParams("json/correctHide.json");
        ReportResult result = feedOfferHidingReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();
        verifyStrollerHidings(null, "page_2", "page_3");
        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    @Test
    @DisplayName("Несколько страниц в строллере. Во время второй получили исключение. Сработал ретрай. Прошли по всем")
    @DbUnitDataSet(before = "csv/FeedOfferHidingReportTest.hidingMode.before.csv")
    void hidingMode_retryOnException_success() {
        mockStrollerHidings(
                "proto/stroller.hidings.page1.response.json",
                EXCEPTION_HOLDER,
                "proto/stroller.hidings.page2.response.json",
                "proto/stroller.hidings.empty.response.json"
        );

        FeedOfferHidingParams params = getParams("json/correctHide.json");
        ReportResult result = feedOfferHidingReport.generate(REPORT_ID, params);
        Assertions.assertEquals(ReportState.DONE, result.getNewState());

        verifyTouchAtTimestamp();
        verifyStrollerHidings(null, "page_2", "page_2", "page_3");
        Mockito.verifyNoMoreInteractions(dataCampShopClient);
    }

    private void mockStrollerHidings(String... responses) {
        String prevToken = null;
        for (String responseFile : responses) {
            if (responseFile == EXCEPTION_HOLDER) {
                Mockito.doThrow(UncheckedIOException.class)
                        .when(dataCampShopClient)
                        .hideOffersByFeed(eq(BUSINESS_ID), eq(PARTNER_ID), eq(FEED_ID), any(Instant.class), any(), eq(prevToken));
                continue;
            }

            SyncHideFeed.HideFeedResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                    SyncHideFeed.HideFeedResponse.class,
                    responseFile,
                    getClass()
            );

            Mockito.doReturn(mockedResponse)
                    .when(dataCampShopClient)
                    .hideOffersByFeed(eq(BUSINESS_ID), eq(PARTNER_ID), eq(FEED_ID), any(Instant.class), any(), eq(prevToken));
            prevToken = mockedResponse.getNextPageToken();
        }
    }

    private void verifyStrollerHidings(String... tokens) {
        for (String token : tokens) {
            Mockito.verify(dataCampShopClient, Mockito.times(1))
                    .hideOffersByFeed(eq(BUSINESS_ID), eq(PARTNER_ID), eq(FEED_ID), any(Instant.class), any(), eq(token));
        }
    }

    private void verifyTouchAtTimestamp() {
        ReportInfo<ReportsType> reportInfo = reportsService.getReportInfo(REPORT_ID);
        Assertions.assertNotNull(reportInfo.getTouchedAt());
    }

    private FeedOfferHidingParams getParams(String paramFile) {
        String json = StringTestUtil.getString(getClass(), paramFile);
        Map<String, Object> paramsMap = ParamsUtils.convertParamsToMap(json);
        return ParamsUtils.convertToParams(
                paramsMap,
                feedOfferHidingReport.getParamsType()
        );
    }
}
