package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.FlatSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.reference.BmdmExternalReferenceFilter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmBooleanAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmEnumAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmInt64AttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmNumericAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStringAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStructAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.entity.BmdmEntityToParamValuesConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.BmdmPathKeeper;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.MdmEntity;

import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType.MEASUREMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType.SUPPLIER;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue.SskuSilverTransportType.DATACAMP;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SERVICE_SSKU_ATTR_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SERVICE_SSKU_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SERVICE_SSKU_SERVICE_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SILVER_BUSINESS_SSKU_ATTR_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SILVER_BUSINESS_SSKU_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SILVER_COMMON_SSKU_BUSINESS_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SILVER_COMMON_SSKU_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SILVER_COMMON_SSKU_SHOP_SKU;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SILVER_COMMON_SSKU_SOURCE_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SILVER_COMMON_SSKU_SOURCE_TYPE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SILVER_SERVICE_SSKU_ATTR_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.SILVER_SERVICE_SSKU_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DATACAMP_MASTER_DATA_VERSION;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MANUFACTURER_COUNTRY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MIN_SHIPMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.QUANTUM_OF_SUPPLY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS;
import static ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.AdditionalParamProcessor.INT64_CONVERTER;
import static ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.AdditionalParamProcessor.STRING_CONVERTER;

public class BmdmEntityToSilverCommonSskuConverterTest extends MdmBaseDbTestClass {
    private static final MasterDataSource TEST_SOURCE = new MasterDataSource(SUPPLIER, "supplier");
    private static final long TEST_MD_VERSION = 99L;

    @Autowired
    private MetadataProvider metadataProvider;

    @Autowired
    private MdmParamCache mdmParamCache;

    private BmdmEntityToSilverCommonSskuConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = createConverter();
    }

    private BmdmEntityToSilverCommonSskuConverter createConverter() {
        BmdmAttributeToMdmParamConverter bmdmAttributeToMdmParamConverter =
            new BmdmAttributeToMdmParamConverterImpl(metadataProvider);
        BmdmEntityToParamValuesConverterImpl universalConverter =
            new BmdmEntityToParamValuesConverterImpl(metadataProvider);
        universalConverter.updateAttributeConverters(List.of(
            new BmdmStructAttributeValuesToParamValuesConverter(universalConverter),
            new BmdmEnumAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmStringAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmBooleanAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmInt64AttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmNumericAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter)
        ));
        var serviceSskuConverter = new BmdmEntityToSilverServiceSskuConverterImpl(universalConverter, mdmParamCache);
        var businessSskuConverter = new BmdmEntityToSilverBusinessSskuConverterImpl(universalConverter, mdmParamCache);

        return new BmdmEntityToSilverCommonSskuConverterImpl(
            serviceSskuConverter,
            businessSskuConverter,
            metadataProvider,
            mdmParamCache
        );
    }

    @Test
    public void shouldConvertSilverCommonSskuToBmdmAndBack() {
        // given
        // Service
        ShopSkuKey serviceSkuKey = new ShopSkuKey(999, "best");
        List<SskuSilverParamValue> silverServiceParamValues = List.of(
            createSskuSilverParamValue(MIN_SHIPMENT, "minShipment", 6, serviceSkuKey)
        );

        SilverServiceSsku silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(serviceSkuKey, TEST_SOURCE));
        silverServiceSsku.setParamValues(silverServiceParamValues);
        silverServiceSsku.setMasterDataVersion(TEST_MD_VERSION);

        // Business
        ShopSkuKey businessSkuKey = new ShopSkuKey(100, "best");
        List<SskuSilverParamValue> silverBusinessParamValues = List.of(
            createSskuSilverParamValue(MANUFACTURER_COUNTRY, "manufacturerCountry", "Ру", businessSkuKey)
        );

        SilverServiceSsku silverBusinessSsku = new SilverServiceSsku();
        silverBusinessSsku.setKey(new SilverSskuKey(businessSkuKey, TEST_SOURCE));
        silverBusinessSsku.setParamValues(silverBusinessParamValues);
        silverBusinessSsku.setMasterDataVersion(TEST_MD_VERSION);

        // Common
        SilverCommonSsku silverCommonSsku = new SilverCommonSsku(silverBusinessSsku.getKey())
            .setBaseSsku(silverBusinessSsku)
            .putServiceSsku(silverServiceSsku);


        // when
        MdmEntity converted = converter.silverCommonSskuToEntity(silverCommonSsku);
        SilverCommonSsku convertedBack = converter.entityToSilverCommonSsku(converted);
        // and make one more lap to make sure that all technical BMDM fields were set
        MdmEntity reconverted = converter.silverCommonSskuToEntity(convertedBack);
        SilverCommonSsku convertedBackAgain = converter.entityToSilverCommonSsku(reconverted);

        // then
        Assertions.assertThat(convertedBack).isEqualTo(convertedBackAgain);
        Assertions.assertThat(silverCommonSsku.getBusinessKey()).isEqualTo(convertedBackAgain.getBusinessKey());
    }

    @Test
    public void shouldConvertSilverCommonSskuToBmdmAndBackWithOnlyBusiness() {
        // given
        // Business
        ShopSkuKey businessSkuKey = new ShopSkuKey(100, "best");
        List<SskuSilverParamValue> silverBusinessParamValues = List.of(
            createSskuSilverParamValue(MANUFACTURER_COUNTRY, "manufacturerCountry", "Ру", businessSkuKey)
        );

        SilverServiceSsku silverBusinessSsku = new SilverServiceSsku();
        silverBusinessSsku.setKey(new SilverSskuKey(businessSkuKey, TEST_SOURCE));
        silverBusinessSsku.setParamValues(silverBusinessParamValues);
        silverBusinessSsku.setMasterDataVersion(TEST_MD_VERSION);

        // Common
        SilverCommonSsku silverCommonSsku = new SilverCommonSsku(silverBusinessSsku.getKey())
            .setBaseSsku(silverBusinessSsku);

        // when
        MdmEntity converted = converter.silverCommonSskuToEntity(silverCommonSsku);
        SilverCommonSsku convertedBack = converter.entityToSilverCommonSsku(converted);
        // and make one more lap to make sure that all technical BMDM fields were set
        MdmEntity reconverted = converter.silverCommonSskuToEntity(convertedBack);
        SilverCommonSsku convertedBackAgain = converter.entityToSilverCommonSsku(reconverted);

        // then
        Assertions.assertThat(convertedBack).isEqualTo(convertedBackAgain);

    }

    @Test
    public void testConvertingSilverWithDifferentMDVersions() {
        // given
        Random random = new Random(12345L);
        MasterDataSource source = new MasterDataSource(SUPPLIER, "IRIS: 100");
        String shopSku = "U-238";
        int bizId = 100;
        int serviceId = 99;

        SilverSskuKey businessKey = new SilverSskuKey(new ShopSkuKey(bizId, shopSku), source);
        SilverServiceSsku business = new SilverServiceSsku(businessKey);
        Stream.concat(WEIGHT_DIMENSIONS_PARAMS.stream(), Stream.of(DATACAMP_MASTER_DATA_VERSION))
            .map(mdmParamCache::get)
            .peek(param -> Assertions.assertThat(param).isNotNull())
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(business::addParamValue);

        SilverSskuKey serviceKey = new SilverSskuKey(new ShopSkuKey(serviceId, shopSku), source);
        SilverServiceSsku service = new SilverServiceSsku(serviceKey);
        Stream.of(MIN_SHIPMENT, QUANTUM_OF_SUPPLY, DATACAMP_MASTER_DATA_VERSION)
            .map(mdmParamCache::get)
            .peek(param -> Assertions.assertThat(param).isNotNull())
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(service::addParamValue);

        long businessMdVersion = business.getParamValue(DATACAMP_MASTER_DATA_VERSION)
            .flatMap(MdmParamValue::getNumeric)
            .map(BigDecimal::longValueExact)
            .orElseThrow();
        long servicesMdVersion = service.getParamValue(DATACAMP_MASTER_DATA_VERSION)
            .flatMap(MdmParamValue::getNumeric)
            .map(BigDecimal::longValueExact)
            .orElseThrow();
        Assertions.assertThat(businessMdVersion).isNotEqualTo(servicesMdVersion);

        SilverCommonSsku silverCommonSsku = new SilverCommonSsku(businessKey)
            .setBaseSsku(business)
            .putServiceSsku(service);

        // when
        MdmEntity converted = converter.silverCommonSskuToEntity(silverCommonSsku);
        SilverCommonSsku convertedBack = converter.entityToSilverCommonSsku(converted);
        MdmEntity reconverted = converter.silverCommonSskuToEntity(convertedBack);
        SilverCommonSsku convertedBackAgain = converter.entityToSilverCommonSsku(reconverted);

        // then
        Assertions.assertThat(TestBmdmUtils.removeBmdmIdAndVersion(convertedBack))
            .isEqualTo(silverCommonSsku);
        Assertions.assertThat(TestBmdmUtils.removeBmdmIdAndVersion(convertedBackAgain))
            .isEqualTo(silverCommonSsku);
        Assertions.assertThat(convertedBackAgain.getBaseSsku().getMasterDataVersion()).isEqualTo(businessMdVersion);
        Assertions.assertThat(convertedBackAgain.getServiceSsku(serviceId))
            .map(FlatSsku::getMasterDataVersion)
            .hasValue(servicesMdVersion);
    }

    @Test
    public void testConvertingFullSilver() {
        // given
        Random random = new Random("Протокол Литвинова".hashCode());
        SilverCommonSsku richSilver = fullRandomSilver(
            100,
            List.of(97, 98, 99),
            "Центральная-фабрика-Союзкино(Мосфильм)",
            new MasterDataSource(MEASUREMENT, "145"),
            random
        );

        // when
        MdmEntity converted = converter.silverCommonSskuToEntity(richSilver);
        SilverCommonSsku convertedBack = converter.entityToSilverCommonSsku(converted);

        // then
        Assertions.assertThat(TestBmdmUtils.removeBmdmIdAndVersion(convertedBack))
            .isEqualTo(richSilver);
    }

    @Test
    public void testConvertingSilverWithEmptyBusiness() {
        // given
        Random random = new Random("si vis pacem para bellum".hashCode());
        MasterDataSource source = new MasterDataSource(SUPPLIER, "Арзамас-16");
        String shopSku = "Pu-239";
        int bizId = 1949;
        int serviceId = 1950;
        SilverCommonSsku silver = new SilverCommonSsku(new SilverSskuKey(new ShopSkuKey(bizId, shopSku), source))
            .putServiceSsku(
                fullRandomSilverService(new SilverSskuKey(new ShopSkuKey(serviceId, shopSku), source), random)
            );

        // when
        MdmEntity converted = converter.silverCommonSskuToEntity(silver);
        SilverCommonSsku convertedBack = converter.entityToSilverCommonSsku(converted);

        // then
        Assertions.assertThat(TestBmdmUtils.removeBmdmIdAndVersion(convertedBack))
            .isEqualTo(silver);
    }

    @Test
    public void testMdmEntityToAndBackConversion() {
        // given
        Random random = new Random("Bmdm is the best thing ever".hashCode());

        SilverSskuKey key = new SilverSskuKey(1949, "Pu-239", SUPPLIER, "Арзамас-16");
        long mdmId = 815;
        long version = 908;

        MdmEntity mdmEntity = MdmEntity.newBuilder()
            .setMdmId(mdmId)
            .setMdmEntityTypeId(SILVER_COMMON_SSKU_ID)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(version))
            .putMdmAttributeValues(SILVER_COMMON_SSKU_SOURCE_TYPE, MdmAttributeValues.newBuilder()
                .setMdmAttributeId(SILVER_COMMON_SSKU_SOURCE_TYPE)
                .addValues(STRING_CONVERTER.apply(key.getSourceType().name()))
                .build())
            .putMdmAttributeValues(SILVER_COMMON_SSKU_SOURCE_ID, MdmAttributeValues.newBuilder()
                .setMdmAttributeId(SILVER_COMMON_SSKU_SOURCE_ID)
                .addValues(STRING_CONVERTER.apply(key.getSourceId()))
                .build())
            .putMdmAttributeValues(SILVER_COMMON_SSKU_SHOP_SKU, MdmAttributeValues.newBuilder()
                .setMdmAttributeId(SILVER_COMMON_SSKU_SHOP_SKU)
                .addValues(STRING_CONVERTER.apply(key.getShopSku()))
                .build())
            .putMdmAttributeValues(SILVER_COMMON_SSKU_BUSINESS_ID, MdmAttributeValues.newBuilder()
                .setMdmAttributeId(SILVER_COMMON_SSKU_BUSINESS_ID)
                .addValues(INT64_CONVERTER.apply((long) key.getSupplierId()))
                .build())
            .putMdmAttributeValues(SILVER_BUSINESS_SSKU_ATTR_ID, MdmAttributeValues.newBuilder()
                .setMdmAttributeId(SILVER_BUSINESS_SSKU_ATTR_ID)
                .addValues(MdmAttributeValue.newBuilder()
                    .setStruct(TestBmdmUtils.createFullRandomEntity(
                            SILVER_BUSINESS_SSKU_ID,
                            random,
                            metadataProvider,
                            Map.of(TestBmdmUtils.TIME_ENTITY_TYPE_ID, (r, mp) -> Optional.empty()))
                        .map(entity -> TestBmdmUtils.recursivelyUpdateExistingSourceMeta(
                            entity,
                            MdmBase.MdmSourceMeta.newBuilder()
                                .setSourceType(key.getSourceType().name())
                                .setSourceId(key.getSourceId())
                                .build()))
                        .orElseThrow()))
                .build())
            // fake service
            .putMdmAttributeValues(
                SILVER_SERVICE_SSKU_ATTR_ID,
                TestBmdmUtils.createSingleStructValue(
                    SILVER_SERVICE_SSKU_ATTR_ID,
                    MdmEntity.newBuilder()
                        .setMdmEntityTypeId(SILVER_SERVICE_SSKU_ID)
                        .putMdmAttributeValues(
                            SERVICE_SSKU_ATTR_ID,
                            TestBmdmUtils.createSingleStructValue(
                                SERVICE_SSKU_ATTR_ID,
                                MdmEntity.newBuilder()
                                    .setMdmEntityTypeId(SERVICE_SSKU_ID)
                                    .putMdmAttributeValues(SERVICE_SSKU_SERVICE_ID, MdmAttributeValues.newBuilder()
                                        .setMdmAttributeId(SERVICE_SSKU_SERVICE_ID)
                                        .addValues(INT64_CONVERTER.apply((long) key.getSupplierId()))
                                        .build())
                                    .build()))
                        .build()))
            .build();

        //when
        SilverCommonSsku converted = converter.entityToSilverCommonSsku(mdmEntity);
        MdmEntity convertedBack = converter.silverCommonSskuToEntity(converted);

        //then
        Assertions.assertThat(convertedBack).isEqualTo(mdmEntity);
    }

    @Test
    public void testMovingBaseParamsFromServicesToBasePart() {
        // given
        Random random = new Random("GG WP".hashCode());

        int serviceId = 12;
        int businessId = 13;
        String shopSku = "U-238";
        ShopSkuKey serviceKey = new ShopSkuKey(serviceId, shopSku);
        ShopSkuKey businessKey = new ShopSkuKey(businessId, shopSku);

        MasterDataSource source = new MasterDataSource(MasterDataSourceType.MEASUREMENT, "172");
        SilverSskuKey silverServiceKey = new SilverSskuKey(serviceKey, source);
        SilverSskuKey businessSilverKey = new SilverSskuKey(businessKey, source);

        MdmParam shelfLifeCommentParam = mdmParamCache.get(KnownMdmParams.SHELF_LIFE_COMMENT);
        MdmParam lifeTimeCommentParam = mdmParamCache.get(KnownMdmParams.LIFE_TIME_COMMENT);
        SilverCommonSsku silverCommonSsku = new SilverCommonSsku(businessSilverKey)
            .addBaseValue(TestMdmParamUtils.createRandomMdmParamValue(random, shelfLifeCommentParam))
            .putServiceSsku((SilverServiceSsku) new SilverServiceSsku(silverServiceKey)
                .addParamValue(TestMdmParamUtils.createRandomMdmParamValue(random, shelfLifeCommentParam))
                .addParamValue(TestMdmParamUtils.createRandomMdmParamValue(random, lifeTimeCommentParam)));

        // when
        SilverCommonSsku convertedToAndBack =
            converter.entityToSilverCommonSsku(converter.silverCommonSskuToEntity(silverCommonSsku));

        // then
        SskuSilverParamValue resultingShelfLifeComment =
            convertedToAndBack.getBaseValue(KnownMdmParams.SHELF_LIFE_COMMENT).orElseThrow();
        SskuSilverParamValue resultingLifeTimeComment =
            convertedToAndBack.getBaseValue(KnownMdmParams.LIFE_TIME_COMMENT).orElseThrow();
        Assertions.assertThat(resultingShelfLifeComment)
            .isEqualTo(silverCommonSsku.getBaseValue(KnownMdmParams.SHELF_LIFE_COMMENT).orElseThrow());
        Assertions.assertThat(resultingLifeTimeComment.setSupplierId(serviceId))
            .isEqualTo(silverCommonSsku.getServiceSsku(serviceId)
                .orElseThrow()
                .getParamValue(KnownMdmParams.LIFE_TIME_COMMENT)
                .orElseThrow());
        Assertions.assertThat(resultingShelfLifeComment.setSupplierId(serviceId))
            .isNotEqualTo(silverCommonSsku.getServiceSsku(serviceId)
                .orElseThrow()
                .getParamValue(KnownMdmParams.SHELF_LIFE_COMMENT)
                .orElseThrow());
    }

    private SilverCommonSsku fullRandomSilver(int bizId,
                                              Collection<Integer> serviceIds,
                                              String shopSku,
                                              MasterDataSource source,
                                              Random random) {
        SilverSskuKey businessKey = new SilverSskuKey(bizId, shopSku, source.getSourceType(), source.getSourceId());
        SilverCommonSsku result = new SilverCommonSsku(businessKey);
        result.setBaseSsku(fullRandomSilverBusiness(businessKey, random));
        serviceIds.stream()
            .map(id -> new SilverSskuKey(id, shopSku, source.getSourceType(), source.getSourceId()))
            .map(key -> fullRandomSilverService(key, random))
            .forEach(result::putServiceSsku);
        return result;
    }

    private SilverServiceSsku fullRandomSilverBusiness(SilverSskuKey key, Random random) {
        SilverServiceSsku result = new SilverServiceSsku(key);

        Map<Long, MdmParamValue> values = TestMdmParamUtils.fixUnlimitedValues(
            TestMdmParamUtils.createRandomMdmParamValues(random, businessParams())
        );

        result.addParamValues(values.values());
        result.syncMasterDataVersionWithParams();
        return result;
    }

    private SilverServiceSsku fullRandomSilverService(SilverSskuKey key, Random random) {
        SilverServiceSsku result = new SilverServiceSsku(key);
        result.addParamValues(TestMdmParamUtils.createRandomMdmParamValues(random, serviceParams()).values());
        result.syncMasterDataVersionWithParams();
        return result;
    }

    private Set<MdmParam> serviceParams() {
        return extractParams(
            new BmdmPathKeeper()
                .addBmdmEntity(SILVER_COMMON_SSKU_ID)
                .addBmdmAttribute(SILVER_SERVICE_SSKU_ATTR_ID)
                .addBmdmEntity(SILVER_SERVICE_SSKU_ID)
                .getPath()
        );
    }

    private Set<MdmParam> businessParams() {
        return extractParams(
            new BmdmPathKeeper()
                .addBmdmEntity(SILVER_COMMON_SSKU_ID)
                .addBmdmAttribute(SILVER_BUSINESS_SSKU_ATTR_ID)
                .addBmdmEntity(SILVER_BUSINESS_SSKU_ID)
                .getPath()
        );
    }

    private Set<MdmParam> extractParams(MdmBase.MdmPath pathPrefix) {
        BmdmExternalReferenceFilter bmdmExternalReferenceFilter = new BmdmExternalReferenceFilter(
            pathPrefix,
            Set.of(MdmBase.MdmExternalSystem.OLD_MDM),
            Set.of(MdmBase.MdmMetaType.MDM_ATTR)
        );
        return metadataProvider.findExternalReferences(bmdmExternalReferenceFilter).stream()
            .map(MdmBase.MdmExternalReference::getExternalId)
            .filter(Predicate.not(KnownMdmParams.UNLIMITED_PARAMS::contains))
            .map(mdmParamCache::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private SskuSilverParamValue createSskuSilverParamValue(long paramId, String xslName,
                                                            Object value,
                                                            ShopSkuKey shopSkuKey) {
        return TestMdmParamUtils.createSskuSilverParamValue(paramId, xslName,
            value,
            TEST_SOURCE.getSourceType(), TEST_SOURCE.getSourceId(),
            shopSkuKey,
            TEST_MD_VERSION,
            DATACAMP);
    }
}
