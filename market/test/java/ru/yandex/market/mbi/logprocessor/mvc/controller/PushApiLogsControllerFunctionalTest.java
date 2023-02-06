package ru.yandex.market.mbi.logprocessor.mvc.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.market.mbi.logprocessor.FunctionalTest;
import ru.yandex.market.mbi.logprocessor.TestUtil;
import ru.yandex.market.mbi.logprocessor.YtInitializer;
import ru.yandex.market.mbi.logprocessor.model.PartnersWithError;
import ru.yandex.market.mbi.logprocessor.storage.yt.model.PushApiLogEntity;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.client.YtClientProxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PushApiLogsControllerFunctionalTest extends FunctionalTest {
    @Autowired
    private BindingTable<PushApiLogEntity> pushApiLogsTableV2;

    @Autowired
    @Qualifier("pushApiLogTargetYt")
    private YtClientProxy targetYt;

    @Autowired
    Cache<Long, Boolean> localPartnerErrorsCache;

    private YtInitializer ytInitializer;

    @BeforeEach
    public void setup() {
        ytInitializer = new YtInitializer(targetYt);
        ytInitializer.initializeFromFile("data/pushapilogs/push-api-inserts.csv", pushApiLogsTableV2);
    }

    @AfterEach
    public void clean() {
        ytInitializer.cleanTable(pushApiLogsTableV2);
        localPartnerErrorsCache.invalidateAll();
    }

    @DisplayName("Поиск по фильтру. Должен вернуть пустой список.")
    @Test
    void testPushApiLogsGet_findShortData_empty_list() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1&" +
                "fromDate=01.01.2020 00:00:00&toDate=02.01.2020 00:00:00", String.class);
        String expectedResult = TestUtil.readString("asserts/pushapi/findShortDataEmptyResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Поиск по фильтру.")
    @Test
    void testPushApiLogsGet_findShortData() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1&" +
                "fromDate=01.05.2020 00:00:00&toDate=31.05.2020 00:00:00", String.class);
        String expectedResult = TestUtil.readString("asserts/pushapi/findShortDataResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Поиск по фильтру c requestBody")
    @Test
    void testPushApiLogsGet_findShortData_requestBody() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=7&" +
                "fromDate=01.05.2020 00:00:00&toDate=31.05.2020 00:00:00" +
                "&body=ADAM Superior Men's Multi 90 капс (NOW)", String.class);
        String expectedResult = TestUtil.readString("asserts/pushapi/findShortDataResponseReqBody.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Поиск по фильтру. Передача даты с таймзоной.")
    @Test
    void testPushApiLogsGet_findShortDataTimezone() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1&" +
                        "fromDate={toDate}&toDate={toDate}", String.class, "2020-05-01T07:00:00+10:00",
                "2020-05-31T07:00:00+10:00");
        String expectedResult = TestUtil.readString("asserts/pushapi/findShortDataResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Поиск по фильтру. Проверка пагинации.")
    @Test
    void testPushApiLogsGet_findShortData_with_paging() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1&" +
                "fromDate=01.05.2020 00:00:00&toDate=31.05.2020 00:00:00&pageSize=1", String.class);
        String page1ExpectedResult = TestUtil.readString("asserts/pushapi/findShortDataPage1Response.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        MbiAsserts.assertJsonEquals(page1ExpectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));

        response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1&" +
                "fromDate=01.05.2020 00:00:00&toDate=31.05.2020 00:00:00&pageSize=1&page=2", String.class);
        String page2ExpectedResult = TestUtil.readString("asserts/pushapi/findShortDataPage2Response.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        body = response.getBody();
        MbiAsserts.assertJsonEquals(page2ExpectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Поиск по фильтру с указанием responseSubError.")
    @Test
    void testPushApiLogsGet_findShortDataWithError() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=2&" +
                "fromDate=01.05.2020 00:00:00&toDate=31.05.2020 00:00:00&responseSubError=SSL_ERROR", String.class);
        String expectedResult = TestUtil.readString("asserts/pushapi/findShortDataWithErrorResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Поиск только ошибок.")
    @Test
    void testPushApiLogsGet_findShortDataWithErrors() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=2&" +
                "fromDate=01.05.2020 00:00:00&toDate=31.05.2020 00:00:00&success=false", String.class);
        String expectedResult = TestUtil.readString("asserts/pushapi/findShortDataWithErrorsResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Поиск по уникальному идентификатору.")
    @Test
    void testPushApiLogsGet_findById() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1&" +
                "requestDate=1589546668000&resource=/cart&eventId=event2", String.class);
        String expectedResult = TestUtil.readString("asserts/pushapi/findByIdResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Поиск по уникальному идентификатору. Должен вернуть пустой список, т.к. запись была не найдена.")
    @Test
    void testPushApiLogsGet_findById_not_found() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1&" +
                "requestDate=1589783598000&resource=/cart&eventId=event3", String.class);
        String expectedResult = TestUtil.readString("asserts/pushapi/findByIdEmptyResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, body, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Указаны неправильные фильтры. Должна возвращаться ошибка.")
    @Test
    void testPushApiLogsGet_notEnoughFilters() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1",
                String.class);

        String expectedResult = TestUtil.readString("asserts/pushapi/notEnoughFiltersResponse.json");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String apiError = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, apiError, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Указан слишком большой период для поиска. Должна возвращаться ошибка.")
    @Test
    void testPushApiLogsGet_tooLongDatesRange() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1&" +
                        "fromDate=01.01.2020 00:00:00&toDate=11.04.2020 00:00:00",
                String.class);

        String expectedResult = TestUtil.readString("asserts/pushapi/tooLongDatesRangeResponse.json");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String apiError = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, apiError, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Указан слишком большой размер страницы. Должна возвращаться ошибка.")
    @Test
    void testPushApiLogsGet_invalidPager() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs?shopId=1&" +
                        "fromDate=01.01.2020 00:00:00&toDate=01.02.2020 00:00:00&pageSize=101",
                String.class);

        String expectedResult = TestUtil.readString("asserts/pushapi/invalidPagerResponse.json");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String apiError = response.getBody();
        MbiAsserts.assertJsonEquals(expectedResult, apiError, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    @DisplayName("Получение статистики по логам")
    @Test
    void testPushapiLogsStatsGet() {
        Instant date = LocalDateTime.of(2020, 5, 18, 21, 0, 0)
                .atZone(ZoneId.systemDefault()).toInstant();
        when(clock.instant()).thenReturn(date);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs/stats?shopId=2" +
                "&fromDate=01.05.2020 00:00:00", String.class);

        String expectedResult = TestUtil.readString("asserts/pushapi/logStatsResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        MbiAsserts.assertJsonEquals(expectedResult, response.getBody(), List.of(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*")),
                new Customization("minEventTime", new RegularExpressionValueMatcher<>("\\d")),
                new Customization("maxEventTime", new RegularExpressionValueMatcher<>("\\d"))));
    }

    @DisplayName("Получение статистики по логам. Передача даты с таймзоной")
    @Test
    void testPushapiLogsStatsGetWithTimezone() {
        Instant date = LocalDateTime.of(2020, 5, 18, 21, 0, 0)
                .atZone(ZoneId.systemDefault()).toInstant();
        when(clock.instant()).thenReturn(date);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs/stats?shopId=2" +
                "&fromDate={fromDate}", String.class, "2020-05-01T00:00:00+10:00");

        String expectedResult = TestUtil.readString("asserts/pushapi/logStatsResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        MbiAsserts.assertJsonEquals(expectedResult, response.getBody(), List.of(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*")),
                new Customization("minEventTime", new RegularExpressionValueMatcher<>("\\d")),
                new Customization("maxEventTime", new RegularExpressionValueMatcher<>("\\d"))));
    }

    @DisplayName("Получение статистики по логам для несуществующего магазина.")
    @Test
    void testPushapiLogsStatsGet_not_found() {
        Instant date = LocalDateTime.of(2020, 5, 18, 21, 0, 0)
                .atZone(ZoneId.systemDefault()).toInstant();
        when(clock.instant()).thenReturn(date);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs/stats?shopId=4" +
                "&fromDate=01.05.2020 00:00:00", String.class);

        String expectedResult = TestUtil.readString("asserts/pushapi/logStatsEmptyResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        MbiAsserts.assertJsonEquals(expectedResult, response.getBody(), List.of(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*")),
                new Customization("minEventTime", new RegularExpressionValueMatcher<>("\\d")),
                new Customization("maxEventTime", new RegularExpressionValueMatcher<>("\\d"))));
    }

    @DisplayName("Получение статистики по логам с момента последнего события.")
    @Test
    void testPushapiLogsStatsFromLastEventGet() {
        Instant date = LocalDateTime.of(2020, 5, 18, 21, 0, 0)
                .atZone(ZoneId.systemDefault()).toInstant();
        when(clock.instant()).thenReturn(date);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        ResponseEntity<String> response = testRestTemplate.getForEntity("/pushapi/logs/stats/fromLastEvent" +
                "?shopId=3&period=PT30m", String.class);
        String expectedResult = TestUtil.readString("asserts/pushapi/logStatsFromLastEventResponse.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        MbiAsserts.assertJsonEquals(expectedResult, response.getBody(), List.of(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*")),
                new Customization("minEventTime", new RegularExpressionValueMatcher<>("\\d")),
                new Customization("maxEventTime", new RegularExpressionValueMatcher<>("\\d"))));
    }

    @ParameterizedTest(name = "{arguments}")
    @DisplayName("Получение партнеров, у которых есть ошибки в push логах за последние полчаса")
    @EnumSource(PartnerErrors.class)
    void testPartnerErrors(PartnerErrors testCase) {
        testCase.mock.accept(memCachingService);
        List<Long> response = getIfPartnerWithErrors(testCase.partnerIds);
        assertEquals(testCase.expectedResult, response);
    }

    @Test
    @DisplayName("Тест ручки Получение партнеров, у которых есть ошибки в push логах за последние полчаса и " +
            "кэша для нее")
    void testPartnerErrorsWithoutCleaningCache() {
        //call on 1, 2, 1, 2, 2 и 1
        Mockito.when(memCachingService.query(any(), any())).thenReturn(null, null, true, null, null);

        List<Long> partnerIds = List.of(1L, 2L);
        List<Long> response;
        response = getIfPartnerWithErrors(partnerIds);
        assertEquals(response, Collections.emptyList());
        assertEquals(0, localPartnerErrorsCache.size());
        //1 и 2 нет в локальном кэше

        response = getIfPartnerWithErrors(partnerIds);
        assertEquals(response, List.of(1L));
        assertEquals(1, localPartnerErrorsCache.size());
        //1 должен появиться в локальном кэше и локальный кэш не должен больше ходить в memcache за 1

        response = getIfPartnerWithErrors(partnerIds);
        assertEquals(response, List.of(1L));
        assertEquals(1, localPartnerErrorsCache.size());
        //1 должен остаться в локальном кэше и локальный кэш не должен больше ходить в memcache за 1
    }

    private List<Long> getIfPartnerWithErrors(List<Long> partnerIds) {
        return testRestTemplate.getForObject(
                "/pushapi/logs/hasErrorsInHalfAnHour?partnerIds=" + listToQuery(partnerIds),
                PartnersWithError.class).getPartnersWithError();
    }

    private String listToQuery(List<Long> partnerIds) {
        return partnerIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private enum PartnerErrors {
        ALL_WITH_ERRORS(List.of(1L, 2L, 3L), List.of(1L, 2L, 3L), "Все партнеры с ошибками",
                m -> Mockito.when(m.query(any(), any())).thenReturn(true)),
        NO_ONE_WITH_ERRORS(List.of(4L, 5L, 6L), Collections.emptyList(), "Все партнеры без ошибок",
                m -> Mockito.when(m.query(any(), any())).thenReturn(null)),
        ONE_WITH_ERROR_ONE_WITHOUT(List.of(1L, 2L), List.of(2L), "Один с ошибкой, другой без",
                m -> {
                    Mockito.when(m.query(any(), eq(1L))).thenReturn(null);
                    Mockito.when(m.query(any(), eq(2L))).thenReturn(true);
                }),
        ONE_WITH_ERROR(List.of(1L), List.of(1L), "Один партнер с ошибкой",
                m -> Mockito.when(m.query(any(), any())).thenReturn(true)),
        ONE_WITHOUT_ERROR(List.of(1L), Collections.emptyList(), "Один партнер без ошибки",
                m -> Mockito.when(m.query(any(), any())).thenReturn(null));


        List<Long> partnerIds;
        List<Long> expectedResult;
        String description;
        Consumer<MemCachingService> mock;

        PartnerErrors(List<Long> partnerIds, List<Long> expectedResult, String description,
                      Consumer<MemCachingService> mock) {
            this.partnerIds = partnerIds;
            this.expectedResult = expectedResult;
            this.description = description;
            this.mock = mock;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
