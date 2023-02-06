package ru.yandex.market.ff.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableSet;
import com.googlecode.protobuf.format.JsonFormat;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.model.bo.InboundAllowance;
import ru.yandex.market.ff.model.bo.SupplierContentMapping;
import ru.yandex.market.ff.model.bo.SupplierContentMappingDimensions;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.service.implementation.SupplierMappingServiceImpl;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.market.mboc.http.DeliveryParamsStub;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mboc.http.MbocCommon.Message;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mdm.http.MdmCommon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff.client.validation.ValidationConstants.DEFAULT_BOX_COUNT;

/**
 * Unit тесты для {@link SupplierMappingService}.
 *
 * @author avetokhin 19/03/18.
 */
class SupplierMappingServiceTest {

    public static final String IMEI_MASK = "^(\\\\d{15}|\\\\d{17})$";
    private static final Long SUPPLIER_ID = 100500L;
    private static final Long SERVICE_ID = 1337L;
    private static final LocalDateTime REQUESTED_DATE = LocalDateTime.now();
    private static final String SUPPLIER_SKU_1 = "sku1";
    private static final String SUPPLIER_SKU_2 = "sku2";
    private static final String SUPPLIER_SKU_3 = "sku3";
    private static final long MARKET_SKU_1 = 1;
    private static final long MARKET_SKU_2 = 2;
    private static final String TITLE_1 = "title1";
    private static final String TITLE_2 = "title2";
    private static final String MARKET_NAME_2 = "name2";
    private static final String VENDOR_CODE_1 = "vendorCode1";
    private static final String VENDOR_CODE_2 = "vendorCode2";
    private static final String BARCODE_1 = "barcode1";
    private static final String SHOP_BARCODES_1 = "shop_bar1,,,shop_bar2,";
    private static final List<String> SHOP_BARCODES_LIST = List.of("shop_bar1", "shop_bar2");
    private static final String BARCODE_2 = "barcode2";
    private static final Integer PACKAGE_NUM_IN_SPIKE = 12;
    private static final Integer BOX_COUNT = 5;
    private static final long MARKET_CATEGORY_1 = 13;
    private static final long MARKET_CATEGORY_2 = 37;
    private static final int SKU_AVAILABILITY_ACTIVE = 0;
    private static final int SKU_AVAILABILITY_DELISTED = 2;
    private static final String INBOUND_NOT_ALLOWED_MESSAGE_CODE = "TestCode";
    private static final String ATTRIBUTES = "{test:test}";
    private static final String TEMPLATE = "This Is template";
    private static final long WIDTH = 100000;
    private static final long HEIGHT = 101000;
    private static final long LENGTH = 102000;
    private static final MdmCommon.WeightDimensionsInfo MBO_DIMENSIONS = MdmCommon.WeightDimensionsInfo.newBuilder()
            .setBoxWidthUm(WIDTH)
            .setBoxHeightUm(HEIGHT)
            .setBoxLengthUm(LENGTH)
            .build();
    private static final MboMappingsForDelivery.OfferFulfilmentInfo SECOND_ITEM_INFO = createSecondItemInfo();
    private static final MboMappingsForDelivery.OfferFulfilmentInfo THIRD_ITEM_INFO = createThirdItemInfo();
    private DeliveryParams deliveryParamsService;
    private ConcreteEnvironmentParamService concreteEnvironmentParamService;
    private SupplierMappingServiceImpl service;

    private static ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo getMasterDataInfo() {
        return ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo.newBuilder()
                .setProviderProductMasterData(
                        ru.yandex.market.mdm.http.MasterDataProto.ProviderProductMasterData.newBuilder()
                                .setBoxCount(BOX_COUNT)
                                .build())
                .build();
    }

    private static MboMappingsForDelivery.OfferFulfilmentInfo createSecondItemInfo() {
        return MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                .setSupplierId(SUPPLIER_ID.intValue())
                .setShopSku(SUPPLIER_SKU_1)
                .setMarketSkuId(MARKET_SKU_1)
                .setShopTitle(TITLE_1)
                .setShopVendorcode(VENDOR_CODE_1)
                .setMasterDataInfo(getMasterDataInfo())
                .addMskuVendorcode(VENDOR_CODE_1)
                .addMskuBarcode(BARCODE_1)
                .setShopBarcode(SHOP_BARCODES_1)
                .setMarketCategoryId(MARKET_CATEGORY_1)
                .setAvailability(SupplierOffer.Availability.valueOf(SKU_AVAILABILITY_ACTIVE))
                .setAllowInbound(true)
                .addCargoTypes(
                        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder().setId(300L).build()
                )
                .setInboundShelflifeDay(
                        MdmCommon.RemainingShelfLife.newBuilder().setValue(10).build()
                )
                .setOutboundShelflifeDay(
                        MdmCommon.RemainingShelfLife.newBuilder().setValue(0).build()
                )
                .setInboundShelflifePercentage(
                        MdmCommon.RemainingShelfLife.newBuilder().setValue(100).build()
                )
                .setOutboundShelflifePercentage(
                        MdmCommon.RemainingShelfLife.newBuilder().setValue(0).build()
                )
                .build();
    }

    private static MboMappingsForDelivery.OfferFulfilmentInfo createThirdItemInfo() {
        ModelStorage.ParameterValue param1 = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SupplierMappingService.EXPIR_DATE_PARAM_ID)
                .setBoolValue(true)
                .build();
        ModelStorage.ParameterValue param2 = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SupplierMappingService.PACKAGE_NUM_IN_SPIKE_PARAM_ID)
                .setNumericValue(PACKAGE_NUM_IN_SPIKE.toString())
                .build();
        ModelStorage.ParameterValue paramCheckImei = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SupplierMappingService.CHECK_IMEI_PARAM_ID)
                .setBoolValue(true)
                .build();
        ModelStorage.ParameterValue paramCheckSn = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SupplierMappingService.CHECK_SN_PARAM_ID)
                .setBoolValue(false)
                .build();
        ModelStorage.ParameterValue paramImeiMask = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SupplierMappingService.IMEI_MASK_PARAM_ID)
                .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setIsoCode("ru").setValue(IMEI_MASK))
                .build();

        return MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                .setSupplierId(SUPPLIER_ID.intValue())
                .setShopSku(SUPPLIER_SKU_2)
                .setMarketSkuId(MARKET_SKU_2)
                .setMskuTitle(MARKET_NAME_2)
                .setShopTitle(TITLE_2)
                .setShopVendorcode(VENDOR_CODE_2)
                .addMskuVendorcode(VENDOR_CODE_1)
                .addMskuVendorcode(VENDOR_CODE_2)
                .addMskuBarcode(BARCODE_1)
                .addMskuBarcode(BARCODE_2)
                .addModelParam(param1)
                .addModelParam(param2)
                .setMarketCategoryId(MARKET_CATEGORY_2)
                .setAvailability(SupplierOffer.Availability.valueOf(SKU_AVAILABILITY_DELISTED))
                .setAllowInbound(false)
                .setAllowInboundComment(
                        Message.newBuilder()
                                .setMessageCode(INBOUND_NOT_ALLOWED_MESSAGE_CODE)
                                .setJsonDataForMustacheTemplate("{test:test}")
                                .setMustacheTemplate("This Is template")
                                .build()
                )
                .addCargoTypes(
                        0, MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder().setId(300L).build()
                )
                .addCargoTypes(
                        1, MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder().setId(320L).build()
                )
                .setGoldenWeightDimensionsInfo(MBO_DIMENSIONS)
                .setInboundShelflifeDay(
                        MdmCommon.RemainingShelfLife.newBuilder().setValue(10).build()
                )
                .setOutboundShelflifeDay(
                        MdmCommon.RemainingShelfLife.newBuilder().setValue(15).build()
                )
                .setInboundShelflifePercentage(
                        MdmCommon.RemainingShelfLife.newBuilder().setValue(20).build()
                )
                .setOutboundShelflifePercentage(
                        MdmCommon.RemainingShelfLife.newBuilder().setValue(25).build()
                )
                .addModelParam(paramCheckImei)
                .addModelParam(paramCheckSn)
                .addModelParam(paramImeiMask)
                .build();
    }

    @BeforeEach
    void init() {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse1 =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                        .build();

        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse2 =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                        .addFulfilmentInfo(SECOND_ITEM_INFO)
                        .build();

        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse3 =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                        .addFulfilmentInfo(THIRD_ITEM_INFO)
                        .build();

        deliveryParamsService = mock(DeliveryParamsStub.class);
        when(deliveryParamsService.searchFulfilmentSskuParams(any()))
                .thenReturn(mappingResponse1)
                .thenReturn(mappingResponse2)
                .thenReturn(mappingResponse3);

        concreteEnvironmentParamService = mock(ConcreteEnvironmentParamService.class);
        when(concreteEnvironmentParamService.getMboBatchSize()).thenReturn(1);

        service = new SupplierMappingServiceImpl(deliveryParamsService, concreteEnvironmentParamService);
        service.setExecutorService(Executors.newSingleThreadExecutor());
    }

    @Test
    void getMarketSkuMapping() {
        final Map<SupplierSkuKey, SupplierContentMapping> mapping =
                service.getMarketSkuMapping(
                        SUPPLIER_ID, SERVICE_ID, REQUESTED_DATE, Set.of(SUPPLIER_SKU_3, SUPPLIER_SKU_1,
                                SUPPLIER_SKU_2));
        assertThat(mapping, notNullValue());
        assertThat(mapping.size(), equalTo(2));

        assertFirst(mapping.get(new SupplierSkuKey(SUPPLIER_ID, SUPPLIER_SKU_1)));
        assertSecond(mapping.get(new SupplierSkuKey(SUPPLIER_ID, SUPPLIER_SKU_2)));
        verify(deliveryParamsService, times(3)).searchFulfilmentSskuParams(any());
    }

    @Test
    void getMarketSkuMappingFewServices() {
        when(concreteEnvironmentParamService.getMboBatchSize()).thenReturn(10);

        var response = loadMboResponseFromJsonFile();
        when(deliveryParamsService.searchFulfillmentSskuParamsForInterval(any())).thenReturn(response);

        String sku1 = "15081910";
        String sku2 = "PI-GC-14803";
        final Map<String, SupplierContentMapping> mapping =
                service.getMarketSkuMapping(
                        SUPPLIER_ID,
                        Collections.singleton(SERVICE_ID),
                        REQUESTED_DATE,
                        2,
                        Set.of(sku1, sku2));
        assertThat(mapping, notNullValue());
        assertThat(mapping.size(), equalTo(2));

        Map<Long, List<InboundAllowance>> allowance1 = mapping.get(sku1).getInboundAllowancePerService();
        assertThat(allowance1, notNullValue());
        assertThat(allowance1.size(), equalTo(3));
        assertThat(allowance1.keySet(), containsInAnyOrder(145L, 147L, 172L));

        List<InboundAllowance> sofAllowance = allowance1.get(172L);
        assertThat(sofAllowance, notNullValue());
        assertThat(sofAllowance.size(), equalTo(2));

        InboundAllowance firstAllowance = sofAllowance.get(0);
        assertThat(firstAllowance.getFrom(), equalTo(LocalDate.parse("2020-06-18")));
        assertThat(firstAllowance.getTo(), equalTo(LocalDate.parse("2020-06-19")));
        assertThat(firstAllowance.getErrorCode(), equalTo("mboc.msku.error.supply-forbidden.warehouse"));

        InboundAllowance secondAllowance = sofAllowance.get(1);
        assertThat(secondAllowance.getFrom(), equalTo(LocalDate.parse("2020-06-19")));
        assertThat(secondAllowance.getTo(), equalTo(LocalDate.parse("2020-06-19")));
        assertThat(secondAllowance.getErrorCode(), equalTo("mboc.msku.error.supply-forbidden.category.warehouse"));

        Map<Long, List<InboundAllowance>> allowance2 = mapping.get(sku1).getInboundAllowancePerService();
        assertThat(allowance2, notNullValue());
        assertThat(allowance2.size(), equalTo(3));
    }

    @SneakyThrows
    private MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse loadMboResponseFromJsonFile() {
        String mboResponseJson =
                FileContentUtils.getFileContent("service/supplier-mapping-service/two_skus_two_days.json");
        var builder = MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder();
        JsonFormat.merge(mboResponseJson, builder);
        return builder.build();
    }

    @Test
    void getMarketSkuMappingMultiSupplier() {
        when(concreteEnvironmentParamService.getMboBatchSize()).thenReturn(10);
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                        .addFulfilmentInfo(SECOND_ITEM_INFO)
                        .addFulfilmentInfo(THIRD_ITEM_INFO)
                        .build();
        when(deliveryParamsService.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        final Map<SupplierSkuKey, SupplierContentMapping> mapping =
                service.getMarketSkuMapping(Set.of(
                        new SupplierSkuKey(SUPPLIER_ID, SUPPLIER_SKU_1),
                        new SupplierSkuKey(SUPPLIER_ID, SUPPLIER_SKU_2),
                        new SupplierSkuKey(SUPPLIER_ID, SUPPLIER_SKU_3)
                ));

        assertThat(mapping, notNullValue());
        assertThat(mapping.size(), equalTo(2));

        assertFirst(mapping.get(new SupplierSkuKey(SUPPLIER_ID, SUPPLIER_SKU_1)));
        assertSecond(mapping.get(new SupplierSkuKey(SUPPLIER_ID, SUPPLIER_SKU_2)));
        verify(deliveryParamsService).searchFulfilmentSskuParams(any());
    }


    private void assertFirst(final SupplierContentMapping mapping) {
        assertThat(mapping, notNullValue());
        assertThat(mapping, samePropertyValuesAs(
                SupplierContentMapping.builder(SUPPLIER_SKU_1, MARKET_SKU_1, TITLE_1)
                        .setMarketName("")
                        .setVendorCode(VENDOR_CODE_1)
                        .setMarketVendorCodes(Collections.singletonList(VENDOR_CODE_1))
                        .setMarketBarcodes(Collections.singletonList(BARCODE_1))
                        .setShopBarcodes(SHOP_BARCODES_LIST)
                        .setBoxCount(BOX_COUNT)
                        .setMarketCategoryId(MARKET_CATEGORY_1)
                        .setOfferAvailability(SKU_AVAILABILITY_ACTIVE)
                        .setInboundAllowance(InboundAllowance.inboundAllowed())
                        .setCargoTypes(ImmutableSet.of(300))
                        .setInboundRemainingLifetimeDays(10)
                        .setOutboundRemainingLifetimeDays(null)
                        .setInboundRemainingLifetimePercentage(null)
                        .setOutboundRemainingLifetimePercentage(null)
                        .setInboundAllowancePerService(Collections.emptyMap())
                        .setWarehouseServices(Collections.emptyList())
                        .build())
        );
        assertThat(mapping.getDimensions(), nullValue());
    }

    private void assertSecond(final SupplierContentMapping mapping) {
        assertThat(mapping, notNullValue());
        assertThat(mapping, samePropertyValuesAs(
                SupplierContentMapping.builder(SUPPLIER_SKU_2, MARKET_SKU_2, TITLE_2)
                        .setMarketName(MARKET_NAME_2)
                        .setHasExpirationDate(true)
                        .setPackageNumInSpike(PACKAGE_NUM_IN_SPIKE)
                        .setVendorCode(VENDOR_CODE_2)
                        .setMarketVendorCodes(Arrays.asList(VENDOR_CODE_1, VENDOR_CODE_2))
                        .setMarketBarcodes(Arrays.asList(BARCODE_1, BARCODE_2))
                        .setShopBarcodes(List.of())
                        .setBoxCount(DEFAULT_BOX_COUNT)
                        .setMarketCategoryId(MARKET_CATEGORY_2)
                        .setOfferAvailability(SKU_AVAILABILITY_DELISTED)
                        .setInboundAllowance(InboundAllowance.inboundNotAllowed(
                                INBOUND_NOT_ALLOWED_MESSAGE_CODE, ATTRIBUTES, TEMPLATE, "", null, null)
                        )
                        .setInboundAllowancePerService(Collections.emptyMap())
                        .setCargoTypes(ImmutableSet.of(300, 320))
                        .setDimensions(new SupplierContentMappingDimensions(WIDTH, HEIGHT, LENGTH))
                        .setInboundRemainingLifetimeDays(10)
                        .setOutboundRemainingLifetimeDays(15)
                        .setInboundRemainingLifetimePercentage(20)
                        .setOutboundRemainingLifetimePercentage(25)
                        .setCheckImei(1)
                        .setImeiMask(IMEI_MASK)
                        .setCheckSn(0)
                        .setWarehouseServices(Collections.emptyList())
                        .build())
        );
    }
}
