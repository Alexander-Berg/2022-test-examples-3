package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmBooleanAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmEnumAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmInt64AttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmNumericAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStringAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStructAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.entity.BmdmEntityToParamValuesConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverterImpl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mdm.http.MdmEntity;

import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.BOX_COUNT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DATACAMP_MASTER_DATA_VERSION;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DELIVERY_TIME;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DOCUMENT_REG_NUMBER;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GTIN;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD_COMMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.HEIGHT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.HIDE_GUARANTEE_PERIOD;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.HIDE_LIFE_TIME;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.HIDE_SHELF_LIFE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.INTERMEDIATE_SHELF_LIFE_PARAMS;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LENGTH;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LIFE_TIME;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LIFE_TIME_COMMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LIFE_TIME_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MANUFACTURER;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MANUFACTURER_COUNTRY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MIN_SHIPMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.QUANTITY_IN_PACK;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.QUANTUM_OF_SUPPLY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SERVICE_EXISTS;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE_COMMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE_PARAMS;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SSKU_HEIGHT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SSKU_LENGTH;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SSKU_SHELF_LIFE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SSKU_SHELF_LIFE_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SSKU_WEIGHT_GROSS;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SSKU_WEIGHT_NET;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SSKU_WEIGHT_TARE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SSKU_WIDTH;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SUPPLY_SCHEDULE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.TIME_UNITS_OPTIONS;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.TRANSPORT_UNIT_SIZE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.USE_IN_MERCURY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.VAT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.VETIS_GUID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.WEIGHT_GROSS;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.WEIGHT_NET;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.WEIGHT_TARE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.WIDTH;

public class BmdmEntityToGoldenBusinessOfferConverterTest extends MdmBaseDbTestClass {

    private static final long TEST_MD_VERSION = 333L;

    @Autowired
    private MetadataProvider metadataProvider;

    @Autowired
    private MdmParamCache mdmParamCache;

    private BmdmEntityToGoldenBusinessOfferConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = createConverter();
    }

    private BmdmEntityToGoldenBusinessOfferConverter createConverter() {
        var bmdmAttributeToMdmParamConverter = new BmdmAttributeToMdmParamConverterImpl(metadataProvider);
        var universalConverter = new BmdmEntityToParamValuesConverterImpl(metadataProvider);
        universalConverter.updateAttributeConverters(
            List.of(
                new BmdmStructAttributeValuesToParamValuesConverter(universalConverter),
                new BmdmEnumAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
                new BmdmStringAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
                new BmdmBooleanAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
                new BmdmInt64AttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
                new BmdmNumericAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter)
            )
        );
        var serviceSskuConverter = new BmdmEntityToGoldenServiceSskuConverterImpl(universalConverter);
        var businessSskuConverter = new BmdmEntityToGoldenBusinessSskuConverterImpl(universalConverter, mdmParamCache);

        return new BmdmEntityToGoldenBusinessOfferConverterImpl(
            metadataProvider,
            serviceSskuConverter,
            businessSskuConverter,
            mdmParamCache
        );
    }

    @Test
    public void shouldConvertCommonSskuToBmdmAndBack() {
        ShopSkuKey serviceSkuKey = new ShopSkuKey(999, "best");
        List<SskuParamValue> goldenServiceParamValues = List.of(
            createSskuParamValue(MIN_SHIPMENT, "minShipment", 6, serviceSkuKey),
            createSskuParamValue(TRANSPORT_UNIT_SIZE, "transportUnitSize", 100, serviceSkuKey),
            createSskuParamValue(QUANTUM_OF_SUPPLY, "quantumOfSupply", 2, serviceSkuKey),
            createSskuParamValue(DELIVERY_TIME, "deliveryTime", 10, serviceSkuKey),
            createSskuParamValue(SERVICE_EXISTS, "serviceExists", true, serviceSkuKey),
            createSskuParamValue(DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", TEST_MD_VERSION, serviceSkuKey),
            createSskuParamValue(VAT, "NDS", new MdmParamOption(1L), serviceSkuKey),
            createSskuParamValue(QUANTITY_IN_PACK, "quantityInPack", 12, serviceSkuKey),
            createSskuParamValue(SUPPLY_SCHEDULE, "supplySchedule", new MdmParamOption(2L), serviceSkuKey)
        );

        ServiceSsku goldenServiceSsku = new ServiceSsku();
        goldenServiceSsku.setKey(serviceSkuKey);
        goldenServiceSsku.setParamValues(goldenServiceParamValues);
        goldenServiceSsku.setMasterDataVersion(TEST_MD_VERSION);

        ShopSkuKey businessSkuKey = new ShopSkuKey(100, "best");
        List<SskuParamValue> goldenBusinessParamValues = List.of(
            // Если в базе вдруг оказался сервисный параметр, попробуем его сохранить:
            // положим его в фейковый сервис в entity
            // при обратной конвертации положим в базу CommonSsku
            createSskuParamValue(SUPPLY_SCHEDULE, "supplySchedule", new MdmParamOption(2L), serviceSkuKey),

            // business params
            createSskuParamValue(MANUFACTURER, "manufacturer", "Яндекс", businessSkuKey),
            createSskuParamValue(MANUFACTURER_COUNTRY, "manufacturerCountry", "Россия", businessSkuKey),
            createSskuParamValue(USE_IN_MERCURY, "useInMercury", true, businessSkuKey),
            createSskuParamValue(BOX_COUNT, "boxCount", 11, businessSkuKey),
            createSskuParamValue(CUSTOMS_COMM_CODE_MDM_ID, "HScode", "0064", businessSkuKey),
            createSskuParamValue(SHELF_LIFE, "LifeShelf", 30, businessSkuKey),
            createSskuParamValue(SHELF_LIFE_UNIT, "ShelfLife_Unit", new MdmParamOption(2L), businessSkuKey),
            createSskuParamValue(SHELF_LIFE_COMMENT, "ShelfLife_Comment", "Аккуратно", businessSkuKey),
            createSskuParamValue(HIDE_SHELF_LIFE, "hide_shelf_life", false, businessSkuKey),
            createSskuParamValue(LIFE_TIME, "ShelfService", "90", businessSkuKey),
            createSskuParamValue(LIFE_TIME_UNIT, "ShelfService_Unit", new MdmParamOption(3L), businessSkuKey),
            createSskuParamValue(LIFE_TIME_COMMENT, "ShelfService_Comment", "Аккуратно!!", businessSkuKey),
            createSskuParamValue(HIDE_LIFE_TIME, "hide_life_time", false, businessSkuKey),
            createSskuParamValue(GUARANTEE_PERIOD, "WarrantyPeriod", "45", businessSkuKey),
            createSskuParamValue(GUARANTEE_PERIOD_UNIT, "WarrantyPeriod_Unit", new MdmParamOption(4L), businessSkuKey),
            createSskuParamValue(GUARANTEE_PERIOD_COMMENT, "WarrantyPeriod_Comment", "Аккуратно??", businessSkuKey),
            createSskuParamValue(HIDE_GUARANTEE_PERIOD, "hide_warranty_period", false, businessSkuKey),
            createSskuParamValue(GTIN, "gtin", "12345678", businessSkuKey),
            createSskuParamValue(VETIS_GUID, "vetisGuids", "guid1", businessSkuKey),
            createSskuParamValue(LENGTH, "mdm_length", 500, businessSkuKey),
            createSskuParamValue(WIDTH, "mdm_width", 100, businessSkuKey),
            createSskuParamValue(HEIGHT, "mdm_height", 20, businessSkuKey),
            createSskuParamValue(WEIGHT_GROSS, "mdm_weight_gross", 40, businessSkuKey),
            createSskuParamValue(WEIGHT_NET, "mdm_weight_net", 30, businessSkuKey),
            createSskuParamValue(WEIGHT_TARE, "mdm_weight_tare", 10, businessSkuKey),
            createSskuParamValue(SSKU_LENGTH, "ssku_mdm_length", 500, businessSkuKey),
            createSskuParamValue(SSKU_WIDTH, "ssku_mdm_width", 100, businessSkuKey),
            createSskuParamValue(SSKU_HEIGHT, "ssku_mdm_height", 20, businessSkuKey),
            createSskuParamValue(SSKU_WEIGHT_GROSS, "ssku_mdm_weight_gross", 40, businessSkuKey),
            createSskuParamValue(SSKU_WEIGHT_NET, "ssku_mdm_weight_net", 30, businessSkuKey),
            createSskuParamValue(SSKU_WEIGHT_TARE, "ssku_mdm_weight_tare", 10, businessSkuKey),
            createSskuParamValue(DOCUMENT_REG_NUMBER, "documentRegNumber", "00040728-00", businessSkuKey),
            createSskuParamValue(DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", TEST_MD_VERSION, businessSkuKey)
        );

        ServiceSsku goldenBusinessSsku = new ServiceSsku();
        goldenBusinessSsku.setKey(businessSkuKey);
        goldenBusinessSsku.setParamValues(goldenBusinessParamValues);
        goldenBusinessSsku.setMasterDataVersion(TEST_MD_VERSION);

        CommonSsku commonSsku = new CommonSsku(businessSkuKey)
            .setBaseSsku(goldenBusinessSsku)
            .putServiceSsku(goldenServiceSsku);

        MdmEntity converted = converter.commonSskuToEntity(commonSsku);
        CommonSsku convertedBack = converter.entityToCommonSsku(converted);

        MdmEntity reconverted = converter.commonSskuToEntity(convertedBack);
        CommonSsku convertedBackAgain = converter.entityToCommonSsku(reconverted);

        Assertions.assertThat(convertedBack).isEqualTo(convertedBackAgain);
        Assertions.assertThat(convertedBack.getBaseValuesByParamId()).containsKeys(
            SSKU_LENGTH, SSKU_WIDTH, SSKU_HEIGHT, SSKU_WEIGHT_GROSS, SSKU_WEIGHT_NET, SSKU_WEIGHT_TARE
        );

        Assertions.assertThat(commonSsku.getKey()).isEqualTo(convertedBackAgain.getKey());
    }

    @Test
    public void testConvertingIntermediateShelfLifeParams() {
        // given
        Random random = new Random("Выполним новую БМДМ пятилетку в четыре года!".hashCode());
        ShopSkuKey shopSkuKey = new ShopSkuKey(12345, "U-238");
        CommonSsku goldSsku = new CommonSsku(shopSkuKey);

        // Intermediate and main shelf life params
        Stream.concat(
                INTERMEDIATE_SHELF_LIFE_PARAMS.stream(),
                SHELF_LIFE_PARAMS.stream()
            )
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .peek(pv -> {
                // Избавимся от unlimited, это отдельным тестом
                if ((pv.getMdmParamId() == SHELF_LIFE_UNIT || pv.getMdmParamId() == SSKU_SHELF_LIFE_UNIT)
                    && TIME_UNITS_OPTIONS.get(pv.getOption().orElseThrow().getId()) == TimeInUnits.TimeUnit.UNLIMITED) {
                    pv.setOption(new MdmParamOption(TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)));
                }
            })
            .forEach(goldSsku::addBaseValue);

        // when
        MdmEntity converted = converter.commonSskuToEntity(goldSsku);
        CommonSsku convertedBack = converter.entityToCommonSsku(converted);

        // then
        Assertions.assertThat(convertedBack).isEqualTo(goldSsku);
    }

    @Test
    public void testConvertingUnlimitedIntermediateShelfLife() {
        // given
        Random random = new Random("Крепи Яндекс Маркет - выполняй пятилетку БМДМ в четыре года!".hashCode());
        ShopSkuKey shopSkuKey = new ShopSkuKey(12345, "U-238");
        CommonSsku goldSsku = new CommonSsku(shopSkuKey);

        INTERMEDIATE_SHELF_LIFE_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(goldSsku::addBaseValue);
        goldSsku.getBaseValue(SSKU_SHELF_LIFE).ifPresent(pv -> pv.setNumeric(BigDecimal.ONE));
        goldSsku.getBaseValue(SSKU_SHELF_LIFE_UNIT).ifPresent(pv ->
            pv.setOption(new MdmParamOption(TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.UNLIMITED))));

        // when
        MdmEntity converted = converter.commonSskuToEntity(goldSsku);
        CommonSsku convertedBack = converter.entityToCommonSsku(converted);

        // then
        Assertions.assertThat(convertedBack).isEqualTo(goldSsku);
    }

    @Test
    public void testConvertingSurplusAndCis() {
        // given
        Random random = new Random("Работать по стахановски!".hashCode());
        ShopSkuKey shopSkuKey = new ShopSkuKey(12345, "U-238");
        CommonSsku goldSsku = new CommonSsku(shopSkuKey);

        Stream.of(KnownMdmParams.CIS, KnownMdmParams.SURPLUS)
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(goldSsku::addBaseValue);

        // when
        MdmEntity converted = converter.commonSskuToEntity(goldSsku);
        CommonSsku convertedBack = converter.entityToCommonSsku(converted);

        // then
        Assertions.assertThat(convertedBack).isEqualTo(goldSsku);
    }

    @Test
    public void testConvertingRsl() {
        // given
        Random random = new Random("Сметем всех саботажников и бюрократов с победного пути БМДМ!".hashCode());

        String shopSku = "U-238";
        ShopSkuKey bizKey = new ShopSkuKey(12345, shopSku);
        ShopSkuKey serviceKey1 = new ShopSkuKey(123456, shopSku);
        ShopSkuKey serviceKey2 = new ShopSkuKey(1234567, shopSku);
        CommonSsku goldSsku = new CommonSsku(bizKey);

        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(goldSsku::addBaseValue);

        List<Long> rslParams = List.of(
            KnownMdmParams.RSL_IN_DAYS,
            KnownMdmParams.RSL_OUT_DAYS,
            KnownMdmParams.RSL_IN_PERCENTS,
            KnownMdmParams.RSL_OUT_PERCENTS
        );

        ServiceSsku serviceSsku1 = new ServiceSsku(serviceKey1);
        rslParams.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(serviceSsku1::addParamValue);
        goldSsku.putServiceSsku(serviceSsku1);

        ServiceSsku serviceSsku2 = new ServiceSsku(serviceKey2);
        rslParams.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(serviceSsku2::addParamValue);
        goldSsku.putServiceSsku(serviceSsku2);

        // when
        MdmEntity converted = converter.commonSskuToEntity(goldSsku);
        CommonSsku convertedBack = converter.entityToCommonSsku(converted);

        // then
        Assertions.assertThat(convertedBack).isEqualTo(goldSsku);
    }

    private SskuParamValue createSskuParamValue(long paramId, String xslName, Object value, ShopSkuKey shopSkuKey) {
        return TestMdmParamUtils.createSskuParamValue(
            paramId,
            shopSkuKey,
            xslName,
            value
        );
    }
}
