package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.modelbids.bidding.model.Bid;
import ru.yandex.vendor.modelbids.bidding.model.BiddingStatus;
import ru.yandex.vendor.modelbids.bidding.model.VendorModelBidBulkResponse;
import ru.yandex.vendor.modelbids.bidding.model.VendorModelBidResponse;
import ru.yandex.vendor.modelbids.promotion.ModelsPromotionService;
import ru.yandex.vendor.security.Role;
import ru.yandex.vendor.util.NettyRestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static java.time.LocalDateTime.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
class ModelsPromotionControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    @Autowired
    private WireMockServer reportMock;
    @Autowired
    private NettyRestClient mbiBiddingRestClient;
    @Autowired
    private ModelsPromotionService modelsPromotionService;
    @Autowired
    private Clock clock;

    @BeforeEach
    void updateHistoricalData() {
        when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.now()));
    }


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModels/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModels/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    public void testGetModels() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModels/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModels/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> new VendorModelBidBulkResponse()).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expected = getStringResource("/testGetModels/expected.json");
        assertResponseForPromotion(expected, response);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithMarketplaceBids/before.csv"
    )
    public void testGetModelsWithMarketplaceBids() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithMarketplaceBids/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithMarketplaceBids/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> new VendorModelBidBulkResponse()).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.marketplace_model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expected = getStringResource("/testGetModelsWithMarketplaceBids/expected.json");
        assertResponseForPromotion(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsForVendorsFromPricelabs/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsForVendorsFromPricelabs/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Проверяем что, если получить значения из pl такие же как и в репорте, то прогнозы останутся такими же. Также проверяем, что если datasourceId попадает в выбранные, то мы не ходим в place modelbidsrecommender")
    public void testGetModelsForVendorsFromPricelabs() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetModelsForVendorsFromPricelabs/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsForVendorsFromPricelabs/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> new VendorModelBidBulkResponse()).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 103L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/103/modelbids/promotion/models?uid=1");
        FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/103/modelbids/promotion/groups/1?uid=1");
        String expected = getStringResource("/testGetModelsForVendorsFromPricelabs/expected.json");
        assertResponseForPromotion(expected, response);
    }

    @Test
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithGroup/before.csv")
    public void testGetModelsWithGroup() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithGroup/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithGroup/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> new VendorModelBidBulkResponse()).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expected = getStringResource("/testGetModelsWithGroup/expected.json");
        assertResponseForPromotion(expected, response);
    }


    @Test
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsFromGroup/before.csv")
    public void testGetModelsFromGroup() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsFromGroup/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsFromGroup/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> new VendorModelBidBulkResponse()).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1" +
                "&groupId=1");
        String expected = getStringResource("/testGetModelsFromGroup/expected.json");
        assertResponseForPromotion(expected, response);
    }

    @Test
    @DisplayName("Проверяем разрешение кофликтов для одной модели у разных вендоров")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsBidForSameModels/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    public void testGetModelsBidForSameModels() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsBidForSameModels/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsBidForSameModels/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> {
                    VendorModelBidBulkResponse vendorModelBidResponses = new VendorModelBidBulkResponse();
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.APPLIED, 1, 111L));
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.PENDING, 2, 222L));
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.PENDING, 3, 333L));
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.PENDING, 4, 444L));
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.APPLIED, 77, 777L));
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.PENDING, 88, 888L));
                    return vendorModelBidResponses;
                }
        ).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 102L);

        String responseFor102vendor = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/102/modelbids/promotion" +
                "/models?uid=1");
        String expectedFor102Vendor = getStringResource("/testGetModelsBidForSameModels/expected_102.json");
        assertResponseForPromotion(expectedFor102Vendor, responseFor102vendor);
    }

    @Test
    @DisplayName("Проверяем приоритезацию моделей")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithPriority/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    public void testGetModelsWithPriority() {

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("priority-models", WireMock.equalTo("111,555,888"))
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsWithPriority/report_prior_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithPriority" +
                        "/report_marketplace.json"))));


        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> {
                    VendorModelBidBulkResponse vendorModelBidResponses = new VendorModelBidBulkResponse();
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.APPLIED, 1, 111L));
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.PENDING, 2, 222L));
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.PENDING, 3, 333L));
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.PENDING, 4, 444L));
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.APPLIED, 77, 777L));
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.PENDING, 88, 888L));
                    return vendorModelBidResponses;
                }
        ).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 102L);

        String responseFor102vendor = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/102/modelbids/promotion" +
                "/models?uid=1&priorityModel=111&priorityModel=555&priorityModel=888&page=2&pageSize=2");
        String expectedFor102Vendor = getStringResource("/testGetModelsWithPriority/expected_102.json");
        assertResponseForPromotion(expectedFor102Vendor, responseFor102vendor);
    }

    @Test
    @DisplayName("Проверяем разрешение кофликтов для одной модели у разных вендоров")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsBidForSameModelsFor101/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    public void testGetModelsBidForSameModelsFor101() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsBidForSameModelsFor101/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsBidForSameModelsFor101/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> {
                    VendorModelBidBulkResponse vendorModelBidResponses = new VendorModelBidBulkResponse();
                    vendorModelBidResponses.add(createResponseForBidding(BiddingStatus.APPLIED, 505, 555L));
                    return vendorModelBidResponses;
                }
        ).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String responseFor101vendor = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expectedFor101vendor = getStringResource("/testGetModelsBidForSameModelsFor101/expected_101.json");
        assertResponseForPromotion(expectedFor101vendor, responseFor101vendor);
    }

    private VendorModelBidResponse createResponseForBidding(BiddingStatus status, int bidValue, long modelId) {
        VendorModelBidResponse vendorModelBidResponse = new VendorModelBidResponse();
        Bid bid = new Bid();
        bid.setAt(1499434642L);
        bid.setStatus(status.getStatus());
        bid.setValue(bidValue);
        vendorModelBidResponse.setModel(modelId);
        vendorModelBidResponse.setGroup(0);
        vendorModelBidResponse.setPp(Collections.singletonMap("model_search", bid));
        return vendorModelBidResponse;
    }


    @Test
    @DisplayName("Вывод краткой информации о группе")
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetSummaryInfoFromGroup/before.csv")
    public void testGetSummaryInfoFromGroup() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetSummaryInfoFromGroup/report_products.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(BiddingStatus.APPLIED, 2200, 666666))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());
        String expected = getStringResource("/testGetSummaryInfoFromGroup/expected.json");
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups/1?uid=1");
        assertResponseForHistory(expected, response);
    }

    @Test
    @DisplayName("Вывод краткой информации о группе. Проверяем, что если нет ставок, то выводим ноль, как отображается на интерфейсе. Т.е. estimatePosition = null")
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetSummaryInfoFromGroupWithZeroValues/before.csv")
    public void testGetSummaryInfoFromGroupWithZeroValues() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetSummaryInfoFromGroupWithZeroValues/report_products.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(BiddingStatus.APPLIED, 2200, 666666))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());
        String expected = getStringResource("/testGetSummaryInfoFromGroupWithZeroValues/expected.json");
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups/1?uid=1");
        assertResponseForHistory(expected, response);
    }

    @Test
    @DisplayName("Проверка на то, что минимум при возвращении -1 превращается в ноль")
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetSummaryInfoFromGroupWithMinusBid/before.csv")
    public void testGetSummaryInfoFromGroupWithMinusBid() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetSummaryInfoFromGroupWithMinusBid/report_products.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(BiddingStatus.APPLIED, 2200, 666666))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());
        String expected = getStringResource("/testGetSummaryInfoFromGroupWithMinusBid/expected.json");
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups/1?uid=1");
        assertResponseForHistory(expected, response);
    }

    @Test
    @DisplayName("Проверка на то, что минимум при возвращении -1 превращается в ноль")
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetSummaryInfoFromGroupWithMissingBid/before.csv")
    public void testGetSummaryInfoFromGroupWithMissingBid() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetSummaryInfoFromGroupWithMissingBid/report_products.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(BiddingStatus.APPLIED, 2200, 666666))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());
        String expected = getStringResource("/testGetSummaryInfoFromGroupWithMissingBid/expected.json");
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups/1?uid=1");
        assertResponseForHistory(expected, response);
    }

    @Test
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithGroupWhereEmptyResponseFromReport/before.csv")
    public void testGetModelsWithGroupWhereEmptyResponseFromReport() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsWithGroupWhereEmptyResponseFromReport/report_products.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(BiddingStatus.APPLIED, null, null))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());
        String expected = getStringResource("/testGetModelsWithGroupWhereEmptyResponseFromReport/expected.json");
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups/1?uid=1");
        assertResponseForHistory(expected, response);
    }


    @Test
    @DisplayName("Проверяем, что при пустых диапазонах цен, вернувшихся из репорта на фронт ничего не отдаем.И проверяем, что если в прогнозах нет запрашиваемой категории, то не падаем с NPE")
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithGroupWhereEmptyPriceResponseFromReport/before.csv")
    public void testGetModelsWithGroupWhereEmptyPriceResponseFromReport() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsWithGroupWhereEmptyPriceResponseFromReport/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody("{}")));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(
                BiddingStatus.APPLIED, null, null))
                .when(mbiBiddingRestClient).postForObject(any(), any());
        String expected = getStringResource("/testGetModelsWithGroupWhereEmptyPriceResponseFromReport/expected.json");
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups/1?uid=1");
        assertResponseForHistory(expected, response);
    }


    @Test
    @DisplayName("Проверяем, не падаем с NPE, когда в pl нет рекомендация для моделей вернувшихся из репорта.Модели из одной категории")
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithGroupWherePriceLabsHasNoModelInOneCategory/before.csv")
    public void testGetModelsWithGroupWherePriceLabsHasNoModelInOneCategory() {
        reportMock.stubFor(get("/?place=brand_products&bsformat=2&vendor_id=999999&pp=7&entities=product&numdoc=1000" +
                "&hyperid=955288%2C217450585")
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsWithGroupWherePriceLabsHasNoModelInOneCategory/report_products.json"))));

        reportMock.stubFor(get("/?place=check_models_in_marketplace&hyperid=955288%2C217450585")
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsWithGroupWherePriceLabsHasNoModelInOneCategory/check_models_in_marketplace_response.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(
                BiddingStatus.APPLIED, null, null))
                .when(mbiBiddingRestClient).postForObject(any(), any());
        String expected = getStringResource(
                "/testGetModelsWithGroupWherePriceLabsHasNoModelInOneCategory/expected.json");
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups/1?uid=1");
        assertResponseForHistory(expected, response);
    }

    @Test
    @DisplayName("Проверяем, не падаем с NPE, когда в pl нет рекомендация для моделей вернувшихся из репорта.Две модели в одной категории, одна в другой. Где две - не пришла из pl только одна модель, где одна модель - не пришло ничего")
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithGroupWherePriceLabsHasNoModelInDifferentCategory/before.csv")
    public void testGetModelsWithGroupWherePriceLabsHasNoModelInDifferentCategory() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsWithGroupWherePriceLabsHasNoModelInDifferentCategory/report_products.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(
                BiddingStatus.APPLIED, null, null))
                .when(mbiBiddingRestClient).postForObject(any(), any());
        String expected = getStringResource(
                "/testGetModelsWithGroupWherePriceLabsHasNoModelInDifferentCategory/expected.json");
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups/1?uid=1");
        assertResponseForHistory(expected, response);
    }


    @Test
    @DisplayName("Проверяем, не падаем с NPE, когда pl ничего не возвращает")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithGroupWherePriceLabsHasNoModels/before.csv"
    )
    public void testGetModelsWithGroupWherePriceLabsHasNoModels() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsWithGroupWherePriceLabsHasNoModels/report_products.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(
                BiddingStatus.APPLIED, null, null))
                .when(mbiBiddingRestClient).postForObject(any(), any());
        String expected = getStringResource(
                "/testGetModelsWithGroupWherePriceLabsHasNoModels/expected.json");
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups/1?uid=1");
        assertResponseForHistory(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithBidInProgress/before.csv"
    )
    void testGetModelsWithBidInProgress() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsWithBidInProgress/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithBidInProgress/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> getVendorModelBidResponses(BiddingStatus.PENDING))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expected = getStringResource("/testGetModelsWithBidInProgress/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsFromSecondPageGroup/before.csv"
    )
    void testGetModelsFromSecondPageGroup() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetModelsFromSecondPageGroup/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsFromSecondPageGroup/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> new VendorModelBidBulkResponse()).when(mbiBiddingRestClient).postForObject(any(), any());

        FunctionalTestHelper.get(
                baseUrl + "/vendors/101/modelbids/promotion/models?uid=1&page=2&pageSize=1&groupId=1");
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsFromFirstPageGroup/before.csv"
    )
    void testGetModelsFromFirstPageGroup() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetModelsFromFirstPageGroup/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsFromFirstPageGroup/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> new VendorModelBidBulkResponse()).when(mbiBiddingRestClient).postForObject(any(), any());

        FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1&page=1&pageSize=1&groupId=1");
    }


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithBidPartlyApplied/before.csv"
    )
    void testGetModelsWithBidPartlyApplied() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetModelsWithBidPartlyApplied/report_products.json"))));
       reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithBidErrorNotAllowed/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> getVendorModelBidResponses(BiddingStatus.APPLIED)).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expected = getStringResource("/testGetModelsWithBidPartlyApplied/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithBidApplied/before.csv"
    )
    void testGetModelsWithBidApplied() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse()
                        .withBody(getStringResource("/testGetModelsWithBidApplied/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithBidApplied/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> getVendorModelBidResponses(BiddingStatus.APPLIED)).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expected = getStringResource("/testGetModelsWithBidApplied/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithBidErrorNotAllowed/before.csv"
    )
    void testGetModelsWithBidErrorNotAllowed() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetModelsWithBidErrorNotAllowed/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithBidErrorNotAllowed/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> getVendorModelBidResponses(BiddingStatus.NOT_ALLOWED))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expected = getStringResource("/testGetModelsWithBidErrorNotAllowed/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithBidErrorNotFound/before.csv"
    )
    void testGetModelsWithBidErrorNotFound() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithBidErrorNotFound/report_products.json"))));
       reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithBidErrorWrongBidValue/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> getVendorModelBidResponses(BiddingStatus.NOT_FOUND)).when(mbiBiddingRestClient).postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expected = getStringResource("/testGetModelsWithBidErrorNotFound/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithBidErrorWrongBidValue/before.csv"
    )
    void testGetModelsWithBidErrorWrongBidValue() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithBidErrorWrongBidValue/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithBidErrorWrongBidValue/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> getVendorModelBidResponses(BiddingStatus.WRONG_BID_VALUE))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expected = getStringResource("/testGetModelsWithBidErrorWrongBidValue/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @Test
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testCreateModelGroup/after.csv"
    )
    void testCreateModelGroup() {
        String body = getStringResource("/testCreateModelGroup/newGroup.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/101/modelbids/promotion/groups?uid=1", body);
        String expected = getStringResource("/testCreateModelGroup/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }


    @Test
    @DisplayName("Создание группы с конфликтами. Возвращаем список конфликтующих моделей")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testCreateModelGroupWithExistingModels/before.csv",
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testCreateModelGroupWithExistingModels/after.csv"
    )
    void testCreateModelGroupWithExistingModels() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testCreateModelGroupWithExistingModels/report_products.json"))));

        String body = getStringResource("/testCreateModelGroupWithExistingModels/newGroup.json");
        String expected = getStringResource("/testCreateModelGroupWithExistingModels/expected.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/101/modelbids/promotion/groups?uid=1", body);
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testCreateModelGroupWithExistingModelsWithForce/before.csv",
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testCreateModelGroupWithExistingModelsWithForce/after.csv"
    )
    void testCreateModelGroupWithExistingModelsWithForce() {
        String body = getStringResource("/testCreateModelGroupWithExistingModelsWithForce/newGroup.json");
        String expected = getStringResource("/testCreateModelGroupWithExistingModelsWithForce/expected.json");
        String response = FunctionalTestHelper.post(
                baseUrl + "/vendors/101/modelbids/promotion/groups?force=true&uid=1", body);
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testDeleteModelGroup/before.csv",
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testDeleteModelGroup/after.csv"
    )
    void testDeleteModelGroup() {
        FunctionalTestHelper.delete(baseUrl + "/vendors/101/modelbids/promotion/groups/2?uid=1");
    }

    @Test
    @DisplayName("Добавляем новые модели в группу, которые уже есть в этой группе")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testUpdateModelGroup/before.csv",
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testUpdateModelGroup/after.csv"
    )
    void testUpdateModelGroup() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testUpdateModelGroup/report_products.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(BiddingStatus.APPLIED, 2200, 666666))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());

        String body = getStringResource("/testUpdateModelGroup/updateGroup.json");
        String response = FunctionalTestHelper.put(
                baseUrl + "/vendors/101/modelbids/promotion/groups/2/models?&uid=1", body);
        String expected = getStringResource("/testUpdateModelGroup/expected.json");
        assertResponseForHistory(expected, response);
    }


    @Test
    @DisplayName(
            "Добавляем в группу A1 модели из другой группы A2 таким образом, что в A1 выделены все модели ")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testUpdateModelGroupWhenDeleteModelsFromUpdatebleGroup/before.csv",
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testUpdateModelGroupWhenDeleteModelsFromUpdatebleGroup/after.csv"
    )
    void testUpdateModelGroupWhenDeleteModelsFromUpdatebleGroup() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testUpdateModelGroupWhenDeleteModelsFromUpdatebleGroup/report_products.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(BiddingStatus.APPLIED, 2200, 666666))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());

        String body = getStringResource("/testUpdateModelGroupWhenDeleteModelsFromUpdatebleGroup/updateGroup.json");
        String response = FunctionalTestHelper.put(
                baseUrl + "/vendors/101/modelbids/promotion/groups/2/models?&uid=1&force=true", body);
        String expected = getStringResource("/testUpdateModelGroupWhenDeleteModelsFromUpdatebleGroup/expected.json");
        assertResponseForHistory(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testUpdateModelGroupName/before.csv",
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testUpdateModelGroupName/after.csv"
    )
    void testUpdateModelGroupName() {
        String body = getStringResource("/testUpdateModelGroupName/updateGroup.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/101/modelbids/promotion/groups/2?uid=1", body);
    }

    @Test
    @DisplayName(
            "Проверяем, что возвращается список конфликтов, при этом в список конфликтов не попадает модель у другого вендор - 555")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testUpdateModelGroupWithExistingModels/before.csv",
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testUpdateModelGroupWithExistingModels/after.csv"
    )
    void testUpdateModelGroupWithExistingModels() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testUpdateModelGroupWithExistingModels/report_products.json"))));

        String body = getStringResource("/testUpdateModelGroupWithExistingModels/newGroup.json");
        String expected = getStringResource("/testUpdateModelGroupWithExistingModels/expected.json");
        String response = FunctionalTestHelper.put(
                baseUrl + "/vendors/101/modelbids/promotion/groups/1/models?uid=1", body);
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @Test
    @DisplayName("Проверяем, что с флагом force переносятся модели из конфликтующих групп в обновляемую группу. При этом модель 777 не конфликтующая, потому что она у другого вендора, и просто добавляется как новая модель. Также проверяется, что группы в которых не осталось моделей удаляются")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testUpdateModelGroupWithForce/before.csv",
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testUpdateModelGroupWithForce/after.csv"
    )
    void testUpdateModelGroupWithForce() {
        reportMock.stubFor(get("/?place=brand_products&bsformat=2&vendor_id=999999&pp=7&entities=product&numdoc=1000" +
                "&hyperid=114%2C24%2C233%2C25%2C777%2C12%2C13%2C14")
                .willReturn(aResponse().withBody(
                        getStringResource("/testUpdateModelGroupWithForce/report_products.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> createVendorModelBidBulkResponse(BiddingStatus.APPLIED, 2200, 666666))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());

        String body = getStringResource("/testUpdateModelGroupWithForce/newGroup.json");
        String expected = getStringResource("/testUpdateModelGroupWithForce/expected.json");
        String response = FunctionalTestHelper.put(
                baseUrl + "/vendors/101/modelbids/promotion/groups/1/models?force=true&uid=1", body);
        assertResponseForHistory(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelGroups/before.csv"
    )
    void testGetModelGroups() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups?uid=1");
        String expected = getStringResource("/testGetModelGroups/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelGroupsByTextFilter/before.csv"
    )
    void testGetModelGroupsByTextFilter() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/promotion/groups?uid=1&text=bb");
        String expected = getStringResource("/testGetModelGroupsByTextFilter/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testDeleteModelsInGroup/before.csv",
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testDeleteModelsInGroup/after.csv"
    )
    void testDeleteModelsInGroup() {
        String body = getStringResource("/testDeleteModelsInGroup/newGroup.json");
        FunctionalTestHelper.delete(baseUrl + "/vendors/101/modelbids/promotion/groups/2/models?uid=1", body);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testDeleteModelsInGroupToEmptyGroup/before.csv",
            after = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testDeleteModelsInGroupToEmptyGroup/after.csv"
    )
    void testDeleteModelsInGroupToEmptyGroup() {
        String body = getStringResource("/testDeleteModelsInGroupToEmptyGroup/newGroup.json");
        FunctionalTestHelper.delete(baseUrl + "/vendors/101/modelbids/promotion/groups/2/models?uid=1", body);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetModelsWithBidErrorWrongBidValueNotSynced/before.csv"
    )
    void testGetModelsWithBidErrorWrongBidValueNotSynced() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource(
                        "/testGetModelsWithBidErrorWrongBidValueNotSynced/report_products.json"))));
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("check_models_in_marketplace"))
                .willReturn(aResponse().withBody(getStringResource("/testGetModelsWithBidErrorWrongBidValueNotSynced/report_marketplace.json"))));

        doReturn(100500L).when(mbiBiddingRestClient).putForObject(any(), any());
        doAnswer(invocation -> getVendorModelBidResponses(BiddingStatus.WRONG_BID_VALUE))
                .when(mbiBiddingRestClient)
                .postForObject(any(), any());

        setVendorUserRoles(Collections.singleton(Role.model_bid_user), 1, 101L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/101/modelbids/promotion/models?uid=1");
        String expected = getStringResource("/testGetModelsWithBidErrorWrongBidValueNotSynced/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @Test
    @DisplayName("Проверяем рекомендации для ставок и баннеров")
    @DbUnitDataSet(
            before = ""
    )
    void testModelbidAndBannerRecommendation() {

    }

    @Test
    @DisplayName("Фильтрация по группе, в группе нет моделей с такими диапазоном ставок")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/getFilteredModelIdsTest/before.csv"
    )
    void getFilteredModelIdsTest() {
        Set<Long> responseSet1 = modelsPromotionService.getFilteredModelIds(1L, 1, 1, 101);
        assertEquals(0, responseSet1.size());

        Set<Long> responseSet2 = modelsPromotionService.getFilteredModelIds(1L, 20, null, 101);
        assertEquals(0, responseSet2.size());

        Set<Long> responseSet3 = modelsPromotionService.getFilteredModelIds(1L, null, 9, 101);
        assertEquals(0, responseSet3.size());
    }

    @Test
    @DisplayName("Фильтрация вне группе, нет моделей с такими диапазоном ставок")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/getFilteredModelIdsTest/before.csv"
    )
    void getFilteredModelIdsTest1() {
        Set<Long> responseSet1 = modelsPromotionService.getFilteredModelIds(null, 1, 1, 101);
        assertEquals(0, responseSet1.size());

        Set<Long> responseSet2 = modelsPromotionService.getFilteredModelIds(null, 20, null, 101);
        assertEquals(0, responseSet2.size());

        Set<Long> responseSet3 = modelsPromotionService.getFilteredModelIds(null, null, 9, 101);
        assertEquals(0, responseSet3.size());
    }

    @Test
    @DisplayName("Фильтрация вне группе, есть модели с таким диапазоном ставок в белом и синем")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/testGetFilteredModelIdsMarketAndMarketplace/before.csv"
    )
    void testGetFilteredModelIdsMarketAndMarketplace() {
        Set<Long> responseSet1 = modelsPromotionService.getFilteredModelIds(null, 1, 10, 101);
        assertEquals(2, responseSet1.size());

        Set<Long> responseSet2 = modelsPromotionService.getFilteredModelIds(null, 9, 20, 101);
        assertEquals(2, responseSet2.size());

        Set<Long> responseSet3 = modelsPromotionService.getFilteredModelIds(null, null, 12, 101);
        assertEquals(2, responseSet3.size());
    }

    @Test
    @DisplayName("Фильтрация по группе, есть модели, попадающие в диапазон")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/getFilteredModelIdsTestWithExitModels/before.csv"
    )
    void getFilteredModelIdsTestWithExitModels() {
        Set<Long> responseSet1 = modelsPromotionService.getFilteredModelIds(1L, 5, 10, 101);
        assertEquals(2, responseSet1.size());

        Set<Long> responseSet2 = modelsPromotionService.getFilteredModelIds(1L, 5, null, 101);
        assertEquals(2, responseSet2.size());

        Set<Long> responseSet3 = modelsPromotionService.getFilteredModelIds(1L, null, 10, 101);
        assertEquals(2, responseSet3.size());
    }

    @Test
    @DisplayName("Фильтрация по группе, есть модели, попадающие в диапазон")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ModelsPromotionControllerFunctionalTest/getFilteredModelIdsTestWithExitModels/before.csv"
    )
    void getFilteredModelIdsTestWithExitModels2() {
        Set<Long> responseSet1 = modelsPromotionService.getFilteredModelIds(null, 5, 10, 101);
        assertEquals(2, responseSet1.size());

        Set<Long> responseSet2 = modelsPromotionService.getFilteredModelIds(null, 5, null, 101);
        assertEquals(2, responseSet2.size());

        Set<Long> responseSet3 = modelsPromotionService.getFilteredModelIds(null, null, 10, 101);
        assertEquals(2, responseSet3.size());
    }

    private VendorModelBidBulkResponse getVendorModelBidResponses(BiddingStatus status) {
        Bid bid = new Bid();
        bid.setAt(1499434642L);
        bid.setStatus(status.getStatus());
        bid.setValue(2200);
        VendorModelBidResponse vendorModelBidResponse = new VendorModelBidResponse();
        vendorModelBidResponse.setModel(955287L);
        vendorModelBidResponse.setGroup(0);
        vendorModelBidResponse.setPp(Collections.singletonMap("model_search", bid));
        VendorModelBidBulkResponse vendorModelBidResponses = new VendorModelBidBulkResponse();
        vendorModelBidResponses.add(vendorModelBidResponse);
        return vendorModelBidResponses;
    }

    private VendorModelBidBulkResponse createVendorModelBidBulkResponse(BiddingStatus status,
                                                                        Integer minBid,
                                                                        Integer maxBid) {
        Bid bid = new Bid();
        bid.setAt(1499434642L);
        bid.setStatus(status.getStatus());
        bid.setValue(minBid);
        VendorModelBidResponse vendorModelBidResponse = new VendorModelBidResponse();
        vendorModelBidResponse.setModel(955287L);
        vendorModelBidResponse.setGroup(0);
        vendorModelBidResponse.setPp(Collections.singletonMap("model_search", bid));
        VendorModelBidBulkResponse vendorModelBidResponses = new VendorModelBidBulkResponse();
        vendorModelBidResponses.add(vendorModelBidResponse);

        bid = new Bid();
        bid.setAt(1499434642L);
        bid.setStatus(status.getStatus());
        bid.setValue(maxBid);
        vendorModelBidResponse = new VendorModelBidResponse();
        vendorModelBidResponse.setModel(6031647L);
        vendorModelBidResponse.setGroup(0);
        vendorModelBidResponse.setPp(Collections.singletonMap("model_search", bid));
        vendorModelBidResponses.add(vendorModelBidResponse);

        return vendorModelBidResponses;
    }

    private static String formatLocalToUTC(LocalDateTime dt) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(TimeUtil.UTC).format(dt.atZone(TimeUtil.LOCAL)) + "Z";
    }

    private void assertResponseForPromotion(String expected, String response) {
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG
                .withMatcher("period1start", equalTo(formatLocalToUTC(now().minusDays(90).with(LocalTime.MIN))))
                .withMatcher("period1end", equalTo(formatLocalToUTC(now().minusDays(61).with(LocalTime.MIN))))
                .withMatcher("period2start", equalTo(formatLocalToUTC(now().minusDays(60).with(LocalTime.MIN))))
                .withMatcher("period2end", equalTo(formatLocalToUTC(now().minusDays(31).with(LocalTime.MIN))))
                .withMatcher("period3start", equalTo(formatLocalToUTC(now().minusDays(30).with(LocalTime.MIN))))
                .withMatcher("period3end", equalTo(formatLocalToUTC(now().minusDays(1).with(LocalTime.MIN))))
        );
    }

    private void assertResponseForHistory(String expected, String response) {
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG
                .withMatcher("period1start", equalTo(formatLocalToUTC(now().minusDays(90).with(LocalTime.MIN))))
                .withMatcher("period1end", equalTo(formatLocalToUTC(now().minusDays(61).with(LocalTime.MIN))))
                .withMatcher("period2start", equalTo(formatLocalToUTC(now().minusDays(60).with(LocalTime.MIN))))
                .withMatcher("period2end", equalTo(formatLocalToUTC(now().minusDays(31).with(LocalTime.MIN))))
                .withMatcher("period3start", equalTo(formatLocalToUTC(now().minusDays(30).with(LocalTime.MIN))))
                .withMatcher("period3end", equalTo(formatLocalToUTC(now().minusDays(1).with(LocalTime.MIN))))
                .withMatcher("periodFstart", equalTo(formatLocalToUTC(now().with(LocalTime.MIN))))
                .withMatcher("periodFend", equalTo(formatLocalToUTC(now().plusDays(30).with(LocalTime.MIN))))
        );
    }
}
