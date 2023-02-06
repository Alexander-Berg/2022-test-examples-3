package ru.yandex.market.marketId.controller;

import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.marketId.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DbUnitDataSet(before = "../Test.before.csv")
class MarketIdControllerTest extends FunctionalTest {


    @Test
    @DisplayName("Тест полной информации по маркет ИД")
    void testFullInfo() {
        String expected = StringTestUtil.getString(MarketIdControllerTest.class, "json/fullInfo.json");
        final long uid = 100;
        final long marketId = 1;
        ResponseEntity<String> response = FunctionalTestHelper.get(fullInfoUrl(uid, marketId), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONAssert.assertEquals(expected,Objects.requireNonNull(response.getBody()), JSONCompareMode.LENIENT);

    }

    @Test
    @DisplayName("Пользователдь найден. Вернули список маркет ИД")
    @DbUnitDataSet(before = "csv/getMarketIdList.before.csv")
    void testListInfo() {
        String expected = StringTestUtil.getString(MarketIdControllerTest.class, "json/marketAccountList.json");
        final long uid = 100;
        ResponseEntity<String> response = FunctionalTestHelper.get(listUrl(uid), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONAssert.assertEquals(expected,Objects.requireNonNull(response.getBody()), JSONCompareMode.STRICT_ORDER);
    }

    @Test
    @DisplayName("Если не нашли пользователя, отдаем 404")
    void testUidNotFound() {
        String expected = StringTestUtil.getString(MarketIdControllerTest.class, "json/marketAccountListError.json");
        final long uid = 404;
        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(listUrl(uid), String.class)
        );
        assertEquals(
                HttpStatus.NOT_FOUND,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                expected,
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Поиск по uid и по рег номеру. найден 1 маркетИД")
    void testFindByUidAndRegNumberFoundOne() {
        String expected = StringTestUtil.getString(MarketIdControllerTest.class, "json/findByUidAndRegNumberOne.json");
        final long uid = 102;
        final String regNumber = "7654321";
        ResponseEntity<String> response = FunctionalTestHelper.get(findUrl(uid, regNumber), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONAssert.assertEquals(expected,Objects.requireNonNull(response.getBody()), JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("Поиск по uid и по рег номеру. Ничего не найдено")
    void testFindByUidAndRegNumberFoundNothing() {
        String expected = "{\"response\":[],\"errors\":null}";
        final long uid = 102;
        final String regNumber = "765";
        ResponseEntity<String> response = FunctionalTestHelper.get(findUrl(uid, regNumber), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonTestUtil.assertEquals(expected, Objects.requireNonNull(response.getBody()));
    }

    @Test
    @DisplayName("Поиск по uid и по рег номеру. Ничего не найдено. У пользователя нет ни одного маркетИД ")
    void testFindByUidAndRegNumberFoundNothingForUser() {
        String expected = "{\"response\":[],\"errors\":null}";
        final long uid = 1045;
        final String regNumber = "765";
        ResponseEntity<String> response = FunctionalTestHelper.get(findUrl(uid, regNumber), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonTestUtil.assertEquals(expected, Objects.requireNonNull(response.getBody()));
    }



    private String fullInfoUrl(long uid, long marketId) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/marketid/fullInfo?uid={uid}&marketId={marketId}")
                .buildAndExpand(uid, marketId)
                .toUriString();
    }

    private String findUrl(long uid, String regNumber) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/marketid/find?uid={uid}&regNumber={regNumber}")
                .buildAndExpand(uid, regNumber)
                .toUriString();
    }

    private String listUrl(long uid) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/marketid/list/{uid}")
                .buildAndExpand(uid)
                .toUriString();
    }
}
