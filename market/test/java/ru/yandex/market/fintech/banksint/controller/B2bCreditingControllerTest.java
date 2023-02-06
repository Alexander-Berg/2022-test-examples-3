package ru.yandex.market.fintech.banksint.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.model.ScoringData;
import ru.yandex.market.fintech.banksint.util.JsonUtils;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class B2bCreditingControllerTest extends FunctionalTest {
    private static final String LOAN_REQUEST_URL = "/b2b_crediting/loan_request?bank_id={bankId}&shop_tin={shopTin}";
    private static final String SCORING_DATA_BASE_URL = "/b2b_crediting/scoring_data?bank_id={bankId}&shop_tin" +
            "={shopTin}" +
            "&start_date={startDate}&end_date={endDate}";

    @Test
    @DisplayName("Тест на сохранение запросов на кредит в БД")
    @DbUnitDataSet(after = "LoanRequestPostTest.after.csv")
    void loanRequestPostTest() {
        var queryParams = new HashMap<>(Map.of(
                "bankId", "139",
                "shopTin", "testShopTin"
        ));

        var responses = new ArrayList<ResponseEntity<String>>();
        //проверка что запись в БД не задублируется
        responses.add(testRestTemplate.postForEntity(LOAN_REQUEST_URL, null, String.class, queryParams));
        responses.add(testRestTemplate.postForEntity(LOAN_REQUEST_URL, null, String.class, queryParams));

        queryParams.put("bankId", "140");

        responses.add(testRestTemplate.postForEntity(LOAN_REQUEST_URL, null, String.class, queryParams));

        for (var response : responses) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("Тест на получение ссылки на тинькофф по определенному идентификатору")
    void loanRequestTinkoffUrlTest() {
        var queryParams = new HashMap<>(Map.of(
                "bankId", "111281",
                "shopTin", "testShopTin"
        ));

        var response = testRestTemplate
                .postForEntity(LOAN_REQUEST_URL, null, String.class, queryParams);

        assertStatusCode(HttpStatus.OK, response);
        assertTrue(response.getBody().contains("tinkoff"));
    }

    @Test
    @DbUnitDataSet(before = "ScoringDataGetTest_Success.before.csv")
    @DisplayName("Успешное получение первой страницы выдачи без токена пагинации")
    void scoringDataGetTest_Success() {
        prepareScoringData();

        var queryParams = Map.of(
                "bankId", "150",
                "shopTin", "testShopTin",
                "startDate", "",
                "endDate", "2021-07-23"
        );

        var response = testRestTemplate.getForEntity(SCORING_DATA_BASE_URL,
                String.class, queryParams);

        assertStatusCode(HttpStatus.OK, response);

        var expectedResponseJson = readClasspathFile("ScoringDataGetTest_Success.expected.json");
        assertThatJson(response.getBody()).isEqualTo(expectedResponseJson);
    }

    @Test
    @DbUnitDataSet(before = "ScoringDataGetTest_Success.before.csv")
    @DisplayName("Успешное получение первой страницы выдачи без токена пагинации")
    void scoringDataGetTest_SuccessWithEmptyRecord() {
        prepareScoringData();

        var queryParams = Map.of(
                "bankId", "150",
                "shopTin", "emptyTestShopTin",
                "startDate", "2021-07-20",
                "endDate", "2021-07-23"
        );

        var response = testRestTemplate.getForEntity(SCORING_DATA_BASE_URL,
                String.class, queryParams);

        assertStatusCode(HttpStatus.OK, response);

        var expectedResponseJson = readClasspathFile("ScoringDataGetTest_SuccessEmptyRecord.expected.json");
        assertThatJson(response.getBody()).isEqualTo(expectedResponseJson);
    }

    @Test
    @DbUnitDataSet(before = "ScoringDataGetTest_Success.before.csv")
    @DisplayName("Успешное получение второй страницы выдачи по токену пагинации от первой страницы")
    void scoringDataGetTest_SecondPageToken() throws IOException {
        prepareScoringData();

        var queryParams = Map.of(
                "bankId", "150",
                "shopTin", "testShopTin",
                "startDate", "",
                "endDate", "2021-07-23",
                "pageSize", "2"
        );

        var firstPageUrl = SCORING_DATA_BASE_URL + "&page_size={pageSize}";
        var firstPageResponse = testRestTemplate.getForEntity(firstPageUrl, String.class, queryParams);

        assertStatusCode(HttpStatus.OK, firstPageResponse);
        var firstPageResponseJson = JsonUtils.getJsonMapper()
                .readValue(firstPageResponse.getBody(), ru.yandex.market.fintech.banksint.model.ScoringData.class);
        var nextPageToken = firstPageResponseJson.getPaging().getNextPageToken();

        assertNotNull(nextPageToken, "Second page pageToken is null");

        var secondPageQueryParams = new HashMap<>(queryParams);
        secondPageQueryParams.put("pageToken", nextPageToken);
        var secondPageUrl = firstPageUrl + "&page_token={pageToken}";

        var secondPageResponse = testRestTemplate.getForEntity(secondPageUrl, String.class, secondPageQueryParams);

        assertStatusCode(HttpStatus.OK, secondPageResponse);
        var expectedResponseJson = readClasspathFile("scoringDataGetTest_SecondPageToken.expected.json");
        assertThatJson(secondPageResponse.getBody()).isEqualTo(expectedResponseJson);
    }

    @Test
    @DbUnitDataSet(before = "ScoringDataGetTest_Success.before.csv")
    @DisplayName("Переход по последнему токену должен возвращать пустую страницу")
    void scoringDataGetTest_LastPageToken() {
        prepareScoringData();

        var queryParams = Map.of(
                "bankId", "150",
                "shopTin", "testShopTin",
                "startDate", "",
                "endDate", "2021-07-23",
                "pageSize", "2",
                "pageToken", "eyJvcCI6Ij4iLCJrZXkiOnsiZHQiOiIyMDIxLTA3LTIzIiwiaWQiOjF9LCJza2lwIjowfQ"
        );

        var url = SCORING_DATA_BASE_URL + "&page_size={pageSize}&page_token={pageToken}";
        var response = testRestTemplate.getForEntity(url, String.class, queryParams);

        assertStatusCode(HttpStatus.OK, response);
        var expectedResponse = readClasspathFile("scoringDataGetTest_LastPageToken.expected.json");
        assertThatJson(response.getBody()).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("Тест запроса несуществующего ИНН")
    void scoringDataGet_NotFound() {
        var queryParams = new HashMap<>(Map.of(
                "bankId", "151",
                "shopTin", "testShopTin1",
                "startDate", "",
                "endDate", ""
        ));

        var response = testRestTemplate.getForEntity(SCORING_DATA_BASE_URL,
                String.class, queryParams);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertThatJson(response.getBody())
                .isEqualTo(errorResponse(
                        "Credit request doesn't exist for 'bank_id' = 151 and 'shop_tin' = testShopTin1"
                ));
    }

    @Test
    @DisplayName("Сервис возвращает 400, если не передан shop_tin")
    void scoringDataGet_TinIsMissing() {
        var queryParams = new HashMap<>(Map.of(
                "bankId", "151"
        ));

        var response = testRestTemplate.getForEntity(
                "/b2b_crediting/scoring_data?bank_id={bankId}",
                String.class, queryParams);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertThatJson(response.getBody())
                .isEqualTo(errorResponse("Required String parameter 'shop_tin' is not present"));
    }

    @Test
    @DisplayName("Сервис возвращает 400, если не передан bank_id")
    void scoringDataGet_BankIdIsMissing() {
        var response = testRestTemplate.getForEntity("/b2b_crediting/scoring_data", String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertThatJson(response.getBody())
                .isEqualTo(errorResponse("Required Integer parameter 'bank_id' is not present"));
    }

    @Test
    @DisplayName("Сервис возвращает 400, если передан пустой shop_tin")
    void scoringDataGet_ShopTinIsEmpty() {
        var response = testRestTemplate.getForEntity(
                "/b2b_crediting/scoring_data?bank_id=151&shop_tin=",
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertThatJson(response.getBody())
                .isEqualTo(errorResponse("'shop_tin' cannot be empty"));
    }

    @Test
    @DisplayName("Сервис возвращает 400, если start_date после end_date")
    void scoringDataGet_StartDateIsAfterEndDate() {
        var queryParams = Map.of(
                "bankId", "150",
                "shopTin", "testShopTin",
                "startDate", "2018-06-19",
                "endDate", "1988-06-16"
        );
        var response = testRestTemplate.getForEntity(
                SCORING_DATA_BASE_URL, String.class, queryParams);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertThatJson(response.getBody())
                .isEqualTo(errorResponse(
                        "'end_date' has to be after 'start_date'. " +
                                "'start_date' = 2018-06-19 'end_date' = 1988-06-16"
                ));
    }

    @Test
    @DisplayName("Сервис возвращает 400, если между start_date и end_date больше 365 дней")
    void scoringDataGet_DaysBetweenStartDateAndEndDateLessThan365() {
        var queryParams = Map.of(
                "bankId", "150",
                "shopTin", "testShopTin",
                "startDate", "1988-06-16",
                "endDate", "2018-06-19"
        );

        var response = testRestTemplate.getForEntity(
                SCORING_DATA_BASE_URL, String.class, queryParams);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertThatJson(response.getBody())
                .isEqualTo(
                        errorResponse("Days interval between 'start_date' and 'end_date' has to be less " +
                                "than 365 days. 'start_date' = 1988-06-16 'end_date' = 2018-06-19")
                );
    }

    private static String errorResponse(String message) {
        return "{\n" +
                "    \"message\": \"" + message + "\",\n" +
                "    \"tracingInfo\": \"${json-unit.any-string}\"\n" +
                "}\n";
    }

    private void prepareScoringData() {
        try {
            var scoringDataJson = readClasspathFile("ScoringDataGetTest_Success.before.json");
            List<ScoringData> scoringData = JsonUtils.getJsonMapper()
                    .readValue(scoringDataJson, new TypeReference<List<ScoringData>>() {
                    });
            for (var dao : scoringData) {
                scoringDataMapper.insertScoringData(dao);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
