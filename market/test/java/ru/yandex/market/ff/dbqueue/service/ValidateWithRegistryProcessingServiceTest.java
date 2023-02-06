package ru.yandex.market.ff.dbqueue.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.ValidateRequestPayload;
import ru.yandex.market.ff.service.RequestItemService;
import ru.yandex.market.ff.service.SupplierMappingService;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuFilter;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.ResultPagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.SearchSkuResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCargoTypesDto;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mdm.http.MasterDataProto;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class ValidateWithRegistryProcessingServiceTest extends IntegrationTest {
    private static final Long SUPPLIER_ID = 1L;
    private static final String SHOP_SKU_1 = "sku1";
    private static final String SHOP_SKU_2 = "sku2";
    private static final String SHOP_SKU_3 = "sku3";
    private static final String SHOP_SKU_4 = "sku4";
    private static final String SHOP_SKU_5 = "sku5";
    private static final long MARKET_SKU_2 = 2L;
    private static final long MARKET_SKU_3 = 3L;
    private static final long MARKET_SKU_4 = 4L;
    private static final long MARKET_SKU_5 = 5L;
    private static final int CARGO_TYPE_1 = 300;
    private static final int CARGO_TYPE_2 = 320;

    @Autowired
    private ValidateWithRegistryProcessingService consumer;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RequestItemService service;

    @Test
    @DatabaseSetup("classpath:db-queue/service/customer_validation/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/customer_validation/after-customer-return.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichRegistry() {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(sku2WithIdentifiers())
                        .addFulfilmentInfo(sku3())
                        .addFulfilmentInfo(sku4())
                        .addFulfilmentInfo(sku5())
                        .build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(
                        Collections.emptyList(), ResultPagination.builder().build(), SearchSkuFilter.empty())
                );

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(600))));

        executeTask(1L);

        service.findAllByRequestId(1L);
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/customer_validation/before_with_box_without_items.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/customer_validation/after_with_box_without_items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldFailOnBoxWithNoItems() {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(sku2())
                        .addFulfilmentInfo(sku3())
                        .addFulfilmentInfo(sku4())
                        .addFulfilmentInfo(sku5())
                        .build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(
                        Collections.emptyList(), ResultPagination.builder().build(), SearchSkuFilter.empty())
                );

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(600))));

        executeTask(1L);

        service.findAllByRequestId(1L);
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/customer_validation/before-unredeemed.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/customer_validation/after-unredeemed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichUnredeemed() {
        jdbcTemplate.update("update request_subtype set " +
                "create_logistic_units_as_initially_accepted = false where id = 1008");
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(sku1())
                        .build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(
                        Collections.emptyList(), ResultPagination.builder().build(), SearchSkuFilter.empty())
                );

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(600))));

        executeTask(1L);
    }


    @Test
    @DatabaseSetup("classpath:db-queue/service/customer_validation/before-boxes-without-order.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/customer_validation/after-boxes-without-order.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichRequestWithoutOrders() {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(sku2())
                        .build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(
                        Collections.emptyList(), ResultPagination.builder().build(), SearchSkuFilter.empty())
                );

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(600))));

        executeTask(1L);
    }


    @Test
    @DatabaseSetup("classpath:db-queue/service/customer_validation/before-return-enrichment.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/customer_validation/after-return-enrichment.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyValidateUpdatableReturn() {
        jdbcTemplate.update("update request_subtype set " +
                "create_logistic_units_as_initially_accepted = false where id = 1008");
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(sku1())
                        .build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(
                        Collections.emptyList(), ResultPagination.builder().build(), SearchSkuFilter.empty())
                );

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(600))));

        executeTask(2L);
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/customer_validation/before-for-order-with-multiple-boxes.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/customer_validation/after-for-order-with-multiple-boxes.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichRegistryForOrderWithMultipleBoxes() {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(sku2WithIdentifiers())
                        .addFulfilmentInfo(sku3())
                        .addFulfilmentInfo(sku4())
                        .addFulfilmentInfo(sku5())
                        .build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(
                        Collections.emptyList(), ResultPagination.builder().build(), SearchSkuFilter.empty())
                );

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(600))));

        executeTask(1L);

        service.findAllByRequestId(1L);
    }


    private void executeTask(long requestId) {
        var payload = new ValidateRequestPayload(requestId);
        transactionTemplate.execute(status -> {
            consumer.processPayload(payload);
            return null;
        });
    }

    private MboMappingsForDelivery.OfferFulfilmentInfo sku1() {
        return sku(SHOP_SKU_1, MARKET_SKU_2, CARGO_TYPE_1).build();
    }

    private MboMappingsForDelivery.OfferFulfilmentInfo sku2() {
        return sku(SHOP_SKU_2, MARKET_SKU_2, CARGO_TYPE_1).build();
    }

    private MboMappingsForDelivery.OfferFulfilmentInfo sku2WithIdentifiers() {
        return sku(SHOP_SKU_2, MARKET_SKU_2, CARGO_TYPE_1)
                .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.CHECK_IMEI_PARAM_ID)
                        .setBoolValue(true)
                        .build())
                .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.IMEI_MASK_PARAM_ID)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue("^(\\d{15}|\\d{17})$").build()
                        )
                        .build())
                .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.CHECK_SN_PARAM_ID)
                        .setBoolValue(true)
                        .build())
                .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.SN_MASK_PARAM_ID)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue(
                                        "^((?:[sS]?[\\dA-Za-z\\/]{10,12})" +
                                                "|([\\dA-Za-z\\/]{14,16})|[\\dA-Za-z\\/]{18,20})$")
                                .build()
                        )
                        .build())
                .build();
    }

    private MboMappingsForDelivery.OfferFulfilmentInfo.Builder sku(String shopSku, long marketSku, int cargoType) {
        MasterDataProto.MasterDataInfo masterDataInfoWithBoxCountGreaterOne = MasterDataProto.MasterDataInfo
                .newBuilder()
                .setProviderProductMasterData(MasterDataProto.ProviderProductMasterData.newBuilder()
                        .setBoxCount(2).build())
                .build();

        return MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                .setSupplierId(SUPPLIER_ID.intValue())
                .setShopSku(shopSku)
                .setMarketSkuId(marketSku)
                .setMasterDataInfo(masterDataInfoWithBoxCountGreaterOne)
                .setMarketCategoryId(10)
                .addCargoTypes(
                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                .setId(cargoType)
                                .build()
                )
                .setShopVendorcode("vendor2")
                .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                        .setNumericValue("2")
                        .build());
    }

    @NotNull
    private MboMappingsForDelivery.OfferFulfilmentInfo sku3() {
        return MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                .setSupplierId(SUPPLIER_ID.intValue())
                .setShopSku(SHOP_SKU_3)
                .setMarketSkuId(MARKET_SKU_3)
                .setMarketCategoryId(11)
                .setShopVendorcode("vendor3")
                .addCargoTypes(
                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                .setId(CARGO_TYPE_2)
                                .build()
                )
                .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                        .setNumericValue("1")
                        .build())
                .build();
    }

    @NotNull
    private MboMappingsForDelivery.OfferFulfilmentInfo sku4() {
        return MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                .setSupplierId(SUPPLIER_ID.intValue())
                .setShopSku(SHOP_SKU_4)
                .setMarketSkuId(MARKET_SKU_4)
                .setMarketCategoryId(12)
                .setShopVendorcode("vendor4")
                .addCargoTypes(
                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                .setId(CARGO_TYPE_1)
                                .build()
                )
                .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                        .setNumericValue("1")
                        .build())
                .build();
    }

    @NotNull
    private MboMappingsForDelivery.OfferFulfilmentInfo sku5() {
        return MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                .setSupplierId(SUPPLIER_ID.intValue())
                .setShopSku(SHOP_SKU_5)
                .setMarketSkuId(MARKET_SKU_5)
                .setMarketCategoryId(13)
                .setShopVendorcode("vendor5")
                .addCargoTypes(
                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                .setId(CARGO_TYPE_1)
                                .build()
                )
                .addModelParam(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                        .setNumericValue("1")
                        .build())
                .build();
    }
}
