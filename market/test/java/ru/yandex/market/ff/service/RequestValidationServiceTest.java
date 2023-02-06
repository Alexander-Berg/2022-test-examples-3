package ru.yandex.market.ff.service;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.abo.api.entity.rating.operational.PartnerRatingDTO;
import ru.yandex.market.abo.api.entity.rating.operational.RatingPartnerType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.dbqueue.SendMbiNotificationPayload;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FailedFreezeStock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemFreeze;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SimpleStock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Sku;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.enums.SSStockType;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageDuplicateFreezeException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuFilter;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.AvailableStockResponse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.StockFreezingResponse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.ResultPagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.SearchSkuResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCargoTypesDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MdmCommon;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff.service.util.MbiNotificationTypes.SUPPLY_IS_INVALID;

/**
 * Функциональный тест для {@link ru.yandex.market.ff.service.implementation.RequestValidationServiceImpl}.
 *
 * @author avetokhin 15/09/17.
 */
@ContextConfiguration(classes = RequestValidationServiceTest.Conf.class)
@ActiveProfiles("RequestValidationServiceTest")
public class RequestValidationServiceTest extends IntegrationTest {

    private static final Long SUPPLIER_ID = 1L;
    private static final int SERVICE_ID = 145;
    private static final String SHOP_SKU_1 = "SHOPSKU1";
    private static final String SHOP_SKU_2 = "SHOPSKU2";
    private static final String SHOP_SKU_3 = "SHOPSKU3";
    private static final String SHOP_SKU_4 = "SHOPSKU4";
    private static final String SHOP_SKU_5 = "SHOPSKU5";
    private static final String SHOP_SKU_6 = "SHOPSKU6";
    private static final String TITLE_1 = "title1";
    private static final String TITLE_2 = "title2";
    private static final String TITLE_3 = "title3";
    private static final String TITLE_4 = "title4";
    private static final String TITLE_5 = "title5";
    private static final String TITLE_6 = "title6";
    private static final long MARKET_SKU_1 = 1L;
    private static final long MARKET_SKU_2 = 2L;
    private static final long MARKET_SKU_3 = 3L;
    private static final long MARKET_SKU_4 = 4L;
    private static final long MARKET_SKU_5 = 5L;
    private static final long MARKET_SKU_6 = 6L;
    private static final String MARKET_NAME_1 = "market_name1";
    private static final String MARKET_NAME_2 = "market_name2";
    private static final String MARKET_NAME_3 = "market_name3";
    private static final String MARKET_NAME_4 = "market_name4";
    private static final String MARKET_NAME_5 = "market_name5";
    private static final String MARKET_NAME_6 = "market_name6";
    private static final String VENDOR_CODE_1 = "vendorCode1";
    private static final String VENDOR_CODE_2 = "vendorCode2";
    private static final String VENDOR_CODE_3 = "vendorCode3";
    private static final String BARCODE_1 = "barcode1";
    private static final String BARCODE_2 = "barcode2";
    private static final String SHOP_BARCODES_1 = "shop_bar1,,,shop_bar2,";
    private static final int CARGO_TYPE_1 = 300;
    private static final int CARGO_TYPE_2 = 320;
    private static final int CARGO_TYPE_CIS_REQUIRED = 980;
    private static final int CARGO_TYPE_CIS_OPTIONAL = 990;
    private static final int CARGO_TYPE_CIS_DISTINCT = 985;

    private static final String INBOUND_NOT_ALLOWED_MESSAGE_CODE = "InboundNotAllowed";

    private static final String NN_SUPPLY_PARAM_XML_FORMAT = ""
            + "<request-info>"
            + "<id>%s</id>"
            + "<destination-warehouse-id>145</destination-warehouse-id>"
            + "<destination-warehouse-name>test</destination-warehouse-name>"
            + "<merchandise-receipt-date>10 октября</merchandise-receipt-date>"
            + "<merchandise-receipt-time>09:09</merchandise-receipt-time>"
            + "</request-info>";

    private static final String NN_WITHDRAW_PARAM_XML_FORMAT = ""
            + "<request-info>"
            + "<id>%s</id>"
            + "<source-warehouse-id>145</source-warehouse-id>"
            + "<source-warehouse-name>test</source-warehouse-name>"
            + "<merchandise-receipt-date>11 октября</merchandise-receipt-date>"
            + "<merchandise-receipt-time>09:09</merchandise-receipt-time>"
            + "</request-info>";

    @Autowired
    private RequestValidationService requestValidationService;

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private LmsClientCachingService lmsClientCachingService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor;
    public static final ModelStorage.ParameterValue PARAM_1 = ModelStorage.ParameterValue.newBuilder()
            .setParamId(SupplierMappingService.EXPIR_DATE_PARAM_ID)
            .setBoolValue(true)
            .build();
    public static final ModelStorage.ParameterValue PARAM_2 = ModelStorage.ParameterValue.newBuilder()
            .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
            .setNumericValue("32")
            .build();
    public static final MboMappingsForDelivery.OfferFulfilmentInfo.Builder MAPPING_BUILDER_1 =
            MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setSurplusHandleMode(MdmCommon.SurplusHandleMode.ACCEPT)
                    .setSupplierId(SUPPLIER_ID.intValue())
                    .setShopSku(SHOP_SKU_1)
                    .setMarketSkuId(MARKET_SKU_1)
                    .setShopTitle(TITLE_1)
                    .setMskuTitle(MARKET_NAME_1)
                    .setShopVendorcode(VENDOR_CODE_1)
                    .addMskuVendorcode(VENDOR_CODE_1)
                    .addMskuBarcode(BARCODE_1)
                    .addModelParam(PARAM_1)
                    .addModelParam(PARAM_2)
                    .setAllowInbound(true)
                    .addCargoTypes(
                            MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                    .setId(CARGO_TYPE_1)
                                    .build()
                    );
    public static final MboMappingsForDelivery.OfferFulfilmentInfo.Builder MAPPING_BUILDER_2 =
            MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setSurplusHandleMode(MdmCommon.SurplusHandleMode.REJECT)
                    .setSupplierId(SUPPLIER_ID.intValue())
                    .setShopSku(SHOP_SKU_2)
                    .setMarketSkuId(MARKET_SKU_2)
                    .setShopTitle(TITLE_2)
                    .setMskuTitle(MARKET_NAME_2)
                    .setShopVendorcode(VENDOR_CODE_2)
                    .addMskuVendorcode(VENDOR_CODE_1)
                    .addMskuVendorcode(VENDOR_CODE_2)
                    .addMskuBarcode(BARCODE_1)
                    .addMskuBarcode(BARCODE_2)
                    .setShopBarcode(SHOP_BARCODES_1)
                    .addModelParam(PARAM_1);
    public static final MboMappingsForDelivery.OfferFulfilmentInfo.Builder MAPPING_BUILDER_3 =
            MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setSurplusHandleMode(MdmCommon.SurplusHandleMode.NOT_SPECIFIED)
                    .setSupplierId(SUPPLIER_ID.intValue())
                    .setShopSku(SHOP_SKU_3)
                    .setMarketSkuId(MARKET_SKU_3)
                    .setShopTitle(TITLE_3)
                    .setMskuTitle(MARKET_NAME_3)
                    .setShopVendorcode(VENDOR_CODE_3)
                    .addMskuVendorcode(VENDOR_CODE_1)
                    .addMskuVendorcode(VENDOR_CODE_2)
                    .addMskuBarcode(BARCODE_1)
                    .addMskuBarcode(BARCODE_2);
    public static final MboMappingsForDelivery.OfferFulfilmentInfo.Builder MAPPING_BUILDER_4 =
            MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setSupplierId(SUPPLIER_ID.intValue())
                    .setShopSku(SHOP_SKU_4)
                    .setMarketSkuId(MARKET_SKU_4)
                    .setShopTitle(TITLE_4)
                    .setMskuTitle(MARKET_NAME_4)
                    .addModelParam(PARAM_1);
    public static final MboMappingsForDelivery.OfferFulfilmentInfo.Builder MAPPING_BUILDER_5 =
            MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setSupplierId(SUPPLIER_ID.intValue())
                    .setShopSku(SHOP_SKU_5)
                    .setMarketSkuId(MARKET_SKU_5)
                    .setShopTitle(TITLE_5)
                    .setMskuTitle(MARKET_NAME_5)
                    .addModelParam(PARAM_1);
    public static final MboMappingsForDelivery.OfferFulfilmentInfo.Builder MAPPING_BUILDER_6 =
            MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setSupplierId(SUPPLIER_ID.intValue())
                    .setShopSku(SHOP_SKU_6)
                    .setMarketSkuId(MARKET_SKU_6)
                    .setShopTitle(TITLE_6)
                    .setMskuTitle(MARKET_NAME_6)
                    .addModelParam(PARAM_1);

    @BeforeEach
    void init() throws MalformedURLException {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL("http://localhost/errors.csv"));
        when(lmsClient.getPartnerExternalParams(any())).thenReturn(createExternalParamsGroup());
        argumentCaptor = ArgumentCaptor.forClass(SendMbiNotificationPayload.class);
        Mockito.reset(sendMbiNotificationQueueProducer);
    }

    @AfterEach
    void invalidateCache() {
        lmsClientCachingService.invalidateCache();
        verifyNoMoreInteractions(trustworthyInfoClient);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-positive.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-positive.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void validationPositiveWithMboControl() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(
                        Collections.singletonList(buildSku()), ResultPagination.builder().build(),
                        SearchSkuFilter.empty())
                );
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        validateAndPrepareAllNewRequests();
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-success-with-enabled-calendaring.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-success-with-enabled-calendaring.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulValidationWithEnabledCalendaring() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(
                        SearchSkuResponse.of(Collections.singletonList(buildSku()), ResultPagination.builder().build(),
                                SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-3p-success-with-enabled-calendaring.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-3p-success-with-enabled-calendaring.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void thirdPartySuccessfulValidationWithEnabledCalendaring() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(
                        SearchSkuResponse.of(Collections.singletonList(buildSku()), ResultPagination.builder().build(),
                                SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-all-errors-items.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-all-error-items.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void saveAsInvalidIfAllItemsContainErrorAndIgnoreErrorFlagIsTrue() throws StockStorageDuplicateFreezeException {
        final ModelStorage.ParameterValue param = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                .setNumericValue("32")
                .build();

        MasterDataProto.MasterDataInfo masterDataInfo = MasterDataProto.MasterDataInfo.newBuilder()
                .setProviderProductMasterData(
                        MasterDataProto.ProviderProductMasterData.newBuilder().setBoxCount(2).build())
                .build();

        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_1).setMarketSkuId(MARKET_SKU_1).setShopTitle(TITLE_1)
                                .addModelParam(param)
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_4).setMarketSkuId(MARKET_SKU_4).setShopTitle(TITLE_4)
                                .addModelParam(param)
                                .setMasterDataInfo(masterDataInfo)
                                .build())
                        .build();

        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        final StockFreezingResponse response = StockFreezingResponse.notEnough(0L,
                Collections.singletonList(FailedFreezeStock.of(SHOP_SKU_1, SUPPLIER_ID, 1, 3, 0)));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(response);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(SUPPLY_IS_INVALID, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals(supplyParamXml(1), argumentCaptor.getValue().getData());
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/" +
            "before-change-item-count-when-some-items-having-freeze-errors.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/" +
                    "after-change-item-count-when-some-items-having-freeze-errors.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulFreezeIfSomeItemsHavingFreezeErrorsAndIgnoreErrorFlagIsTrue()
            throws StockStorageDuplicateFreezeException {
        when(stockStorageOutboundClient.getFreezes(anyString())).thenReturn(Collections.emptyList());

        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(StockFreezingResponse.notEnough(1L,
                        List.of(FailedFreezeStock.of("SHOPSKU1", 1L, 145, 0, 1))))
                .thenReturn(StockFreezingResponse.success(1L));

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
        verify(stockStorageOutboundClient, never()).getAvailable(anyLong(), anyInt(), any(), anyList());
        verify(stockStorageOutboundClient, times(2)).freezeStocks(any(), anyList());
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/" +
            "before-change-item-count-when-some-items-having-freeze-errors.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/" +
                    "after-change-item-count-when-all-items-having-freeze-errors.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void unSuccessfulFreezeIfAllItemsHavingFreezeErrorsAndIgnoreErrorFlagIsTrue()
            throws StockStorageDuplicateFreezeException {
        when(stockStorageOutboundClient.getFreezes(anyString())).thenReturn(Collections.emptyList());

        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(StockFreezingResponse.notEnough(1L,
                        List.of(FailedFreezeStock.of("SHOPSKU1", 1L, 145, 0, 1))))
                .thenReturn(StockFreezingResponse.notEnough(1L,
                        List.of(FailedFreezeStock.of("SHOPSKU2", 1L, 145, 0, 10))));

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
        verify(stockStorageOutboundClient, never()).getAvailable(anyLong(), anyInt(), any(), anyList());
        verify(stockStorageOutboundClient, times(2)).freezeStocks(any(), anyList());
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-successful-cis-validations.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-successful-cis-validations.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulCisValidations() {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_1).setMarketSkuId(MARKET_SKU_1).setShopTitle(TITLE_1)
                                .setCisHandleMode(MdmCommon.CisHandleMode.ACCEPT_ONLY_DECLARED)
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_CIS_REQUIRED)
                                                .build()
                                )
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_2).setMarketSkuId(MARKET_SKU_2).setShopTitle(TITLE_2)
                                .setCisHandleMode(MdmCommon.CisHandleMode.ACCEPT_ONLY_DECLARED)
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_CIS_DISTINCT)
                                                .build()
                                )
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_3).setMarketSkuId(MARKET_SKU_3).setShopTitle(TITLE_3)
                                .setCisHandleMode(MdmCommon.CisHandleMode.ACCEPT_ONLY_DECLARED)
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_CIS_REQUIRED)
                                                .build()
                                )
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_4).setMarketSkuId(MARKET_SKU_4).setShopTitle(TITLE_4)
                                .setCisHandleMode(MdmCommon.CisHandleMode.ACCEPT_ONLY_DECLARED)
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_CIS_OPTIONAL)
                                                .build()
                                )
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_5).setMarketSkuId(MARKET_SKU_5).setShopTitle(TITLE_5)
                                .setCisHandleMode(MdmCommon.CisHandleMode.NO_RESTRICTION)
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_CIS_REQUIRED)
                                                .build()
                                )
                                .build())
                        .build();

        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null,
                        Set.of(CARGO_TYPE_CIS_REQUIRED, CARGO_TYPE_CIS_OPTIONAL, CARGO_TYPE_CIS_DISTINCT))));

        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-failed-cis-validations.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-failed-cis-validations.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failedCisValidations() {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_1).setMarketSkuId(MARKET_SKU_1).setShopTitle(TITLE_1)
                                .setCisHandleMode(MdmCommon.CisHandleMode.ACCEPT_ONLY_DECLARED)
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_CIS_REQUIRED)
                                                .build()
                                )
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_2).setMarketSkuId(MARKET_SKU_2).setShopTitle(TITLE_2)
                                .setCisHandleMode(MdmCommon.CisHandleMode.ACCEPT_ONLY_DECLARED)
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_CIS_REQUIRED)
                                                .build()
                                )
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_3).setMarketSkuId(MARKET_SKU_3).setShopTitle(TITLE_3)
                                .setCisHandleMode(MdmCommon.CisHandleMode.ACCEPT_ONLY_DECLARED)
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_CIS_REQUIRED)
                                                .build()
                                )
                                .build())
                        .build();

        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null,
                        Set.of(CARGO_TYPE_CIS_REQUIRED, CARGO_TYPE_CIS_OPTIONAL, CARGO_TYPE_CIS_DISTINCT))));

        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-change-item-count.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-change-item-count.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulWithdrawValidationWithChangeItemsCountWhileFreeze() {

        when(stockStorageOutboundClient.getFreezes(anyString())).thenReturn(Collections.emptyList());
        when(stockStorageOutboundClient.getAvailable(anyLong(), anyInt(), any(), anyList()))
                .thenReturn(getAvailableStockResponse(30, 5, 50));

        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-change-item-count.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-change-item-count.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulWithdrawValidationWithChangeItemsCountAndAlreadyExistingFreeze() {

        when(stockStorageOutboundClient.getFreezes(anyString())).thenReturn(getFreezesResponse(30, 5, 20));
        Mockito.verify(stockStorageOutboundClient, never()).getAvailable(anyLong(), anyInt(), any(), anyList());
        Mockito.verify(stockStorageOutboundClient, never()).freezeStocks(any(), anyList());

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-change-item-count.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-change-item-count-filter-non-positive.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulWithdrawValidationWithFilterNonPositiveItems() {

        when(stockStorageOutboundClient.getFreezes(anyString())).thenReturn(Collections.emptyList());
        when(stockStorageOutboundClient.getAvailable(anyLong(), anyInt(), any(), anyList()))
                .thenReturn(getAvailableStockResponse(30, 5, 0));

        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();

        verify(stockStorageOutboundClient).freezeStocks(any(), argThat(list -> list.size() == 2));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-change-item-count.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-negative-not-enough.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void withdrawValidationFailedCauseNotEnoughOnStockByAllPositions() {

        when(stockStorageOutboundClient.getFreezes(anyString())).thenReturn(Collections.emptyList());
        when(stockStorageOutboundClient.getAvailable(anyLong(), anyInt(), any(), anyList()))
                .thenReturn(getAvailableStockResponse(0, -1, 0));

        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();

        verify(stockStorageOutboundClient, never()).freezeStocks(any(), any());
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-change-item-count.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-negative-not-enough.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void withdrawValidationFailedCauseSomeSkusHaveNotBeenFoundByGetAvailableAmount() {

        when(stockStorageOutboundClient.getFreezes(anyString())).thenReturn(Collections.emptyList());
        when(stockStorageOutboundClient.getAvailable(anyLong(), anyInt(), any(), anyList()))
                .thenReturn(AvailableStockResponse.success(
                        StockType.FIT,
                        List.of(new SimpleStock(MARKET_NAME_1, SUPPLIER_ID, SHOP_SKU_1, 0, SERVICE_ID, false)
                        )
                ));

        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();

        verify(stockStorageOutboundClient, never()).freezeStocks(any(), any());
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-withdraw-with-not-enough-quota.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-withdraw-with-not-enough-quota.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulWithdrawValidationWithNotEnoughQuota() {

        when(stockStorageOutboundClient.getFreezes(anyString())).thenReturn(Collections.emptyList());
        when(stockStorageOutboundClient.getAvailable(anyLong(), anyInt(), any(), anyList()))
                .thenReturn(getAvailableStockResponse(30, 5, 50));

        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
    }

    private AvailableStockResponse getAvailableStockResponse(int firstQuantity,
                                                             int secondQuantity,
                                                             int thirdQuantity) {
        return AvailableStockResponse.success(StockType.FIT,
                List.of(
                        new SimpleStock(MARKET_NAME_1, SUPPLIER_ID, SHOP_SKU_1, firstQuantity, SERVICE_ID, false),
                        new SimpleStock(MARKET_NAME_2, SUPPLIER_ID, SHOP_SKU_2, secondQuantity, SERVICE_ID, false),
                        new SimpleStock(MARKET_NAME_3, SUPPLIER_ID, SHOP_SKU_3, thirdQuantity, SERVICE_ID, false)
                )
        );
    }

    private List<SSItemFreeze> getFreezesResponse(int firstQuantity, int secondQuantity, int thirdQuantity) {
        SSItem firstItem = SSItem.of(SHOP_SKU_1, SUPPLIER_ID, SERVICE_ID);
        SSItem secondItem = SSItem.of(SHOP_SKU_2, SUPPLIER_ID, SERVICE_ID);
        SSItem thirdItem = SSItem.of(SHOP_SKU_3, SUPPLIER_ID, SERVICE_ID);
        return List.of(
                SSItemFreeze.of(firstItem, firstQuantity, false, 0, SSStockType.FIT, false),
                SSItemFreeze.of(secondItem, secondQuantity, false, 0, SSStockType.FIT, false),
                SSItemFreeze.of(thirdItem, thirdQuantity, false, 0, SSStockType.FIT, false)
        );
    }


    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-ignore-errors.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-ignore-errors.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulValidationWithIgnoreErrors() throws StockStorageDuplicateFreezeException {
        final ModelStorage.ParameterValue param = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                .setNumericValue("32")
                .build();

        MasterDataProto.MasterDataInfo masterDataInfo = MasterDataProto.MasterDataInfo.newBuilder()
                .setProviderProductMasterData(
                        MasterDataProto.ProviderProductMasterData.newBuilder().setBoxCount(2).build())
                .build();

        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_1).setMarketSkuId(MARKET_SKU_1).setShopTitle(TITLE_1)
                                .addModelParam(param)
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_4).setMarketSkuId(MARKET_SKU_4).setShopTitle(TITLE_4)
                                .addModelParam(param)
                                .setMasterDataInfo(masterDataInfo)
                                .build())
                        .build();

        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        final StockFreezingResponse response = StockFreezingResponse.notEnough(0L,
                Collections.singletonList(FailedFreezeStock.of(SHOP_SKU_1, SUPPLIER_ID, 1, 3, 0)));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(response);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-negative-with-mbo-control.xml")
    @ExpectedDatabase(value = "classpath:service/shop-request-flow/after-negative-with-mbo-control.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void validationNegativeWithMboControl() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_1)
                                .setMarketSkuId(MARKET_SKU_1)
                                .setShopTitle(TITLE_1)
                                .setAllowInbound(false)
                                .setAllowInboundComment(
                                        MbocCommon.Message.newBuilder()
                                                .setMessageCode(INBOUND_NOT_ALLOWED_MESSAGE_CODE)
                                                .setJsonDataForMustacheTemplate("{test:test}")
                                                .setMustacheTemplate("This Is template")
                                                .build()
                                )
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_1)
                                                .build()
                                )
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_2)
                                                .build()
                                )
                                .build())
                        .build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);
        final StockFreezingResponse response = StockFreezingResponse.notEnough(0L,
                Collections.singletonList(FailedFreezeStock.of(SHOP_SKU_1, SUPPLIER_ID, 1, 3, 0)));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(response);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(
                        Collections.emptyList(), ResultPagination.builder().build(), SearchSkuFilter.empty())
                );

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));

        validateAndPrepareAllNewRequests();
        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(SUPPLY_IS_INVALID, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals(supplyParamXml(1), argumentCaptor.getValue().getData());
        verify(lmsClient).getPartnerCargoTypes(anyList());
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-too-long-market-name-and-vendor-code.xml")
    @ExpectedDatabase(value = "classpath:service/shop-request-flow/after-too-long-market-name-and-vendor-code.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void unsuccessfulValidationBecauseOfTooLongMarketNameAndVendorCode() throws StockStorageDuplicateFreezeException {
        String firstMarketName = String.join("", Collections.nCopies(2001, "a"));
        String firstVendorCode = String.join("", Collections.nCopies(3900, "a"));
        String secondMarketName = String.join("", Collections.nCopies(2000, "a"));
        String secondVendorCode = String.join("", Collections.nCopies(3901, "a"));
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_1)
                                .setMarketSkuId(MARKET_SKU_1)
                                .setShopTitle(TITLE_1)
                                .setAllowInbound(true)
                                .setMskuTitle(firstMarketName)
                                .setShopVendorcode(firstVendorCode)
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_2)
                                .setMarketSkuId(MARKET_SKU_2)
                                .setShopTitle(TITLE_2)
                                .setAllowInbound(true)
                                .setMskuTitle(secondMarketName)
                                .setShopVendorcode(secondVendorCode)
                                .build())
                        .build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);
        StockFreezingResponse response = StockFreezingResponse.notEnough(0L,
                Collections.singletonList(FailedFreezeStock.of(SHOP_SKU_1, SUPPLIER_ID, 1, 3, 0)));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(response);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));

        validateAndPrepareAllNewRequests();
    }

    /**
     * Валидирует 2 заявки от одного поставщика к складу, проходящие по лимиту.
     */
    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/shop-request-validation/before-positive-limit.xml"),
            @DatabaseSetup("classpath:params/check-supply-limits.xml")})
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/after-positive-limit.xml",
            assertionMode = NON_STRICT)
    public void validateLimitsPositive() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(
                        SearchSkuResponse.of(Collections.singletonList(buildSku()), ResultPagination.builder().build(),
                                SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
    }

    /**
     * На складе кончилась квота по палетам.
     * Но так как поставка не календаризируется, она все равно должна быть создана.
     */
    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-limits-with-empty-pallets-quota-for-warehouse.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/after-limits-with-empty-pallets-quota-for-warehouse.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void validateLimitsWithEmptyPalletsQuotaForWarehouse() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(),
                        SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-transfers-to-incorrect-inbounds.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/after-transfers-to-incorrect-inbounds.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void validateTransfersForIncorrectInbounds() throws StockStorageDuplicateFreezeException {
        when(stockStorageOutboundClient.getFreezes("2")).thenReturn(List.of());
        when(stockStorageOutboundClient.getFreezes("3")).thenReturn(List.of());
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder().build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        validateAndPrepareAllNewRequests();
        verify(deliveryParams, times(2)).searchFulfilmentSskuParams(any());
        verify(stockStorageOutboundClient).getFreezes("2");
        verify(stockStorageOutboundClient).getFreezes("3");
        verifyNoMoreInteractions(
                stockStorageOutboundClient, sendMbiNotificationQueueProducer, stockStorageSearchClient, deliveryParams);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-transfers-to-correct-inbounds.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/after-transfers-to-correct-inbounds.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void validateTransfersForCorrectInbounds() throws StockStorageDuplicateFreezeException {
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder().build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        validateAndPrepareAllNewRequests();
        verify(deliveryParams, times(2)).searchFulfilmentSskuParams(any());
        verifyZeroInteractions(sendMbiNotificationQueueProducer, stockStorageSearchClient, deliveryParams);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-transfers-to-utilization.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/after-transfers-to-utilization.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void validateTransfersForUtilization() throws StockStorageDuplicateFreezeException {
        AvailableStockResponse availableStockResponse = AvailableStockResponse.success(StockType.DEFECT,
                List.of(
                        new SimpleStock(MARKET_NAME_1, SUPPLIER_ID, SHOP_SKU_1, 2, SERVICE_ID, false),
                        new SimpleStock(MARKET_NAME_2, SUPPLIER_ID, SHOP_SKU_2, 0, SERVICE_ID, false)
                )
        );
        when(stockStorageOutboundClient.getAvailable(eq(SUPPLIER_ID), eq(SERVICE_ID), eq(StockType.DEFECT), anyList()))
                .thenReturn(availableStockResponse);
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                        .addFulfilmentInfo(setDimensionsIfRequired(true, createWeightDimensions(),
                                MAPPING_BUILDER_1.clone())
                                .setMasterDataInfo(MasterDataProto.MasterDataInfo.newBuilder()
                                        .setProviderProductMasterData(
                                                MasterDataProto.ProviderProductMasterData.newBuilder().setBoxCount(2)
                                                        .build())
                                        .build())
                                .build())
                        .addFulfilmentInfo(setDimensionsIfRequired(true, createWeightDimensions(),
                                MAPPING_BUILDER_2.clone())
                                .setMasterDataInfo(MasterDataProto.MasterDataInfo.newBuilder()
                                        .setProviderProductMasterData(
                                                MasterDataProto.ProviderProductMasterData.newBuilder().setBoxCount(2)
                                                        .build())
                                        .build())
                                .build())
                        .build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        validateAndPrepareAllNewRequests();
        verify(deliveryParams, times(2)).searchFulfilmentSskuParams(any());
        verifyZeroInteractions(sendMbiNotificationQueueProducer, stockStorageSearchClient, deliveryParams);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-xdoc-supply-without-relations.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/after-xdoc-supply-without-relations.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void validateXDocWhenThereIsNotXDocFulfillmentRelation() throws StockStorageDuplicateFreezeException {
        when(lmsClient.getXDocPartnerRelations(145L)).thenReturn(Collections.emptyList());
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-valid-unredeemed.xml")
    @ExpectedDatabase(value = "classpath:service/shop-request-validation/after-valid-unredeemed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void validateValidUnredeemed() {
        assertReturnRegistryValidationCorrect();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-invalid-unredeemed.xml")
    @ExpectedDatabase(value = "classpath:service/shop-request-validation/after-invalid-unredeemed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void validateInvalidUnredeemed() {
        assertReturnRegistryValidationCorrect();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-customer-return.xml")
    @ExpectedDatabase(value = "classpath:service/shop-request-validation/after-customer-return.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void validateCustomerReturn() {
        transactionTemplate.execute(status -> {
            assertReturnRegistryValidationCorrect();
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-with-enabled-rating-check.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-unsuccessful-rating-validation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void unsuccessfulRatingValidation() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));
        when(aboApi.getPartnerRating(SUPPLIER_ID, RatingPartnerType.FULFILLMENT))
                .thenReturn(createPartnerRating(33.22, false));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-with-enabled-rating-check.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-successful-validation-with-low-rating.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulValidationWithLowRating() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));
        when(aboApi.getPartnerRating(SUPPLIER_ID, RatingPartnerType.FULFILLMENT))
                .thenReturn(createPartnerRating(33.22, true));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-with-enabled-rating-check.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-successful-rating-in-interval-validation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulValidationWithRatingInInterval() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));
        when(aboApi.getPartnerRating(SUPPLIER_ID, RatingPartnerType.FULFILLMENT))
                .thenReturn(createPartnerRating(53.32, false));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-with-enabled-rating-check.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-successful-rating-without-interval-validation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulValidationWithRatingWithoutInterval() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));
        when(aboApi.getPartnerRating(SUPPLIER_ID, RatingPartnerType.FULFILLMENT))
                .thenReturn(createPartnerRating(97.99, false));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-with-enabled-rating-check.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-successful-validation-with-abo-failure.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulValidationWithAboFailure() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));
        when(aboApi.getPartnerRating(SUPPLIER_ID, RatingPartnerType.FULFILLMENT))
                .thenThrow(new RuntimeException("Connection timeout"));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-with-enabled-rating-check.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-successful-validation-for-vip-supplier.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulValidationForVipSupplier() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));
        when(aboApi.getPartnerRating(SUPPLIER_ID, RatingPartnerType.FULFILLMENT))
                .thenReturn(createPartnerRating(33.22, true, true));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-with-enabled-rating-check-for-newbie.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-successful-validation-for-newbie.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulValidationForNewbie() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));
        validateAndPrepareAllNewRequests();
        verifyZeroInteractions(aboApi);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-utilization-outbound-validation.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-utilization-outbound-validation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void utilizationOutboundValidation() throws StockStorageDuplicateFreezeException {
        validateAndPrepareAllNewRequests();
        verifyZeroInteractions(deliveryParams, sendMbiNotificationQueueProducer,
                stockStorageOutboundClient, stockStorageSearchClient);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-flow/before-utilization-outbound-without-child-validation.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-flow/after-utilization-outbound-without-child-validation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void utilizationOutboundWithoutChildOutboundsValidation() throws StockStorageDuplicateFreezeException {
        validateAndPrepareAllNewRequests();
        verifyZeroInteractions(deliveryParams, sendMbiNotificationQueueProducer,
                stockStorageOutboundClient, stockStorageSearchClient);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-xdoc-supply-without-date.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/after-xdoc-supply-without-date.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void validationPositiveWithEmptyRequestedDate() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(
                        Collections.singletonList(buildSku()), ResultPagination.builder().build(),
                        SearchSkuFilter.empty())
                );
        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-expendable-materials-on-incorrect-service.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/after-expendable-materials-on-incorrect-service.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void validateExpendableMaterialsOnIncorrectService() throws StockStorageDuplicateFreezeException {
        validateAndPrepareAllNewRequests();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-validation/before-movement-supply-on-incorrect-service.xml")
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/after-movement-supply-on-incorrect-service.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void validateMovementSupplyOnIncorrectService() throws StockStorageDuplicateFreezeException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        validateAndPrepareAllNewRequests();
    }

    /**
     * SHOP_SKU_1 - нет маппинга
     * SHOP_SKU_2 - спаечный товар
     * SHOP_SKU_3 - товар с некорректным карготипом
     * SHOP_SKU_4 - отсутствует в предыдущих поставках
     * SHOP_SKU_5 - полностью корректный товар
     */
    private void assertReturnRegistryValidationCorrect() {
        MasterDataProto.MasterDataInfo masterDataInfoWithBoxCountGreaterOne = MasterDataProto.MasterDataInfo
                .newBuilder()
                .setProviderProductMasterData(MasterDataProto.ProviderProductMasterData.newBuilder()
                        .setBoxCount(2).build())
                .build();
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse
                        .newBuilder()
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_2)
                                .setMarketSkuId(MARKET_SKU_2)
                                .setMasterDataInfo(masterDataInfoWithBoxCountGreaterOne)
                                .setMarketCategoryId(10)
                                .addCargoTypes(
                                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                                                .setId(CARGO_TYPE_1)
                                                .build()
                                )
                                .setShopVendorcode("vendor2")
                                .addModelParam(ModelStorage.ParameterValue.newBuilder()
                                        .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                                        .setNumericValue("2")
                                        .build())
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
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
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
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
                                .build())
                        .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
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
                                .build())
                        .build();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);
        StockFreezingResponse response = StockFreezingResponse.notEnough(0L,
                Collections.singletonList(FailedFreezeStock.of(SHOP_SKU_1, SUPPLIER_ID, 1, 3, 0)));
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(response);

        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(
                        Collections.emptyList(), ResultPagination.builder().build(), SearchSkuFilter.empty())
                );

        when(lmsClient.getPartnerCargoTypes(anyList()))
                .thenReturn(List.of(new PartnerCargoTypesDto(null, null, Set.of(CARGO_TYPE_1))));

        validateAndPrepareAllNewRequests();
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
        verify(lmsClient, times(1)).getPartnerCargoTypes(anyList());
    }

    private void validateAndPrepareAllNewRequests() {
        shopRequestFetchingService.getNotInternalRequestsByStatusAndTypes(RequestStatus.CREATED,
                Arrays.asList(RequestType.values()))
                .forEach(request -> requestValidationService.validateAndPrepare(request));
    }

    private String supplyParamXml(long requestId) {
        return String.format(NN_SUPPLY_PARAM_XML_FORMAT, requestId);
    }

    private String withdrawParamXml(long requestId) {
        return String.format(NN_WITHDRAW_PARAM_XML_FORMAT, requestId);
    }

    private MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse buildMappingResponse(
            boolean hasWeightDimensionsInfoInMdm
    ) {
        return MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                .addFulfilmentInfo(setDimensionsIfRequired(hasWeightDimensionsInfoInMdm, createWeightDimensions(),
                        MAPPING_BUILDER_1.clone())
                        .build())
                .addFulfilmentInfo(setDimensionsIfRequired(hasWeightDimensionsInfoInMdm, createWeightDimensions(),
                        MAPPING_BUILDER_2.clone())
                        .build())
                .addFulfilmentInfo(setDimensionsIfRequired(hasWeightDimensionsInfoInMdm, createWeightDimensions(),
                        MAPPING_BUILDER_3.clone())
                        .build())
                .addFulfilmentInfo(setDimensionsIfRequired(hasWeightDimensionsInfoInMdm, createWeightDimensions(),
                        MAPPING_BUILDER_4.clone())
                        .build())
                .addFulfilmentInfo(setDimensionsIfRequired(hasWeightDimensionsInfoInMdm, createLargeWeightDimensions(),
                        MAPPING_BUILDER_5.clone())
                        .build())
                .addFulfilmentInfo(setDimensionsIfRequired(hasWeightDimensionsInfoInMdm, createLargeWeightDimensions(),
                        MAPPING_BUILDER_6.clone())
                        .build())
                .build();
    }

    private Sku buildSku() {
        return Sku.builder()
                .withUnitId(SSItem.of(SHOP_SKU_2, SUPPLIER_ID.intValue(), SERVICE_ID))
                .withStocks(Collections.singletonList(Stock.of(1, 0, 1, "type")))
                .withEnabled(true)
                .withUpdatable(true)
                .build();
    }

    private MboMappingsForDelivery.OfferFulfilmentInfo.Builder setDimensionsIfRequired(
            boolean required,
            MdmCommon.WeightDimensionsInfo dimensions,
            MboMappingsForDelivery.OfferFulfilmentInfo.Builder builder
    ) {
        if (required) {
            builder.setGoldenWeightDimensionsInfo(dimensions);
        }
        return builder;
    }

    private MdmCommon.WeightDimensionsInfo createWeightDimensions() {
        return MdmCommon.WeightDimensionsInfo.newBuilder()
                .setBoxWidthUm(100000)
                .setBoxHeightUm(101000)
                .setBoxLengthUm(102000)
                .build();
    }

    private MdmCommon.WeightDimensionsInfo createLargeWeightDimensions() {
        return MdmCommon.WeightDimensionsInfo.newBuilder()
                .setBoxWidthUm(3000000)
                .setBoxHeightUm(3001000)
                .setBoxLengthUm(3002000)
                .build();
    }

    private List<PartnerExternalParamGroup> createExternalParamsGroup() {
        PartnerExternalParam param = new PartnerExternalParam(
                PartnerExternalParamType.IS_CALENDARING_ENABLED.name(), "", "true");
        PartnerExternalParamGroup group =
                new PartnerExternalParamGroup(145L, Collections.singletonList(param));
        return Collections.singletonList(group);
    }

    private PartnerRatingDTO createPartnerRating(double rating, boolean inboundAllowedOnLowRating) {
        return createPartnerRating(rating, inboundAllowedOnLowRating, false);
    }

    private PartnerRatingDTO createPartnerRating(double rating,
                                                 boolean inboundAllowedOnLowRating,
                                                 boolean ignoreInboundRestrictions) {
        return new PartnerRatingDTO(
                SUPPLIER_ID,
                rating,
                10,
                new ArrayList<>(),
                inboundAllowedOnLowRating,
                ignoreInboundRestrictions
        );
    }

    @Configuration
    @Profile("RequestValidationServiceTest")
    public static class Conf {
        @Bean
        @Primary
        public ErrorDocumentGenerationService errorDocumentGenerationService() {
            final ErrorDocumentGenerationService service = mock(ErrorDocumentGenerationService.class);
            when(service.generateDocumentFile(any(), any(), anyBoolean()))
                    .thenReturn(new ByteArrayInputStream(new byte[]{}));
            return service;
        }
    }
}
