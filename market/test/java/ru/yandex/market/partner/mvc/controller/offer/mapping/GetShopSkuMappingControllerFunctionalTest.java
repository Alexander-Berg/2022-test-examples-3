package ru.yandex.market.partner.mvc.controller.offer.mapping;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.SyncAPI.SyncChangeOffer;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.offer.mapping.TimePeriodWithUnits;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.MappingProcessingStatus;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo;
import static ru.yandex.market.mdm.http.MasterDataProto.ProviderProductMasterData;
import static ru.yandex.market.mdm.http.MasterDataProto.ProviderProductSupplyEvent;
import static ru.yandex.market.mdm.http.MasterDataProto.ProviderProductSupplySchedule;

/**
 * Функциональные тесты на на {@link MappingController}.
 */
// AbstractMappingControllerFunctionalTest экстендит общий для ПИ FunctionText
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "MappingControllerFunctionalTest.csv")
class GetShopSkuMappingControllerFunctionalTest
        extends AbstractMappingControllerFunctionalTest /* extends FunctionText */ {

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @Autowired
    private DataCampClient dataCampShopClient;

    @Test
    @DisplayName("Поиск оферов по Shop-SKU")
    void testQueryByShopSku() {
        Mockito.when(patientMboMappingsService.searchMappingsByKeys(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test 1")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("1")
                                                .setMarketCategoryId(123)
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build())
                                                .addUrls("https://beru.ru/product/100324822646")
                                                .build())
                                .build());
        doReturnOffersFromJson("proto/containsOffer.json");
        String url = String.format("%s/shop-skus/by-shop-sku?shop_sku={shopSku}", mappingUrl(CAMPAIGN_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url, "H123");
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "{\n" +
                                        "  \"shopSku\":\"1\",\n" +
                                        "  \"title\":\"Test 1\",\n" +
                                        "  \"barcodes\":[\n" +
                                        "\n" +
                                        "  ],\n" +
                                        "  \"description\":\"\",\n" +
                                        "  \"vendorCode\":\"\",\n" +
                                        "  \"offerProcessingStatus\":\"IN_WORK\",\n" +
                                        "  \"offerProcessingComments\":[],\n" +
                                        "  \"availability\":\"ACTIVE\",\n" +
                                        "  \"urls\":[\"https://beru.ru/product/100324822646\"],\n" +
                                        "  \"marketCategoryId\": 123,\n" +
                                        "  \"marketCategoryName\":\"Электроника\",\n" +
                                        "      \"price\": 3.0000000,\n" +
                                        "      \"oldPrice\": 2.0000000,\n" +
                                        "      \"vat\": 7,\n" +
                                        "  \"acceptGoodContent\": true,\n" +
                                        "  \"mappings\":{\n" +
                                        "    \"rejected\":[\n" +
                                        "\n" +
                                        "    ],\n" +
                                        "    \"active\":{\n" +
                                        "      \"marketSku\":1288,\n" +
                                        "       \"contentType\": \"market\",\n" +
                                        "       \"categoryName\": \"\"" +
                                        "    },\n" +
                                        "    \"awaitingModeration\":{\n" +
                                        "      \"marketSku\":1214,\n" +
                                        "       \"contentType\": \"market\",\n" +
                                        "       \"categoryName\": \"\"" +
                                        "    }\n" +
                                        "  }\n" +
                                        "}"))));
        Mockito.verify(patientMboMappingsService)
                .searchMappingsByKeys(Mockito.argThat(request -> {
                    return request.getKeysCount() == 1
                            && request.getKeys(0).hasSupplierId()
                            && request.getKeys(0).getSupplierId() == 774
                            && request.getKeys(0).hasShopSku()
                            && request.getKeys(0).getShopSku().equals("H123")
                            && request.hasReturnMasterData()
                            && request.getReturnMasterData();
                }));
    }

    @Test
    @DisplayName("Поиск оферов по Shop-SKU и отображение мастер-данных")
    void testQueryByShopSkuWithMasterData() {
        Mockito.when(patientMboMappingsService.searchMappingsByKeys(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H123")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H123")
                                                .setMarketCategoryId(101)
                                                .setMasterDataInfo(MasterDataInfo.newBuilder()
                                                        .setShelfLifeWithUnits(TimePeriodWithUnits.ofDays(10).getTime())
                                                        .setGuaranteePeriodWithUnits(TimePeriodWithUnits.ofDays(180).getTime())
                                                        .setLifeTimeWithUnits(TimePeriodWithUnits.ofYears(1).getTime())
                                                        .setProviderProductMasterData(ProviderProductMasterData.newBuilder()
                                                                .setCustomsCommodityCode("HG405235")
                                                                .setMinShipment(100)
                                                                .setDeliveryTime(3)
                                                                .setManufacturer("ОАО Ромашка")
                                                                .addManufacturerCountry("Россия")
                                                                .addManufacturerCountry("Казахстан")
                                                                .setQuantumOfSupply(20)
                                                                .setTransportUnitSize(10)
                                                                .setSupplySchedule(ProviderProductSupplySchedule.newBuilder()
                                                                        .addSupplyEvent(ProviderProductSupplyEvent.newBuilder()
                                                                                .setDayOfWeek(2)
                                                                                .build())
                                                                        .addSupplyEvent(ProviderProductSupplyEvent.newBuilder()
                                                                                .setDayOfWeek(5)
                                                                                .build())
                                                                        .build())
                                                                .setBoxCount(5)
                                                                .build())
                                                        .build())
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build())
                                                .addUrls("https://beru.ru/product/100324822646")
                                                .build())
                                .build());
        doReturnOffersFromJson("proto/emptyOffers.json");
        String url = String.format("%s/shop-skus/by-shop-sku?shop_sku={shopSku}", mappingUrl(CAMPAIGN_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url, "H123");
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "{\n"
                                        + "  \"shopSku\": \"H123\",\n"
                                        + "  \"title\": \"Test H123\",\n"
                                        + "  \"barcodes\": [],\n"
                                        + "  \"description\":\"\",\n"
                                        + "  \"vendorCode\":\"\",\n"
                                        + "  \"masterData\": {\n"
                                        + "    \"supplyScheduleDays\": [\n"
                                        + "      \"TUESDAY\",\n"
                                        + "      \"FRIDAY\"\n"
                                        + "    ],\n"
                                        + "    \"shelfLife\": {\n"
                                        + "      \"timePeriod\": 10,\n"
                                        + "      \"timeUnit\": \"DAY\"\n"
                                        + "    },\n"
                                        + "    \"lifeTime\": {\n"
                                        + "      \"timePeriod\": 1,\n"
                                        + "      \"timeUnit\": \"YEAR\"\n"
                                        + "    },\n"
                                        + "    \"guaranteePeriod\": {\n"
                                        + "      \"timePeriod\": 180,\n"
                                        + "      \"timeUnit\": \"DAY\"\n"
                                        + "    },\n"
                                        + "    \"manufacturer\": \"ОАО Ромашка\",\n"
                                        + "    \"manufacturerCountry\": \"Россия, Казахстан\",\n"
                                        + "    \"minShipment\": 100,\n"
                                        + "    \"transportUnitSize\": 10,\n"
                                        + "    \"quantumOfSupply\": 20,\n"
                                        + "    \"deliveryDurationDays\": 3,\n"
                                        + "    \"shelfLifeDays\": 10,\n"
                                        + "    \"lifeTimeDays\": 365,\n"
                                        + "    \"guaranteePeriodDays\": 180\n,"
                                        + "    \"boxCount\": 5,\n"
                                        + "    \"customsCommodityCodes\": [\"HG405235\"]\n"
                                        + "  },\n"
                                        + "  \"urls\": [\n"
                                        + "    \"https://beru.ru/product/100324822646\"\n"
                                        + "  ],\n"
                                        + "  \"offerProcessingComments\": [],\n"
                                        + "  \"availability\": \"ACTIVE\",\n"
                                        + "  \"acceptGoodContent\": false,\n"
                                        + "  \"marketCategoryId\": 101,\n"
                                        + "  \"marketCategoryName\":\"Все товары\",\n"
                                        + "  \"mappings\": {\n"
                                        + "    \"active\": {\n"
                                        + "      \"marketSku\": 1288,\n"
                                        + "       \"contentType\": \"market\",\n"
                                        + "       \"categoryName\": \"\""
                                        + "    },\n"
                                        + "    \"awaitingModeration\": {\n"
                                        + "      \"marketSku\": 1214,\n"
                                        + "       \"contentType\": \"market\",\n"
                                        + "       \"categoryName\": \"\""
                                        + "    },\n"
                                        + "    \"rejected\": []\n"
                                        + "  },\n"
                                        + "  \"offerProcessingStatus\": \"IN_WORK\"\n"
                                        + "}"))));
    }

    @Test
    @DisplayName("Поиск оферов по Shop-SKU: смотрим что не передаём на фронт свойство \"masterData\","
            + " когда там внутри пусто")
    void testQueryByShopSkuWithEmptyMasterData() {
        Mockito.when(patientMboMappingsService.searchMappingsByKeys(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H123")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H123")
                                                .setMasterDataInfo(MasterDataInfo.newBuilder().build())
                                                .setMarketCategoryId(123)
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build())
                                                .addUrls("https://beru.ru/product/100324822646")
                                                .build())
                                .build());
        doReturnOffersFromJson("proto/emptyOffers.json");
        String url = String.format("%s/shop-skus/by-shop-sku?shop_sku={shopSku}", mappingUrl(CAMPAIGN_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url, "H123");
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "\n" +
                                        "{\n" +
                                        "    \"shopSku\":\"H123\",\n" +
                                        "    \"title\":\"Test H123\",\n" +
                                        "    \"barcodes\": [],\n" +
                                        "  \"description\":\"\",\n" +
                                        "  \"vendorCode\":\"\",\n" +
                                        "    \"offerProcessingStatus\":\"IN_WORK\", \n" +
                                        "    \"offerProcessingComments\":[], \n" +
                                        "    \"marketCategoryId\":123,\n" +
                                        "    \"marketCategoryName\":\"Электроника\",\n" +
                                        "    \"acceptGoodContent\":true,\n" +
                                        "    \"availability\":\"ACTIVE\",\n" +
                                        "    \"urls\":[\"https://beru.ru/product/100324822646\"],\n" +
                                        "    \"mappings\":{\n" +
                                        "        \"rejected\":[],\n" +
                                        "        \"active\":{" +
                                        "       \"marketSku\":1288," +
                                        "       \"contentType\": \"market\",\n" +
                                        "       \"categoryName\": \"\"" +
                                        "},\n" +
                                        "        \"awaitingModeration\":{" +
                                        "       \"marketSku\":1214," +
                                        "       \"contentType\": \"market\",\n" +
                                        "       \"categoryName\": \"\"" +
                                        "}\n" +
                                        "    }\n" +
                                        "}\n"))));
    }

    @Test
    @DisplayName("Поиск оферов по Shop-SKU и"
            + " отображение результатов со всеми поддерживаемыми полями оферов")
    void testQueryByShopSkuWithAllProperties() {
        Mockito.when(patientMboMappingsService.searchMappingsByKeys(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H123")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H123")
                                                .setShopCategoryName("Shop/Category/Name")
                                                .setBarcode("sdkgjsdh12431254, sdjgh124314231, dskjghs124152")
                                                .setVendorCode("sgsd23523")
                                                .setShopVendor("Apple")
                                                .setDescription("Test H123 Description")
                                                .setMarketCategoryId(123)
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build())
                                                .addUrls("https://beru.ru/product/100324822646")
                                                .build())
                                .build());
        doReturnOffersFromJson("proto/emptyOffers.json");
        String url = String.format("%s/shop-skus/by-shop-sku?shop_sku={shopSku}", mappingUrl(CAMPAIGN_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url, "H123");
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "{\n" +
                                        "  \"shopSku\":\"H123\",\n" +
                                        "  \"title\":\"Test H123\",\n" +
                                        "  \"barcodes\":[\n" +
                                        "    \"sdkgjsdh12431254\",\n" +
                                        "    \"sdjgh124314231\",\n" +
                                        "    \"dskjghs124152\"\n" +
                                        "  ],\n" +
                                        "  \"offerProcessingStatus\":\"IN_WORK\", \n" +
                                        "  \"offerProcessingComments\":[], \n" +
                                        "  \"acceptGoodContent\":true,\n" +
                                        "  \"marketCategoryId\":123,\n" +
                                        "  \"marketCategoryName\":\"Электроника\",\n" +
                                        "  \"availability\":\"ACTIVE\",\n" +
                                        "  \"urls\":[\"https://beru.ru/product/100324822646\"],\n" +
                                        "  \"vendorCode\":\"sgsd23523\",\n" +
                                        "  \"categoryName\":\"Shop/Category/Name\",\n" +
                                        "  \"brand\":\"Apple\",\n" +
                                        "  \"description\":\"Test H123 Description\",\n" +
                                        "  \"mappings\":{\n" +
                                        "    \"rejected\":[\n" +
                                        "\n" +
                                        "    ],\n" +
                                        "    \"active\":{\n" +
                                        "      \"marketSku\":1288,\n" +
                                        "       \"contentType\": \"market\",\n" +
                                        "       \"categoryName\": \"\"" +
                                        "    },\n" +
                                        "    \"awaitingModeration\":{\n" +
                                        "      \"marketSku\":1214,\n" +
                                        "       \"contentType\": \"market\",\n" +
                                        "       \"categoryName\": \"\"" +
                                        "    }\n" +
                                        "  }\n" +
                                        "}"))));
    }

    @Test
    @DisplayName("Поиск оферов по Shop-SKU")
    void testQueryByShopSkuWithSuggestedMapping() {
        Mockito.when(patientMboMappingsService.searchMappingsByKeys(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H123")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H123")
                                                .setMarketCategoryId(101)
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build())
                                                .setSuggestMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build())
                                                .addUrls("https://beru.ru/product/100324822646")
                                                .build())
                                .build());
        doReturnOffersFromJson("proto/emptyOffers.json");
        String url = String.format("%s/shop-skus/by-shop-sku?shop_sku={shopSku}", mappingUrl(CAMPAIGN_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url, "H123");
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/
                                        "{\n" +
                                        "  \"shopSku\":\"H123\",\n" +
                                        "  \"title\":\"Test H123\",\n" +
                                        "  \"description\":\"\",\n" +
                                        "  \"vendorCode\":\"\",\n" +
                                        "  \"barcodes\":[\n" +
                                        "\n" +
                                        "  ],\n" +
                                        "  \"offerProcessingStatus\":\"IN_WORK\",\n" +
                                        "  \"offerProcessingComments\":[],\n" +
                                        "  \"availability\":\"ACTIVE\",\n" +
                                        "  \"marketCategoryId\":101,\n" +
                                        "  \"marketCategoryName\":\"Все товары\",\n" +
                                        "  \"acceptGoodContent\":false,\n" +
                                        "  \"urls\":[\"https://beru.ru/product/100324822646\"],\n" +
                                        "  \"mappings\":{\n" +
                                        "    \"rejected\":[\n" +
                                        "\n" +
                                        "    ],\n" +
                                        "    \"awaitingModeration\":{\n" +
                                        "      \"marketSku\":1214,\n" +
                                        "      \"contentType\":\"market\",\n" +
                                        "       \"categoryName\":\"\"\n" +
                                        "    },\n" +
                                        "    \"suggested\":{\n" +
                                        "      \"marketSku\":1288,\n" +
                                        "      \"contentType\":\"market\",\n" +
                                        "       \"categoryName\":\"\"\n" +
                                        "    }\n" +
                                        "  }\n" +
                                        "}"))));
        Mockito.verify(patientMboMappingsService)
                .searchMappingsByKeys(Mockito.argThat(request -> {
                    return request.getKeysCount() == 1
                            && request.getKeys(0).hasSupplierId()
                            && request.getKeys(0).getSupplierId() == 774
                            && request.getKeys(0).hasShopSku()
                            && request.getKeys(0).getShopSku().equals("H123")
                            && request.hasReturnMasterData()
                            && request.getReturnMasterData();
                }));
    }

    private void doReturnOffersFromJson(String jsonPath) {
        doReturn(ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.FullOfferResponse.class,
                jsonPath,
                getClass()
        )).when(dataCampShopClient).getOffers(anyLong(), any(), any());
    }
}
