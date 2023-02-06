package ru.yandex.market.mbi.logprocessor.mvc.controller;

import java.util.Collections;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.logprocessor.FunctionalTest;
import ru.yandex.market.mbi.logprocessor.TestUtil;
import ru.yandex.market.mbi.logprocessor.YtInitializer;
import ru.yandex.market.mbi.logprocessor.storage.yt.model.ApiLogEntity;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.client.YtClientProxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты написанные до того как в запросы появилась возможность отдавать businessId оканчиваются на 'old'.
 */
class ApiLogsControllerFunctionalTest extends FunctionalTest {
    @Autowired
    @Qualifier("cpaApiLogTable")
    private BindingTable<ApiLogEntity> cpaApiLogsTable;

    @Autowired
    @Qualifier("apiLogTable")
    private BindingTable<ApiLogEntity> apiLogsTable;

    @Autowired
    @Qualifier("cpaApiLogTargetYt")
    private YtClientProxy cpaApiLogTargetYt;

    @Autowired
    @Qualifier("apiLogTargetYt")
    private YtClientProxy apiLogTargetYt;

    private YtInitializer cpaApiLogsTableInitializer;
    private YtInitializer apiLogsTableInitializer;
    private HttpHeaders headers;

    @BeforeEach
    public void setup() {
        cpaApiLogsTableInitializer = new YtInitializer(cpaApiLogTargetYt);
        apiLogsTableInitializer = new YtInitializer(apiLogTargetYt);
        cpaApiLogsTableInitializer.initializeFromFile("data/apilogs/api-log-data.csv", cpaApiLogsTable);
        apiLogsTableInitializer.initializeFromFile("data/apilogs/api-log-data.csv", apiLogsTable);
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @AfterEach
    public void clean() {
        cpaApiLogsTableInitializer.cleanTable(cpaApiLogsTable);
        apiLogsTableInitializer.cleanTable(apiLogsTable);
    }

    @ParameterizedTest(name = "{arguments}")
    @EnumSource(CpaApiLogTestCase.class)
    void testCpaApiLogsGet(CpaApiLogTestCase testCase) {
        setMbiOpenClient(testCase);

        ResponseEntity<String> response = testRestTemplate.postForEntity("/apilog/cpa",
                requestBody(testCase.requestBodyPath), String.class);
        String expectedResult = TestUtil.readString(testCase.expectedResponsePath);
        assertEquals(testCase.expectedResponseStatus, response.getStatusCode());

        assertResponse(expectedResult, response.getBody());
    }

    @ParameterizedTest(name = "{arguments}")
    @EnumSource(ApiLogsTestCase.class)
    void testApiLogsGet(ApiLogsTestCase testCase) {
        setMbiOpenClient(testCase);

        ResponseEntity<String> response = testRestTemplate.postForEntity("/apilog/all",
                requestBody(testCase.requestBodyPath), String.class);
        String expectedResult = TestUtil.readString(testCase.expectedResponsePath);
        assertResponse(expectedResult, response.getBody());
        assertEquals(testCase.expectedResponseStatus, response.getStatusCode());

    }

    @ParameterizedTest(name = "{arguments}")
    @EnumSource(CpaApiLogTestCaseOld.class)
    void testCpaApiLogsGetOld(CpaApiLogTestCaseOld testCase) {
        ResponseEntity<String> response = testRestTemplate.postForEntity("/apilog/cpa",
                requestBody(testCase.requestBodyPath), String.class);
        String expectedResult = TestUtil.readString(testCase.expectedResponsePath);
        assertEquals(testCase.expectedResponseStatus, response.getStatusCode());

        assertResponse(expectedResult, response.getBody());
    }

    @ParameterizedTest(name = "{arguments}")
    @EnumSource(ApiLogsTestCaseOld.class)
    void testApiLogsGetOld(ApiLogsTestCaseOld testCase) {
        ResponseEntity<String> response = testRestTemplate.postForEntity("/apilog/all",
                requestBody(testCase.requestBodyPath), String.class);
        String expectedResult = TestUtil.readString(testCase.expectedResponsePath);
        assertEquals(testCase.expectedResponseStatus, response.getStatusCode());

        assertResponse(expectedResult, response.getBody());
    }

    private HttpEntity<String> requestBody(String jsonPath) {
        return new HttpEntity<>(TestUtil.readString(jsonPath), headers);
    }

    private void assertResponse(String expectedResult, String responseBody) {
        MbiAsserts.assertJsonEquals(expectedResult, responseBody, Collections.singletonList(
                new Customization("timestamp", new RegularExpressionValueMatcher<>("[\\s\\S]*"))));
    }

    private void setMbiOpenClient(TestCase testCase) {
        JsonObject json = JsonTestUtil.parseJson(TestUtil.readString(testCase.getRequestBodyPath())).getAsJsonObject();
        JsonElement businessId = json.get("businessId");
        List<Long> partnerIds = testCase.getPartnerIdsOfBusiness();
        if (businessId != null) {
            Mockito.when(mbiOpenApiClient.getPartnerIdsByBusinessId(businessId.getAsLong())).thenReturn(partnerIds);
        }
    }

    private interface TestCase {
        List<Long> getPartnerIdsOfBusiness();

        String getRequestBodyPath();
    }

    private enum CpaApiLogTestCase implements TestCase {
        FIND_SHORT_DATA_EMPTY_LIST("asserts/apilog/request/findShortDataEmptyListRequestBusiness.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataEmptyResponse.json",
                "Поиск по фильтру только по бизнессу. Должен вернуть пустой список.", List.of(1L, 2L)),
        FIND_SHORT_DATA("asserts/apilog/request/findShortDataRequestBusiness.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataResponseBusiness.json",
                "Поиск по фильтру только по бизнессу.", List.of(6L, 7L)),
        FIND_SHORT_DATA_PARTNER_IDs("asserts/apilog/request/findShortDataAll.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataResponseBusiness.json",
                "Поиск по фильтру по partnerIds", List.of(6L, 7L));


        String requestBodyPath;
        HttpStatus expectedResponseStatus;
        String expectedResponsePath;
        String description;
        List<Long> partnerIdsOfBusiness;

        CpaApiLogTestCase(String requestBodyPath, HttpStatus expectedResponseStatus,
                          String expectedResponsePath,
                          String description,
                          List<Long> partnerIdsOfBusiness) {
            this.requestBodyPath = requestBodyPath;
            this.expectedResponseStatus = expectedResponseStatus;
            this.expectedResponsePath = expectedResponsePath;
            this.description = description;
            this.partnerIdsOfBusiness = partnerIdsOfBusiness;
        }

        @Override
        public String toString() {
            return description;
        }

        @Override
        public List<Long> getPartnerIdsOfBusiness() {
            return partnerIdsOfBusiness;
        }

        @Override
        public String getRequestBodyPath() {
            return requestBodyPath;
        }
    }

    private enum ApiLogsTestCase implements TestCase {
        FIND_SHORT_DATA_EMPTY_LIST("asserts/apilog/request/findShortDataEmptyListRequestBusiness.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataEmptyResponse.json",
                "Поиск по фильтру только по бизнессу. Должен вернуть пустой список.", List.of(1L, 2L)),
        FIND_SHORT_DATA("asserts/apilog/request/findShortDataRequestBusiness.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataResponseBusiness.json",
                "Поиск по фильтру только по бизнессу.", List.of(6L, 7L)),
        FIND_SHORT_DATA_PARTNER_IDs("asserts/apilog/request/findShortDataAll.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataResponseBusiness.json",
                "Поиск по фильтру по partnerIds.", List.of(6L, 7L));

        String requestBodyPath;
        HttpStatus expectedResponseStatus;
        String expectedResponsePath;
        String description;
        List<Long> partnerIdsOfBusiness;

        ApiLogsTestCase(String requestBodyPath, HttpStatus expectedResponseStatus,
                        String expectedResponsePath,
                        String description,
                        List<Long> partnerIdsOfBusiness) {
            this.requestBodyPath = requestBodyPath;
            this.expectedResponseStatus = expectedResponseStatus;
            this.expectedResponsePath = expectedResponsePath;
            this.description = description;
            this.partnerIdsOfBusiness = partnerIdsOfBusiness;
        }

        @Override
        public String toString() {
            return description;
        }

        @Override
        public List<Long> getPartnerIdsOfBusiness() {
            return partnerIdsOfBusiness;
        }

        @Override
        public String getRequestBodyPath() {
            return requestBodyPath;
        }
    }

    private interface TestCaseOld {
        String getRequestBodyPath();
    }

    private enum CpaApiLogTestCaseOld implements TestCaseOld {
        FIND_SHORT_DATA_EMPTY_LIST("asserts/apilog/request/findShortDataEmptyListRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataEmptyResponse.json",
                "Поиск по фильтру. Должен вернуть пустой список."),
        FIND_SHORT_DATA("asserts/apilog/request/findShortDataRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataResponse.json", "Поиск по фильтру."),
        FIND_SHORT_DATA_TIMEZONE("asserts/apilog/request/findShortDataTimezoneRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSingleApiLogResponse.json",
                "Поиск по фильтру. Передача даты с таймзоной."),
        FIND_SHORT_DATA_FIRST_PAGE("asserts/apilog/request/findShortDataFirstPageRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataPage1Response.json",
                "Поиск по фильтру - первая страница."),
        FIND_SHORT_DATA_SECOND_PAGE("asserts/apilog/request/findShortDataSecondPageRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataPage2Response.json",
                "Поиск по фильтру - вторая страница."),
        FIND_SPECIFIC_API_LOG("asserts/apilog/request/findSpecificApiLogRequest.json", HttpStatus.OK,
                "asserts/apilog/response/findSpecificApiLogResponse.json", "Поиск по уникальному идентификатору."),
        FIND_SHORT_DATA_BY_USER_ID("asserts/apilog/request/findShortDataByUserIdRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSingleApiLogResponse.json",
                "Поиск по фильтру. Фильтр по userId и success."),
        FIND_SHORT_DATA_BY_RESOURCE_CODE("asserts/apilog/request/findShortDataByResourceCodeRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSingleApiLogResponse.json",
                "Поиск по фильтру. Фильтр по resourceCode и success."),
        FIND_SHORT_DATA_BY_CAMPAIGN_ID("asserts/apilog/request/findShortDataByCampaignIdRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSingleApiLogResponse.json",
                "Поиск по фильтру. Фильтр по campaignId."),
        FIND_SHORT_DATA_BY_ORDER_ID("asserts/apilog/request/findShortDataByOrderIdRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataByOrderIdResponse.json",
                "Поиск по фильтру. Фильтр по orderId."),
        FIND_SHORT_DATA_NOT_ENOUGH_BUSINESS_ID("asserts/apilog/request/findShortDataNotEnoughPartnerIdRequest.json",
                HttpStatus.BAD_REQUEST, "asserts/apilog/response/notEnoughFiltersResponse.json",
                "Поиск по фильтру. Не указан businessId."),
        FIND_SHORT_DATA_NOT_ENOUGH_FROM_DATE("asserts/apilog/request/findShortDataNotEnoughFromDateRequest.json",
                HttpStatus.BAD_REQUEST, "asserts/apilog/response/notEnoughFiltersResponse.json",
                "Поиск по фильтру. Не указан fromDate."),
        FIND_SHORT_DATA_NOT_ENOUGH_TO_DATE("asserts/apilog/request/findShortDataNotEnoughToDateRequest.json",
                HttpStatus.BAD_REQUEST, "asserts/apilog/response/notEnoughFiltersResponse.json",
                "Поиск по фильтру. Не указан toDate."),
        FIND_SHORT_DATA_INVALID_MAX_DATES_RANGE("asserts/apilog/request/findShortDataInvalidDatesRangeRequest.json",
                HttpStatus.BAD_REQUEST, "asserts/apilog/response/tooLongDatesRangeResponse.json",
                "Поиск по фильтру. Указан слишком большой период для поиска."),
        FIND_SHORT_DATA_INVALID_PAGER("asserts/apilog/request/findShortDataInvalidPagerRequest.json",
                HttpStatus.BAD_REQUEST, "asserts/apilog/response/invalidPagerResponse.json",
                "Поиск по фильтру. Указан слишком большой размер страницы.");

        String requestBodyPath;
        HttpStatus expectedResponseStatus;
        String expectedResponsePath;
        String description;

        CpaApiLogTestCaseOld(String requestBodyPath, HttpStatus expectedResponseStatus,
                             String expectedResponsePath,
                             String description) {
            this.requestBodyPath = requestBodyPath;
            this.expectedResponseStatus = expectedResponseStatus;
            this.expectedResponsePath = expectedResponsePath;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }


        @Override
        public String getRequestBodyPath() {
            return requestBodyPath;
        }
    }

    private enum ApiLogsTestCaseOld implements TestCaseOld {
        FIND_SHORT_DATA("asserts/apilog/request/findShortDataRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataResponse.json", "Поиск по фильтру."),
        FIND_SHORT_DATA_TIMEZONE("asserts/apilog/request/findShortDataTimezoneRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSingleApiLogResponse.json",
                "Поиск по фильтру. Передача даты с таймзоной."),
        FIND_SHORT_DATA_FIRST_PAGE("asserts/apilog/request/findShortDataFirstPageRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataPage1Response.json",
                "Поиск по фильтру - первая страница."),
        FIND_SHORT_DATA_SECOND_PAGE("asserts/apilog/request/findShortDataSecondPageRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataPage2Response.json",
                "Поиск по фильтру - вторая страница."),
        FIND_SPECIFIC_API_LOG("asserts/apilog/request/findSpecificApiLogRequest.json", HttpStatus.OK,
                "asserts/apilog/response/findSpecificApiLogResponse.json", "Поиск по уникальному идентификатору."),
        FIND_SHORT_DATA_BY_IP("asserts/apilog/request/findShortDataByIpRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSingleApiLogResponse.json",
                "Поиск по фильтру. Фильтр по ip."),
        FIND_SHORT_DATA_BY_DEBUG_KEY("asserts/apilog/request/findShortDataByDebugKeyRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSingleApiLogResponse.json",
                "Поиск по фильтру. Фильтр по debugKey."),
        FIND_SHORT_DATA_BY_RESOURCE_CODE("asserts/apilog/request/findShortDataByResourceCodeRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSingleApiLogResponse.json",
                "Поиск по фильтру. Фильтр по resourceCode и success."),
        FIND_SHORT_DATA_BY_CAMPAIGN_ID("asserts/apilog/request/findShortDataByCampaignIdRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSingleApiLogResponse.json",
                "Поиск по фильтру. Фильтр по campaignId."),
        FIND_SHORT_DATA_BY_METHOD("asserts/apilog/request/findShortDataByMethodRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSingleApiLogResponse.json",
                "Поиск по фильтру. Фильтр по method."),
        FIND_SHORT_DATA_BY_RESPONSE_CODE("asserts/apilog/request/findShortDataByResponseCodeRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataByResponseCodeResponse.json",
                "Поиск по фильтру. Фильтр по responseCode."),
        SORT_BY_RESPONSE_CODE("asserts/apilog/request/findShortDataSortByResponseCodeRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSortByResponseCodeResponse.json",
                "Поиск по фильтру. Сортировка по responseCode по возрастанию."),
        SORT_BY_RESPONSE_CODE_DESC("asserts/apilog/request/findShortDataSortByResponseCodeDescRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSortByResponseCodeDescResponse.json",
                "Поиск по фильтру. Сортировка по responseCode по убыванию."),
        SORT_BY_CAMPAIGN_ID("asserts/apilog/request/findShortDataSortByCampaignIdRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSortByCampaignIdResponse.json",
                "Поиск по фильтру. Сортировка по campaignId по возрастанию."),
        SORT_BY_CAMPAIGN_ID_DESC("asserts/apilog/request/findShortDataSortByCampaignIdDescRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSortByCampaignIdDescResponse.json",
                "Поиск по фильтру. Сортировка по campaignId по убыванию."),
        SORT_BY_REQUEST_DATE("asserts/apilog/request/findShortDataSortByRequestDateRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSortByRequestDateResponse.json",
                "Поиск по фильтру. Сортировка по requestDate по возрастанию."),
        SORT_BY_REQUEST_DATE_DESC("asserts/apilog/request/findShortDataSortByRequestDateDescRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSortByRequestDateDescResponse.json",
                "Поиск по фильтру. Сортировка по requestDate по убыванию."),
        SORT_BY_METHOD("asserts/apilog/request/findShortDataSortByMethodRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSortByMethodResponse.json",
                "Поиск по фильтру. Сортировка по method по возрастанию."),
        SORT_BY_METHOD_DESC("asserts/apilog/request/findShortDataSortByMethodDescRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSortByMethodDescResponse.json",
                "Поиск по фильтру. Сортировка по method по убыванию."),
        SORT_BY_IP("asserts/apilog/request/findShortDataSortByIpRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSortByIpResponse.json",
                "Поиск по фильтру. Сортировка по ip по возрастанию."),
        SORT_BY_IP_DESC("asserts/apilog/request/findShortDataSortByIpDescRequest.json",
                HttpStatus.OK, "asserts/apilog/response/findShortDataSortByIpDescResponse.json",
                "Поиск по фильтру. Сортировка по ip по убыванию."),
        FIND_SHORT_DATA_NOT_ENOUGH_BUSINESS_ID("asserts/apilog/request/findShortDataNotEnoughPartnerIdRequest.json",
                HttpStatus.BAD_REQUEST, "asserts/apilog/response/notEnoughFiltersResponse.json",
                "Поиск по фильтру. Не указан businessId."),
        FIND_SHORT_DATA_NOT_ENOUGH_FROM_DATE("asserts/apilog/request/findShortDataNotEnoughFromDateRequest.json",
                HttpStatus.BAD_REQUEST, "asserts/apilog/response/notEnoughFiltersResponse.json",
                "Поиск по фильтру. Не указан fromDate."),
        FIND_SHORT_DATA_NOT_ENOUGH_TO_DATE("asserts/apilog/request/findShortDataNotEnoughToDateRequest.json",
                HttpStatus.BAD_REQUEST, "asserts/apilog/response/notEnoughFiltersResponse.json",
                "Поиск по фильтру. Не указан toDate."),
        FIND_SHORT_DATA_INVALID_MAX_DATES_RANGE("asserts/apilog/request/findShortDataInvalidDatesRangeRequest.json",
                HttpStatus.BAD_REQUEST, "asserts/apilog/response/tooLongDatesRangeResponse.json",
                "Поиск по фильтру. Указан слишком большой период для поиска."),
        FIND_SHORT_DATA_INVALID_PAGER("asserts/apilog/request/findShortDataInvalidPagerRequest.json",
                HttpStatus.BAD_REQUEST, "asserts/apilog/response/invalidPagerResponse.json",
                "Поиск по фильтру. Указан слишком большой размер страницы.");

        String requestBodyPath;
        HttpStatus expectedResponseStatus;
        String expectedResponsePath;
        String description;

        ApiLogsTestCaseOld(String requestBodyPath, HttpStatus expectedResponseStatus,
                           String expectedResponsePath,
                           String description) {
            this.requestBodyPath = requestBodyPath;
            this.expectedResponseStatus = expectedResponseStatus;
            this.expectedResponsePath = expectedResponsePath;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }

        @Override
        public String getRequestBodyPath() {
            return requestBodyPath;
        }
    }
}
