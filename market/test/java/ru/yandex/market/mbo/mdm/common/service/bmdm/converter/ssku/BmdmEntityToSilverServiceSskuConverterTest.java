package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku;

import java.util.List;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
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
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.MdmEntity;

import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType.SUPPLIER;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue.SskuSilverTransportType.DATACAMP;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DATACAMP_MASTER_DATA_VERSION;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DELIVERY_TIME;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MIN_SHIPMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.QUANTITY_IN_PACK;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.QUANTUM_OF_SUPPLY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SERVICE_EXISTS;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SUPPLY_SCHEDULE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.TRANSPORT_UNIT_SIZE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.VAT;

public class BmdmEntityToSilverServiceSskuConverterTest extends MdmBaseDbTestClass {

    private static final MasterDataSource TEST_SOURCE = new MasterDataSource(SUPPLIER, "supplier");
    private static final long TEST_MD_VERSION = 99L;

    @Autowired
    private MetadataProvider metadataProvider;

    @Autowired
    private MdmParamCache mdmParamCache;

    private BmdmEntityToSilverServiceSskuConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = createConverter();
    }

    private BmdmEntityToSilverServiceSskuConverter createConverter() {
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
        return new BmdmEntityToSilverServiceSskuConverterImpl(universalConverter, mdmParamCache);
    }

    @Test
    public void shouldConvertSilverServiceSskuToBmdmAndBack() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        List<SskuSilverParamValue> silverParamValues = List.of(
            createSskuSilverParamValue(MIN_SHIPMENT, "minShipment", 6, skuKey),
            createSskuSilverParamValue(TRANSPORT_UNIT_SIZE, "transportUnitSize", 100, skuKey),
            createSskuSilverParamValue(QUANTUM_OF_SUPPLY, "quantumOfSupply", 2, skuKey),
            createSskuSilverParamValue(DELIVERY_TIME, "deliveryTime", 10, skuKey),
            createSskuSilverParamValue(SERVICE_EXISTS, "serviceExists", true, skuKey),
            createSskuSilverParamValue(
                DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", TEST_MD_VERSION, skuKey),
            createSskuSilverParamValue(VAT, "NDS", new MdmParamOption(1L), skuKey),
            createSskuSilverParamValue(QUANTITY_IN_PACK, "quantityInPack", 12, skuKey),
            createSskuSilverParamValue(SUPPLY_SCHEDULE, "supplySchedule", new MdmParamOption(2L), skuKey)
        );

        SilverServiceSsku silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(skuKey, TEST_SOURCE));
        silverServiceSsku.setParamValues(silverParamValues);
        silverServiceSsku.setMasterDataVersion(TEST_MD_VERSION);

        // when
        MdmEntity converted = converter.silverServiceSskuToEntity(silverServiceSsku);
        SilverServiceSsku convertedBack = converter.entityToSilverServiceSsku(converted, skuKey.getShopSku(),
            TEST_SOURCE);

        // then
        Assertions.assertThat(convertedBack).isEqualTo(silverServiceSsku);
        Assertions.assertThat(convertedBack.getKey()).isEqualTo(silverServiceSsku.getKey());
    }

    @Test
    public void shouldConvertSilverServiceSskuToBmdmEvenIfNoDatacampVersionParam() {
        // given minimal ssku
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        List<SskuSilverParamValue> silverParamValues = List.of(
            createSskuSilverParamValue(MIN_SHIPMENT, "minShipment", 6, skuKey)
        );

        SilverServiceSsku silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(skuKey, TEST_SOURCE));
        silverServiceSsku.setParamValues(silverParamValues);
        silverServiceSsku.setMasterDataVersion(TEST_MD_VERSION);

        // when
        MdmEntity converted = converter.silverServiceSskuToEntity(silverServiceSsku);
        SilverServiceSsku convertedBack = converter.entityToSilverServiceSsku(converted, skuKey.getShopSku(),
            TEST_SOURCE);

        // then
        Assertions.assertThat(convertedBack.getKey()).isEqualTo(silverServiceSsku.getKey());
        Assertions.assertThat(convertedBack.getParamValue(MIN_SHIPMENT).orElseThrow())
            .usingComparator(((o1, o2) -> o1.valueAndSourceEquals(o2) ? 0 : 1))
            .isEqualTo(silverServiceSsku.getParamValue(MIN_SHIPMENT).orElseThrow());
    }

    @Test
    public void testFullEntityToAndBackConversion() {
        // given
        Random random = new Random("EAV is the best solution".hashCode());
        MasterDataSource source = new MasterDataSource(MasterDataSourceType.MDM_OPERATOR, "vasya");
        String shopSku = "qwerty";

        MdmEntity mdmEntity =
            TestBmdmUtils.createFullRandomEntity(KnownBmdmIds.SILVER_SERVICE_SSKU_ID, random, metadataProvider)
                .map(BmdmEntityToSilverServiceSskuConverterTest::clearServiceIdMeta)
                .map(entity -> TestBmdmUtils.recursivelyUpdateExistingSourceMeta(
                    entity,
                    MdmBase.MdmSourceMeta.newBuilder()
                        .setSourceType(source.getSourceType().name())
                        .setSourceId(source.getSourceId())
                        .build()))
                .orElseThrow();

        //when
        SilverServiceSsku converted = converter.entityToSilverServiceSsku(mdmEntity, shopSku, source);
        MdmEntity convertedBack = converter.silverServiceSskuToEntity(converted);

        //then
        Assertions.assertThat(convertedBack).isEqualTo(mdmEntity);
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

    private static MdmEntity clearServiceIdMeta(MdmEntity silverService) {
        MdmEntity.Builder builder = silverService.toBuilder();
        if (silverService.containsMdmAttributeValues(KnownBmdmIds.SERVICE_SSKU_ATTR_ID)) {
            MdmAttributeValues serviceSskuFrameworkAttributeValues =
                silverService.getMdmAttributeValuesOrThrow(KnownBmdmIds.SERVICE_SSKU_ATTR_ID);
            MdmEntity serviceSskuFramework = serviceSskuFrameworkAttributeValues.getValuesList().stream()
                    .map(MdmAttributeValue::getStruct)
                    .findFirst()
                    .orElseThrow();
            MdmEntity.Builder newServiceSskuFrameworkBuilder = serviceSskuFramework.toBuilder();

            if (serviceSskuFramework.containsMdmAttributeValues(KnownBmdmIds.SERVICE_SSKU_SERVICE_ID)) {
                newServiceSskuFrameworkBuilder.putMdmAttributeValues(
                    KnownBmdmIds.SERVICE_SSKU_SERVICE_ID,
                    serviceSskuFramework.getMdmAttributeValuesOrThrow(KnownBmdmIds.SERVICE_SSKU_SERVICE_ID).toBuilder()
                        .clearMdmSourceMeta()
                        .clearMdmUpdateMeta()
                        .build()
                );
            }

            builder.putMdmAttributeValues(
                KnownBmdmIds.SERVICE_SSKU_ATTR_ID,
                serviceSskuFrameworkAttributeValues.toBuilder()
                    .clearValues()
                    .addValues(MdmAttributeValue.newBuilder().setStruct(newServiceSskuFrameworkBuilder))
                    .build()
            );
        }
        return builder.build();
    }
}
