package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
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
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverterImpl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mdm.http.MdmEntity;

import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.BOX_COUNT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DATACAMP_MASTER_DATA_VERSION;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DOCUMENT_REG_NUMBER;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GTIN;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD_COMMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.HEIGHT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.HIDE_GUARANTEE_PERIOD;
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

public class BmdmEntityToGoldenBusinessSskuConverterTest extends MdmBaseDbTestClass {

    private static final long TEST_MD_VERSION = 222L;

    @Autowired
    private MdmParamCache mdmParamCache;

    @Autowired
    private MetadataProvider metadataProvider;

    private BmdmEntityToGoldenBusinessSskuConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = createConverter();
    }

    private BmdmEntityToGoldenBusinessSskuConverter createConverter() {
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
        return new BmdmEntityToGoldenBusinessSskuConverterImpl(universalConverter, mdmParamCache);
    }

    @Test
    public void shouldConvertGoldenBusinessSskuToBmdmAndBack() {
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        List<SskuParamValue> paramValues = List.of(
            createSskuParamValue(MANUFACTURER, "manufacturer", "Яндекс", skuKey),
            createSskuParamValue(MANUFACTURER_COUNTRY, "manufacturerCountry", "Россия", skuKey),
            createSskuParamValue(USE_IN_MERCURY, "useInMercury", true, skuKey),
            createSskuParamValue(BOX_COUNT, "boxCount", 11, skuKey),
            createSskuParamValue(CUSTOMS_COMM_CODE_MDM_ID, "HScode", "0064", skuKey),
            createSskuParamValue(SHELF_LIFE, "LifeShelf", 30, skuKey),
            createSskuParamValue(SHELF_LIFE_UNIT, "ShelfLife_Unit", new MdmParamOption(2L), skuKey),
            createSskuParamValue(SHELF_LIFE_COMMENT, "ShelfLife_Comment", "Аккуратно", skuKey),
            createSskuParamValue(HIDE_SHELF_LIFE, "hide_shelf_life", false, skuKey),
            createSskuParamValue(LIFE_TIME, "ShelfService", "90", skuKey),
            createSskuParamValue(LIFE_TIME_UNIT, "ShelfService_Unit", new MdmParamOption(3L), skuKey),
            createSskuParamValue(LIFE_TIME_COMMENT, "ShelfService_Comment", "Аккуратно!!", skuKey),
            createSskuParamValue(HIDE_LIFE_TIME, "hide_life_time", false, skuKey),
            createSskuParamValue(GUARANTEE_PERIOD, "WarrantyPeriod", "45", skuKey),
            createSskuParamValue(GUARANTEE_PERIOD_UNIT, "WarrantyPeriod_Unit", new MdmParamOption(4L), skuKey),
            createSskuParamValue(GUARANTEE_PERIOD_COMMENT, "WarrantyPeriod_Comment", "Аккуратно??", skuKey),
            createSskuParamValue(HIDE_GUARANTEE_PERIOD, "hide_warranty_period", false, skuKey),
            createSskuParamValue(GTIN, "gtin", "12345678", skuKey),
            createSskuParamValue(VETIS_GUID, "vetisGuids", "guid1", skuKey),
            createSskuParamValue(LENGTH, "mdm_length", 500, skuKey),
            createSskuParamValue(WIDTH, "mdm_width", 100, skuKey),
            createSskuParamValue(HEIGHT, "mdm_height", 20, skuKey),
            createSskuParamValue(WEIGHT_GROSS, "mdm_weight_gross", 40, skuKey),
            createSskuParamValue(WEIGHT_NET, "mdm_weight_net", 30, skuKey),
            createSskuParamValue(WEIGHT_TARE, "mdm_weight_tare", 10, skuKey),
            createSskuParamValue(DOCUMENT_REG_NUMBER, "documentRegNumber", "00040728-00", skuKey),
            createSskuParamValue(DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", TEST_MD_VERSION, skuKey)
        );

        ServiceSsku goldenBusinessSsku = new ServiceSsku();
        goldenBusinessSsku.setKey(skuKey);
        goldenBusinessSsku.setParamValues(paramValues);

        MdmEntity converted = converter.goldenBusinessSskuToEntity(goldenBusinessSsku);
        ServiceSsku convertedBack = converter.entityToGoldenBusinessSsku(converted, skuKey);

        Assertions.assertThat(convertedBack).isEqualTo(goldenBusinessSsku);
    }

    private SskuParamValue createSskuParamValue(long paramId, String xslName, Object value, ShopSkuKey shopSkuKey) {
        return TestMdmParamUtils.createSskuParamValue(
            paramId,
            shopSkuKey,
            xslName,
            value);
    }
}
