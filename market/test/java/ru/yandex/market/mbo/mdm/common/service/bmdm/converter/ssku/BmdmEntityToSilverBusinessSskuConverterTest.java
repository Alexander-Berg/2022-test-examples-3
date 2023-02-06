package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
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
import ru.yandex.market.mdm.http.MdmEntity;

import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType.SUPPLIER;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue.SskuSilverTransportType.DATACAMP;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.BOX_COUNT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DATACAMP_MASTER_DATA_VERSION;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DOCUMENT_REG_NUMBER;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GTIN;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD_COMMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.HEIGHT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.HIDE_LIFE_TIME;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.HIDE_SHELF_LIFE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LENGTH;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LIFE_TIME;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LIFE_TIME_COMMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LIFE_TIME_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MANUFACTURER;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MANUFACTURER_COUNTRY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE_COMMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.USE_IN_MERCURY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.VETIS_GUID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.WEIGHT_GROSS;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.WEIGHT_NET;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.WEIGHT_TARE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.WIDTH;

public class BmdmEntityToSilverBusinessSskuConverterTest extends MdmBaseDbTestClass {

    private static final MasterDataSource TEST_SOURCE = new MasterDataSource(SUPPLIER, "supplier");
    private static final long TEST_MD_VERSION = 99L;

    @Autowired
    private MetadataProvider metadataProvider;

    @Autowired
    private MdmParamCache mdmParamCache;

    private BmdmEntityToSilverBusinessSskuConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = createConverter();
    }

    private BmdmEntityToSilverBusinessSskuConverter createConverter() {
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
        return new BmdmEntityToSilverBusinessSskuConverterImpl(universalConverter, mdmParamCache);
    }

    @Test
    public void shouldConvertSilverBusinessSskuToBmdmAndBack() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        List<SskuSilverParamValue> silverParamValues = List.of(
            createSskuSilverParamValue(MANUFACTURER, "manufacturer", "Яндекс", skuKey),
            createSskuSilverParamValue(MANUFACTURER_COUNTRY, "manufacturerCountry", "Россия", skuKey),
            createSskuSilverParamValue(USE_IN_MERCURY, "useInMercury", true, skuKey),
            createSskuSilverParamValue(BOX_COUNT, "boxCount", 11, skuKey),
            createSskuSilverParamValue(CUSTOMS_COMM_CODE_MDM_ID, "HScode", "0064", skuKey),
            createSskuSilverParamValue(SHELF_LIFE, "LifeShelf", 30, skuKey),
            createSskuSilverParamValue(SHELF_LIFE_UNIT, "ShelfLife_Unit", new MdmParamOption(2L), skuKey),
            createSskuSilverParamValue(SHELF_LIFE_COMMENT, "ShelfLife_Comment", "Аккуратно", skuKey),
            createSskuSilverParamValue(HIDE_SHELF_LIFE, "hide_shelf_life", false, skuKey),
            createSskuSilverParamValue(LIFE_TIME, "ShelfService", "90", skuKey),
            createSskuSilverParamValue(LIFE_TIME_UNIT, "ShelfService_Unit", new MdmParamOption(3L), skuKey),
            createSskuSilverParamValue(LIFE_TIME_COMMENT, "ShelfService_Comment", "Аккуратно!!", skuKey),
            createSskuSilverParamValue(HIDE_LIFE_TIME, "hide_life_time", true, skuKey),
            createSskuSilverParamValue(GUARANTEE_PERIOD, "WarrantyPeriod", "45", skuKey),
            createSskuSilverParamValue(GUARANTEE_PERIOD_UNIT, "WarrantyPeriod_Unit", new MdmParamOption(4L), skuKey),
            createSskuSilverParamValue(GUARANTEE_PERIOD_COMMENT, "WarrantyPeriod_Comment", "Аккуратно??", skuKey),
            createSskuSilverParamValue(GTIN, "gtin", "12345678", skuKey),
            createSskuSilverParamValue(VETIS_GUID, "vetisGuids", "guid1", skuKey),
            createSskuSilverParamValue(LENGTH, "mdm_length", 500, skuKey),
            createSskuSilverParamValue(WIDTH, "mdm_width", 100, skuKey),
            createSskuSilverParamValue(HEIGHT, "mdm_height", 20, skuKey),
            createSskuSilverParamValue(WEIGHT_GROSS, "mdm_weight_gross", 40, skuKey),
            createSskuSilverParamValue(WEIGHT_NET, "mdm_weight_net", 30, skuKey),
            createSskuSilverParamValue(WEIGHT_TARE, "mdm_weight_tare", 10, skuKey),
            createSskuSilverParamValue(DOCUMENT_REG_NUMBER, "documentRegNumber", "00040728-00", skuKey),
            createSskuSilverParamValue(DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", TEST_MD_VERSION, skuKey)
        );

        SilverServiceSsku silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(skuKey, TEST_SOURCE));
        silverServiceSsku.setParamValues(silverParamValues);
        silverServiceSsku.setMasterDataVersion(TEST_MD_VERSION);

        // when
        MdmEntity converted = converter.silverBusinessSskuToEntity(silverServiceSsku);
        SilverServiceSsku convertedBack = converter.entityToSilverBusinessSsku(converted, skuKey, TEST_SOURCE);

        // then
        Assertions.assertThat(convertedBack).isEqualTo(silverServiceSsku);
    }

    @Test
    public void shouldConvertSilverBusinessSskuToBmdmEvenIfNoDatacampVersionParam() {
        // given minimal ssku
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        List<SskuSilverParamValue> silverParamValues = List.of(
            createSskuSilverParamValue(MANUFACTURER, "manufacturer", "Яндекс", skuKey)
        );

        SilverServiceSsku silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(skuKey, TEST_SOURCE));
        silverServiceSsku.setParamValues(silverParamValues);
        silverServiceSsku.setMasterDataVersion(TEST_MD_VERSION);

        // when
        MdmEntity converted = converter.silverBusinessSskuToEntity(silverServiceSsku);
        SilverServiceSsku convertedBack = converter.entityToSilverBusinessSsku(converted, skuKey, TEST_SOURCE);

        // then
        Assertions.assertThat(convertedBack.getKey()).isEqualTo(silverServiceSsku.getKey());
        Assertions.assertThat(convertedBack.getParamValue(MANUFACTURER).orElseThrow())
            .usingComparator(((o1, o2) -> o1.valueAndSourceEquals(o2) ? 0 : 1))
            .isEqualTo(silverServiceSsku.getParamValue(MANUFACTURER).orElseThrow());
    }

    @Test
    public void shouldConvertSilverBusinessSskuToBmdmWithUnlimitedLifeTime() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        List<SskuSilverParamValue> silverParamValues = List.of(
            createSskuSilverParamValue(LIFE_TIME, "ShelfService", "1", skuKey), // дефолт для неограничен
            createSskuSilverParamValue(LIFE_TIME_UNIT, "ShelfService_Unit", new MdmParamOption(6L), skuKey),
            createSskuSilverParamValue(LIFE_TIME_COMMENT, "ShelfService_Comment", "Неограничено", skuKey),
            createSskuSilverParamValue(HIDE_LIFE_TIME, "hide_life_time", true, skuKey),
            createSskuSilverParamValue(DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", TEST_MD_VERSION, skuKey)
        );

        SilverServiceSsku silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(skuKey, TEST_SOURCE));
        silverServiceSsku.setParamValues(silverParamValues);
        silverServiceSsku.setMasterDataVersion(TEST_MD_VERSION);

        // when
        MdmEntity converted = converter.silverBusinessSskuToEntity(silverServiceSsku);
        SilverServiceSsku convertedBack = converter.entityToSilverBusinessSsku(converted, skuKey, TEST_SOURCE);

        // then
        Assertions.assertThat(convertedBack).isEqualTo(silverServiceSsku);
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
