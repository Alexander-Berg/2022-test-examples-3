package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.api.view.VendorModelRequestView;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.brand.BrandInfoService;
import ru.yandex.vendor.security.Role;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

/**
 * Тест для {@link VendorController}.
 */
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class VendorControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private final WireMockServer staffMock;
    private final WireMockServer csBillingApiMock;
    private final WireMockServer blackboxMock;
    private final WireMockServer javaSecApiMock;
    private final BrandInfoService brandInfoService;
    private final Clock clock;
    @Autowired
    private WireMockServer reportMock;

    @Autowired
    public VendorControllerFunctionalTest(WireMockServer staffMock,
                                          WireMockServer csBillingApiMock,
                                          WireMockServer blackboxMock,
                                          WireMockServer javaSecApiMock,
                                          BrandInfoService brandInfoService,
                                          Clock clock) {
        this.staffMock = staffMock;
        this.csBillingApiMock = csBillingApiMock;
        this.blackboxMock = blackboxMock;
        this.javaSecApiMock = javaSecApiMock;
        this.brandInfoService = brandInfoService;
        this.clock = clock;
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.now()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin_user",
            "feedback_charge_free_user",
            "manager_ro_user",
            "manager_user",
            "model_charge_free_user",
            "questions_charge_free_user",
            "recommended_shops_user",
            "support_user"
    })
    void testJavaSecOk(String roleName) {
        setVendorUserRoles(singletonList(Role.valueOf(roleName)), 100500, 321L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/321?uid=100500");
        String expected = getStringResource("/testJavaSecOk/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetVendorProductsBalanceUser() {
        initBalanceServiceByClientId(100);

        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/search?clientId=100")
                .willReturn(okJson(getStringResource("/testGetVendorProductsBalanceUser/csBillingApiResponse.json"))));
        csBillingApiMock.stubFor(WireMock.get("/service/206/datasource/search?clientId=100")
                .willReturn(okJson("{}")));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/321?uid=100500");
        String expected = getStringResource("/testGetVendorProductsBalanceUser/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code GET /vendors/{vendorId}}
     * ({@link VendorController#getVendor(long, long)})
     * возвращает корректный результат.
     */
    @Test
    void testGetVendorById() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321?uid=100500");
        String expected = getStringResource("/testGetVendorById/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code GET /vendors/{vendorId}}
     * ({@link VendorController#getVendor(long, long)}})
     * возвращает флаг hasFreeProducts=false при наличии катоффов по документам по любой из услуг.
     */
    @Test
    void testGetVendorByIdWithDocumentsCutoff() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/654?uid=100500");
        String expected = getStringResource("/testGetVendorByIdWithDocumentsCutoff/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetManagerByVendorByIdCrmEmail() {
        staffMock.stubFor(get(urlPathEqualTo("/v3/persons"))
                .willReturn(aResponse().withBody(getStringResource("/testGetManagerByVendorByIdCrmEmail" +
                        "/staff_response.json"))));
        javaSecApiMock.stubFor(get(urlPathEqualTo("/userInfo/staffLogin"))
                .willReturn(aResponse().withBody("robot-dj-roomba")));


        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/manager?uid=100500");
        String expected = getStringResource("/testGetManagerByVendorByIdCrmEmail/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetManagerByVendorByIdYandexTeamEmail() {
        staffMock.stubFor(get(urlPathEqualTo("/v3/persons"))
                .willReturn(aResponse().withBody(getStringResource("/testGetManagerByVendorByIdYandexTeamEmail" +
                        "/staff_response.json"))));
        javaSecApiMock.stubFor(get(urlPathEqualTo("/userInfo/staffLogin"))
                .willReturn(aResponse().withBody("robot-dj-roomba")));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/manager?uid=100500");
        String expected = getStringResource("/testGetManagerByVendorByIdYandexTeamEmail/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetManagerByVendorByIdNoEmail() {
        staffMock.stubFor(get(urlPathEqualTo("/v3/persons"))
                .willReturn(aResponse().withBody(getStringResource("/testGetManagerByVendorByIdNoEmail/staff_response" +
                        ".json"))));
        javaSecApiMock.stubFor(get(urlPathEqualTo("/userInfo/staffLogin"))
                .willReturn(aResponse().withBody("robot-dj-roomba")));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/manager?uid=100500");
        String expected = getStringResource("/testGetManagerByVendorByIdNoEmail/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Создание карточки вендора
     */
    @Test
    void testPostVendor() {
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testPostVendor/blackbox_response_100500.json"))));
        staffMock.stubFor(get(urlPathEqualTo("/v3/persons"))
                .willReturn(aResponse().withBody(getStringResource("/testPostVendor/staff_response.json"))));

        setVendorUserRoles(Collections.singleton(Role.manager_user), 100500);

        String body = getStringResource("/testPostVendor/request.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors?uid=100500", body);
        String expected = getStringResource("/testPostVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Попытка создания карточки вендора саппортом (вендор с таким брендом уже зарегистрирован)
     */
    @Test
    void testPostVendorBySupportWithAlreadyRegisteredBrand() {
        // dirty hack to refresh brands
        brandInfoService.brandById(0L);

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource(
                        "/testPostVendorBySupportWithAlreadyRegisteredBrand/blackbox_response_100500.json"))));
        staffMock.stubFor(get(urlPathEqualTo("/v3/persons"))
                .willReturn(aResponse().withBody(getStringResource(
                        "/testPostVendorBySupportWithAlreadyRegisteredBrand/staff_response.json"))));

        setVendorUserRoles(Collections.singleton(Role.support_user), 100500);

        String body = getStringResource("/testPostVendorBySupportWithAlreadyRegisteredBrand/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors?uid=100500", body)
        );
        String response = exception.getResponseBodyAsString();
        String expected = getStringResource("/testPostVendorBySupportWithAlreadyRegisteredBrand/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testPutVendorCategoryShouldChangeVendorCategories() {
        String body = getStringResource("/testPutVendorCategoryShouldChangeVendorCategories/request.json");
        String actualCategoryResult = FunctionalTestHelper.put(baseUrl + "/vendors/321/categories?uid=100500", body);
        String expectedCategoryResult = getStringResource("/testPutVendorCategoryShouldChangeVendorCategories" +
                "/expected_category.json");
        JsonAssert.assertJsonEquals(expectedCategoryResult, actualCategoryResult, when(IGNORING_ARRAY_ORDER));

        String actualVendor = FunctionalTestHelper.get(baseUrl + "/vendors/321?uid=100500");
        String expectedVendor = getStringResource("/testPutVendorCategoryShouldChangeVendorCategories/expected_vendor" +
                ".json");
        JsonAssert.assertJsonEquals(expectedVendor, actualVendor, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testPutVendorCategoryShouldNotChangeVendorCategoriesIfInvalid() {
        String body = getStringResource("/testPutVendorCategoryShouldNotChangeVendorCategoriesIfInvalid/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(baseUrl + "/vendors/321/categories?uid=100500", body)
        );
        String actualCategoryResult = exception.getResponseBodyAsString();
        String expectedCategoryResult = getStringResource(
                "/testPutVendorCategoryShouldNotChangeVendorCategoriesIfInvalid/expected_category.json");
        JsonAssert.assertJsonEquals(expectedCategoryResult, actualCategoryResult, when(IGNORING_ARRAY_ORDER));

        String actualVendor = FunctionalTestHelper.get(baseUrl + "/vendors/321?uid=100500");
        String expectedVendor = getStringResource("/testPutVendorCategoryShouldNotChangeVendorCategoriesIfInvalid" +
                "/expected_vendor.json");
        JsonAssert.assertJsonEquals(expectedVendor, actualVendor, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testPutVendorCategoryShouldValidateBrandCategories() {
        String body = getStringResource("/testPutVendorCategoryShouldValidateBrandCategories/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(baseUrl + "/vendors/987/categories?uid=100500", body)
        );

        String actualCategoryResult = exception.getResponseBodyAsString();
        String expectedCategoryResult = getStringResource("/testPutVendorCategoryShouldValidateBrandCategories" +
                "/expected.json");
        JsonAssert.assertJsonEquals(expectedCategoryResult, actualCategoryResult, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorModelsByCategory/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetVendorModelsByCategory() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/models/byCategory?uid=100500");
        String expected = getStringResource("/testGetVendorModelsByCategory/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorModelsByCategoryWithTextFilter/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetVendorModelsByCategoryWithTextFilter() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/models/byCategory?uid=100500&text=66");
        String expected = getStringResource("/testGetVendorModelsByCategoryWithTextFilter/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorModels/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetVendorModels() {
        VendorModelRequestView vendorModelRequestView = new VendorModelRequestView();
        vendorModelRequestView.setModels(Arrays.asList(10557849L, 8537818L));

        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/models?uid=100500", vendorModelRequestView);
        String expected = getStringResource("/testGetVendorModels/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorModelsReport/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetVendorModelsReport() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", equalTo("brand_products"))
                .withQueryParam("vendor_id", equalTo("110321"))
                .willReturn(aResponse().withBody(getStringResource("/testGetVendorModelsReport/report-response-blue.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/models/report?models=10557849&models=8537818");
        String expected = getStringResource("/testGetVendorModelsReport/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorModels/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetVendorModelsWithSkuId() {
        VendorModelRequestView vendorModelRequestView = new VendorModelRequestView();
        vendorModelRequestView.setModels(singletonList(10557849L));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", equalTo("model_skus_info"))
                .willReturn(aResponse().withBody(getStringResource("/testGetVendorModels/report-response-blue.json"))));

        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/models?uid=100500&showSku=true", vendorModelRequestView);
        String expected = getStringResource("/testGetVendorModels/expected-blue.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorProducts/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetVendorProducts() {
        setVendorUserRoles(singletonList(Role.recommended_shops_user), 100500, 321L);

        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=37&tariffTypeId=38")
                .willReturn(aResponse().withBody(getStringResource("/testGetVendorProducts/retrofit2_response.json"))));

        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1321/billingPeriod")
                .willReturn(okJson(getStringResource("/testGetVendorProducts/csBillingApiPeriodResponse.json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/321/products?uid=100500");
        String expected = getStringResource("/testGetVendorProducts/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorProductsRecommended/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorProductsRecommended/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetVendorProductsRecommended() {
        setVendorUserRoles(singletonList(Role.recommended_shops_user), 100500, 321L);

        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=37&tariffTypeId=38")
                .willReturn(aResponse().withBody(getStringResource("/testGetVendorProductsRecommended/retrofit2_response.json"))));

        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1321/billingPeriod")
                .willReturn(okJson(getStringResource("/testGetVendorProductsRecommended/csBillingApiPeriodResponse" +
                        ".json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/321/products?uid=100500");
        String expected = getStringResource("/testGetVendorProductsRecommended/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorProductsMarketingBanners/before.csv")
    void testGetVendorProductsMarketingBanners() {
        LocalDateTime now = LocalDateTime.of(2020, Month.JUNE, 16, 10, 23);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));
        setVendorUserRoles(singletonList(Role.marketing_banners_user), 100500, 111L);
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1111/billingPeriod")
                .willReturn(okJson(getStringResource("/testGetVendorProductsMarketingBanners" +
                        "/csBillingApiPeriodResponse.json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/111/products?uid=100500");
        String expected = getStringResource("/testGetVendorProductsMarketingBanners/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorProductsWithoutRole/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetVendorProductsWithoutRole() {
        setVendorUserRoles(singletonList(Role.feedback_charge_free_user), 100500, 321L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/321/products?uid=100500");
        String expected = getStringResource("/testGetVendorProductsWithoutRole/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorProductsManager/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetVendorProductsManager() {
        setVendorUserRoles(singletonList(Role.manager_user), 100500);

        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=37&tariffTypeId=38")
                .willReturn(aResponse().withBody(getStringResource("/testGetVendorProductsManager/retrofit2_response.json"))));

        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1321/billingPeriod")
                .willReturn(okJson(getStringResource("/testGetVendorProductsManager/csBillingApiPeriodResponse.json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/321/products?uid=100500");
        String expected = getStringResource("/testGetVendorProductsManager/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorProductsSupport/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorControllerFunctionalTest/testGetVendorProductsSupport/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetVendorProductsSupport() {
        setVendorUserRoles(singletonList(Role.support_user), 100500);

        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=37&tariffTypeId=38")
                .willReturn(aResponse().withBody(getStringResource("/testGetVendorProductsSupport/retrofit2_response.json"))));
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1321/billingPeriod")
                .willReturn(okJson(getStringResource("/testGetVendorProductsSupport/csBillingApiPeriodResponse.json"))));
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1321/balance")
                .willReturn(okJson(getStringResource("/testGetVendorProductsSupport/serviceDatasourceBalanceResponse" +
                        ".json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/321/products?uid=100500");
        String expected = getStringResource("/testGetVendorProductsSupport/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetProductDatasources() {
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/search?isOffer=true&sourceId=1")
                .willReturn(okJson(getStringResource("/testGetProductDatasources/csBillingApiResponse.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/datasources?uid=100500&vendorId=321&vendorId" +
                "=654&product=RECOMMENDED_SHOPS&isOffer=true");
        String expected = getStringResource("/testGetProductDatasources/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetUnpaidOverdraftInvoices() {
        csBillingApiMock.stubFor(WireMock.get("/services/invoices/unpaid/overdraft?serviceId=132&clientId=100")
                .willReturn(okJson(getStringResource("/testGetUnpaidOverdraftInvoices/csBillingApiResponse.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/invoices/unpaid/overdraft?uid=100500");
        String expected = getStringResource("/testGetUnpaidOverdraftInvoices/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }
}
