package ru.yandex.market.partner.mvc.controller.indexer;

import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.environment.TestEnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Тесты для {@link IndexingReportController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "csv/IndexingReportControllerTest.before.csv")
class IndexingReportControllerTest extends FunctionalTest {

    private static final long FEED_ID = 12345;
    private static final long NONEXISTENT_FEED = 987654321;
    private static final long META_ID = 1613;
    private static final long CAMPAIGN_ID = 11001;
    private static final long SMB_CAMPAIGN_ID = 11003;
    private static final long ANOTHER_CAMPAIGN_ID = 11002;
    private static final String SESSION_NAME = "20192810";
    private static final String NONEXISTENT_SESSION_NAME = "20070109";
    private static final String SESSION_WITH_ERRORS_NAME = "20192710";

    @Autowired
    private TestEnvironmentService environmentService;

    @BeforeEach
    void init() {
        environmentService.setEnvironmentType(EnvironmentType.TESTING);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("environment");
    }

    @Test
    @DisplayName("Прямой сценаний, детализация получена успешно")
    @DbUnitDataSet(before = "csv/testGetDetailsOk.before.csv")
    void testGetDetailsOk() {
        final ResponseEntity<String> response = callGetDetails(CAMPAIGN_ID, FEED_ID, META_ID, SESSION_WITH_ERRORS_NAME);
        JsonTestUtil.assertEquals(response, getClass(), "json/expectedErrors.json");
    }

    @Test
    @DisplayName("Прямой сценаний, ошибок и предупреждений нет")
    @DbUnitDataSet(before = "csv/testGoodFeedSession.before.csv")
    void testGoodFeedSession() {
        final ResponseEntity<String> response = callGetDetails(CAMPAIGN_ID, FEED_ID, META_ID, SESSION_NAME);
        JsonTestUtil.assertEquals(response, getClass(), "json/expectedEmptyErrors.json");
    }

    @Test
    @DisplayName("Есть запись в feed log history, но в feed session нет")
    void testGetDetailsFeedSessionIsNull() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> callGetDetails(CAMPAIGN_ID, FEED_ID, META_ID, NONEXISTENT_SESSION_NAME))
                .satisfies(e -> MatcherAssert.assertThat(e, HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)));
    }

    @Test
    @DisplayName("Нет записей в feed log history")
    void testGetDetailsFeedLogHistoryIsEmpty() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> callGetDetails(CAMPAIGN_ID, 12346, META_ID, SESSION_NAME))
                .satisfies(e -> MatcherAssert.assertThat(e, HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)));
    }


    @Test
    @DisplayName("Прямой сценарий, получаем три вида пагинаций для изменения цен")
    @DbUnitDataSet(before = "csv/testGetDiff.before.csv")
    void testGetDiff() {
        final ResponseEntity<String> first = callReportFeedDiffHistory(CAMPAIGN_ID, FEED_ID, 1, 4);
        final ResponseEntity<String> second = callReportFeedDiffHistory(CAMPAIGN_ID, FEED_ID, 2, 3);
        final ResponseEntity<String> one = callReportFeedDiffHistory(CAMPAIGN_ID, FEED_ID, 1, 100);

        JsonTestUtil.assertEquals(first, getClass(), "json/firstPageDiffExpected.json");
        JsonTestUtil.assertEquals(second, getClass(), "json/secondPageDiffExpected.json");
        JsonTestUtil.assertEquals(one, getClass(), "json/onePageDiffExpected.json");
    }

    @Test
    @DisplayName("Только последние 7 дней")
    @DbUnitDataSet(before = "csv/testGetDiffDays.before.csv")
    void testGetDiffDays() {
        var responseEntity = callReportFeedDiffHistory(CAMPAIGN_ID, FEED_ID, 1, 100);
        var jsonArray = JsonTestUtil.parseJson(responseEntity.getBody())
                .getAsJsonObject().get("result")
                .getAsJsonObject().get("history")
                .getAsJsonArray();
        assertThat(IntStream.range(0, jsonArray.size())
                .mapToObj(i -> jsonArray.get(i)
                        .getAsJsonObject()
                        .get("generationId")
                        .getAsLong()
                )
        ).containsOnly(4L, 5L);
    }

    @Test
    @DisplayName("Прямой сценарий, получаем две пагинации для полных поколений")
    @DbUnitDataSet(before = "csv/testGetFull.before.csv")
    void testGetFull() {
        final ResponseEntity<String> response = callReportFeedFullHistory(CAMPAIGN_ID, FEED_ID);
        JsonTestUtil.assertEquals(response, getClass(),
                "json/testGetFull.json");
    }

    @Test
    @DisplayName("Прямой сценарий, получаем историю изменения цен для фида у которого еще нет записей в feed log history")
    void testGetEmptyDiffList() {
        final ResponseEntity<String> diffResponse = callReportFeedDiffHistory(CAMPAIGN_ID, 12344, 1, 10);
        JsonTestUtil.assertEquals(diffResponse, getClass(),
                "json/testGetEmptyDiffList.json");
    }

    @Test
    @DisplayName("Прямой сценарий, получаем историю полных индексаций для фида у которого еще нет записей в feed log history")
    void testGetEmptyFullList() {
        final ResponseEntity<String> response = callReportFeedFullHistory(CAMPAIGN_ID, 12344);
        JsonTestUtil.assertEquals(response, getClass(),
                "json/testGetEmptyFullList.json");
    }

    @Test
    @DisplayName("Размер страницы больше максимального")
    void testPageSizeIsTooLarge() {
        var tooLargePageSize = Integer.MAX_VALUE;
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> callReportFeedDiffHistory(CAMPAIGN_ID, FEED_ID, 1, tooLargePageSize));
    }

    private ResponseEntity<String> callReportFeedDiffHistory(long campaignId, long feedId, int page, int pageSize) {
        var url = "idx/report/feed/history/diff?" +
                "campaign_id={campaign_id}&feed_id={feed_id}&page={page}&pageSize={pageSize}";
        return FunctionalTestHelper.get(baseUrl + url, campaignId, feedId, page, pageSize);
    }

    private ResponseEntity<String> callReportFeedFullHistory(long campaignId, long feedId) {
        var url = "idx/report/feed/history/full?" +
                "campaign_id={campaign_id}&feed_id={feed_id}";
        return FunctionalTestHelper.get(baseUrl + url, campaignId, feedId);
    }

    private ResponseEntity<String> callGetDetails(long campaignId, long feedId, long generationId, String sessionName) {
        var url = "/idx/report/feed/details?" +
                "campaign_id={campaign_id}&feed_id={feed_id}&generation_id={generation_id}&session_name={session_name}";
        return FunctionalTestHelper.get(baseUrl + url, campaignId, feedId, generationId, sessionName);
    }

    @Test
    @DisplayName("Фид существует, но принадлежит другой компании")
    void testAnotherCampaignFeed() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> callGetDetails(ANOTHER_CAMPAIGN_ID, FEED_ID, META_ID, "sessionName"))
                .satisfies(e -> MatcherAssert.assertThat(e, HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)));
    }

    @Test
    @DisplayName("Фид не существует, при запросе детализации")
    void testFeedDoesNotExistDuringGetDetails() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> callGetDetails(ANOTHER_CAMPAIGN_ID, NONEXISTENT_FEED, META_ID, "sessionName"))
                .satisfies(e -> MatcherAssert.assertThat(e, HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)));
    }

    @Test
    @DisplayName("Фид не существует, при запросе истории")
    void testFeedDoesNotExistDuringGetHistory() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> callReportFeedDiffHistory(ANOTHER_CAMPAIGN_ID, NONEXISTENT_FEED, 1, 10))
                .satisfies(e -> MatcherAssert.assertThat(e, HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)));
    }

    @Test
    @DisplayName("Ссылка на фид из архива. Фид загружен через файл")
    @DbUnitDataSet(before = "csv/testFeedArchiveFile.before.csv")
    void testFeedArchiveFile() {
        final ResponseEntity<String> response = callFeedArchive(13344L, CAMPAIGN_ID, 123L, "123_456");
        JsonTestUtil.assertEquals(response, getClass(),
                "json/testFeedArchiveFile.json");
    }

    @Test
    @DisplayName("Ссылка на фид из архива. Фид загружен через ссылку")
    @DbUnitDataSet(before = "csv/testFeedArchiveUrl.before.csv")
    void testFeedArchiveUrl() {
        final ResponseEntity<String> response = callFeedArchive(12344L, CAMPAIGN_ID, 123L, "123_456");
        JsonTestUtil.assertEquals(response, getClass(),
                "json/testFeedArchiveUrl.json");
    }

    @Test
    @DisplayName("Ссылка на фид из архива. Фид без ссылки")
    @DbUnitDataSet(before = "csv/testFeedArchiveEmpty.before.csv")
    void testFeedArchiveEmpty() {
        final ResponseEntity<String> response = callFeedArchive(12344L, CAMPAIGN_ID, 123L, "123_456");
        JsonTestUtil.assertEquals(response, getClass(),
                "json/testFeedArchiveEmpty.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/testFeedArchiveDefault.before.csv")
    @DisplayName("Ссылка на фид из архива. Дефолтные поколения и сессия")
    void testFeedArchiveDefault() {
        final ResponseEntity<String> response = callFeedArchive(12344L, CAMPAIGN_ID, null, null);
        JsonTestUtil.assertEquals(response, getClass(),
                "json/testFeedArchiveDefault.json");
    }

    private ResponseEntity<String> callFeedArchive(final long feedId, final long campaignId, final Long generationId,
                                                   final String sessionName) {
        final String url = baseUrl + "/idx/report/feed/archive" +
                "?feed_id={feed_id}&session_name={session_name}&id={campaign_id}&generation_id={generation_id}";
        return FunctionalTestHelper.get(url, feedId, sessionName, campaignId, generationId);
    }

    @Test
    @DisplayName("Сводная информация по фидам. Магазин без фидов")
    void testFeedSummaryWithoutFeeds() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11002");
        JsonTestUtil.assertEquals(result, "[]");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Фид сломан в последней индексации")
    @DbUnitDataSet(before = "csv/testFeedSummaryBrokenFeed.before.csv")
    void testFeedSummaryBrokenFeed() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11001");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedSummaryBrokenFeed.json");
    }

    @Test
    @DisplayName("Названия фидов переводятся в кириллицу")
    @DbUnitDataSet(before = "csv/testFeedName.before.csv")
    void testFeedName() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11002");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedName.json");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Магазин с фидом без feed_status")
    @DbUnitDataSet(before = "csv/testFeedSummaryWithoutFeedStatus.before.csv")
    void testFeedSummaryWithoutFeedStatus() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11001");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedSummaryWithoutFeedStatus.json");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Магазин с фидом, с feed_status, но не было НЕ фатальных индексаций (= фид еще ни разу нормально не индексировался)")
    @DbUnitDataSet(before = "csv/testFeedSummaryWithoutNonFatalGeneration.before.csv")
    void testFeedSummaryWithoutNonFatalGeneration() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11001");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedSummaryWithoutNonFatalGeneration.json");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Магазин с фидом и с feed_status")
    @DbUnitDataSet(before = "csv/testFeedSummaryWithFeedStatus.before.csv")
    void testFeedSummaryWithFeedStatus() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11001");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedSummaryWithFeedStatus.json");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Магазин с фидом и с feed_status. Запрос конкретного фида")
    @DbUnitDataSet(before = "csv/testFeedSummaryWithFeedStatus.before.csv")
    void testFeedSummaryWithSingleFeedStatus() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(
                baseUrl + "/idx/report/summary?id=11001&feed_id=12346"
        );
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedSummaryWithSingleFeedStatus.json");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Магазин с фидом и с feed_status. Фид выпал из индекса. Была фатальная ошибка")
    @DbUnitDataSet(before = "csv/testFeedNotInIndexWithError.before.csv")
    void testFeedNotInIndexWithError() {
        final ResponseEntity<String> result =
                FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11001&feed_id=12346");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedNotInIndexWithError.json");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Фид был в песочном, а сейчас в боевом индексе. Из песочного еще не выпал")
    @DbUnitDataSet(before = "csv/testFeedSummaryPsMainIndexes.before.csv")
    void testFeedSummaryPsMainIndexes() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11002");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedSummaryPsMainIndexes.json");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Фид не в индексе. Идентификатор поколения песочного отсутствует")
    @DbUnitDataSet(before = "csv/testFeedSummaryPsNullMainNotIndexes.before.csv")
    void testFeedSummaryPsNullMainNotIndexes() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11002");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedSummaryPsNulMainNotIndexes.json");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Фид не в индексе. Идентификатор поколения боевого отсутствует")
    @DbUnitDataSet(before = "csv/testFeedSummaryPsMainNullNotIndexes.before.csv")
    void testFeedSummaryPsMainNullNotIndexes() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11002");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedSummaryPsMainNullNotIndexes.json");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Фид не в индексе. Идентификаторы поколений отсутствуют")
    @DbUnitDataSet(before = "csv/testFeedSummaryPsNullMainNullNotIndexes.before.csv")
    void testFeedSummaryPsNullMainNullNotIndexes() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11002");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedSummaryPsNullMainNullNotIndexes.json");
    }

    @Test
    @DisplayName("Сводная информация по фидам. Фид выпал из песочного и боевого индекса")
    @DbUnitDataSet(before = "csv/testFeedSummaryNotPsMainIndexes.before.csv")
    void testFeedSummaryNotPsMainIndexes() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=11002");
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testFeedSummaryNotPsMainIndexes.json");
    }

    @Test
    @DisplayName("Сводная информация по дефолтному фиду для обычных (не SMB) партнеров возвращается для push схем")
    @DbUnitDataSet(before = "csv/testDefaultFeedSummaryNonSmb.before.csv")
    void testDefaultFeedSummaryPushNonSmb() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(baseUrl + "/idx/report/summary?id=" + 11004);
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testDefaultFeedSummaryPushNonSmb.json");
    }

    @Test
    @DisplayName("Сводная информация по дефолтному фиду для обычных (не SMB) партнеров не возвращается для pull схем")
    @DbUnitDataSet(before = "csv/testDefaultFeedSummaryNonSmb.before.csv")
    void testDefaultFeedSummaryPullNonSmb() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(
                baseUrl + "/idx/report/summary?id=" + SMB_CAMPAIGN_ID
        );
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testDefaultFeedSummaryNonSmb.json");
    }

    @Test
    @DisplayName("Сводная информация по дефолтному фиду для SMB-партнеров типа PUSH возвращается.")
    @DbUnitDataSet(before = "csv/testDefaultFeedSummarySmb.before.csv")
    void testDefaultFeedSummaryPushSmb() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(
                baseUrl + "/idx/report/summary?id=" + SMB_CAMPAIGN_ID
        );
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testDefaultFeedSummarySmb.json");
    }

    @Test
    @DisplayName("Сводная информация по дефолтному фиду для SMB-партнеров типа PULL не возвращается.")
    @DbUnitDataSet(before = "csv/testDefaultFeedSummarySmb.before.csv")
    void testDefaultFeedSummaryPullSmb() {
        final ResponseEntity<String> result = FunctionalTestHelper.get(
                baseUrl + "/idx/report/summary?id=" + 11005
        );
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testDefaultFeedSummarySmbNonPush.json");
    }

    @Test
    @DisplayName("История полных поколений, для опубликованной сессии счетчики должны браться из feed_log_history," +
            "для неопубликованной - из feed_session")
    @DbUnitDataSet(before = "csv/testGetFullHistoryPublished.before.csv")
    void testGetFullHistoryPublished() {
        final ResponseEntity<String> result = callReportFeedFullHistory(CAMPAIGN_ID, FEED_ID);
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testGetFullHistoryPublished.json");
    }

    @Test
    @DisplayName("История полных поколений, planeshift не должен быть ACTUAL")
    @DbUnitDataSet(before = "csv/testGetFullHistoryPlaneshiftNotActual.before.csv")
    void testGetFullHistoryPlaneshiftNotActual() {
        final ResponseEntity<String> result = callReportFeedFullHistory(CAMPAIGN_ID, FEED_ID);
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testGetFullHistoryPlaneshiftNotActual.json");
    }

    @Test
    @DisplayName("История полных поколений, не показывать сессии в которых не было парсинга")
    @DbUnitDataSet(before = "csv/testGetFullHistoryNotParsed.before.csv")
    void testGetFullHistoryNotParsed() {
        final ResponseEntity<String> result = callReportFeedFullHistory(CAMPAIGN_ID, FEED_ID);
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testGetFullHistoryNotParsed.json");
    }

    @Test
    @DisplayName("История полных поколений, диффы не должны попадать в историю")
    @DbUnitDataSet(before = "csv/testGetFullHistoryNotParsed.before.csv")
    void testGetFullHistoryDiff() {
        final ResponseEntity<String> result = callReportFeedFullHistory(CAMPAIGN_ID, FEED_ID);
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testGetFullHistoryNotParsed.json");
    }

    @Test
    @DisplayName("История полных поколений, записи с retcode FATAL не должны показываться пользователю")
    @DbUnitDataSet(before = "csv/testGetFullHistoryNotParsed.before.csv")
    void testGetFullHistoryFatalRetcode() {
        final ResponseEntity<String> result = callReportFeedFullHistory(CAMPAIGN_ID, FEED_ID);
        JsonTestUtil.assertEquals(result, IndexingReportControllerTest.class,
                "json/testGetFullHistoryFatalRetcode.json");
    }
}
