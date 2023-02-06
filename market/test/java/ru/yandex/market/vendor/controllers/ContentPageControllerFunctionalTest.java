package ru.yandex.market.vendor.controllers;

import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.content.CategoryInfo;
import ru.yandex.vendor.content.CategoryInfoService;
import ru.yandex.vendor.content.EntityType;
import ru.yandex.vendor.security.Role;
import ru.yandex.vendor.util.NettyRestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@Disabled
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/ContentPageControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/ContentPageControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class ContentPageControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    private WireMockServer reportMock;
    @Autowired
    private NettyRestClient persQaRestClient;
    @Autowired
    private CategoryInfoService categoryInfoService;

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Фильтр по типу MODELS.
     */
    @Test
    void testGetModels() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForBrandProduct())
                .withQueryParam("show-msku", equalTo("1"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModels/report.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForModelInfo())
                .willReturn(aResponse().withBody(getStringResource("/testGetModels/report.json"))));

        doAnswer(invocation -> List.of(
                Map.ofEntries(
                        Map.entry("modelId", 14206637),
                        Map.entry("noAnswerCount", 10),
                        Map.entry("totalCount", 20)
                ),
                Map.ofEntries(
                        Map.entry("modelId", 14262592),
                        Map.entry("noAnswerCount", 0),
                        Map.entry("totalCount", 1000)
                )
        )).when(persQaRestClient).getForObject(any());
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500&entityType=MODEL");
        String expected = getStringResource("/testGetModels/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет доступ в ресурс {@code GET /vendors/[vendorId]/content/models}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "balance_client_user",
            "manager_ro_user",
            "manager_user",
            "model_charge_free_user",
            "brand_zone_user",
            "model_bid_user",
            "recommended_shops_user",
            "support_user"
    })
    void testGetModelsJavaSecOk(String roleName) {

        if ("balance_client_user".equals(roleName)) {
            initBalanceServiceByClientId(100500);
        } else {
            setVendorUserRoles(List.of(Role.valueOf(roleName)), 100500, 101L);
        }

        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForBrandProduct())
                .willReturn(aResponse().withBody(getStringResource("/testJavaSecOk/report.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForModelInfo())
                .willReturn(aResponse().withBody(getStringResource("/testJavaSecOk/report_modelinfo.json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/content/models?uid=100500");
        String expected = getStringResource("/testJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку отсутсвия доступа в ручку {@code GET /vendors/[vendorId]/content/models} для определенных пользователей
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "moderator_user",
            "feedback_charge_free_user",
            "questions_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testGetModelsJavaSecForbidden(String roleName) {
        setVendorUserRoles(List.of(Role.valueOf(roleName)), 100500, 101L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/content/models?uid=100500")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку несуществующего вендора в ресурсе {@code GET /vendors/[vendorId]/content/models}
     */
    @Test
    void testGetModelsUnknownVendor() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/vendors/21231/content/models?uid=100500")
        );
        String expected = getStringResource("/testUnknownVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Фильтр по вхождению в названии модели.
     */
    @Test
    void testGetModelsByModelName() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForBrandProduct())
                .withQueryParam("show-msku", equalTo("1"))
                .withQueryParam("msku-only", equalTo("0"))
                .withQueryParam("text", equalTo("\"iphone%20x\""))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsByModelName/report.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForModelInfo())
                .withQueryParam("hyperid", WireMock.matching("(1759344220|1759344262)"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsByModelName/report_modelinfo.json"))));

        doAnswer(invocation -> List.of(
                Map.ofEntries(
                        Map.entry("modelId", 1759344262),
                        Map.entry("noAnswerCount", 0),
                        Map.entry("totalCount", 1)
                )
        )).when(persQaRestClient).getForObject(any());

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500&modelName=iphone%20x");
        String expected = getStringResource("/testGetModelsByModelName/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Фильтр по точному названию модели.
     */
    @Test
    void testGetModelsByExactModelName() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForBrandProduct())
                .withQueryParam("text", equalTo("\"iPhone%20X%20256GB\""))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsByExactModelName/report.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForModelInfo())
                .withQueryParam("hyperid", equalTo("1759344220"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsByExactModelName/report_modelinfo.json"))));

        doAnswer(invocation -> List.of(
                Map.ofEntries(
                        Map.entry("modelId", 1759344220),
                        Map.entry("noAnswerCount", 0),
                        Map.entry("totalCount", 0)
                )
        )).when(persQaRestClient).getForObject(any());

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500&modelName=iPhone%20X%20256GB");
        String expected = getStringResource("/testGetModelsByExactModelName/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Фильтр по названию несуществющей модели (ожидаем пустой результат).
     */
    @Test
    void testGetModelsWithEmptyResult() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForBrandProduct())
                .withQueryParam("text", equalTo("\"blablabla\""))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithEmptyResult/report.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500&modelName=blablabla");
        String expected = getStringResource("/testGetModelsWithEmptyResult/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Ождаем ошибку от сервиса Pers - не доступны данные о количестве вопросов.
     */
    @Test
    void testGetModelsPersQaError() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForBrandProduct())
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsPersQaError/report.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForModelInfo())
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsPersQaError/report_modelinfo.json"))));

        doThrow(new RuntimeException("PersQA is awesome!")).when(persQaRestClient).getForObject(any());

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=1&entityType=MODEL");
        String expected = getStringResource("/testGetModelsPersQaError/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Фильтр по типу SKU.
     */
    @Test
    void testGetSku() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForBrandProduct())
                .withQueryParam("show-msku", equalTo("0"))
                .withQueryParam("msku-only", equalTo("1"))
                .willReturn(aResponse().withBody(getStringResource("/testGetSku/report.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForModelInfo())
                .willReturn(aResponse().withBody(getStringResource("/testGetSku/report_modelinfo.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500&entityType=SKU");
        String expected = getStringResource("/testGetSku/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Запрос без фильтров - получаем все модели и заявки.
     */
    @Test
    void testGetModelsAndRequests() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForBrandProduct())
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsAndRequests/report.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForModelInfo())
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsAndRequests/report_modelinfo.json"))));

        doAnswer(invocation -> List.of(
                Map.ofEntries(
                        Map.entry("modelId", 14206637),
                        Map.entry("noAnswerCount", 10),
                        Map.entry("totalCount", 20)
                ),
                Map.ofEntries(
                        Map.entry("modelId", 14262592),
                        Map.entry("noAnswerCount", 0),
                        Map.entry("totalCount", 1000)
                )
        )).when(persQaRestClient).getForObject(any());

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500");
        String expected = getStringResource("/testGetModelsAndRequests/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Фильтр по статусу заявки.
     */
    @Test
    void testGetRequestsStateFilter() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500&entityType=REQUEST&requestState=PROCESSING,EDITING");
        String expected = getStringResource("/testGetRequestsStateFilter/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Фильтр по категории и типу REQUEST.
     */
    @Test
    void testGetRequestsCategoryFilter() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500&entityType=REQUEST&categoryId=2000");
        String expected = getStringResource("/testGetRequestsCategoryFilter/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Фильтр по категории и типу MODEL.
     */
    @Test
    void testGetModelsCategoryFilter() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForBrandProduct())
                .withQueryParam("hid", equalTo("90555"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsCategoryFilter/report.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForModelInfo())
                .withQueryParam("hyperid", matching("(14206836|8478688)"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsCategoryFilter/report_modelinfo.json"))));

        doAnswer(invocation -> List.of(
                Map.ofEntries(
                        Map.entry("modelId", 14206836),
                        Map.entry("noAnswerCount", 9),
                        Map.entry("totalCount", 51)
                ),
                Map.ofEntries(
                        Map.entry("modelId", 8478688),
                        Map.entry("noAnswerCount", 12),
                        Map.entry("totalCount", 34)
                )
        )).when(persQaRestClient).getForObject(any());

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500&entityType=MODEL&categoryId=90555");
        String expected = getStringResource("/testGetModelsCategoryFilter/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Фильтрация по невалидному статусу заявки.
     */
    @Test
    void testGetRequestsInvalidState() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500&entityType=REQUEST&requestState=INVALID_STATE")
        );
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/models} {@link ContentPageController#vendorUnits(long, long, Integer, Integer, EntityType, String, Long, List, String, String)}  }.
     * Фильтрация по невалидной категории.
     */
    @Test
    void testGetModelsReportError() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParams(commonParamsSetForBrandProduct())
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsReportError/report.json"))));

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models?uid=100500&entityType=MODEL&categoryId=0")
        );
        String expected = getStringResource("/testGetModelsReportError/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет получение количества моделей и sku для вендора в ресурсе {@code GET /vendors/[vendorId]/content/models/count} {@link ContentPageController#modelsCount(long, long)}
     */
    @Test
    void testGetModelsCount() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", equalTo("brand_products"))
                .withQueryParam("bsformat", equalTo("2"))
                .withQueryParam("vendor_id", equalTo("999999"))
                .withQueryParam("nosearchresults", equalTo("1"))
                .withQueryParam("show-msku", equalTo("0"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsCount/report_models.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", equalTo("brand_products"))
                .withQueryParam("bsformat", equalTo("2"))
                .withQueryParam("vendor_id", equalTo("999999"))
                .withQueryParam("nosearchresults", equalTo("1"))
                .withQueryParam("msku-only", equalTo("1"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsCount/report_sku.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/models/count?uid=100500");
        String expected = getStringResource("/testGetModelsCount/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет доступ в ресурс {@code GET /vendors/[vendorId]/content/models/count} {@link ContentPageController#modelsCount(long, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "balance_client_user",
            "brand_zone_user",
            "manager_ro_user",
            "manager_user",
            "model_bid_user",
            "model_charge_free_user",
            "recommended_shops_user",
            "support_user"
    })
    void testGetModelsCountJavaSecOk(String roleName) {
        if ("balance_client_user".equals(roleName)) {
            initBalanceServiceByClientId(100500);
        } else {
            setVendorUserRoles(List.of(Role.valueOf(roleName)), 100500, 101L);
        }

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("show-msku", equalTo("0"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsCount/report_models.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("msku-only", equalTo("1"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsCount/report_sku.json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/content/models/count?uid=100500");
        String expected = getStringResource("/testJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет отсутствие доступа в ресрус {@code GET /vendors/[vendorId]/content/models/count} {@link ContentPageController#modelsCount(long, long)}
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "moderator_user",
            "feedback_charge_free_user",
            "questions_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testGetModelsCountJavaSecForbidden(String roleName) {
        setVendorUserRoles(List.of(Role.valueOf(roleName)), 100500, 101L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/content/models/count?uid=100500")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/requests/count} {@link ContentPageController#requestsCount}  }.
     * Запрос количества заявок на изменение моделей.
     */
    @Test
    void testGetRequestsCount() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/requests/count?uid=100500");
        String expected = getStringResource("/testGetRequestsCount/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет доступ в ресурс {@code GET /vendors/[vendorId]/content/requests/count} {@link ContentPageController#requestsCount}  }.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "balance_client_user",
            "brand_zone_user",
            "manager_ro_user",
            "manager_user",
            "model_bid_user",
            "model_charge_free_user",
            "recommended_shops_user",
            "support_user"
    })
    void testGetRequestCountJavaSecOk(String roleName) {
        if ("balance_client_user".equals(roleName)) {
            initBalanceServiceByClientId(100500);
        } else {
            setVendorUserRoles(List.of(Role.valueOf(roleName)), 100500, 101L);
        }
        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/content/requests/count?uid=100500");
        String expected = getStringResource("/testJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет отсутствие доступа в ресурс {@code GET /vendors/[vendorId]/content/requests/count} {@link ContentPageController#requestsCount}  }.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "moderator_user",
            "feedback_charge_free_user",
            "questions_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testGetRequestCountJavaSecForbidden(String roleName) {
        if ("balance_client_user".equals(roleName)) {
            initBalanceServiceByClientId(100500);
        } else {
            setVendorUserRoles(List.of(Role.valueOf(roleName)), 100500, 101L);
        }
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/content/requests/count?uid=100500")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку несуществующего вендора в ресурсе {@code GET /vendors/[vendorId]/content/requests/count}
     */
    @Test
    void testGetRequestCountUnknownVendor() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/vendors/21231/content/requests/count?uid=100500")
        );
        String expected = getStringResource("/testUnknownVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/categories} {@link ContentPageController#categories}  }.
     * Запрос дерева категорий.
     */
    @Test
    void testGetCategories() {
        returnCategoriesForVendor();
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/categories?uid=100500");
        String expected = getStringResource("/testGetCategories/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет доступ в ресурс {@code GET /vendors/[vendorId]/content/categories} {@link ContentPageController#categories
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "balance_client_user",
            "brand_zone_user",
            "manager_ro_user",
            "manager_user",
            "model_bid_user",
            "model_charge_free_user",
            "recommended_shops_user",
            "support_user"})
    void testGetCategoriesJavaSecOk(String roleName) {
        if ("balance_client_user".equals(roleName)) {
            initBalanceServiceByClientId(100500);
        } else {
            setVendorUserRoles(List.of(Role.valueOf(roleName)), 100500, 101L);
        }
        returnCategoriesForVendor();
        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/content/categories?uid=100500");
        String expected = getStringResource("/testJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет отсуствие доступа в ресурс {@code GET /vendors/[vendorId]/content/categories} {@link ContentPageController#categories
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "moderator_user",
            "feedback_charge_free_user",
            "questions_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testGetCategoriesJavaSecForbidden(String roleName) {
        setVendorUserRoles(List.of(Role.valueOf(roleName)), 100500, 101L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/content/categories?uid=100500")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку несуществующего вендора в ресурсе {@code GET /vendors/[vendorId]/content/categories}
     */
    @Test
    void testGetCategoriesUnknownVendor() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/vendors/21231/content/categories?uid=100500")
        );
        String expected = getStringResource("/testUnknownVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет фильтрацию контента в ресурсе {@code GET /vendors/[vendorId]/content/categories/[categoryId]} {@link ContentPageController#categories}  }.
     * Запрос информации по категории.
     */
    @Test
    void testGetCategoryFullInfo() {
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetCategoryFullInfo/report.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/categories/3000?uid=1&zoom=full");
        String expected = getStringResource("/testGetCategoryFullInfo/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проеряет доступ в ресурс {@code GET /vendors/[vendorId]/content/categories/[categoryId]} {@link ContentPageController#categories}  }.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "balance_client_user",
            "brand_zone_user",
            "manager_ro_user",
            "manager_user",
            "model_bid_user",
            "model_charge_free_user",
            "recommended_shops_user",
            "support_user"
    })
    void testGetCategoryFullInfoJavaSecOk(String roleName) {
        if ("balance_client_user".equals(roleName)) {
            initBalanceServiceByClientId(100500);
        } else {
            setVendorUserRoles(List.of(Role.valueOf(roleName)), 100500, 101L);
        }
        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetCategoryFullInfo/report.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/content/categories/3000?uid=100500&zoom=full");
        String expected = getStringResource("/testJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет отсутствие доступа в ресурс {@code GET /vendors/[vendorId]/content/categories/[categoryId]} {@link ContentPageController#categories}  }.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "moderator_user",
            "feedback_charge_free_user",
            "questions_charge_free_user",
            "analytics_user",
            "shop_model_user",
            "shop_model_ro_user",
            "shop_batch_model_user",
            "shop_batch_model_ro_user",
            "entry_creator_user"
    })
    void testGetCategoryFulInfoForbidden(String roleName) {
        setVendorUserRoles(List.of(Role.valueOf(roleName)), 100500, 101L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/content/categories/3000?uid=100500&zoom=full")
        );
        String expected = getStringResource("/testJavaSecForbidden/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест на проверку несуществующего вендора в ресурсе {@code GET /vendors/[vendorId]/content/categories}
     */
    @Test
    void testGetCategoryUnknownVendor() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/vendors/21231/content/categories/3000?uid=100500")
        );
        String expected = getStringResource("/testUnknownVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    private CategoryInfo createCategoryInfo(int id, Long parentId) {
        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.setId(id);
        categoryInfo.setName("category " + id);
        categoryInfo.setParentId(parentId);
        categoryInfo.setUniqueName("category " + id + " unique");
        return categoryInfo;
    }

    private void returnCategoriesForVendor() {
        CategoryInfo category1 = createCategoryInfo(1, null);
        CategoryInfo category11 = createCategoryInfo(11, 1L);
        CategoryInfo category12 = createCategoryInfo(12, 1L);
        category1.setChildren(List.of(category11, category12));

        CategoryInfo category101 = createCategoryInfo(101, 11L);
        category11.setChildren(List.of(category101));

        CategoryInfo category2 = createCategoryInfo(2, null);

        doReturn(List.of(category1, category2))
                .when(categoryInfoService).getCategories(anyLong(), anyLong(), any());
    }

    private static Map<String, StringValuePattern> commonParamsSetForBrandProduct() {
        return Map.ofEntries(
                Map.entry("place", equalTo("brand_products")),
                Map.entry("bsformat", equalTo("2")),
                Map.entry("vendor_id", equalTo("999999")),
                Map.entry("entities", equalTo("product")),
                Map.entry("how", equalTo("guru_popularity")),
                Map.entry("numdoc", equalTo("30")),
                Map.entry("page", equalTo("1"))
        );
    }

    private static Map<String, StringValuePattern> commonParamsSetForModelInfo() {
        return Map.ofEntries(
                Map.entry("place", equalTo("modelinfo")),
                Map.entry("rids", equalTo("0")),
                Map.entry("bsformat", equalTo("2"))
        );
    }
}
